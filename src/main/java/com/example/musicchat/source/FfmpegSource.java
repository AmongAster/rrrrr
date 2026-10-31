// src/main/java/com/example/musicchat/source/FfmpegSource.java
package com.example.musicchat.source;

import com.example.musicchat.config.ModConfig;
import com.example.musicchat.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Опциональный декодер через ВНЕШНИЙ процесс ffmpeg (stdout: s16le PCM mono 48k).
 * Не линкуется с модом (LGPL/GPL-safe: отдельный бинарь пользователя).
 * Доступен только если ffmpeg найден в PATH или указан ffmpegPath в конфиге.
 */
public final class FfmpegSource implements AudioSource {

    private static volatile Boolean availableCache;

    private final String name;
    private final Process process;
    private final InputStream pcm;
    private final AtomicBoolean closed = new AtomicBoolean();
    private long framesConsumed;

    /** Запускает ffmpeg: stdin=encoded data -> stdout=s16le 48000 mono. */
    public static FfmpegSource openPiped(String displayName, InputStream encoded) throws IOException {
        Process p = start("-i", "pipe:0", "-vn", "-f", "s16le", "-acodec", "pcm_s16le",
                "-ar", "48000", "-ac", "1", "pipe:1");
        Thread pump = new Thread(() -> {
            try (var os = p.getOutputStream(); InputStream is = encoded) {
                is.transferTo(os);
            } catch (IOException ignored) { }
        }, "musicchat-ffmpeg-stdin");
        pump.setDaemon(true);
        pump.start();
        return new FfmpegSource(displayName, p, new BufferedInputStream(p.getInputStream(), 1 << 16));
    }

    private static Process start(String... args) throws IOException {
        String exe = resolveFfmpeg();
        if (exe == null) throw new IOException("ffmpeg is not available");
        String[] cmd = new String[args.length + 3];
        cmd[0] = exe; cmd[1] = "-loglevel"; cmd[2] = "error";
        System.arraycopy(args, 0, cmd, 3, args.length);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(false);
        return pb.start();
    }

    private FfmpegSource(String name, Process p, InputStream pcm) {
        this.name = name; this.process = p; this.pcm = pcm;
    }

    public static boolean isAvailable() { return resolveFfmpeg() != null; }

    private static String resolveFfmpeg() {
        Boolean c = availableCache;
        if (c != null) return c ? lastExe : null;
        String cfg = ModConfig.get().ffmpegPath;
        String[] candidates = cfg != null && !cfg.isBlank()
                ? new String[]{cfg}
                : new String[]{"ffmpeg", "/usr/bin/ffmpeg", "C:\\ffmpeg\\bin\\ffmpeg.exe"};
        for (String cand : candidates) {
            try {
                Process p = new ProcessBuilder(cand, "-version").redirectErrorStream(true).start();
                boolean done = p.waitFor(5, TimeUnit.SECONDS);
                if (done && p.exitValue() == 0) {
                    lastExe = cand; availableCache = true;
                    Log.info("ffmpeg found: {}", cand);
                    return cand;
                }
            } catch (Exception ignored) { }
        }
        availableCache = false;
        return null;
    }

    private static String lastExe;

    @Override public boolean isLive() { return false; }
    @Override public int sampleRate() { return 48_000; }
    @Override public int channels() { return 1; }

    @Override
    public int read(float[] out, int maxFrames) throws IOException {
        if (closed.get()) return -1;
        int needBytes = maxFrames * 2; // s16le mono
        byte[] buf = new byte[needBytes];
        int off = 0;
        while (off < needBytes) {
            int r = pcm.read(buf, off, needBytes - off);
            if (r < 0) break;
            off += r;
        }
        if (off == 0) { close(); return -1; }
        ByteBuffer bb = ByteBuffer.wrap(buf, 0, off).order(ByteOrder.LITTLE_ENDIAN);
        int frames = off / 2;
        for (int i = 0; i < frames; i++) out[i] = bb.getShort() / 32768f;
        framesConsumed += frames;
        return frames;
    }

    @Override public long durationMs() { return -1; }
    @Override public long positionMs() { return framesConsumed * 1000L / 48_000; }
    @Override public boolean canSeek() { return false; }
    @Override public boolean seekTo(long ms) { return false; }
    @Override public String displayName() { return name; }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        try { pcm.close(); } catch (IOException ignored) { }
        process.destroy();
        try {
            if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly();
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /** Диагностика наличия ffmpeg для GUI. */
    public static String statusText() {
        String exe = resolveFfmpeg();
        return exe == null ? "not found" : exe;
    }

    /** Read small stream fully for header sniffing (used by Http sources). */
    public static byte[] readN(InputStream is, int n) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(n);
        byte[] b = new byte[Math.min(n, 8192)];
        int got = 0;
        while (got < n) {
            int r = is.read(b, 0, Math.min(b.length, n - got));
            if (r < 0) break;
            bos.write(b, 0, r);
            got += r;
        }
        return bos.toByteArray();
    }

    public static String utf8(byte[] b) { return new String(b, StandardCharsets.UTF_8); }


}
