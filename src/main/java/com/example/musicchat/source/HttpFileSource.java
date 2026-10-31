// src/main/java/com/example/musicchat/source/HttpFileSource.java
package com.example.musicchat.source;

import com.example.musicchat.net.Http;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * Прямая HTTP(S)-ссылка на аудиофайл/поток. Определяет декодер по Content-Type/расширению.
 * Все операции — на decode-worker потоке.
 */
public final class HttpFileSource implements StreamedAudioSource {

    private final String url;
    private AudioSource delegate;

    public HttpFileSource(String url) throws IOException {
        this.url = url;
        open();
    }

    private void open() throws IOException {
        HttpURLConnection c = Http.open(url);
        String ct = c.getContentType();
        InputStream body = new FixedLengthInputStream(c.getInputStream(), contentLen(c));
        this.delegate = DecoderFactory.openStream(url, ct, body);
    }

    private static long contentLen(HttpURLConnection c) {
        long l = c.getContentLengthLong();
        return l > 0 ? l : -1;
    }

    @Override public InputStream openStream() throws IOException {
        HttpURLConnection c = Http.open(url);
        return c.getInputStream();
    }

    @Override public boolean isLive() { return delegate.isLive(); }
    @Override public int sampleRate() { return delegate.sampleRate(); }
    @Override public int channels() { return delegate.channels(); }
    @Override public int read(float[] out, int maxFrames) throws Exception { return delegate.read(out, maxFrames); }
    @Override public long durationMs() { return delegate.durationMs(); }
    @Override public long positionMs() { return delegate.positionMs(); }
    @Override public boolean canSeek() { return delegate.canSeek(); }
    @Override public boolean seekTo(long ms) { return delegate.seekTo(ms); }
    @Override public String displayName() { return delegate.displayName(); }
    @Override public void close() throws IOException { delegate.close(); }

    /** Ограничитель длины ответа (защита от «бесконечного» тела при обманчивом Content-Length). */
    static final class FixedLengthInputStream extends java.io.FilterInputStream {
        private long remaining;
        FixedLengthInputStream(InputStream in, long len) { super(in); this.remaining = len; }
        @Override public int read() throws IOException {
            if (remaining == 0) return -1;
            int b = super.read();
            if (b >= 0 && remaining > 0) remaining--;
            return b;
        }
        @Override public int read(byte[] b, int off, int n) throws IOException {
            if (remaining == 0) return -1;
            if (remaining > 0) n = (int) Math.min(n, remaining);
            int r = super.read(b, off, n);
            if (r > 0 && remaining > 0) remaining -= r;
            return r;
        }
    }
}
