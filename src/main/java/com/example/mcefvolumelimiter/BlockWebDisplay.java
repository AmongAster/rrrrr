package com.example.mcefvolumelimiter;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockWebDisplay extends Block {
    public BlockWebDisplay() {
        super(Material.ROCK);
        setUnlocalizedName(McefVolumeLimiterMod.MOD_ID + ".web_display");
        setRegistryName("web_display");
        setHardness(2.0F);
        setResistance(10.0F);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityWebDisplay();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityWebDisplay)) return false;

        TileEntityWebDisplay display = (TileEntityWebDisplay) te;
        if (!world.isRemote) {
            if (player.isSneaking()) {
                if (player instanceof EntityPlayerMP && ((EntityPlayerMP) player).canUseCommand(2, "mcefdisplay")) {
                    ItemStack held = player.getHeldItem(hand);
                    if (!held.isEmpty() && held.hasTagCompound()) {
                        NBTTagCompound tag = held.getTagCompound();
                        if (tag.hasKey("mcef_url")) {
                            String url = tag.getString("mcef_url");
                            display.setUrl(url);
                            ChatUtils.msgServer((EntityPlayerMP) player, "Display URL set to: " + url);
                            return true;
                        }
                    }
                    ChatUtils.msgServer((EntityPlayerMP) player, "Sneak-right-click with an item containing NBT key mcef_url to set link.");
                    return true;
                }
                ChatUtils.msgServer((EntityPlayerMP) player, "Only server operators can change display links.");
                return true;
            }
        } else {
            McefIntegration.openDisplayClient(display.getUrl());
        }
        return true;
    }
}
