package com.example.mcefvolumelimiter;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.Locale;

public final class ChatUtils {
    private ChatUtils() {}

    public static void msg(String text) {
        if (Minecraft.getMinecraft().player != null) {
            Minecraft.getMinecraft().player.sendMessage(new TextComponentString("[MCEF Volume Limiter] " + text));
        }
    }

    public static String formatVolume(double volume) {
        return String.format(Locale.US, "%.2f", volume);
    }

    public static void msgServer(EntityPlayerMP player, String text) {
        if (player != null) {
            player.sendMessage(new TextComponentString("[MCEF Volume Limiter] " + text));
        }
    }
}
