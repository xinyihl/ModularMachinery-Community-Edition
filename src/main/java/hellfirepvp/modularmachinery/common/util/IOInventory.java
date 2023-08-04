/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.util;

import hellfirepvp.modularmachinery.common.tiles.base.TileEntitySynchronized;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: IOInventory
 * Created by HellFirePvP
 * Date: 28.06.2017 / 17:42
 */
public class IOInventory extends IItemHandlerImpl {

    private final TileEntitySynchronized owner;
    private InventoryUpdateListener listener = null;

    private IOInventory(TileEntitySynchronized owner) {
        this.owner = owner;
    }

    public IOInventory(TileEntitySynchronized owner, int[] inSlots, int[] outSlots) {
        this(owner, inSlots, outSlots, EnumFacing.VALUES);
    }

    public IOInventory(TileEntitySynchronized owner, int[] inSlots, int[] outSlots, EnumFacing... accessibleFrom) {
        this.owner = owner;
        this.inSlots = inSlots;
        this.outSlots = outSlots;
        for (int slot : inSlots) {
            this.inventory.put(slot, new SlotStackHolder(slot));
        }
        for (int slot : outSlots) {
            this.inventory.put(slot, new SlotStackHolder(slot));
        }
        this.accessibleSides = Arrays.asList(accessibleFrom);
    }

    public static IOInventory deserialize(TileEntitySynchronized owner, NBTTagCompound tag) {
        IOInventory inv = new IOInventory(owner);
        inv.readNBT(tag);
        return inv;
    }

    public static IOInventory mergeBuild(TileEntitySynchronized tile, IOInventory... inventories) {
        IOInventory merged = new IOInventory(tile);
        int slotOffset = 0;
        for (IOInventory inventory : inventories) {
            for (int key : inventory.inventory.keySet()) {
                merged.inventory.put(key + slotOffset, inventory.inventory.get(key));
            }
            for (int key : inventory.slotLimits.keySet()) {
                merged.slotLimits.put(key + slotOffset, inventory.slotLimits.get(key));
            }
            slotOffset += inventory.inventory.size();
        }
        return merged;
    }

    public IOInventory setListener(InventoryUpdateListener listener) {
        this.listener = listener;
        return this;
    }

    public TileEntitySynchronized getOwner() {
        return owner;
    }

    public IItemHandlerModifiable asGUIAccess() {
        return new GuiAccess(this);
    }

    @Override
    public synchronized void setStackInSlot(int slot, @Nonnull ItemStack stack) {
        super.setStackInSlot(slot, stack);
        owner.markForUpdateSync();
        if (listener != null) {
            listener.onChange();
        }
    }

    @Override
    @Nonnull
    public synchronized ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return stack;
        ItemStack inserted = insertItemInternal(slot, stack, simulate);
        if (!simulate) {
            if (listener != null) {
                listener.onChange();
            }
            owner.markForUpdateSync();
        }
        return inserted;
    }

    @Override
    @Nonnull
    public synchronized ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack extracted = super.extractItem(slot, amount, simulate);
        if (!simulate) {
            if (listener != null) {
                listener.onChange();
            }
            owner.markForUpdateSync();
        }
        return extracted;
    }

    public NBTTagCompound writeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setIntArray("inSlots", this.inSlots);
        tag.setIntArray("outSlots", this.outSlots);
        tag.setIntArray("miscSlots", this.miscSlots);

        NBTTagList inv = new NBTTagList();
        for (int slot : this.inventory.keySet()) {
            SlotStackHolder holder = this.inventory.get(slot);
            NBTTagCompound holderTag = new NBTTagCompound();
            holderTag.setBoolean("holderEmpty", holder.itemStack.isEmpty());
            holderTag.setInteger("holderId", slot);
            if (!holder.itemStack.isEmpty()) {
                holder.itemStack.writeToNBT(holderTag);
            }
            inv.appendTag(holderTag);
        }
        tag.setTag("inventoryArray", inv);

        int[] sides = new int[accessibleSides.size()];
        for (int i = 0; i < accessibleSides.size(); i++) {
            EnumFacing side = accessibleSides.get(i);
            sides[i] = side.ordinal();
        }
        tag.setIntArray("sides", sides);
        return tag;
    }

    public void readNBT(NBTTagCompound tag) {
        this.inSlots = tag.getIntArray("inSlots");
        this.outSlots = tag.getIntArray("outSlots");
        this.miscSlots = tag.getIntArray("miscSlots");

        this.inventory.clear();
        NBTTagList list = tag.getTagList("inventoryArray", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound holderTag = list.getCompoundTagAt(i);
            int slot = holderTag.getInteger("holderId");
            boolean isEmpty = holderTag.getBoolean("holderEmpty");
            ItemStack stack = ItemStack.EMPTY;
            if (!isEmpty) {
                stack = new ItemStack(holderTag);
            }
            SlotStackHolder holder = new SlotStackHolder(slot);
            holder.itemStack = stack;
            this.inventory.put(slot, holder);
        }

        int[] sides = tag.getIntArray("sides");
        for (int i : sides) {
            this.accessibleSides.add(EnumFacing.values()[i]);
        }

        if (listener != null) {
            listener.onChange();
        }
    }

    public boolean hasCapability(EnumFacing facing) {
        return facing == null || accessibleSides.contains(facing);
    }

    public IItemHandlerModifiable getCapability(EnumFacing facing) {
        if (hasCapability(facing)) {
            return this;
        }
        return null;
    }

    public int calcRedstoneFromInventory() {
        int i = 0;
        float f = 0.0F;
        for (int j = 0; j < getSlots(); ++j) {
            ItemStack itemstack = getStackInSlot(j);
            if (!itemstack.isEmpty()) {
                f += (float) itemstack.getCount() / (float) Math.min(getSlotLimit(j), itemstack.getMaxStackSize());
                ++i;
            }
        }
        f = f / (float) getSlots();
        return MathHelper.floor(f * 14.0F) + (i > 0 ? 1 : 0);
    }

    public static class GuiAccess implements IItemHandlerModifiable {

        private final IOInventory inventory;

        public GuiAccess(IOInventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
            inventory.setStackInSlot(slot, stack);
        }

        @Override
        public int getSlots() {
            return inventory.getSlots();
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            boolean allowPrev = inventory.allowAnySlots;
            inventory.allowAnySlots = true;
            ItemStack insert = inventory.insertItem(slot, stack, simulate);
            inventory.allowAnySlots = allowPrev;
            return insert;
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            boolean allowPrev = inventory.allowAnySlots;
            inventory.allowAnySlots = true;
            ItemStack extract = inventory.extractItem(slot, amount, simulate);
            inventory.allowAnySlots = allowPrev;
            return extract;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }
    }

}
