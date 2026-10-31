// src/main/java/com/example/musicchat/audio/DecodeWorker.java
package com.example.musicchat.audio;

import com.example.musicchat.MusicChatMod;
import com.example.musicchat.source.AudioSource;
import com.example.musicchat.util.Log;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Единственный поток-производитель RingBuffer. Владение текущим {@link AudioSource} — эксклюзивное:
 * source.close() вызывается ТОЛЬКО этим потоком (или после join при shutdown).
 * Команда смены источника передаётся через AtomicReference (GUI пишет, worker забирает).
 */
public final class DecodeWorker implements AutoCloseable {

    public enum EndBehavior { STOP_AT_END, NEXT_TRACK }

    private final SpscRingBuffer ring;
    private final Resampler resampler = new Resampler(MusicChatMod.SAMPLE_RATE, MusicChatMod.SAMPLE_RATE);
    private final Thread thread;
    private volatile boolean running = true;
    /** GUI выставляет новый источник сюда; worker подхватывает и закрывает старый. */
    public final AtomicReference<AudioSource> pendingSource = new AtomicReference<>();
    /** Вызывается из worker-потока при EOF текущего источника. */
    public volatile Runnable onTrackEnded;
    public volatile Runnable onStreamError;
    public volatile EndBehavior endBehavior = EndBehavior.NEXT_TRACK;
    private volatile AudioSource current;

    private final float[] inBuf = new float[4096 * 2];   // вход декодера (interleaved)
    private final float[] outBuf = new float[8192];      // после ресемплинга mono

    public DecodeWorker(SpscRingBuffer ring) {
        this.ring = ring;
        this.thread = new Thread(this::loop, "musicchat-decode");
        this.thread.setDaemon(true);
    }

    public void start() { if (!thread.isAlive()) thread.start(); }

    /** Установка нового источника без блокировок: старый будет закрыт воркером. */
    public void setSource(AudioSource next) {
        var old = pendingSource.getAndSet(next);
        if (old != null) closeQuiet(old); // если предыдущий ещё не подхвачен
    }

    private void loop() {
        while (running) {
            try {
                swapIfNeeded();
                if (current == null) { sleep(20); continue; }
                int ch = Math.max(current.channels(), 1);
                int got = current.read(inBuf, inBuf.length / ch);
                if (got < 0) {
                    handleEnd();
                    continue;
                }
                if (got == 0) { sleep(5); continue; }
                int produced = resample(got, ch);
                writeWithBackpressure(produced);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                Log.warn("decode error: {}", e.toString());
                closeCurrent();
                fire(onStreamError);
            }
        }
        closeCurrent();
    }

    private void swapIfNeeded() {
        AudioSource next = pendingSource.getAndSet(null);
        if (next == null) return;
        closeCurrent();
        current = next;
        resampler.configure(Math.max(next.sampleRate(), 8000));
        resampler.reset();
    }

    private int resample(int frames, int channels) {
        int sr = current.sampleRate();
        if (sr == MusicChatMod.SAMPLE_RATE && channels == 1) {
            System.arraycopy(inBuf, 0, outBuf, 0, frames);
            return frames;
        }
        return resampler.process(inBuf, frames * channels, channels, outBuf);
    }

    /** Если буфер переполнен — дропаем новые данные (producer wait-free), с редким предупреждением. */
    private long lastDropWarn;
    private void writeWithBackpressure(int n) throws InterruptedException {
        int off = 0;
        while (off < n && running) {
            int w = ring.write(outBuf, off, n - off);
            off += w;
            if (off < n) {
                long now = System.currentTimeMillis();
                if (now - lastDropWarn > 5000) {
                    lastDropWarn = now;
                    Log.debug("ring full, throttling decoder");
                }
                sleep(5);
            }
        }
    }

    private void handleEnd() throws InterruptedException {
        closeCurrent();
        if (endBehavior == EndBehavior.STOP_AT_END) { fire(onTrackEnded); return; }
        fire(onTrackEnded);
        // ждём пока GUI поставит следующий трек
        int waited = 0;
        while (running && current == null && pendingSource.get() == null && waited < 30_000) {
            sleep(50); waited += 50;
        }
    }

    private static void fire(Runnable r) { if (r != null) { try { r.run(); } catch (Exception ignored) { } } }

    private void closeCurrent() {
        AudioSource c = current;
        current = null;
        if (c != null) closeQuiet(c);
    }

    private static void closeQuiet(AudioSource s) {
        try { s.close(); } catch (IOException ignored) { }
    }

    private static void sleep(long ms) throws InterruptedException { Thread.sleep(ms); }

    @Override
    public void close() {
        running = false;
        thread.interrupt();
        try { thread.join(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        closeCurrent();
        AudioSource p = pendingSource.getAndSet(null);
        if (p != null) closeQuiet(p);
    }
}
