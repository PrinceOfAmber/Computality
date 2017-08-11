/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2016. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.printer;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.media.items.ItemPrintout;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.common.TilePeripheralBase;
import dan200.computercraft.shared.util.InventoryUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemStackHandler;

public class TilePrinter extends TilePeripheralBase
        implements IInventory, ISidedInventory {
    // Statics

    private static final int[] bottomSlots = {7, 8, 9, 10, 11, 12};
    private static final int[] topSlots = {1, 2, 3, 4, 5, 6};
    private static final int[] sideSlots = {0};

    // Members
    private final Terminal m_page;
    ItemStackHandler inventory;
    private String m_pageTitle;
    private boolean m_printing;

    public TilePrinter() {
        inventory = new ItemStackHandler(13);
        m_page = new Terminal(ItemPrintout.LINE_MAX_LENGTH, ItemPrintout.LINES_PER_PAGE);
        m_pageTitle = "";
        m_printing = false;
    }

    @Override
    public void destroy() {
        ejectContents();
    }

    @Override
    public boolean onActivate(EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (!player.isSneaking()) {
            if (!getWorld().isRemote) {
                ComputerCraft.openPrinterGUI(player, this);
            }
            return true;
        }
        return false;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbttagcompound) {
        super.readFromNBT(nbttagcompound);

        // Read page
        synchronized (m_page) {
            m_printing = nbttagcompound.getBoolean("printing");
            m_pageTitle = nbttagcompound.getString("pageTitle");
            m_page.readFromNBT(nbttagcompound);
        }

        // Read inventory
        synchronized (inventory) {
            inventory.deserializeNBT(nbttagcompound.getCompoundTag("inventory"));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbttagcompound) {
        nbttagcompound = super.writeToNBT(nbttagcompound);

        // Write page
        synchronized (m_page) {
            nbttagcompound.setBoolean("printing", m_printing);
            nbttagcompound.setString("pageTitle", m_pageTitle);
            m_page.writeToNBT(nbttagcompound);
        }

        // Write inventory
        synchronized (inventory) {
            nbttagcompound.setTag("inventory", inventory.serializeNBT());
        }

        return nbttagcompound;
    }

    @Override
    public final void readDescription(NBTTagCompound nbttagcompound) {
        super.readDescription(nbttagcompound);
        updateBlock();
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return super.shouldRefresh(world, pos, oldState, newState) || ComputerCraft.Blocks.peripheral.getPeripheralType(newState) != PeripheralType.Printer;
    }

    public boolean isPrinting() {
        return m_printing;
    }

    // IInventory implementation

    @Override
    public int getSizeInventory() {
        return inventory.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        synchronized (inventory) {
            return inventory.getStackInSlot(i);
        }
    }

    @Override
    public ItemStack removeStackFromSlot(int i) {
        synchronized (inventory) {
            ItemStack result = inventory.getStackInSlot(i);
            inventory.setStackInSlot(i, ItemStack.EMPTY);
            updateAnim();
            return result;
        }
    }

    @Override
    public ItemStack decrStackSize(int i, int j) {
        synchronized (inventory) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                return ItemStack.EMPTY;
            }

            if (inventory.getStackInSlot(i).getCount() <= j) {
                ItemStack itemstack = inventory.getStackInSlot(i);
                inventory.setStackInSlot(i, ItemStack.EMPTY);
                markDirty();
                updateAnim();
                return itemstack;
            }

            ItemStack part = inventory.getStackInSlot(i).splitStack(j);
            if (inventory.getStackInSlot(i).getCount() == 0) {
                inventory.setStackInSlot(i, ItemStack.EMPTY);
                updateAnim();
            }
            markDirty();
            return part;
        }
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack stack) {
        synchronized (inventory) {
            inventory.setStackInSlot(i, stack);
            markDirty();
            updateAnim();
        }
    }

    @Override
    public void clear() {
        synchronized (inventory) {
            for (int i = 0; i < inventory.getSlots(); ++i) {
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
            markDirty();
            updateAnim();
        }
    }

    @Override
    public boolean hasCustomName() {
        return getLabel() != null;
    }

    @Override
    public String getName() {
        String label = getLabel();
        if (label != null) {
            return label;
        } else {
            return "tile.computercraft:printer.name";
        }
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
    public boolean isItemValidForSlot(int slot, ItemStack itemstack) {
        return true;
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

    // ISidedInventory implementation

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        switch (side) {
            case DOWN:
                return bottomSlots;    // Bottom (Out tray)
            case UP:
                return topSlots; // Top (In tray)
            default:
                return sideSlots;     // Sides (Ink)
        }
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack itemstack, EnumFacing face) {
        return isItemValidForSlot(slot, itemstack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemstack, EnumFacing face) {
        return true;
    }

    // IPeripheralTile implementation

    @Override
    public IPeripheral getPeripheral(EnumFacing side) {
        return new PrinterPeripheral(this);
    }

    public Terminal getCurrentPage() {
        if (m_printing) {
            return m_page;
        }
        return null;
    }

    public boolean startNewPage() {
        synchronized (inventory) {
            if (canInputPage()) {
                if (m_printing && !outputPage()) {
                    return false;
                }
                if (inputPage()) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean endCurrentPage() {
        synchronized (inventory) {
            if (m_printing && outputPage()) {
                return true;
            }
        }
        return false;
    }

    public int getInkLevel() {
        synchronized (inventory) {
            ItemStack inkStack = inventory.getStackInSlot(0);
            if (!inkStack.isEmpty() && isInk(inkStack)) {
                return inkStack.getCount();
            }
        }
        return 0;
    }

    public int getPaperLevel() {
        int count = 0;
        synchronized (inventory) {
            for (int i = 1; i < 7; ++i) {
                ItemStack paperStack = inventory.getStackInSlot(i);
                if (!paperStack.isEmpty() && isPaper(paperStack)) {
                    count += paperStack.getCount();
                }
            }
        }
        return count;
    }

    public void setPageTitle(String title) {
        if (m_printing) {
            m_pageTitle = title;
        }
    }

    private boolean isInk(ItemStack stack) {
        return (stack.getItem() == Items.DYE);
    }

    private boolean isPaper(ItemStack stack) {
        Item item = stack.getItem();
        return (item == Items.PAPER || (item instanceof ItemPrintout && ItemPrintout.getType(stack) == ItemPrintout.Type.Single));
    }

    private boolean canInputPage() {
        synchronized (inventory) {
            ItemStack inkStack = inventory.getStackInSlot(0);
            if (inkStack.isEmpty() || !isInk(inkStack)) {
                return false;
            }
            return getPaperLevel() > 0;
        }
    }

    private boolean inputPage() {
        synchronized (inventory) {
            ItemStack inkStack = inventory.getStackInSlot(0);
            if (inkStack.isEmpty() || !isInk(inkStack)) {
                return false;
            }

            for (int i = 1; i < 7; ++i) {
                ItemStack paperStack = inventory.getStackInSlot(i);
                if (!paperStack.isEmpty() && isPaper(paperStack)) {
                    // Decrement ink
                    inkStack.shrink(1);
                    if (inkStack.getCount() <= 0) {
                        inventory.setStackInSlot(0, ItemStack.EMPTY);
                    }

                    // Decrement paper
                    paperStack.shrink(1);
                    if (paperStack.getCount() <= 0) {
                        inventory.setStackInSlot(i, ItemStack.EMPTY);
                        updateAnim();
                    }

                    // Setup the new page
                    int colour = inkStack.getItemDamage();
                    if (colour >= 0 && colour < 16) {
                        m_page.setTextColour(15 - colour);
                    } else {
                        m_page.setTextColour(15);
                    }

                    m_page.clear();
                    if (paperStack.getItem() instanceof ItemPrintout) {
                        m_pageTitle = ItemPrintout.getTitle(paperStack);
                        String[] text = ItemPrintout.getText(paperStack);
                        String[] textColour = ItemPrintout.getColours(paperStack);
                        for (int y = 0; y < m_page.getHeight(); ++y) {
                            m_page.setLine(y, text[y], textColour[y], "");
                        }
                    } else {
                        m_pageTitle = "";
                    }
                    m_page.setCursorPos(0, 0);

                    markDirty();
                    m_printing = true;
                    return true;
                }
            }
            return false;
        }
    }

    private boolean outputPage() {
        synchronized (m_page) {
            int height = m_page.getHeight();
            String[] lines = new String[height];
            String[] colours = new String[height];
            for (int i = 0; i < height; ++i) {
                lines[i] = m_page.getLine(i).toString();
                colours[i] = m_page.getTextColourLine(i).toString();
            }

            ItemStack stack = ItemPrintout.createSingleFromTitleAndText(m_pageTitle, lines, colours);
            synchronized (inventory) {
                ItemStack remainder = InventoryUtil.storeItems(stack, this, 7, 6, 7);
                if (remainder.isEmpty()) {
                    m_printing = false;
                    return true;
                }
            }
            return false;
        }
    }

    private void ejectContents() {
        synchronized (inventory) {
            for (int i = 0; i < 13; ++i) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    // Remove the stack from the inventory
                    setInventorySlotContents(i, ItemStack.EMPTY);

                    // Spawn the item in the world
                    BlockPos pos = getPos();
                    double x = (double) pos.getX() + 0.5;
                    double y = (double) pos.getY() + 0.75;
                    double z = (double) pos.getZ() + 0.5;
                    EntityItem entityitem = new EntityItem(getWorld(), x, y, z, stack);
                    entityitem.motionX = getWorld().rand.nextFloat() * 0.2 - 0.1;
                    entityitem.motionY = getWorld().rand.nextFloat() * 0.2 - 0.1;
                    entityitem.motionZ = getWorld().rand.nextFloat() * 0.2 - 0.1;
                    getWorld().spawnEntity(entityitem);
                }
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    private void updateAnim() {
        synchronized (inventory) {
            int anim = 0;
            for (int i = 1; i < 7; ++i) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty() && isPaper(stack)) {
                    anim += 1;
                    break;
                }
            }
            for (int i = 7; i < 13; ++i) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty() && isPaper(stack)) {
                    anim += 2;
                    break;
                }
            }
            setAnim(anim);
        }
    }
}
