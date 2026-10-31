// src/main/java/com/example/musicchat/source/IcecastSource.java
package com.example.musicchat.source;

import com.example.musicchat.net.Http;
import com.example.musicchat.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Icecast/Shoutcast live-стрим. Особенности реализации:
 *  - заголовок "icy-metaint": каждые N байт идёт 1-байтная длина метаданных (*16) + блок StreamTitle;
 *  - Shoutcast v2 может вернуть HTTP 200 с телом icy200 вместо аудио — проверяется sniff'ом;
 *  - при обрыве — автоматический reconnect с backoff (1..8 с), статус публикуется в {@link #statusText}.
 * Декодер выбирается по Content-Type (MP3 — основной live-формат; прочее через ffmpeg если есть).
 */
public final class IcecastSource implements AudioSource {

    private static final int RECONNECT_BASE_MS = 1_000;
    private static final int RECONNECT_MAX_MS = 8_000;
    private static final int SNIFF_LEN = 16;

    private final String url;
    public final AtomicReference<String> streamTitle = new AtomicReference<>("");
    public final AtomicReference<String> statusText = new AtomicReference<>("musicchat.status.connecting");

    private volatile boolean closedByUser;
    private Connection conn; // per-attempt, только decode-worker тред обращается

    public IcecastSource(String url) throws IOException {
        this.url = url;
        this.conn = connect(); // первый коннект синхронно — чтобы errors всплыли сразу
    }

    private static final class Connection {
        InputStream audio;   // уже очищенный от метаданных поток
        Mp3Source mp3;       // либо mp3-декодер поверх audio
        int sampleRate = 44100, channels = 2;
        long bytesDecodedSamples;
    }

    private Connection connect() throws IOException {
        HttpURLConnection c = Http.open(url);
        String ct = c.getContentType() == null ? "" : c.getContentType().toLowerCase();
        int metaint = parseIntSafe(c.getHeaderField("icy-metaint"), 0);
        InputStream raw = new java.io.BufferedInputStream(c.getInputStream(), 1 << 16);
        byte[] sniff = FfmpegSource.readN(raw, SNIFF_LEN);
        if (new String(sniff, 0, Math.min(sniff.length, 5), StandardCharsets.US_ASCII).startsWith("icy20")) {
            throw new IOException("Shoutcast returned icy200 (source is offline or max listeners reached)");
        }
        InputStream body = new SniffedInputStream(raw, sniff);
        InputStream metaless = metaint > 0 ? new IcyMetaStripper(body, metaint, streamTitle) : body;

        Connection cn = new Connection();
        if ((ct.contains("ogg") || ct.contains("opus")) && !ct.contains("mpeg")) {
            // OGG live требует полноценного декодера — только через ffmpeg (честное ограничение MVP)
            cn.mp3 = null;
            cn.audio = null;
            throw new IOException("OGG/Opus live streams need ffmpeg (see README limitations)");
        }
        cn.mp3 = new Mp3Source(shortName(), metaless);
        return cn;
    }

    @Override public boolean isLive() { return true; }
    @Override public int sampleRate() { return conn.mp3 != null ? conn.mp3.sampleRate() : 44100; }
    @Override public int channels() { return conn.mp3 != null ? conn.mp3.channels() : 2; }

    @Override
    public int read(float[] out, int maxFrames) throws Exception {
        int attempt = 0;
        while (!closedByUser) {
            try {
                int r = conn.mp3.read(out, maxFrames);
                if (r != -1) { statusText.set("musicchat.status.live"); return r; }
                throw new IOException("stream ended");
            } catch (Exception e) {
                closeQuiet(conn);
                attempt++;
                int backoff = Math.min(RECONNECT_BASE_MS << (attempt - 1), RECONNECT_MAX_MS);
                statusText.set("musicchat.status.reconnecting");
                Log.warn("Icecast reconnect in {}ms ({}): {}", backoff, shortName(), e.toString());
                Thread.sleep(backoff);
                conn = connect();
                attempt = Math.min(attempt, 4);
            }
        }
        return -1;
    }

    @Override public long durationMs() { return -1; }
    @Override public long positionMs() { return -1; }
    @Override public boolean canSeek() { return false; }
    @Override public boolean seekTo(long ms) { return false; }
    @Override public String displayName() { String t = streamTitle.get(); return t.isBlank() ? shortName() : t; }

    @Override
    public void close() {
        closedByUser = true;
        closeQuiet(conn);
    }

    private void closeQuiet(Connection c) {
        try { if (c != null && c.mp3 != null) c.mp3.close(); } catch (IOException ignored) { }
    }

    private String shortName() {
        int q = url.indexOf("//");
        String s = url.substring(q < 0 ? 0 : q + 2);
        return s.length() > 50 ? s.substring(0, 50) + "…" : s;
    }

    private static int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    /** Восстанавливает «съеденный» sniff-префикс. */
    static final class SniffedInputStream extends InputStream {
        private final InputStream in; private final byte[] prefix; private int p;
        SniffedInputStream(InputStream in, byte[] prefix) { this.in = in; this.prefix = prefix; }
        @Override public int read() throws IOException {
            if (p < prefix.length) return prefix[p++] & 0xFF;
            return in.read();
        }
        @Override public int read(byte[] b, int off, int n) throws IOException {
            if (p < prefix.length) {
                int c = Math.min(n, prefix.length - p);
                System.arraycopy(prefix, p, b, off, c);
                p += c; return c;
            }
            return in.read(b, off, n);
        }
        @Override public void close() throws IOException { in.close(); }
    }

    /** Удаляет ICY-метаданные из потока и обновляет streamTitle. */
    static final class IcyMetaStripper extends InputStream {
        private final InputStream in;
        private final int metaint;
        private int untilMeta = metaint;
        private final AtomicReference<String> titleRef;

        IcyMetaStripper(InputStream in, int metaint, AtomicReference<String> titleRef) {
            this.in = in; this.metaint = metaint; this.titleRef = titleRef;
        }

        @Override public int read() throws IOException {
            if (untilMeta == 0) { readMetaBlock(); }
            int b = in.read();
            if (b >= 0) untilMeta--;
            return b;
        }

        @Override public int read(byte[] b, int off, int n) throws IOException {
            if (untilMeta == 0) readMetaBlock();
            int r = in.read(b, off, Math.min(n, untilMeta));
            if (r > 0) untilMeta -= r;
            return r;
        }

        private void readMetaBlock() throws IOException {
            int len = in.read();
            if (len < 0) { untilMeta = metaint; return; }
            int bytes = len * 16;
            if (bytes > 0) {
                byte[] buf = new byte[bytes];
                int got = 0;
                while (got < bytes) { int r = in.read(buf, got, bytes - got); if (r < 0) break; got += r; }
                String meta = new String(buf, 0, got, StandardCharsets.ISO_8859_1);
                String st = extract(meta, "StreamTitle='", "';");
                if (st == null) st = extract(meta, "StreamTitle=\"", "\";");
                if (st != null && !st.isBlank()) titleRef.set(st.trim());
            }
            untilMeta = metaint;
        }

        private static String extract(String s, String open, String close) {
            int a = s.indexOf(open); if (a < 0) return null;
            a += open.length();
            int b = s.indexOf(close, a); if (b < 0) b = s.indexOf("'", a);
            if (b < 0) b = s.length();
            return s.substring(a, b);
        }

        @Override public void close() throws IOException { in.close(); }
    }
}
