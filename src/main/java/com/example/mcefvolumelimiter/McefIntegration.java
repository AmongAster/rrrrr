package com.example.mcefvolumelimiter;

import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefLoadHandlerAdapter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public final class McefIntegration {
    private static boolean mcefPresent;

    private McefIntegration() {}

    public static void detectAndHook() {
        try {
            Class.forName("net.montoyo.mcef.MCEF");
            mcefPresent = true;
            McefVolumeLimiterMod.LOGGER.info("MCEF detected. Installing hooks.");
            hookKnownClients();
        } catch (Throwable t) {
            mcefPresent = false;
            McefVolumeLimiterMod.LOGGER.warn("MCEF not detected, mod will stay passive.");
            if (ModConfig.debug) McefVolumeLimiterMod.LOGGER.debug("Detection exception", t);
        }
    }

    public static boolean isMcefPresent() {
        return mcefPresent;
    }

    @SuppressWarnings("unchecked")
    private static void hookKnownClients() {
        try {
            CefApp app = CefApp.getInstance();
            Field f = CefApp.class.getDeclaredField("clients_");
            f.setAccessible(true);
            Object val = f.get(app);
            if (val instanceof Map) {
                for (Object c : ((Map<?, ?>) val).values()) {
                    if (c instanceof CefClient) attachLoadHook((CefClient) c);
                }
            }
        } catch (Throwable t) {
            if (ModConfig.debug) McefVolumeLimiterMod.LOGGER.warn("Failed clients_ reflection, trying fallback", t);
            tryFallbackCreateHook();
        }
    }

    private static void tryFallbackCreateHook() {
        try {
            Method m = CefApp.class.getMethod("createClient");
            CefClient client = (CefClient) m.invoke(CefApp.getInstance());
            attachLoadHook(client);
        } catch (Throwable t) {
            if (ModConfig.debug) McefVolumeLimiterMod.LOGGER.warn("Failed fallback CEF hook", t);
        }
    }

    private static void attachLoadHook(CefClient client) {
        if (client == null) return;
        try {
            client.addLoadHandler(new CefLoadHandlerAdapter() {
                @Override
                public void onLoadEnd(CefBrowser browser, org.cef.browser.CefFrame frame, int httpStatusCode) {
                    BrowserTracker.registerBrowser(browser);
                    BrowserTracker.injectInto(browser);
                }
            });
            if (ModConfig.debug) McefVolumeLimiterMod.LOGGER.info("Attached CefLoadHandler hook to client " + client);
        } catch (Throwable t) {
            if (ModConfig.debug) McefVolumeLimiterMod.LOGGER.warn("Failed attaching load hook", t);
        }
    }
}
