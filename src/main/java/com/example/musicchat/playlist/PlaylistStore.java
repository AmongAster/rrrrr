// src/main/java/com/example/musicchat/playlist/PlaylistStore.java
package com.example.musicchat.playlist;

import com.example.musicchat.config.ModConfig;
import com.example.musicchat.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Персистентность плейлистов: config/musicchat/playlists/&lt;name&gt;.json (Gson, schema v1).
 * Атомарная запись tmp+move. Имена файлов санитизируются.
 */
public final class PlaylistStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Path dir() { return ModConfig.configDir().resolve("playlists"); }

    public static String sanitize(String name) {
        String s = name.replaceAll("[^a-zA-Z0-9_\\-. ]", "_").trim();
        if (s.isEmpty()) s = "default";
        return s.length() > 64 ? s.substring(0, 64) : s;
    }

    public static List<String> listNames() {
        try {
            Files.createDirectories(dir());
            try (Stream<Path> s = Files.list(dir())) {
                List<String> out = new ArrayList<>();
                s.filter(p -> p.toString().endsWith(".json")).forEach(p ->
                        out.add(p.getFileName().toString().replace(".json", "")));
                return out;
            }
        } catch (IOException e) {
            Log.error("list playlists", e);
            return List.of();
        }
    }

    public static Playlist load(String name) {
        Path p = dir().resolve(sanitize(name) + ".json");
        try {
            if (!Files.exists(p)) return new Playlist(name);
            JsonObject root = GSON.fromJson(Files.readString(p), JsonObject.class);
            Playlist pl = new Playlist(root.has("name") ? root.get("name").getAsString() : name);
            JsonArray arr = root.getAsJsonArray("tracks");
            if (arr != null) for (var el : arr) {
                JsonObject o = el.getAsJsonObject();
                pl.add(new Track(
                        o.get("id").getAsString(),
                        Track.Kind.valueOf(o.get("kind").getAsString()),
                        o.get("ref").getAsString(),
                        o.get("title").getAsString(),
                        o.has("durationMs") ? o.get("durationMs").getAsLong() : -1));
            }
            return pl;
        } catch (Exception e) {
            Log.error("load playlist " + name, e);
            return new Playlist(name);
        }
    }

    public static void save(Playlist pl) {
        JsonObject root = new JsonObject();
        root.addProperty("schema", 1);
        root.addProperty("name", pl.name);
        JsonArray arr = new JsonArray();
        for (Track t : pl.tracks) {
            JsonObject o = new JsonObject();
            o.addProperty("id", t.id);
            o.addProperty("kind", t.kind.name());
            o.addProperty("ref", t.ref);
            o.addProperty("title", t.title);
            o.addProperty("durationMs", t.durationMs);
            arr.add(o);
        }
        root.add("tracks", arr);
        writeAtomic(dir().resolve(sanitize(pl.name) + ".json"), GSON.toJson(root));
    }

    public static void delete(String name) {
        try { Files.deleteIfExists(dir().resolve(sanitize(name) + ".json")); }
        catch (IOException e) { Log.error("delete playlist " + name, e); }
    }

    private static void writeAtomic(Path target, String content) {
        try {
            Files.createDirectories(target.getParent());
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.writeString(tmp, content);
            try {
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            Log.error("save " + target, e);
        }
    }
}
