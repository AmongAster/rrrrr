// src/main/java/com/example/musicchat/net/PlaylistParser.java
package com.example.musicchat.net;

import com.example.musicchat.playlist.Track;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Парсеры .m3u/.m3u8 (текстовые) и .pls — локальных и скачанных файлов. Возвращает список Tracks. */
public final class PlaylistParser {

    private PlaylistParser() {}

    public static boolean isPlaylistFile(String name) {
        String n = name.toLowerCase();
        return n.endsWith(".m3u") || n.endsWith(".m3u8") || n.endsWith(".pls");
    }

    /** @param baseUri для резолва относительных путей (null => пути как есть / относительно URL) */
    public static List<Track> parse(String fileName, InputStream in, String baseUri) throws IOException {
        String lower = fileName.toLowerCase();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            if (lower.endsWith(".pls")) return parsePls(r, baseUri);
            return parseM3u(r, baseUri);
        }
    }

    private static List<Track> parseM3u(BufferedReader r, String baseUri) throws IOException {
        List<Track> out = new ArrayList<>();
        String line, pendingTitle = null;
        while ((line = r.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#EXTM3U")) continue;
            if (line.startsWith("#EXTINF")) {
                int q = line.indexOf(',');
                pendingTitle = q >= 0 ? line.substring(q + 1).trim() : null;
                continue;
            }
            if (line.startsWith("#")) continue;
            String ref = resolve(line, baseUri);
            Track.Kind k = Track.guessKind(ref);
            out.add(new Track(k, ref, pendingTitle));
            pendingTitle = null;
        }
        return out;
    }

    private static List<Track> parsePls(BufferedReader r, String baseUri) throws IOException {
        Map<Integer, String> files = new LinkedHashMap<>();
        Map<Integer, String> titles = new LinkedHashMap<>();
        String line;
        while ((line = r.readLine()) != null) {
            line = line.trim();
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            String key = line.substring(0, eq).trim().toLowerCase();
            String val = line.substring(eq + 1).trim();
            if (key.startsWith("file")) put(files, Integer.parseInt(key.substring(4)), resolve(val, baseUri));
            else if (key.startsWith("title")) put(titles, Integer.parseInt(key.substring(5)), val);
        }
        List<Track> out = new ArrayList<>();
        for (Map.Entry<Integer, String> e : files.entrySet()) {
            Track.Kind k = Track.guessKind(e.getValue());
            out.add(new Track(k, e.getValue(), titles.get(e.getKey())));
        }
        return out;
    }

    private static void put(Map<Integer, String> m, int idx, String v) {
        if (idx > 0 && idx <= 10000) m.put(idx, v);
    }

    private static String resolve(String ref, String baseUri) {
        if (baseUri == null) return ref;
        try {
            if (ref.startsWith("http://") || ref.startsWith("https://")) return ref;
            return URI.create(baseUri).resolve(ref).toString();
        } catch (Exception e) { return ref; }
    }
}
