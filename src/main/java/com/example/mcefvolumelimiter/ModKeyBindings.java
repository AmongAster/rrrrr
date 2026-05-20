package com.example.mcefvolumelimiter;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

public final class ModKeyBindings {
    public static KeyBinding INCREASE;
    public static KeyBinding DECREASE;
    public static KeyBinding TOGGLE;

    private ModKeyBindings() {}

    public static void register() {
        INCREASE = new KeyBinding("key.mcef_volume_limiter.increase", Keyboard.KEY_EQUALS, "key.categories.mcef_volume_limiter");
        DECREASE = new KeyBinding("key.mcef_volume_limiter.decrease", Keyboard.KEY_MINUS, "key.categories.mcef_volume_limiter");
        TOGGLE = new KeyBinding("key.mcef_volume_limiter.toggle", Keyboard.KEY_BACKSLASH, "key.categories.mcef_volume_limiter");
        ClientRegistry.registerKeyBinding(INCREASE);
        ClientRegistry.registerKeyBinding(DECREASE);
        ClientRegistry.registerKeyBinding(TOGGLE);
    }
}
