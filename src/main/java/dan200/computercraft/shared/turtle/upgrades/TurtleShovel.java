/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2016. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.upgrades;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemSpade;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TurtleShovel extends TurtleTool {
    public TurtleShovel(ResourceLocation id, int legacyId, String adjective, Item item) {
        super(id, legacyId, adjective, item);
    }

    @Override
    protected boolean canBreakBlock(World world, BlockPos pos) {
        if (super.canBreakBlock(world, pos)) {
            IBlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            Material material = block.getMaterial(state);
            return
                    material == Material.GROUND ||
                            material == Material.SAND ||
                            material == Material.SNOW ||
                            material == Material.CLAY ||
                            material == Material.CRAFTED_SNOW ||
                            material == Material.GRASS ||
                            material == Material.PLANTS ||
                            material == Material.CACTUS ||
                            material == Material.GOURD ||
                            material == Material.LEAVES ||
                            material == Material.VINE;
        }
        return false;
    }
}