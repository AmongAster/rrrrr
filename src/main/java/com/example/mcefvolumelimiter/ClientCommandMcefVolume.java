package com.example.mcefvolumelimiter;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.client.ClientCommandHandler;

import java.util.Arrays;
import java.util.List;

public class ClientCommandMcefVolume extends CommandBase {
    public static void register() {
        ClientCommandHandler.instance.registerCommand(new ClientCommandMcefVolume());
    }

    @Override public String getName() { return "mcefvolume"; }
    @Override public String getUsage(ICommandSender sender) { return "/mcefvolume <get|set|enable|disable|reload>"; }
    @Override public int getRequiredPermissionLevel() { return 0; }
    @Override public List<String> getAliases() { return Arrays.asList("mcefv"); }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length == 0) { ChatUtils.msg(getUsage(sender)); return; }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "get":
                ChatUtils.msg("Enabled=" + ModConfig.enabled + " maxVolume=" + format(ModConfig.maxVolume));
                break;
            case "set":
                if (args.length < 2) { ChatUtils.msg("Usage: /mcefvolume set <0.0-1.0>"); return; }
                try {
                    double v = clamp(Double.parseDouble(args[1]));
                    ModConfig.maxVolume = v; ModConfig.save();
                    ChatUtils.msg("Max volume set to " + format(v));
                    VolumeInjector.reinjectAll();
                } catch (NumberFormatException e) { ChatUtils.msg("Invalid number."); }
                break;
            case "enable":
                ModConfig.enabled = true; ModConfig.save(); ChatUtils.msg("Limiter enabled."); VolumeInjector.reinjectAll(); break;
            case "disable":
                ModConfig.enabled = false; ModConfig.save(); ChatUtils.msg("Limiter disabled."); break;
            case "reload":
                ModConfig.load(); ChatUtils.msg("Config reloaded."); VolumeInjector.reinjectAll(); break;
            default:
                ChatUtils.msg(getUsage(sender));
        }
        if (ModConfig.debug) McefVolumeLimiterMod.LOGGER.info("Command by " + Minecraft.getMinecraft().player.getName() + ": " + Arrays.toString(args));
    }

    private static double clamp(double v) { return Math.max(0.0D, Math.min(1.0D, v)); }
    private static String format(double d) { return String.format(java.util.Locale.US, "%.2f", d); }
}
