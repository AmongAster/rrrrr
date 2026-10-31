// src/main/java/com/example/musicchat/source/HlsSource.java
package com.example.musicchat.source;

import com.example.musicchat.net.Http;
import com.example.musicchat.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * HLS (.m3u8) без тяжёлых зависимостей: парсит master/media плейлист, скачивает сегменты
 * (только raw audio — .mp3/.aac/.ts НЕ поддерживается без ffmpeg-демухера; для .ts нужен ffmpeg).
 * VOD-плейлисты воспроизводятся последовательно; live-плейлист — скользящее окно последних 6 сегментов.
 */
public final class HlsSource implements AudioSource {

    private static final int LIVE_WINDOW = 6;
    private static final int MAX_SEGMENTS_VOD = 2048;

    private final String playlistUrl;
    private final List<String> segments = new ArrayList<>();
    private final ArrayDeque<String> queue = new ArrayDeque<>();
    private int segIndex;
    private boolean live;
    private AudioSource current; // декодер текущего сегмента
    private long totalSamplesOut;

    public HlsSource(String url) throws IOException {
        this.playlistUrl = url;
        loadPlaylist(true);
        if (segments.isEmpty()) throw new IOException("no audio segments in m3u8");
        advanceToNextSegment();
    }

    private void loadPlaylist(boolean first) throws IOException {
        HttpURLConnection c = Http.open(playlistUrl);
        String text;
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            in.transferTo(bos);
            text = bos.toString(StandardCharsets.UTF_8);
        }
        if (!text.contains("#EXTM3U")) throw new IOException("not an m3u8 playlist");
        List<String> uris = parseMediaUris(text, playlistUrl);
        live = !text.contains("#EXT-X-ENDLIST");
        if (uris.isEmpty() && text.contains("#EXT-X-STREAM-INF")) {
            // master -> берём первое variant-адрес и перезагружаем
            String variant = resolve(uris.isEmpty() ? firstVariant(text) : uris.get(0), playlistUrl);
            HttpURLConnection vc = Http.open(variant);
            try (InputStream in = vc.getInputStream()) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                in.transferTo(bos);
                text = bos.toString(StandardCharsets.UTF_8);
            }
            uris = parseMediaUris(text, variant);
        }
        if (live) {
            // скользящее окно: только новые сегменты с момента последней загрузки
            int from = Math.max(0, uris.size() - LIVE_WINDOW);
            for (int i = from; i < uris.size(); i++) enqueueIfNew(uris.get(i));
        } else {
            segments.clear();
            for (String u : uris.subList(0, Math.min(uris.size(), MAX_SEGMENTS_VOD))) {
                if (!segments.contains(u)) segments.add(u);
            }
            segIndex = first ? 0 : segIndex;
            queue.clear();
            queue.addAll(segments.subList(Math.min(segIndex, segments.size()), segments.size()));
        }
    }

    private void enqueueIfNew(String u) {
        if (!queue.contains(u) && !(current != null && u.equals(lastUri))) queue.add(u);
    }

    private String lastUri;

    private static String firstVariant(String text) {
        for (String line : text.split("\n")) {
            String l = line.trim();
            if (!l.isEmpty() && !l.startsWith("#")) return l;
        }
        return null;
    }

    private static List<String> parseMediaUris(String text, String base) {
        List<String> out = new ArrayList<>();
        boolean nextIsUri = false;
        for (String line : text.split("\n")) {
            String l = line.trim();
            if (l.startsWith("#EXT-X-MEDIA-SEQUENCE") || l.startsWith("#EXTINF")) { nextIsUri = true; continue; }
            if (l.startsWith("#")) continue;
            if (l.isEmpty()) continue;
            if (nextIsUri || !l.contains("#")) { out.add(resolve(l, base)); nextIsUri = false; }
        }
        return out;
    }

    private static String resolve(String ref, String base) {
        try {
            if (ref.startsWith("http://") || ref.startsWith("https://")) return ref;
            return URI.create(base).resolve(ref).toString();
        } catch (Exception e) { return ref; }
    }

    private void advanceToNextSegment() throws Exception {
        closeCurrent();
        if (live) {
            if (queue.isEmpty()) loadPlaylist(false);
            if (queue.isEmpty()) { Thread.sleep(2000); return; } // live idle
        }
        String uri = queue.poll();
        if (uri == null) return; // end of VOD
        lastUri = uri;
        HttpURLConnection c = Http.open(uri);
        String ct = c.getContentType();
        if (uri.toLowerCase().endsWith(".ts") || (ct != null && ct.contains("mp2t"))) {
            if (!FfmpegSource.isAvailable()) throw new IOException("HLS MPEG-TS segments require ffmpeg (README limitations)");
            current = FfmpegSource.openPiped(fileName(uri), c.getInputStream());
        } else {
            current = DecoderFactory.openStream(uri, ct, c.getInputStream());
        }
    }

    private static String fileName(String uri) {
        int q = uri.lastIndexOf('/');
        return q >= 0 ? uri.substring(q + 1) : uri;
    }

    @Override public boolean isLive() { return live; }
    @Override public int sampleRate() { return current != null ? current.sampleRate() : 44100; }
    @Override public int channels() { return current != null ? current.channels() : 2; }

    @Override
    public int read(float[] out, int maxFrames) throws Exception {
        int attempt = 0;
        while (true) {
            if (current == null) {
                if (!live) return -1;
                advanceToNextSegment();
                if (current == null) continue;
            }
            try {
                int r = current.read(out, maxFrames);
                if (r > 0) return r;
                if (r == -1) { // segment done
                    advanceToNextSegment();
                    if (current == null && !live) return -1;
                    continue;
                }
                return 0;
            } catch (Exception e) {
                Log.warn("HLS segment error: {}", e.toString());
                if (++attempt > 3) throw e;
                advanceToNextSegment();
            }
        }
    }

    @Override public long durationMs() { return -1; }
    @Override public long positionMs() { return -1; }
    @Override public boolean canSeek() { return false; }
    @Override public boolean seekTo(long ms) { return false; }
    @Override public String displayName() { return fileName(playlistUrl); }

    private void closeCurrent() {
        try { if (current != null) current.close(); } catch (IOException ignored) { }
        current = null;
    }

    @Override public void close() { closeCurrent(); }
}
