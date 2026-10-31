// src/main/java/com/example/musicchat/net/Http.java
package com.example.musicchat.net;

import com.example.musicchat.MusicChatMod;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.Locale;

/** Утилиты HTTP: валидация, таймауты, редиректы, User-Agent, ICY-заголовки. */
public final class Http {

    public static final int CONNECT_TIMEOUT_MS = 8_000;
    public static final int READ_TIMEOUT_MS = 15_000;
    public static final int MAX_REDIRECTS = 5;

    private Http() {}

    /** Базовая валидация URL для GUI (не сеть). @return null если ok, иначе ключ локализации ошибки. */
    public static String validate(String url) {
        if (url == null || url.isBlank()) return "musicchat.error.url_empty";
        try {
            URI u = URI.create(url.trim());
            String scheme = u.getScheme() == null ? "" : u.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) return "musicchat.error.url_scheme";
            if (u.getHost() == null) return "musicchat.error.url_host";
            // блокировка file:// и прочих — только http(s)
            return null;
        } catch (IllegalArgumentException e) {
            return "musicchat.error.url_parse";
        }
    }

    /** Открывает соединение с поддержкой редиректов и ICY-заголовков. Блокирующий — только из worker-потока. */
    public static HttpURLConnection open(String url) throws IOException {
        String cur = url.trim();
        for (int i = 0; i <= MAX_REDIRECTS; i++) {
            HttpURLConnection c = (HttpURLConnection) new URL(cur).openConnection();
            c.setConnectTimeout(CONNECT_TIMEOUT_MS);
            c.setReadTimeout(READ_TIMEOUT_MS);
            c.setRequestProperty("User-Agent", MusicChatMod.USER_AGENT);
            c.setRequestProperty("Accept", "audio/*, */*");
            c.setRequestProperty("Icy-MetaData", "1"); // Icecast: просим метаданные в потоке
            c.setInstanceFollowRedirects(false);
            c.connect();
            int code = c.getResponseCode();
            if (in3xx(code)) {
                String loc = c.getHeaderField("Location");
                c.disconnect();
                if (loc == null) throw new IOException("redirect without Location");
                cur = new URL(new URL(cur), loc).toString();
                continue;
            }
            if (code != HttpURLConnection.HTTP_OK) {
                c.disconnect();
                throw new IOException("HTTP " + code + " " + c.getResponseMessage());
            }
            return c;
        }
        throw new IOException("too many redirects");
    }

    private static boolean in3xx(int code) { return code >= 301 && code <= 308; }

    /** Проверка резолва хоста без загрузки тела (для быстрой валидации в GUI-фоном). */
    public static boolean hostResolves(String url) {
        try {
            URI u = URI.create(url.trim());
            return u.getHost() != null && InetAddress.getByName(u.getHost()) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
