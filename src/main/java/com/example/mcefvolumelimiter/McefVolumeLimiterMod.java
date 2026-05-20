package com.example.mcefvolumelimiter;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = McefVolumeLimiterMod.MOD_ID, name = McefVolumeLimiterMod.MOD_NAME, version = McefVolumeLimiterMod.VERSION, clientSideOnly = true)
public class McefVolumeLimiterMod {
    public static final String MOD_ID = "mcef_volume_limiter";
    public static final String MOD_NAME = "MCEF Volume Limiter";
    public static final String VERSION = "1.0.0";

    @Mod.Instance(MOD_ID)
    public static McefVolumeLimiterMod INSTANCE;

    public static Logger LOGGER;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        ModConfig.init(event.getSuggestedConfigurationFile());
        McefIntegration.detectAndHook();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        ClientCommandMcefVolume.register();
        ModKeyBindings.register();
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
    }
}
