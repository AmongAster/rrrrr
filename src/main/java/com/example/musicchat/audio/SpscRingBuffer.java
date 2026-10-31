// src/main/java/com/example/musicchat/audio/SpscRingBuffer.java
package com.example.musicchat.audio;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Lock-free single-producer / single-consumer буфер PCM float (48 kHz mono).
 * Производитель — decode-worker, потребитель — audio clock. Ёмкость — степень двойки.
 */
public final class SpscRingBuffer {
    private final float[] data;
    private final int mask;
    private final AtomicLong writePos = new AtomicLong();
    private final AtomicLong readPos = new AtomicLong();

    public SpscRingBuffer(int capacityPow2) {
        if (capacityPow2 <= 0 || Integer.bitCount(capacityPow2) != 1) {
            throw new IllegalArgumentException("capacity must be power of two");
        }
        this.data = new float[capacityPow2];
        this.mask = capacityPow2 - 1;
    }

    /** Запись производителем. Возвращает количество реально записанных сэмплов (0 если full). */
    public int write(float[] buf, int off, int want) {
        long w = writePos.get();
        long r = readPos.getAcquire();
        int free = data.length - (int) (w - r);
        int n = Math.min(want, free);
        for (int i = 0; i < n; i++) data[(int) ((w + i) & mask)] = buf[off + i];
        writePos.lazySet(w + n);
        return n;
    }

    /** Чтение потребителем: true если удалось прочитать ровно {@code want} сэмплов. */
    public boolean read(float[] dst, int want) {
        long r = readPos.get();
        long w = writePos.getAcquire();
        if (w - r < want) return false;
        for (int i = 0; i < want; i++) dst[i] = data[(int) ((r + i) & mask)];
        readPos.lazySet(r + want);
        return true;
    }

    public int available() { return (int) (writePos.getAcquire() - readPos.get()); }
    public int capacity() { return data.length; }

    /** Сброс (только когда производитель остановлен или готов к дропу). */
    public void clear() {
        long w = writePos.get();
        readPos.set(w);
    }
}
