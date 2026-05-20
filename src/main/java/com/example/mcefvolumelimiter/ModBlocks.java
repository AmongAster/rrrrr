package com.example.mcefvolumelimiter;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod.EventBusSubscriber(modid = McefVolumeLimiterMod.MOD_ID)
public final class ModBlocks {
    public static final BlockWebDisplay WEB_DISPLAY = new BlockWebDisplay();

    private ModBlocks() {}

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(WEB_DISPLAY);
        GameRegistry.registerTileEntity(TileEntityWebDisplay.class, McefVolumeLimiterMod.MOD_ID + ":web_display");
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<net.minecraft.item.Item> event) {
        ItemBlock item = new ItemBlock(WEB_DISPLAY);
        item.setRegistryName(WEB_DISPLAY.getRegistryName());
        event.getRegistry().register(item);
    }
}
