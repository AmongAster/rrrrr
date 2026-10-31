// src/main/java/com/example/musicchat/audio/AudioSendThread.java
package com.example.musicchat.audio;

import com.example.musicchat.MusicChatMod;
import com.example.musicchat.config.ModConfig;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Аудио-«часы» мода: строго периодический (20 мс) pull из {@link SpscRingBuffer},
 * fade -> volume -> soft-limit -> float->short, публикация готового mono-фрейма
 * через {@link #currentFrame} для потоков SVC (MixMixin / DirectSender).
 *
 * <p>Никаких блокирующих операций и аллокаций в горячем цикле (кроме редкого clone()
 * публикации — 1.9 КБ/кадр, acceptable; zero-copy вариант в roadmap).</p>
 */
public final class AudioSendThread implements AutoCloseable {

    /** Последний готовый PCM-фрейм (48k mono short[960]); null = тишина/нет данных. */
    public final AtomicReference<short[]> currentFrame = new AtomicReference<>();
    /** Счётчик опубликованных фреймов (для диагностики рассинхрона). */
    public final AtomicLong framesPublished = new AtomicLong();
    /** true если за последний кадр буфер был пуст (starve) — дёргается GUI как индикатор. */
    public volatile boolean starved;

    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "musicchat-audio-clock");
        t.setDaemon(true);
        t.setPriority(Thread.MAX_PRIORITY);
        return t;
    });
    private final SpscRingBuffer ring;
    private final FadeController fade = new FadeController();
    private final SoftLimiter limiter = new SoftLimiter();
    private final float[] frameF = new float[MusicChatMod.FRAME_SAMPLES];
    private final short[] frameS = new short[MusicChatMod.FRAME_SAMPLES];
    private volatile boolean playing;
    private ScheduledFuture<?> task;

    public AudioSendThread(SpscRingBuffer ring) { this.ring = ring; }

    public synchronized void start() {
        if (task != null) return;
        task = exec.scheduleAtFixedRate(this::tick,
                MusicChatMod.FRAME_DURATION_MS, MusicChatMod.FRAME_DURATION_MS, TimeUnit.MILLISECONDS);
    }

    private void tick() {
        try {
            ModConfig.AudioMirrorSnapshot cfg = ModConfig.AudioMirror.get();
            if (!playing || cfg.mode() == ModConfig.Mode.MIC_ONLY || cfg.musicGain() <= 0f) {
                currentFrame.set(null);
                return;
            }
            if (!ring.read(frameF, MusicChatMod.FRAME_SAMPLES)) {
                // starve: comfort silence вместо щелчка; producer догонит сам (drift handled by write-drop)
                java.util.Arrays.fill(frameF, 0f);
                starved = true;
            } else {
                starved = false;
            }
            float g = fade.nextGain() * cfg.musicGain();
            for (int i = 0; i < MusicChatMod.FRAME_SAMPLES; i++) frameF[i] *= g;
            limiter.apply(frameF, MusicChatMod.FRAME_SAMPLES);
            for (int i = 0; i < MusicChatMod.FRAME_SAMPLES; i++) {
                float v = Math.max(-1f, Math.min(1f, frameF[i]));
                frameS[i] = (short) (v * Short.MAX_VALUE);
            }
            currentFrame.set(frameS.clone());
            framesPublished.incrementAndGet();
        } catch (Throwable t) {
            // timer должен жить любой ценой
        }
    }

    public void setPlaying(boolean p) {
        if (p == playing) return;
        int fm = ModConfig.get().fadeMs;
        if (p) { fade.forceFullVolume(); fade.fadeIn(fm, MusicChatMod.FRAME_DURATION_MS); }
        else fade.fadeOut(fm, MusicChatMod.FRAME_DURATION_MS);
        playing = p;
    }

    public boolean isPlaying() { return playing; }

    /** Вызывается при переключении трека: дроп старого контента с фейдом. */
    public void onTrackSwitch() {
        int fm = ModConfig.get().fadeMs;
        fade.fadeOut(Math.min(fm, 60), MusicChatMod.FRAME_DURATION_MS);
        ring.clear();
        fade.fadeIn(Math.min(fm, 60), MusicChatMod.FRAME_DURATION_MS);
    }

    @Override
    public synchronized void close() {
        if (task != null) task.cancel(false);
        exec.shutdownNow();
        currentFrame.set(null);
        playing = false;
    }
}
