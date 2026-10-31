// src/main/java/com/example/musicchat/engine/PlaybackEngine.java
package com.example.musicchat.engine;

import com.example.musicchat.MusicChatMod;
import com.example.musicchat.audio.AudioSendThread;
import com.example.musicchat.audio.DecodeWorker;
import com.example.musicchat.audio.SpscRingBuffer;
import com.example.musicchat.config.ModConfig;
import com.example.musicchat.net.Http;
import com.example.musicchat.playlist.Playlist;
import com.example.musicchat.playlist.PlaylistStore;
import com.example.musicchat.playlist.Track;
import com.example.musicchat.source.AudioSource;
import com.example.musicchat.source.DecoderFactory;
import com.example.musicchat.source.FfmpegSource;
import com.example.musicchat.source.HlsSource;
import com.example.musicchat.source.HttpFileSource;
import com.example.musicchat.source.IcecastSource;
import com.example.musicchat.util.Log;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Фасад плеера: единственный владелец плейлиста, индекса трека и источников.
 *
 * <p>Модель потоков:</p>
 * <ul>
 *   <li>GUI (client thread) вызывает методы {@code enqueue*} — только кладут команды в
 *       {@link ConcurrentLinkedQueue}, никогда не блокируются;</li>
 *   <li>{@code musicchat-engine} поток исполняет команды (открывает файлы/HTTP — это
 *       блокирующие операции, поэтому НЕ на client thread);</li>
 *   <li>состояние наружу — одна volatile-ссылка {@link #snapshot} (immutable).</li>
 * </ul>
 */
public final class PlaybackEngine implements AutoCloseable {

    /** Команды GUI -> engine-потока (sealed иерархия, immutable). */
    private sealed interface Cmd permits SimpleCmd, ToggleCmd, PlayAt, SetVolume, SeekTo,
            OpenUrl, AddLocalFiles, AddFolder {}
    private enum SimpleCmd implements Cmd { PLAY_PAUSE, STOP, NEXT, PREV }
    private enum ToggleCmd implements Cmd { SHUFFLE, REPEAT }
    private record PlayAt(int index) implements Cmd {}
    private record SetVolume(float v) implements Cmd {}
    private record SeekTo(long ms) implements Cmd {}
    private record OpenUrl(String url) implements Cmd {}
    private record AddLocalFiles(List<Path> files) implements Cmd {}
    private record AddFolder(Path folder) implements Cmd {}

    public static final PlaybackEngine INSTANCE = new PlaybackEngine();

    private final SpscRingBuffer ring = new SpscRingBuffer(1 << 15); // 32768 float ~0.68 c
    public final AudioSendThread sendThread = new AudioSendThread(ring);
    public final DecodeWorker decodeWorker = new DecodeWorker(ring);

    private final ConcurrentLinkedQueue<Cmd> queue = new ConcurrentLinkedQueue<>();
    private final AtomicReference<AudioSource> liveRef = new AtomicReference<>(); // для streamTitle/position
    private volatile StateSnapshot snapshot = StateSnapshot.empty();

    // mutable state — ТОЛЬКО engine-поток
    private Playlist playlist;
    private int index = -1;
    private boolean shuffle;
    private int repeatMode; // 0 off, 1 all, 2 one
    private List<Integer> shuffleOrder;
    private int shufflePos;
    private String statusKey = "";

    private Thread engineThread;

    private PlaybackEngine() {}

    /** Вызывается один раз из SVC plugin initialize (или client init без SVC). */
    public synchronized void start() {
        if (engineThread != null) return;
        ring.clear();
        decodeWorker.onTrackEnded = this::onTrackEnded;
        decodeWorker.onStreamError = () -> setStatus("musicchat.status.stream_error");
        decodeWorker.start();
        sendThread.start();
        loadPlaylistFromConfig();
        engineThread = new Thread(this::loop, "musicchat-engine");
        engineThread.setDaemon(true);
        engineThread.start();
        publish();
        Log.info("PlaybackEngine started, playlists dir: {}", PlaylistStore.dir());
    }

    // ============================ API для GUI (thread-safe) ============================

    public StateSnapshot snapshot() { return snapshot; }

    public void playPause() { queue.add(SimpleCmd.PLAY_PAUSE); }
    public void stop()      { queue.add(SimpleCmd.STOP); }
    public void next()      { queue.add(SimpleCmd.NEXT); }
    public void prev()      { queue.add(SimpleCmd.PREV); }
    public void toggleShuffle() { queue.add(ToggleCmd.SHUFFLE); }
    public void cycleRepeat()   { queue.add(ToggleCmd.REPEAT); }
    public void setVolume(float v) { queue.add(new SetVolume(v)); }
    public void seekTo(long ms)    { queue.add(new SeekTo(ms)); }
    /** @param url проверяется на client thread (без сети); null = ok, иначе lang-ключ ошибки. */
    public String validateUrl(String url) { return Http.validate(url); }
    public void openAndPlayUrl(String url) { queue.add(new OpenUrl(url)); }
    public void addLocalFiles(List<Path> files) { queue.add(new AddLocalFiles(files)); }
    public void addFolder(Path folder)          { queue.add(new AddFolder(folder)); }
    public void playAt(int i)                   { queue.add(new PlayAt(i)); }

    public void setConnected(boolean connected) {
        // вызывается из SVC event-потоков; safe: только volatile write + publish
        StateSnapshot s = snapshot;
        snapshot = new StateSnapshot(connected, s.playing(), s.title(), s.positionMs(), s.durationMs(),
                s.seekable(), s.live(), s.streamTitle(), s.index(), s.trackCount(), s.trackTitles(),
                s.shuffle(), s.repeatMode(), s.volume(), s.balance(), s.modeOrdinal(), s.starved(),
                s.statusKey(), s.ffmpegStatus());
    }

    // ============================ engine loop ============================

    private void loop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Cmd c = queue.poll();
                if (c == null) { refreshDynamic(); Thread.sleep(100); continue; }
                execute(c);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                Log.error("engine command failed", e);
                setStatus("musicchat.status.error");
            }
        }
    }

    private void execute(Cmd c) throws IOException {
        if (c instanceof SimpleCmd sc) {
            switch (sc) {
                case PLAY_PAUSE -> {
                    if (index < 0 && !playlist.tracks.isEmpty()) { playIndex(0); return; }
                    sendThread.setPlaying(!sendThread.isPlaying());
                }
                case STOP -> { sendThread.setPlaying(false); closeCurrentSource(); index = -1; }
                case NEXT -> skip(1);
                case PREV -> skip(-1);
            }
        } else if (c instanceof PlayAt p) {
            playIndex(p.index());
        } else if (c instanceof SetVolume v) {
            ModConfig.get().musicVolume = clamp01(v.v());
            ModConfig.save();
            sendThread.setVolume(clamp01(v.v()));
        } else if (c instanceof SeekTo s) {
            AudioSource a = liveRef.get();
            if (a != null && a.canSeek()) a.seekTo(s.ms());
        } else if (c instanceof OpenUrl u) {
            openUrlAsTempTrack(u.url());
        } else if (c instanceof AddLocalFiles f) {
            for (Path p : f.files()) addTrackForFile(p);
            persistAndPublish();
        } else if (c instanceof AddFolder fo) {
            scanFolder(fo.folder());
            persistAndPublish();
        } else if (c instanceof ToggleCmd t) {
            if (t == ToggleCmd.SHUFFLE) shuffle = !shuffle;
            else repeatMode = (repeatMode + 1) % 3;
            publish();
        }
    }

    private void skip(int dir) {
        if (playlist.tracks.isEmpty()) return;
        if (repeatMode == 2) { playIndex(index); return; } // repeat one — рестарт
        int ni = pickNext(dir);
        if (ni < 0) { sendThread.setPlaying(false); return; }
        playIndex(ni);
    }

    private int pickNext(int dir) {
        int n = playlist.tracks.size();
        if (shuffle) {
            ensureShuffleOrder();
            shufflePos += dir;
            if (shufflePos >= n) {
                if (repeatMode == 0) return -1;
                shufflePos = 0; ensureShuffleOrder();
            }
            if (shufflePos < 0) shufflePos = Math.max(0, n - 1);
            return shuffleOrder.get(shufflePos);
        }
        int ni = index + dir;
        if (ni >= n) { if (repeatMode == 0) return -1; ni = 0; }
        if (ni < 0) ni = n - 1;
        return ni;
    }

    private void ensureShuffleOrder() {
        int n = playlist.tracks.size();
        if (shuffleOrder == null || shuffleOrder.size() != n) {
            List<Integer> l = new ArrayList<>();
            for (int i = 0; i < n; i++) l.add(i);
            Collections.shuffle(l);
            shuffleOrder = l;
            shufflePos = l.indexOf(Math.max(0, index));
        }
    }

    private void onTrackEnded() {
        // вызывается из decode-worker потока — только ставим команду
        queue.add(repeatMode == 2 ? new PlayAt(index) : SimpleCmd.NEXT);
    }

    private void playIndex(int i) {
        if (i < 0 || i >= playlist.tracks.size()) return;
        Track t = playlist.tracks.get(i);
        try {
            AudioSource src = openTrack(t);
            liveRef.set(src);
            decodeWorker.setSource(src);
            sendThread.onTrackSwitch();
            sendThread.setPlaying(true);
            index = i;
            if (shuffle) { ensureShuffleOrder(); shufflePos = shuffleOrder.indexOf(i); }
            setStatus(t.isStream() ? "musicchat.status.buffering" : "");
        } catch (Exception e) {
            Log.warn("open track failed: {}", e.toString());
            setStatus(errKey(e));
            publish();
        }
    }

    private void openUrlAsTempTrack(String url) {
        String err = Http.validate(url);
        if (err != null) { setStatus(err); publish(); return; }
        Track.Kind k = guessRemoteKind(url);
        Track t = new Track(k, url, shortName(url));
        playlist.add(t);
        persistAndPublish();
        playIndex(playlist.tracks.size() - 1);
    }

    private static Track.Kind guessRemoteKind(String url) {
        String u = url.toLowerCase(java.util.Locale.ROOT);
        if (u.contains(".m3u8") || u.contains("/hls")) return Track.Kind.HLS;
        if (u.contains(";stream") || u.contains(":8000") || u.contains("icecast") || u.contains("shoutcast"))
            return Track.Kind.ICECAST;
        return Track.Kind.URL;
    }

    private AudioSource openTrack(Track t) throws IOException {
        try {
            return switch (t.kind) {
                case LOCAL -> DecoderFactory.openFile(Path.of(t.ref));
                case URL -> new HttpFileSource(t.ref);
                case ICECAST -> new IcecastSource(t.ref);
                case HLS -> new HlsSource(t.ref);
            };
        } catch (IOException e) {
            throw e;
        }
    }

    private void addTrackForFile(Path p) {
        String ext = DecoderFactory.ext(p.getFileName().toString());
        if (!DecoderFactory.SUPPORTED_MVP.contains(ext)
                && !(FfmpegSource.isAvailable() && DecoderFactory.SUPPORTED_WITH_FFMPEG.contains(ext))) {
            setStatus("musicchat.status.unsupported_format");
            return;
        }
        playlist.add(new Track(Track.Kind.LOCAL, p.toAbsolutePath().toString(), null));
    }

    private void scanFolder(Path folder) {
        try (var walk = java.nio.file.Files.walk(folder)) {
            walk.filter(java.nio.file.Files::isRegularFile).forEach(this::addTrackForFile);
        } catch (IOException e) {
            setStatus("musicchat.status.error");
        }
    }

    private void closeCurrentSource() {
        liveRef.set(null);
        decodeWorker.setSource(null);
    }

    private void refreshDynamic() {
        StateSnapshot s = snapshot;
        AudioSource a = liveRef.get();
        long pos = a != null ? a.positionMs() : -1;
        String st = "";
        if (a instanceof IcecastSource ic) st = ic.streamTitle.get();
        boolean starved = sendThread.starved;
        if (pos != s.positionMs() || !st.equals(s.streamTitle()) || starved != s.starved()
                || a != null && !s.title().equals(a.displayName())) {
            publish();
        }
    }

    private void loadPlaylistFromConfig() {
        String name = ModConfig.get().activePlaylist;
        playlist = PlaylistStore.load(name);
    }

    private void persistAndPublish() {
        PlaylistStore.save(playlist);
        publish();
    }

    private void setStatus(String key) { statusKey = key == null ? "" : key; }

    private static String errKey(Exception e) {
        String m = String.valueOf(e.getMessage()).toLowerCase(java.util.Locale.ROOT);
        if (m.contains("unsupported")) return "musicchat.status.unsupported_format";
        if (m.contains("404")) return "musicchat.status.http_404";
        if (m.contains("timeout") || m.contains("connect")) return "musicchat.status.network";
        return "musicchat.status.error";
    }

    private void publish() {
        List<String> titles = new ArrayList<>();
        if (playlist != null) for (Track t : playlist.tracks) titles.add(t.title);
        AudioSource a = liveRef.get();
        String title = a != null ? a.displayName()
                : (index >= 0 && playlist != null ? playlist.tracks.get(index).title : "");
        ModConfig cfg = ModConfig.get();
        snapshot = new StateSnapshot(
                snapshot.connected(), sendThread.isPlaying(), title,
                a != null ? a.positionMs() : -1,
                a != null ? a.durationMs() : -1,
                a != null && a.canSeek(),
                a != null && a.isLive(),
                a instanceof IcecastSource ic ? ic.streamTitle.get() : "",
                index, titles.size(), List.copyOf(titles),
                shuffle, repeatMode, cfg.musicVolume, cfg.micBalance, cfg.mode.ordinal(),
                sendThread.starved, statusKey, FfmpegSource.isAvailable() ? "on" : "off");
    }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
    private static String shortName(String url) {
        int q = url.indexOf('?');
        String u = q > 0 ? url.substring(0, q) : url;
        int s = u.lastIndexOf('/');
        return s >= 0 && s < u.length() - 1 ? u.substring(s + 1) : u;
    }

    @Override
    public synchronized void close() {
        if (engineThread != null) { engineThread.interrupt(); engineThread = null; }
        decodeWorker.close();
        sendThread.close();
        Log.info("PlaybackEngine stopped (no leaked threads)");
    }
}
