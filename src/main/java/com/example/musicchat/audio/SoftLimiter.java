// src/main/java/com/example/musicchat/audio/SoftLimiter.java
package com.example.musicchat.audio;

/** Тангенциальный soft-clipper: y = tanh(K*x)/tanh(K), применяется только к клиппующим сэмплам. */
public final class SoftLimiter {
    private static final double K = 1.5;
    private static final float NORM = (float) Math.tanh(K);

    public void apply(float[] buf, int len) {
        for (int i = 0; i < len; i++) {
            float v = buf[i];
            if (v > 1f || v < -1f) {
                buf[i] = (float) (Math.tanh(K * v) / NORM);
            }
        }
    }
}
