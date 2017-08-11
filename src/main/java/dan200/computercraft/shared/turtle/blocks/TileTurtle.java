/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2016. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import dan200.computercraft.shared.computer.blocks.TileComputerBase;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.turtle.apis.TurtleAPI;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.RedstoneUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import java.util.List;

public class TileTurtle extends TileComputerBase
        implements ITurtleTile, IInventory, ITickable {
    // Statics

    public static final int INVENTORY_SIZE = 16;
    public static final int INVENTORY_WIDTH = 4;
    public static final int INVENTORY_HEIGHT = 4;

    // Members

    ItemStackHandler inventory;
    ItemStackHandler prevInventory;
    private boolean m_inventoryChanged;
    private TurtleBrain m_brain;
    private boolean m_moved;

    public TileTurtle() {
        inventory = new ItemStackHandler(INVENTORY_SIZE);
        prevInventory = new ItemStackHandler(INVENTORY_SIZE);
        m_inventoryChanged = false;
        m_brain = createBrain();
        m_moved = false;
    }

    public boolean hasMoved() {
        return m_moved;
    }

    protected TurtleBrain createBrain() {
        return new TurtleBrain(this);
    }

    protected final ServerComputer createComputer(int instanceID, int id, int termWidth, int termHeight) {
        ServerComputer computer = new ServerComputer(
                getWorld(),
                id,
                m_label,
                instanceID,
                getFamily(),
                termWidth,
                termHeight
        );
        computer.setPosition(getPos());
        computer.addAPI(new TurtleAPI(computer.getAPIEnvironment(), getAccess()));
        m_brain.setupComputer(computer);
        return computer;
    }

    @Override
    protected ServerComputer createComputer(int instanceID, int id) {
        return createComputer(instanceID, id, ComputerCraft.terminalWidth_turtle, ComputerCraft.terminalHeight_turtle);
    }

    @Override
    public void destroy() {
        if (!hasMoved()) {
            // Stop computer
            super.destroy();

            // Drop contents
            if (!getWorld().isRemote) {
                int size = getSizeInventory();
                for (int i = 0; i < size; ++i) {
                    ItemStack stack = getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        WorldUtil.dropItemStack(stack, getWorld(), getPos());
                    }
                }
            }
        } else {
            // Just turn off any redstone we had on
            for (EnumFacing dir : EnumFacing.VALUES) {
                RedstoneUtil.propogateRedstoneOutput(getWorld(), getPos(), dir);
            }
        }
    }

    @Override
    protected void unload() {
        if (!hasMoved()) {
            super.unload();
        }
    }

    @Override
    public void getDroppedItems(List<ItemStack> drops, boolean creative) {
        IComputer computer = getComputer();
        if (!creative || (computer != null && computer.getLabel() != null)) {
            drops.add(TurtleItemFactory.create(this));
        }
    }

    @Override
    public ItemStack getPickedItem() {
        return TurtleItemFactory.create(this);
    }

    @Override
    public boolean onActivate(EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ) {
        // Request description from server
        requestTileEntityUpdate();

        // Apply dye
        ItemStack currentItem = player.getHeldItem(EnumHand.MAIN_HAND);
        if (!currentItem.isEmpty()) {
            if (currentItem.getItem() == Items.DYE) {
                // Dye to change turtle colour
                if (!getWorld().isRemote) {
                    int dye = (currentItem.getItemDamage() & 0xf);
                    if (m_brain.getDyeColour() != dye) {
                        m_brain.setDyeColour(dye);
                        if (!player.capabilities.isCreativeMode) {
                            currentItem.shrink(1);
                        }
                    }
                }
                return true;
            } else if (currentItem.getItem() == Items.WATER_BUCKET && m_brain.getDyeColour() != -1) {
                // Water to remove turtle colour
                if (!getWorld().isRemote) {
                    if (m_brain.getDyeColour() != -1) {
                        m_brain.setDyeColour(-1);
                        if (!player.capabilities.isCreativeMode) {
                            player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.BUCKET));
                        }
                    }
                }
                return true;
            }
        }

        // Open GUI or whatever
        return super.onActivate(player, side, hitX, hitY, hitZ);
    }

    @Override
    protected boolean canNameWithTag(EntityPlayer player) {
        return true;
    }

    @Override
    public void openGUI(EntityPlayer player) {
        ComputerCraft.openTurtleGUI(player, this);
    }

    @Override
    public boolean isSolidOnSide(int side) {
        return false;
    }

    @Override
    public boolean isImmuneToExplosion(Entity exploder) {
        if (getFamily() == ComputerFamily.Advanced) {
            return true;
        } else {
            return exploder != null && (exploder instanceof EntityLivingBase || exploder instanceof EntityFireball);
        }
    }

    @Override
    public AxisAlignedBB getBounds() {
        Vec3d offset = getRenderOffset(1.0f);
        return new AxisAlignedBB(
                offset.xCoord + 0.125, offset.yCoord + 0.125, offset.zCoord + 0.125,
                offset.xCoord + 0.875, offset.yCoord + 0.875, offset.zCoord + 0.875
        );
    }

    @Override
    protected double getInteractRange(EntityPlayer player) {
        return 12.0;
    }

    @Override
    public void update() {
        super.update();
        m_brain.update();
        synchronized (inventory) {
            if (!getWorld().isRemote && m_inventoryChanged) {
                IComputer computer = getComputer();
                if (computer != null) {
                    computer.queueEvent("turtle_inventory");
                }

                m_inventoryChanged = false;
                for (int n = 0; n < getSizeInventory(); ++n) {
                    prevInventory.setStackInSlot(n, InventoryUtil.copyItem(getStackInSlot(n)));
                }
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbttagcompound) {
        super.readFromNBT(nbttagcompound);

        // Read inventory
        inventory.deserializeNBT(nbttagcompound.getCompoundTag("inventory"));
        prevInventory.deserializeNBT(nbttagcompound.getCompoundTag("prevInventory"));

        // Read state
        m_brain.readFromNBT(nbttagcompound);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbttagcompound) {
        nbttagcompound = super.writeToNBT(nbttagcompound);

        // Write inventory
        nbttagcompound.setTag("inventory", inventory.serializeNBT());
        nbttagcompound.setTag("prevInventory", prevInventory.serializeNBT());

        // Write brain
        nbttagcompound = m_brain.writeToNBT(nbttagcompound);

        return nbttagcompound;
    }

    @Override
    protected boolean isPeripheralBlockedOnSide(int localSide) {
        return hasPeripheralUpgradeOnSide(localSide);
    }

    @Override
    protected boolean isRedstoneBlockedOnSide(int localSide) {
        return hasPeripheralUpgradeOnSide(localSide);
    }

    // IDirectionalTile

    @Override
    public EnumFacing getDirection() {
        return m_brain.getDirection();
    }

    @Override
    public void setDirection(EnumFacing dir) {
        m_brain.setDirection(dir);
    }

    // ITurtleTile

    @Override
    public ITurtleUpgrade getUpgrade(TurtleSide side) {
        return m_brain.getUpgrade(side);
    }

    @Override
    public Colour getColour() {
        int dye = m_brain.getDyeColour();
        if (dye >= 0) {
            return Colour.values()[dye];
        }
        return null;
    }

    @Override
    public ResourceLocation getOverlay() {
        return m_brain.getOverlay();
    }

    @Override
    public ITurtleAccess getAccess() {
        return m_brain;
    }

    @Override
    public Vec3d getRenderOffset(float f) {
        return m_brain.getRenderOffset(f);
    }

    @Override
    public float getRenderYaw(float f) {
        return m_brain.getVisualYaw(f);
    }

    @Override
    public float getToolRenderAngle(TurtleSide side, float f) {
        return m_brain.getToolRenderAngle(side, f);
    }

    // IInventory

    @Override
    public int getSizeInventory() {
        return INVENTORY_SIZE;
    }

    @Override
    @Nonnull
    public ItemStack getStackInSlot(int slot) {
        if (slot >= 0 && slot < INVENTORY_SIZE) {
            synchronized (inventory) {
                return inventory.getStackInSlot(slot);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStackFromSlot(int slot) {
        synchronized (inventory) {
            ItemStack result = getStackInSlot(slot);
            setInventorySlotContents(slot, ItemStack.EMPTY);
            return result;
        }
    }

    @Override
    public ItemStack decrStackSize(int slot, int count) {
        if (count == 0) {
            return ItemStack.EMPTY;
        }

        synchronized (inventory) {
            ItemStack stack = getStackInSlot(slot);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            if (stack.getCount() <= count) {
                setInventorySlotContents(slot, ItemStack.EMPTY);
                return stack;
            }

            ItemStack part = stack.splitStack(count);
            onInventoryDefinitelyChanged();
            return part;
        }
    }

    @Override
    public void setInventorySlotContents(int i, @Nonnull ItemStack stack) {
        if (i >= 0 && i < INVENTORY_SIZE) {
            synchronized (inventory) {
                if (!InventoryUtil.areItemsEqual(stack, inventory.getStackInSlot(i))) {
                    inventory.setStackInSlot(i, stack);
                    onInventoryDefinitelyChanged();
                }
            }
        }
    }

    @Override
    public void clear() {
        synchronized (inventory) {
            boolean changed = false;
            for (int i = 0; i < INVENTORY_SIZE; ++i) {
                if (!inventory.getStackInSlot(i).isEmpty()) {
                    inventory.setStackInSlot(i, ItemStack.EMPTY);
                    changed = true;
                }
            }
            if (changed) {
                onInventoryDefinitelyChanged();
            }
        }
    }

    @Override
    public String getName() {
        IComputer computer = getComputer();
        if (computer != null) {
            String label = computer.getLabel();
            if (label != null && label.length() > 0) {
                return label;
            }
        }
        return "tile.computercraft.turtle.name";
    }

    @Override
    public boolean hasCustomName() {
        IComputer computer = getComputer();
        if (computer != null) {
            String label = computer.getLabel();
            if (label != null && label.length() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        if (hasCustomName()) {
            return new TextComponentString(getName());
        } else {
            return new TextComponentTranslation(getName());
        }
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void openInventory(EntityPlayer player) {
    }

    @Override
    public void closeInventory(EntityPlayer player) {
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public void markDirty() {
        super.markDirty();
        synchronized (inventory) {
            if (!m_inventoryChanged) {
                for (int n = 0; n < getSizeInventory(); ++n) {
                    if (!ItemStack.areItemStacksEqual(getStackInSlot(n), prevInventory.getStackInSlot(n))) {
                        m_inventoryChanged = true;
                        break;
                    }
                }
            }
        }
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return isUsable(player, false);
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    public boolean isUseableByRemote(EntityPlayer player) {
        return isUsable(player, true);
    }

    public void onInventoryDefinitelyChanged() {
        super.markDirty();
        m_inventoryChanged = true;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    public void onTileEntityChange() {
        super.markDirty();
    }

    // Networking stuff

    @Override
    public void writeDescription(NBTTagCompound nbttagcompound) {
        super.writeDescription(nbttagcompound);
        m_brain.writeDescription(nbttagcompound);
    }

    @Override
    public void readDescription(NBTTagCompound nbttagcompound) {
        super.readDescription(nbttagcompound);
        m_brain.readDescription(nbttagcompound);
        updateBlock();
    }

    // Privates

    private boolean hasPeripheralUpgradeOnSide(int side) {
        ITurtleUpgrade upgrade;
        switch (side) {
            case 4:
                upgrade = getUpgrade(TurtleSide.Right);
                break;
            case 5:
                upgrade = getUpgrade(TurtleSide.Left);
                break;
            default:
                return false;
        }
        return upgrade != null && upgrade.getType().isPeripheral();
    }

    public void transferStateFrom(TileTurtle copy) {
        super.transferStateFrom(copy);
        inventory = copy.inventory;
        prevInventory = copy.prevInventory;
        m_inventoryChanged = copy.m_inventoryChanged;
        m_brain = copy.m_brain;
        m_brain.setOwner(this);
        copy.m_moved = true;
    }
}