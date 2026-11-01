// src/main/java/com/example/musicchat/source/WavDecoder.java
package com.example.musicchat.source;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Собственный WAV-парсер (RIFF/WAVE, PCM 8/16/24/32-bit int и IEEE float), без Java Sound SPI.
 * Читает по потоку, поддерживает повторный seek через переоткрытие канала.
 */
public final class WavDecoder implements AudioSource {

    private final Path file;
    private InputStream in;
    private int sampleRate;
    private int channels;
    private int bitsPerSample;
    private boolean ieeeFloat;
    private long dataStart;   // смещение блока data
    private long dataLength;  // байт в data
    private long posBytes;    // текущая позиция внутри data
    private final byte[] scratch = new byte[8];

    public WavDecoder(Path file) throws IOException {
        this.file = file;
        open(0);
    }

    private void open(long seekMs) throws IOException {
        closeQuietly();
        in = Files.newInputStream(file);
        parseHeader(in);
        if (seekMs > 0 && !seekTo(seekMs)) throw new IOException("bad seek");
    }

    private void parseHeader(InputStream is) throws IOException {
        byte[] h = new byte[12];
        readFully(is, h);
        ByteBuffer bb = ByteBuffer.wrap(h).order(ByteOrder.LITTLE_ENDIAN);
        if (bb.getInt(0) != RIFF || bb.getInt(8) != WAVE) throw new IOException("not a WAV file");
        sampleRate = 44100; channels = 1; bitsPerSample = 16; ieeeFloat = false;
        boolean fmtSeen = false;
        while (true) {
            byte[] ch = new byte[8];
            int n = is.read(ch);
            if (n < 8) break;
            ByteBuffer cb = ByteBuffer.wrap(ch).order(ByteOrder.LITTLE_ENDIAN);
            int id = cb.getInt(0);
            long size = Integer.toUnsignedLong(cb.getInt(4));
            if (id == FMT) {
                byte[] fmt = new byte[(int) Math.min(size, 40)];
                readFully(is, fmt);
                ByteBuffer fb = ByteBuffer.wrap(fmt).order(ByteOrder.LITTLE_ENDIAN);
                int audioFormat = Short.toUnsignedInt(fb.getShort(0));
                channels = Short.toUnsignedInt(fb.getShort(2));
                sampleRate = fb.getInt(4);
                bitsPerSample = Short.toUnsignedInt(fb.getShort(14));
                ieeeFloat = (audioFormat == 3);
                if (audioFormat != 1 && audioFormat != 3) throw new IOException("unsupported WAV format " + audioFormat);
                fmtSeen = true;
            } else if (id == DATA) {
                if (!fmtSeen) throw new IOException("data before fmt");
                dataStart = 0; // позиция уже после заголовка chunk
                dataLength = size;
                posBytes = 0;
                return; // поток оставлен на начале data
            } else {
                skip(is, size + (size & 1));
            }
        }
        throw new IOException("no data chunk");
    }

    private static final int RIFF = 0x52494646, WAVE = 0x57415645, FMT = 0x666d7420, DATA = 0x64617461;

    private static void readFully(InputStream is, byte[] b) throws IOException {
        int off = 0;
        while (off < b.length) {
            int r = is.read(b, off, b.length - off);
            if (r < 0) throw new EOFExceptionShort();
            off += r;
        }
    }

    private static final class EOFExceptionShort extends IOException { EOFExceptionShort() { super("unexpected EOF"); } }

    private static void skip(InputStream is, long n) throws IOException {
        while (n > 0) { long s = is.skip(n); if (s <= 0) { is.read(); n--; } else n -= s; }
    }

    @Override public boolean isLive() { return false; }
    @Override public int sampleRate() { return sampleRate; }
    @Override public int channels() { return channels; }

    @Override
    public int read(float[] out, int maxFrames) throws IOException {
        int bytesPerFrame = channels * (bitsPerSample / 8);
        long remaining = dataLength - posBytes;
        if (remaining <= 0) return -1;
        int frames = (int) Math.min(maxFrames, remaining / bytesPerFrame);
        int need = frames * bytesPerFrame;
        byte[] buf = new byte[Math.min(need, 1 << 16)];
        int done = 0;
        while (done < need) {
            int chunk = Math.min(buf.length, need - done);
            readFully(in, java.util.Arrays.copyOfRange(buf, 0, chunk));
            // readFully above allocates; hot path for files is decode-worker, acceptable. Decode from buf:
            decodeChunk(buf, chunk, out, done / bytesPerFrame, bytesPerFrame);
            done += chunk;
        }
        posBytes += done;
        return frames;
    }

    private void decodeChunk(byte[] buf, int len, float[] out, int frameOffset, int bytesPerFrame) {
        ByteBuffer bb = ByteBuffer.wrap(buf, 0, len).order(ByteOrder.LITTLE_ENDIAN);
        int totalFrames = len / bytesPerFrame;
        for (int i = 0; i < totalFrames; i++) {
            for (int c = 0; c < channels; c++) {
                float v = switch (bitsPerSample) {
                    case 8 -> (buf[i * bytesPerFrame + c] & 0xFF) / 127.5f - 1f;
                    case 16 -> bb.getShort() / 32768f;
                    case 24 -> (bb.get() & 0xFF | (bb.get() & 0xFF) << 8 | bb.get() << 16) / 8388608f;
                    case 32 -> ieeeFloat ? bb.getFloat() : bb.getInt() / 2147483648f;
                    default -> 0f;
                };
                out[(frameOffset + i) * channels + c] = v;
            }
        }
    }

    @Override
    public long durationMs() {
        int bytesPerFrame = channels * (bitsPerSample / 8);
        return dataLength * 1000L / ((long) sampleRate * bytesPerFrame);
    }

    @Override
    public long positionMs() {
        int bytesPerFrame = channels * (bitsPerSample / 8);
        return posBytes * 1000L / ((long) sampleRate * bytesPerFrame);
    }

    @Override
    public boolean seekTo(long ms) {
        try {
            int bytesPerFrame = channels * (bitsPerSample / 8);
            long target = Math.max(0, Math.min(dataLength, ms * (long) sampleRate * bytesPerFrame / 1000L));
            target -= target % bytesPerFrame;
            open(0);
            skip(in, target);
            posBytes = target;
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override public String displayName() { return file.getFileName().toString(); }

    @Override
    public void close() throws IOException { closeQuietly(); }

    private void closeQuietly() {
        try { if (in != null) in.close(); } catch (IOException ignored) { }
    }
}
