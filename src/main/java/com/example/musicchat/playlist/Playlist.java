// src/main/java/com/example/musicchat/playlist/Playlist.java
package com.example.musicchat.playlist;

import java.util.ArrayList;
import java.util.List;

/** Именованный список треков. Изменяется только из GUI-потока; снимки — List.copyOf. */
public final class Playlist {
    public String name;
    public final List<Track> tracks = new ArrayList<>();

    public Playlist(String name) { this.name = name; }

    public void add(Track t) { tracks.add(t); }

    public void addAll(List<Track> ts) { tracks.addAll(ts); }

    public void removeIds(java.util.Set<String> ids) { tracks.removeIf(t -> ids.contains(t.id)); }

    public void move(int from, int to) {
        if (from < 0 || from >= tracks.size() || to < 0 || to >= tracks.size()) return;
        Track t = tracks.remove(from);
        tracks.add(to, t);
    }

    public List<Track> snapshot() { return List.copyOf(tracks); }
}
