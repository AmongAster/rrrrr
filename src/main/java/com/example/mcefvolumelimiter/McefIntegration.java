package com.example.mcefvolumelimiter;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

public final class McefIntegration {
    private static boolean mcefPresent;

    private McefIntegration() {}

    public static void detectAndHook() {
        try {
            Class.forName("net.montoyo.mcef.MCEF");
            Class.forName("org.cef.CefApp");
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

    private static void hookKnownClients() {
        try {
            Class<?> cefAppClass = Class.forName("org.cef.CefApp");
            Object app = cefAppClass.getMethod("getInstance").invoke(null);
            Field clientsField = cefAppClass.getDeclaredField("clients_");
            clientsField.setAccessible(true);
            Object value = clientsField.get(app);
            if (value instanceof Map) {
                for (Object client : ((Map<?, ?>) value).values()) {
                    attachLoadHook(client);
                }
            }
        } catch (Throwable t) {
            if (ModConfig.debug) McefVolumeLimiterMod.LOGGER.warn("Failed clients_ reflection, trying fallback", t);
            tryFallbackCreateHook();
        }
    }

    private static void tryFallbackCreateHook() {
        try {
            Class<?> cefAppClass = Class.forName("org.cef.CefApp");
            Object app = cefAppClass.getMethod("getInstance").invoke(null);
            Object client = cefAppClass.getMethod("createClient").invoke(app);
            attachLoadHook(client);
        } catch (Throwable t) {
            if (ModConfig.debug) McefVolumeLimiterMod.LOGGER.warn("Failed fallback CEF hook", t);
        }
    }

    private static void attachLoadHook(Object client) {
        if (client == null) return;
        try {
            Class<?> loadHandlerClass = Class.forName("org.cef.handler.CefLoadHandler");
            Object proxy = Proxy.newProxyInstance(
                    loadHandlerClass.getClassLoader(),
                    new Class<?>[]{loadHandlerClass},
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object p, Method method, Object[] args) {
                            if ("onLoadEnd".equals(method.getName()) && args != null && args.length > 0) {
                                Object browser = args[0];
                                BrowserTracker.registerBrowser(browser);
                                BrowserTracker.injectInto(browser);
                            }
                            return null;
                        }
                    }
            );
            Method addLoadHandler = client.getClass().getMethod("addLoadHandler", loadHandlerClass);
            addLoadHandler.invoke(client, proxy);
            if (ModConfig.debug) McefVolumeLimiterMod.LOGGER.info("Attached load hook to client " + client);
        } catch (Throwable t) {
            if (ModConfig.debug) McefVolumeLimiterMod.LOGGER.warn("Failed attaching load hook", t);
        }
    }
}
