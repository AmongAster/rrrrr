// src/main/java/com/example/musicchat/source/DecoderFactory.java
package com.example.musicchat.source;

import com.example.musicchat.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Фабрика аудио-источников. MVP: WAV (собственный парсер) + MP3 (JLayer).
 * OGG/Vorbis/FLAC/AAC/M4A — через опциональный внешний ffmpeg (см. {@link FfmpegSource});
 * без ffmpeg форматы честно помечаются как неподдерживаемые (ошибка в GUI, не краш).
 */
public final class DecoderFactory {

    public static final java.util.Set<String> SUPPORTED_MVP =
            java.util.Set.of("wav", "mp3");
    public static final java.util.Set<String> SUPPORTED_WITH_FFMPEG =
            java.util.Set.of("ogg", "oga", "opus", "flac", "aac", "m4a", "wma");

    private DecoderFactory() {}

    public static String ext(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    /** Локальный файл -> AudioSource. Бросает IOException при неподдерживаемом формате. */
    public static AudioSource openFile(Path file) throws IOException {
        String e = ext(file.getFileName().toString());
        switch (e) {
            case "wav":
                return new WavDecoder(file);
            case "mp3":
                return new Mp3Source(file.getFileName().toString(), Files.newInputStream(file));
            default:
                if (SUPPORTED_WITH_FFMPEG.contains(e) && FfmpegSource.isAvailable()) {
                    return FfmpegSource.openPiped(file.getFileName().toString(), Files.newInputStream(file));
                }
                throw new IOException("Unsupported format ." + e
                        + (FfmpegSource.isAvailable() ? "" : " (install ffmpeg or set ffmpegPath in config)"));
        }
    }

    /** HTTP/стримовой поток -> AudioSource по content-type/расширению URL. */
    public static AudioSource openStream(String url, String contentType, InputStream in) throws IOException {
        String e = ext(url.toLowerCase(Locale.ROOT).split("\\?")[0]);
        String ct = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        Log.debug("openStream url=%s ct=%s ext=%s", url, ct, e);
        if (ct.contains("mpeg") || ct.contains("mp3") || e.equals("mp3")) {
            return new Mp3Source(shortName(url), in);
        }
        if (ct.contains("wav") || e.equals("wav")) {
            // WAV из потока без seek — читаем в tmp-файл на decode-worker (не UI!) потоково, до 200 МБ лимит
            Path tmp = Files.createTempFile("musicchat-", ".wav");
            tmp.toFile().deleteOnExit();
            long copied = 0L, limit = 200L << 20;
            try (InputStream is = in; var os = Files.newOutputStream(tmp)) {
                byte[] b = new byte[1 << 16];
                int r;
                while ((r = is.read(b)) > 0) { os.write(b, 0, r); copied += r; if (copied > limit) break; }
            }
            return new WavDecoder(tmp);
        }
        if (FfmpegSource.isAvailable()) {
            return FfmpegSource.openPiped(shortName(url), in);
        }
        throw new IOException("Cannot decode stream type '" + ct + "' from " + shortName(url)
                + "; supported live: MP3/Icecast" + (FfmpegSource.isAvailable() ? "+ffmpeg" : " (ffmpeg not found for other codecs)"));
    }

    private static String shortName(String url) {
        int q = url.indexOf("//");
        String s = url.substring(q < 0 ? 0 : q + 2);
        int sl = Math.max(s.lastIndexOf('/'), 0);
        return s.length() > 60 ? s.substring(0, 60) + "…" : s;
    }
}
