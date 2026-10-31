// src/main/java/com/example/musicchat/source/AudioSource.java
package com.example.musicchat.source;

import java.io.Closeable;

/**
 * Поток PCM float (interleaved) с известным sample rate/каналами.
 * Реализации: локальный файл, HTTP-файл, Icecast, HLS. Все блокирующие операции — только
 * внутри read(), которая вызывается исключительно из decode-worker потока.
 */
public interface AudioSource extends Closeable {

    /** @return true, если источник исчерпан (EOF файла / конец плейлиста сегментов). Для live-стримов всегда false. */
    boolean isLive();

    int sampleRate();

    int channels();

    /**
     * Читает до {@code maxFrames} аудиокадров в {@code out} (interleaved float -1..1).
     * @return число КАДРОВ (не элементов), или -1 при ошибке. 0 = временно нет данных (для live).
     */
    int read(float[] out, int maxFrames) throws Exception;

    /** Длительность в мс, или -1 если неизвестна (стримы). */
    long durationMs();

    /** Позиция воспроизведения в мс, или -1 если seek недоступен. */
    long positionMs();

    /** Поддержка seek (для стрима false). */
    default boolean canSeek() { return true; }

    /** @return true если seek выполнен */
    boolean seekTo(long ms);

    /** Человекочитаемое имя источника для GUI. */
    String displayName();
}
