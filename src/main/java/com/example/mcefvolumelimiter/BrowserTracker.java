package com.example.mcefvolumelimiter;

import org.cef.browser.CefBrowser;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BrowserTracker {
    private static final Set<CefBrowser> BROWSERS = Collections.newSetFromMap(new ConcurrentHashMap<CefBrowser, Boolean>());

    private BrowserTracker() {}

    public static void registerBrowser(CefBrowser browser) {
        if (browser != null && BROWSERS.add(browser)) {
            log("Registered browser: " + browser);
            injectInto(browser);
        }
    }

    public static void unregisterBrowser(CefBrowser browser) {
        if (browser != null && BROWSERS.remove(browser)) {
            log("Unregistered browser: " + browser);
        }
    }

    public static void injectIntoAll() {
        for (CefBrowser browser : BROWSERS) injectInto(browser);
    }

    public static void injectInto(CefBrowser browser) {
        if (browser == null || !ModConfig.enabled) return;
        try {
            browser.executeJavaScript(VolumeScriptBuilder.build(ModConfig.maxVolume), browser.getURL(), 0);
            log("Injected limiter into browser URL=" + browser.getURL());
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
