/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2016. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.pocket.recipes;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.pocket.items.PocketComputerItemFactory;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

import javax.annotation.Nonnull;

public class PocketComputerUpgradeRecipe implements IRecipe {
    public PocketComputerUpgradeRecipe() {

    }

    @Override
    public int getRecipeSize() {
        return 2;
    }

    @Override
    @Nonnull
    public ItemStack getRecipeOutput() {
        return PocketComputerItemFactory.create(-1, null, ComputerFamily.Normal, null);
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        return !getCraftingResult(inventory).isEmpty();
    }

    @Override
    @Nonnull
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        // Scan the grid for a pocket computer
        ItemStack computer = ItemStack.EMPTY;
        int computerX = -1;
        int computerY = -1;
        for (int y = 0; y < inventory.getHeight(); ++y) {
            for (int x = 0; x < inventory.getWidth(); ++x) {
                ItemStack item = inventory.getStackInRowAndColumn(x, y);
                if (!item.isEmpty() && item.getItem() instanceof ItemPocketComputer) {
                    computer = item;
                    computerX = x;
                    computerY = y;
                    break;
                }
            }
            if (!computer.isEmpty()) {
                break;
            }
        }

        if (computer.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if(!(computer.getItem() instanceof ItemPocketComputer))
            return ItemStack.EMPTY;

        ItemPocketComputer itemComputer = (ItemPocketComputer) computer.getItem();
        if (ItemPocketComputer.getUpgrade(computer) != null) {
            return ItemStack.EMPTY;
        }

        // Check for upgrades around the item
        IPocketUpgrade upgrade = null;
        for (int y = 0; y < inventory.getHeight(); ++y) {
            for (int x = 0; x < inventory.getWidth(); ++x) {
                ItemStack item = inventory.getStackInRowAndColumn(x, y);
                if (x == computerX && y == computerY) {
                    continue;
                } else if (x == computerX && y == computerY - 1) {
                    upgrade = ComputerCraft.getPocketUpgrade(item);
                    if (upgrade == null) return ItemStack.EMPTY;
                    break;
                } else if (!item.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
            if(upgrade!=null)
                break;
        }

        if (upgrade == null) {
            return ItemStack.EMPTY;
        }

        // Construct the new stack
        ComputerFamily family = itemComputer.getFamily(computer);
        int computerID = itemComputer.getComputerID(computer);
        String label = itemComputer.getLabel(computer);
        return PocketComputerItemFactory.create(computerID, label, family, upgrade);
    }

    @Override
    @Nonnull
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inventoryCrafting) {
        NonNullList<ItemStack> list = NonNullList.create();
        for (int i = 0; i < inventoryCrafting.getSizeInventory(); ++i) {
            ItemStack stack = inventoryCrafting.getStackInSlot(i);
            list.add(ForgeHooks.getContainerItem(stack));
        }
        return list;
    }
}
