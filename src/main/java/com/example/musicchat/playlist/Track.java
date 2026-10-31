// src/main/java/com/example/musicchat/playlist/Track.java
package com.example.musicchat.playlist;

import java.util.Locale;
import java.util.UUID;

/** Элемент плейлиста. Immutable для GUI-снимков (id — стабильный ключ). */
public final class Track {
    public enum Kind { LOCAL, URL, ICECAST, HLS }

    public final String id;
    public final Kind kind;
    /** Путь (LOCAL) или URL (остальные). */
    public final String ref;
    public final String title;
    /** Кэш длительности мс (-1 неизвестно). */
    public volatile long durationMs = -1;

    public Track(Kind kind, String ref, String title) {
        this.id = UUID.randomUUID().toString();
        this.kind = kind;
        this.ref = ref;
        this.title = title == null || title.isBlank() ? defaultTitle(kind, ref) : title;
    }

    public Track(String id, Kind kind, String ref, String title, long durationMs) {
        this.id = id; this.kind = kind; this.ref = ref; this.title = title; this.durationMs = durationMs;
    }

    private static String defaultTitle(Kind k, String ref) {
        if (k == Kind.LOCAL) {
            int s = Math.max(ref.lastIndexOf('/'), ref.lastIndexOf('\\'));
            return s >= 0 ? ref.substring(s + 1) : ref;
        }
        return ref;
    }

    public boolean isStream() { return kind != Kind.LOCAL; }

    public static Kind guessKind(String ref) {
        String r = ref.toLowerCase(Locale.ROOT);
        if (!r.startsWith("http://") && !r.startsWith("https://")) return Kind.LOCAL;
        if (r.contains(".m3u8") || r.contains("/hls") || r.contains("format=m3u8")) return Kind.HLS;
        if (r.contains("mp3") && (r.contains(":8000") || r.contains(";") || r.contains("stream"))) return Kind.ICECAST;
        return Kind.URL;
    }
}
