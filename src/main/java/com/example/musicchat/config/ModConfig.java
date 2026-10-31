// src/main/java/com/example/musicchat/config/ModConfig.java
package com.example.musicchat.config;

import com.example.musicchat.MusicChatMod;
import com.example.musicchat.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Конфиг мода: {@code config/musicchat/config.json}.
 * Атомарная запись (tmp + move), миграция схемы, потокобезопасный снимок для аудио-потока.
 */
public final class ModConfig {

    /** Текущая версия схемы конфига. При несовпадении — миграция значений по умолчанию. */
    public static final int SCHEMA_VERSION = 2;

    public enum Mode { MUSIC_ONLY, MIX, MIC_ONLY }

    // ---- Serializable fields ----
    public int schemaVersion = SCHEMA_VERSION;
    public Mode mode = Mode.MIX;
    /** Громкость музыки 0..1 (домножается на Volume Category SVC). */
    public float musicVolume = 0.8f;
    /** Баланс микса: 0 = только микрофон, 1 = только музыка. Только для MIX. */
    public float micBalance = 0.5f;
    /** Паузировать музыку при mute/PTT-выключении микрофона пользователя. */
    public boolean pauseMusicOnMute = false;
    /** Автовоспроизведение последнего трека при подключении к voice-каналу. */
    public boolean autoplay = false;
    /** Плавные fade-in/out при старте/паузе/переключении, мс. */
    public int fadeMs = 150;
    /** Локальный мониторинг («слышу сам себя»), без отправки в сеть. */
    public boolean localMonitoring = false;
    /** Путь к ffmpeg (опционально, для FLAC/AAC/M4A). Пусто = автодетект в PATH. */
    public String ffmpegPath = "";
    /** Активный плейлист (имя файла без расширения). */
    public String activePlaylist = "default";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile ModConfig INSTANCE = new ModConfig();

    public static ModConfig get() { return INSTANCE; }

    public static Path configDir() {
        return Path.of(System.getProperty("user.dir"), "config", MusicChatMod.MODID);
    }

    private static Path configFile() { return configDir().resolve("config.json"); }

    /** Загрузка с дефолтами при любой ошибке. Вызывается из клиентского init. */
    public static synchronized void load() {
        try {
            Path p = configFile();
            if (Files.exists(p)) {
                String json = Files.readString(p);
                ModConfig parsed = GSON.fromJson(json, ModConfig.class);
                if (parsed != null) {
                    migrate(parsed);
                    INSTANCE = parsed;
                }
            } else {
                save();
            }
        } catch (Exception e) {
            Log.warn("Failed to read config, using defaults: {}", e.toString());
            INSTANCE = new ModConfig();
        }
        AudioMirror.publish(INSTANCE);
    }

    /** Миграция старых схем: недостающие поля получают дефолты (Gson уже сделал это), версии нормализуются. */
    private static void migrate(ModConfig c) {
        if (c.schemaVersion < 2) {
            // v1 не имел micBalance/fadeMs/localMonitoring — Gson оставил дефолты класса.
            c.schemaVersion = SCHEMA_VERSION;
            Log.info("Migrated config schema -> v{}", SCHEMA_VERSION);
        }
        c.musicVolume = clamp01(c.musicVolume);
        c.micBalance = clamp01(c.micBalance);
        c.fadeMs = Math.max(0, Math.min(3000, c.fadeMs));
    }

    /** Атомарное сохранение: tmp + ATOMIC_MOVE (fallback — обычный replace). */
    public static synchronized void save() {
        ModConfig c = INSTANCE;
        try {
            Path dir = configDir();
            Files.createDirectories(dir);
            Path tmp = dir.resolve("config.json.tmp");
            Files.writeString(tmp, GSON.toJson(c));
            Path target = configFile();
            try {
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            Log.error("Failed to save config", e);
        }
        AudioMirror.publish(c);
    }

    public static void set(ModConfig c) { INSTANCE = c; AudioMirror.publish(c); }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    /**
     * Immutable-снимок конфигурации для аудио-потока (никаких volatile-чтений полей модели
     * конкурентно с записью GUI). Обновляется атомарно одной ссылкой.
     */
    public record AudioMirrorSnapshot(Mode mode, float musicGain, float micGain, int fadeMs,
                                      boolean pauseMusicOnMute, boolean localMonitoring) {}

    /** Потокобезопасный снимок конфигурации для аудио-потоков. */
    public static final class AudioMirror {
        private static volatile AudioMirrorSnapshot snapshot = build(INSTANCE);
        public static AudioMirrorSnapshot get() { return snapshot; }
        public static void publish(ModConfig c) { snapshot = build(c); }
        private static AudioMirrorSnapshot build(ModConfig c) {
            float m = c.musicVolume;
            float musicGain, micGain;
            switch (c.mode) {
                case MUSIC_ONLY -> { musicGain = m; micGain = 0f; }
                case MIC_ONLY   -> { musicGain = 0f; micGain = 1f; }
                default         -> { musicGain = m * c.micBalance * 2f; micGain = 1f - c.micBalance; }
            }
            return new AudioMirrorSnapshot(c.mode, Math.min(musicGain, 1f), micGain, c.fadeMs,
                    c.pauseMusicOnMute, c.localMonitoring);
        }
    }
}
