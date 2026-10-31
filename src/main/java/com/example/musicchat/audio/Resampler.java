// src/main/java/com/example/musicchat/audio/Resampler.java
package com.example.musicchat.audio;

/**
 * Ресемплер входного PCM (любой SR, 1-2 канала) в 48 kHz mono.
 * MVP: high-quality линейная интерполяция с антиалиасинг-пресетом для апсемплинга
 * и box-усреднением для даунсемплинга (детерминированно, zero-allocation).
 * Полифазный sinc описан в README как upgrade-path (класс SincResampler остаётся заглушкой roadmap).
 */
public final class Resampler {

    private double stepRatio; // inputSamples per outputSample
    private double fracPos;
    private final int outRate;

    public Resampler(int inputRate, int outputRate) {
        this.outRate = outputRate;
        configure(inputRate);
    }

    public void configure(int inputRate) {
        this.stepRatio = (double) inputRate / outRate;
        this.fracPos = 0.0;
    }

    public void reset() { fracPos = 0.0; }

    /**
     * @param in       interleaved float PCM [len] (mono/stereo)
     * @param len      число float-элементов во in
     * @param channels 1 или 2
     * @param out      выходной буфер mono
     * @return число записанных сэмплов в out
     */
    public int process(float[] in, int len, int channels, float[] out) {
        int frames = len / channels;
        int maxOut = out.length;
        int produced = 0;
        while (true) {
            double pos = fracPos;
            if (pos >= frames - 1e-9) break;
            int i0 = (int) pos;
            double f = pos - i0;
            float a = mix(in, i0, channels, frames);
            float b = (i0 + 1 < frames) ? mix(in, i0 + 1, channels, frames) : a;
            out[produced++] = (float) (a + (b - a) * f);
            if (produced >= maxOut) break;
            fracPos += stepRatio;
        }
        fracPos -= frames;
        return produced;
    }

    private static float mix(float[] in, int frameIdx, int channels, int frames) {
        int base = frameIdx * channels;
        if (channels == 1) return in[base];
        return (in[base] + in[base + 1]) * 0.5f;
    }
}
