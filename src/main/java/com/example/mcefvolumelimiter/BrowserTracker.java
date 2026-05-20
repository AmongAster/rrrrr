package com.example.mcefvolumelimiter;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BrowserTracker {
    private static final Set<Object> BROWSERS = Collections.newSetFromMap(new ConcurrentHashMap<Object, Boolean>());

    private BrowserTracker() {}

    public static void registerBrowser(Object browser) {
        if (browser != null && BROWSERS.add(browser)) {
            log("Registered browser: " + browser);
            injectInto(browser);
        }
    }

    public static void unregisterBrowser(Object browser) {
        if (browser != null && BROWSERS.remove(browser)) {
            log("Unregistered browser: " + browser);
        }
    }

    public static void injectIntoAll() {
        for (Object browser : BROWSERS) {
            injectInto(browser);
        }
    }

    public static void injectInto(Object browser) {
        if (browser == null || !ModConfig.enabled) return;
        try {
            Method getUrl = browser.getClass().getMethod("getURL");
            String url = (String) getUrl.invoke(browser);
            Method execJs = browser.getClass().getMethod("executeJavaScript", String.class, String.class, int.class);
            execJs.invoke(browser, VolumeScriptBuilder.build(ModConfig.maxVolume), url, 0);
            log("Injected limiter into browser URL=" + url);
        } catch (Throwable t) {
            logError("Injection failed", t);
        }
    }

    private static void log(String msg) {
        if (ModConfig.debug) McefVolumeLimiterMod.LOGGER.info(msg);
    }

    private static void logError(String msg, Throwable t) {
        if (ModConfig.debug) McefVolumeLimiterMod.LOGGER.warn(msg, t);
    }
}
