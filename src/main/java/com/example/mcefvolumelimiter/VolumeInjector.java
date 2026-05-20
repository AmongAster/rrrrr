package com.example.mcefvolumelimiter;

public final class VolumeInjector {
    private VolumeInjector() {}

    public static void reinjectAll() {
        if (!McefIntegration.isMcefPresent()) return;
        BrowserTracker.injectIntoAll();
    }
}
