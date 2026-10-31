// src/main/java/com/example/musicchat/audio/FadeController.java
package com.example.musicchat.audio;

/**
 * Простой fade-in/out поверх громкости. Поток-потребитель вызывает nextGain() раз в кадр.
 * Команды fadeIn/fadeOut могут приходить из GUI — метод синхронизирован (редко, вне горячего пути).
 */
public final class FadeController {
    private enum State { NONE, IN, OUT }

    private volatile State state = State.NONE;
    private int remainingFrames;
    private int totalFrames = 1;
    private boolean finishedOut;

    public void fadeIn(int ms, int frameMs) { start(State.IN, ms, frameMs); }
    public void fadeOut(int ms, int frameMs) { start(State.OUT, ms, frameMs); }

    private synchronized void start(State s, int ms, int frameMs) {
        if (ms <= 0) { state = State.NONE; finishedOut = (s == State.OUT); return; }
        state = s;
        totalFrames = Math.max(1, ms / frameMs);
        remainingFrames = totalFrames;
        finishedOut = false;
    }

    /** Вызывается ТОЛЬКО из audio-clock потока, один раз на кадр. */
    public float nextGain() {
        State s = state;
        switch (s) {
            case IN:
                if (remainingFrames > 0) return 1f - (--remainingFrames / (float) totalFrames);
                state = State.NONE;
                return 1f;
            case OUT:
                if (remainingFrames > 0) {
                    float g = --remainingFrames / (float) totalFrames;
                    if (remainingFrames == 0) { finishedOut = true; state = State.NONE; }
                    return g;
                }
                state = State.NONE;
                return 0f;
            default:
                return finishedOut ? 0f : 1f;
        }
    }

    public synchronized boolean isSilentAfterFadeOut() { return finishedOut && state == State.NONE; }

    public synchronized void forceFullVolume() { state = State.NONE; finishedOut = false; }
}
