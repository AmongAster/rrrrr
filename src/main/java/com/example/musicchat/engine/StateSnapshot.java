// src/main/java/com/example/musicchat/engine/StateSnapshot.java
package com.example.musicchat.engine;

import java.util.List;

/**
 * Immutable-снимок состояния плеера для GUI. Публикуется одной volatile-ссылкой,
 * поэтому UI никогда не видит «наполовину обновлённое» состояние и никогда не блокируется
 * аудио-логикой (модель потоков: writer — engine/audio threads, reader — client thread).
 */
public record StateSnapshot(
        boolean connected,          // SVC voice-соединение активно
        boolean playing,            // часы публикуют кадры
        String title,               // now playing (или "")
        long positionMs,            // позиция трека (-1 для live)
        long durationMs,            // длительность (-1 неизвестно/стрим)
        boolean seekable,           // активен seek-слайдер
        boolean live,               // live-стрим (ICY/HLS)
        String streamTitle,         // ICY StreamTitle или ""
        int index,                  // индекс активного трека в плейлисте (-1 нет)
        int trackCount,             // размер плейлиста
        List<String> trackTitles,   // snapshot названий (для списка)
        boolean shuffle,
        int repeatMode,             // 0 off / 1 all / 2 one
        float volume,               // 0..1
        float balance,              // 0..1 mic-баланс MIX
        int modeOrdinal,            // ModConfig.Mode.ordinal()
        boolean starved,            // буфер пустовал в последний кадр (индикатор)
        String statusKey,           // lang-ключ статуса/ошибки или ""
        String ffmpegStatus         // "on"/"off" — доступность ffmpeg
) {
    public static StateSnapshot empty() {
        return new StateSnapshot(false, false, "", -1, -1, false, false, "", -1, 0,
                List.of(), false, 0, 0.8f, 0.5f, 1, false, "", "off");
    }
}
