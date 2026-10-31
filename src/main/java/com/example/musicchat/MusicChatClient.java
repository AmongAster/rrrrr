// src/main/java/com/example/musicchat/MusicChatClient.java
package com.example.musicchat;

import com.example.musicchat.config.ModConfig;
import com.example.musicchat.engine.PlaybackEngine;
import com.example.musicchat.gui.MusicScreen;
import com.example.musicchat.util.Log;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Fabric client entrypoint. Работает и БЕЗ Simple Voice Chat (мягкая деградация):
 * движок запускается лениво — при открытии GUI, сам GUI тогда показывает
 * "voice chat not connected"; инъекция в mic-тракт появляется только когда
 * SVC загрузит свой entrypoint {@code voicechat}.
 */
public class MusicChatClient implements ClientModInitializer {

    /** Кейбинд открытия GUI (настраивается в Controls, дефолт M). */
    public static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        ModConfig.load();

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.musicchat.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "category.musicchat"));

        // Команда /musicchat <open|play|pause|next|prev|stop>
        ClientCommandManager.literal("musicchat")
                .then(ClientCommandManager.literal("open").executes(c -> {
                    c.getSource().getClient().setScreen(new MusicScreen());
                    return 1;
                }))
                .then(ClientCommandManager.literal("play").executes(c -> act(c, "play")))
                .then(ClientCommandManager.literal("pause").executes(c -> act(c, "pause")))
                .then(ClientCommandManager.literal("next").executes(c -> act(c, "next")))
                .then(ClientCommandManager.literal("prev").executes(c -> act(c, "prev")))
                .then(ClientCommandManager.literal("stop").executes(c -> act(c, "stop")))
                .executes(c -> {
                    c.getSource().sendFeedback(net.minecraft.text.Text.translatable("musicchat.command.usage"));
                    return 0;
                });

        // Graceful shutdown: никаких висящих потоков после выхода из игры.
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> PlaybackEngine.INSTANCE.close());

        // Тик-хук кейбинда.
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new MusicScreen());
            }
        });

        Log.info("MusicVChat client initialized (SVC present: {})",
                net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("voicechat"));
    }

    private static int act(FabricClientCommandSource src, String what) {
        PlaybackEngine.INSTANCE.start(); // идемпотентно — на случай запуска без SVC
        switch (what) {
            case "play", "pause" -> PlaybackEngine.INSTANCE.playPause();
            case "next" -> PlaybackEngine.INSTANCE.next();
            case "prev" -> PlaybackEngine.INSTANCE.prev();
            case "stop" -> PlaybackEngine.INSTANCE.stop();
        }
        src.sendFeedback(net.minecraft.text.Text.translatable("musicchat.status.ok"));
        return 1;
    }
}
