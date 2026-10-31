// src/main/java/com/example/musicchat/util/Log.java
package com.example.musicchat.util;

import com.example.musicchat.MusicChatMod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Централизованный логгер мода (SLF4J, как в Fabric/MC).
 * Никакого спама в игровой чат — только логи.
 */
public final class Log {

    public static final Logger LOGGER = LoggerFactory.getLogger(MusicChatMod.MODID);

    private Log() {
    }

    public static void info(String fmt, Object... args) {
        LOGGER.info(fmt, args);
    }

    public static void warn(String fmt, Object... args) {
        LOGGER.warn(fmt, args);
    }

    public static void debug(String fmt, Object... args) {
        LOGGER.debug(fmt, args);
    }

    public static void error(String msg, Throwable t) {
        LOGGER.error(msg, t);
    }
}
