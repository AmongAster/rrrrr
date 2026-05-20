package com.example.mcefvolumelimiter;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public final class ModConfig {
    public static boolean enabled = true;
    public static double maxVolume = 0.20D;
    public static boolean debug = false;
    public static int reinjectIntervalTicks = 100;

    private static Configuration config;

    private ModConfig() {}

    public static void init(File suggested) {
        config = new Configuration(new File(suggested.getParentFile(), "mcef_volume_limiter.cfg"));
        load();
    }

    public static void load() {
        if (config == null) return;
        enabled = config.getBoolean("enabled", Configuration.CATEGORY_GENERAL, true, "Enable/disable global limiter.");
        maxVolume = config.getFloat("maxVolume", Configuration.CATEGORY_GENERAL, 0.20F, 0.0F, 1.0F, "Global max browser media volume [0.0-1.0].");
        debug = config.getBoolean("debug", Configuration.CATEGORY_GENERAL, false, "Enable verbose debug logging.");
        reinjectIntervalTicks = config.getInt("reinjectIntervalTicks", Configuration.CATEGORY_GENERAL, 100, 20, Integer.MAX_VALUE, "Tick interval for periodic limiter reinjection. Minimum 20.");
        saveIfChanged();
    }

    public static void save() {
        if (config != null) {
            saveIfChanged();
        }
    }

    private static void saveIfChanged() {
        if (config.hasChanged()) {
            config.save();
        }
    }
}
