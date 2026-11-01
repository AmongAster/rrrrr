// src/main/java/com/example/musicchat/MusicChatMod.java
package com.example.musicchat;

/**
 * Общие константы мода MusicVChat (modid {@value #MODID}).
 *
 * <p>Класс не содержит логики — только "магические числа", вынесенные в константы
 * согласно требованиям к качеству: единая точка правки формата аудио.</p>
 */
public final class MusicChatMod {

    /** Mod-идентификатор, используется в путях config, lang-ключах и fabric.mod.json. */
    public static final String MODID = "musicchat";

    // ---- Формат PCM, обязательный для Simple Voice Chat ----
    /** Частота дискретизации SVC (Гц). Проверено по исходникам SVC 2.6.x (Utils.SAMPLE_RATE). */
    public static final int SAMPLE_RATE = 48_000;
    /** Длительность одного пакета SVC в миллисекундах. */
    public static final int FRAME_DURATION_MS = 20;
    /** Количество сэмплов в одном фрейме: 48000 Гц * 20 мс = 960. */
    public static final int FRAME_SAMPLES = SAMPLE_RATE * FRAME_DURATION_MS / 1000;
    /** Максимальный размер Opus-пакета в байтах (нативный лимит opus). */
    public static final int MAX_OPUS_PACKET_BYTES = 512;
    /** Битрейт музыкального энкодера (бит/с). Компромисс качество/CPU для VoIP-канала. */
    public static final int MUSIC_BITRATE = 96_000;

    /** Идентификатор собственной Volume Category. */
    public static final String CATEGORY_ID = MODID + ".music";

    /** User-Agent для HTTP-запросов (некоторые Icecast-серверы его требуют). */
    public static final String USER_AGENT = "MusicVChat/0.1.0 (+SimpleVoiceChat addon)";

    private MusicChatMod() {
    }
}
