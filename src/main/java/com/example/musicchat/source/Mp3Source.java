// src/main/java/com/example/musicchat/source/Mp3Source.java
package com.example.musicchat.source;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * MP3-декодер на JLayer (relocated пакет com.example.musicchat.libs.jlayer.* в релизном jar).
 * Работает поверх любого {@link StreamedAudioSource}: файл или live-поток.
 * Sample rate/каналы определяются по первому кадру (потоково, без probe-файла).
 */
public final class Mp3Source implements AudioSource {

    private static final int READ_CHUNK_FRAMES = 1152; // max samples per mp3 frame per channel

    private final String name;
    private final InputStream raw;
    private final Bitstream bitstream;
    private final Decoder decoder;
    private final float[] scratch = new float[READ_CHUNK_FRAMES * 2];
    private int sampleRate = -1;
    private int channels = -1;
    private long samplesConsumed;
    private volatile boolean closed;

    public Mp3Source(String displayName, InputStream in) {
        this.name = displayName;
        this.raw = in instanceof BufferedInputStream ? in : new BufferedInputStream(in, 1 << 16);
        this.bitstream = new Bitstream(raw);
        this.decoder = new Decoder();
    }

    @Override public boolean isLive() { return false; }

    @Override
    public synchronized int sampleRate() {
        if (sampleRate < 0) tryPrimeHeader();
        return Math.max(sampleRate, 44100);
    }

    @Override
    public synchronized int channels() {
        if (channels < 0) tryPrimeHeader();
        return Math.max(channels, 2);
    }

    private void tryPrimeHeader() {
        try {
            Header h = bitstream.readFrame();
            if (h != null) {
                sampleRate = h.sample_frequency;
                channels = h.mode == 3 ? 1 : 2;
                bitstream.closeFrame();
            }
        } catch (Exception ignored) { /* попробуем ещё раз при read */ }
    }

    @Override
    public synchronized int read(float[] out, int maxFrames) throws Exception {
        if (closed) return -1;
        int framesDone = 0;
        while (framesDone < maxFrames) {
            Header h = bitstream.readFrame();
            if (h == null) {
                bitstream.close();
                closed = true;
                return framesDone > 0 ? framesDone : -1;
            }
            if (sampleRate < 0) { sampleRate = h.sample_frequency; channels = h.mode == 3 ? 1 : 2; }
            SampleBuffer sb = (SampleBuffer) decoder.decodeFrame(h, bitstream);
            bitstream.closeFrame();
            int n = sb.getElementCount();          // samples total (interleaved)
            int ch = sb.getChannelCount();
            short[] buf = sb.getBuffer();
            for (int i = 0; i < n && framesDone < maxFrames; ) {
                for (int c = 0; c < ch && i < n && framesDone < maxFrames; c++, i++) {
                    out[framesDone * ch + c] = buf[i] / 32768f;
                }
                if (ch == 1) { /* mono stays mono */ }
                framesDone++;
            }
            samplesConsumed += n / Math.max(ch, 1);
        }
        return framesDone;
    }

    @Override public long durationMs() { return -1; } // MP3 без Xing/CBR-расчёта — неизвестно (в GUI "—")
    @Override public long positionMs() {
        int sr = sampleRate();
        return sr > 0 ? samplesConsumed * 1000L / sr : -1;
    }
    @Override public boolean canSeek() { return false; }
    @Override public boolean seekTo(long ms) { return false; }
    @Override public String displayName() { return name; }

    @Override
    public synchronized void close() throws IOException {
        closed = true;
        try { bitstream.close(); } catch (Exception ignored) { }
        raw.close();
    }
}
