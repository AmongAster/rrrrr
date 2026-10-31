// src/main/java/com/example/musicchat/source/StreamedAudioSource.java
package com.example.musicchat.source;

import java.io.IOException;
import java.io.InputStream;

/** Источник, читающий закодированные байты из потока (HTTP-файл, Icecast, HLS-сегменты). */
public interface StreamedAudioSource extends AudioSource {
    /** Поток encoded-аудио (MP3/OGG...). Закрывается вместе с источником. */
    InputStream openStream() throws IOException;
}
