package com.example.mcefvolumelimiter;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ClientEventHandler {
    private int ticks;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        handleKeybinds();
        ticks++;
        if (ticks >= ModConfig.reinjectIntervalTicks) {
            ticks = 0;
            if (ModConfig.enabled) VolumeInjector.reinjectAll();
        }
    }

    private void handleKeybinds() {
        while (ModKeyBindings.INCREASE.isPressed()) {
            ModConfig.maxVolume = Math.min(1.0D, ModConfig.maxVolume + 0.05D);
            ModConfig.save(); ChatUtils.msg("Max volume: " + ChatUtils.formatVolume(ModConfig.maxVolume)); VolumeInjector.reinjectAll();
        }
        while (ModKeyBindings.DECREASE.isPressed()) {
            ModConfig.maxVolume = Math.max(0.0D, ModConfig.maxVolume - 0.05D);
            ModConfig.save(); ChatUtils.msg("Max volume: " + ChatUtils.formatVolume(ModConfig.maxVolume)); VolumeInjector.reinjectAll();
        }
        while (ModKeyBindings.TOGGLE.isPressed()) {
            ModConfig.enabled = !ModConfig.enabled;
            ModConfig.save(); ChatUtils.msg("Limiter " + (ModConfig.enabled ? "enabled" : "disabled")); if (ModConfig.enabled) VolumeInjector.reinjectAll();
        }
    }
}
