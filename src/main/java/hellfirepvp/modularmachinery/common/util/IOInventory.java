/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.util;

import github.kasuminova.mmce.common.concurrent.Sync;
import hellfirepvp.modularmachinery.common.tiles.base.TileEntitySynchronized;
import hellfirepvp.modularmachinery.common.tiles.base.TileItemBus;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: IOInventory
 * Created by HellFirePvP
 * Date: 28.06.2017 / 17:42
 */
public class IOInventory implements IItemHandlerModifiable {

    private final TileEntitySynchronized owner;
    private final Map<Integer, Integer> slotLimits = new HashMap<>(); // Value not present means default, aka 64.
    private final Map<Integer, SlotStackHolder> inventory = new HashMap<>();
    public boolean allowAnySlots = false;

    /**
     * <p>当启用时，调用 setStackInSlot() 将会重定向输出到附近的容器。</p>
     * <p>When enabled, calling setStackInSlot() will output directly to the nearby container.</p>
     */
    private boolean redirectOutput = false;
    public List<EnumFacing> accessibleSides = new ArrayList<>();
    private int[] inSlots = new int[0], outSlots = new int[0], miscSlots = new int[0];
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
        for (Integer slot : inSlots) {
            this.inventory.put(slot, new SlotStackHolder(slot));
        }
        for (Integer slot : outSlots) {
            this.inventory.put(slot, new SlotStackHolder(slot));
        }
        this.accessibleSides = Arrays.asList(accessibleFrom);
    }

    @Nonnull
    private static ItemStack copyWithSize(@Nonnull ItemStack stack, int amount) {
        if (stack.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        ItemStack copiedStack = stack.copy();
        copiedStack.setCount(Math.min(amount, stack.getMaxStackSize()));
        return copiedStack;
    }

    private static boolean arrayContains(int[] array, int i) {
        return Arrays.binarySearch(array, i) >= 0;
    }

    private static boolean canMergeItemStacks(@Nonnull ItemStack stack, @Nonnull ItemStack other) {
        if (stack.isEmpty() || other.isEmpty() || !stack.isStackable() || !other.isStackable()) {
            return false;
        }
        return stack.isItemEqual(other) && ItemStack.areItemStackTagsEqual(stack, other);
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
            for (Integer key : inventory.inventory.keySet()) {
                merged.inventory.put(key + slotOffset, inventory.inventory.get(key));
            }
            for (Integer key : inventory.slotLimits.keySet()) {
                merged.slotLimits.put(key + slotOffset, inventory.slotLimits.get(key));
            }
            slotOffset += inventory.inventory.size();
        }
        return merged;
    }

    public boolean isRedirectOutput() {
        return redirectOutput;
    }

    public void setRedirectOutput(boolean redirectOutput) {
        this.redirectOutput = redirectOutput;
    }

    public IOInventory setMiscSlots(int... miscSlots) {
        this.miscSlots = miscSlots;
        for (Integer slot : miscSlots) {
            this.inventory.put(slot, new SlotStackHolder(slot));
        }
        return this;
    }

    public IOInventory setStackLimit(int limit, int... slots) {
        for (int slot : slots) {
            this.slotLimits.put(slot, limit);
        }
        return this;
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

    private void redirectItemStackToNearContainers(int internalSlotId, ItemStack willBeInserted) {
        ItemStack beInserted = willBeInserted;
        for (EnumFacing facing : EnumFacing.VALUES) {
            BlockPos offset = owner.getPos().offset(facing);
            TileEntity te = owner.getWorld().getTileEntity(offset);
            if (te == null || te instanceof TileItemBus) {
                continue;
            }

            EnumFacing accessingSide = facing.getOpposite();

            IItemHandler itemHandler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, accessingSide);
            if (itemHandler == null) {
                continue;
            }

            ItemStack notInserted = insertItemStackToContainer(itemHandler, beInserted);
            if (notInserted == ItemStack.EMPTY) {
                return;
            } else {
                beInserted = notInserted;
            }
        }

        setStackInSlotStrict(internalSlotId, beInserted);
    }

    private static ItemStack insertItemStackToContainer(IItemHandler external, ItemStack willBeInserted) {
        ItemStack beInserted = willBeInserted;
        for (int externalSlotId = 0; externalSlotId < external.getSlots(); externalSlotId++) {
            ItemStack stackInSlot = external.getStackInSlot(externalSlotId);

            if (stackInSlot == ItemStack.EMPTY) {
                ItemStack notInserted = external.insertItem(externalSlotId, beInserted, false);
                if (notInserted == ItemStack.EMPTY) {
                    return ItemStack.EMPTY;
                } else {
                    beInserted = notInserted;
                    continue;
                }
            }

            if (ItemUtils.matchStacks(stackInSlot, willBeInserted)) {
                ItemStack notInserted = external.insertItem(externalSlotId, beInserted, false);
                if (notInserted == ItemStack.EMPTY) {
                    return ItemStack.EMPTY;
                } else {
                    beInserted = notInserted;
                }
            }
        }

        return beInserted;
    }

    @Override
    public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
        Sync.doSyncAction(() -> {
            if (redirectOutput) {
                redirectItemStackToNearContainers(slot, stack);
            } else {
                setStackInSlotStrict(slot, stack);
            }
        });
    }

    public void setStackInSlotStrict(int slot, @Nonnull ItemStack stack) {
        if (this.inventory.containsKey(slot)) {
            this.inventory.get(slot).itemStack = stack;
            owner.markForUpdateSync();
            if (listener != null) {
                listener.onChange();
            }
        }
    }

    @Override
    public int getSlots() {
        return inventory.size();
    }

    @Override
    public int getSlotLimit(int slot) {
        if (slotLimits.containsKey(slot)) {
            return slotLimits.get(slot);
        }
        return 64;
    }

    @Override
    @Nonnull
    public ItemStack getStackInSlot(int slot) {
        return inventory.containsKey(slot) ? inventory.get(slot).itemStack : ItemStack.EMPTY;
    }

    @Override
    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return stack;

        AtomicReference<ItemStack> stackRef = new AtomicReference<>(stack);
        Sync.doSyncAction(() -> stackRef.set(insertItemInternal(slot, stack, simulate)));
        return stackRef.get();
    }

    private ItemStack insertItemInternal(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (!allowAnySlots) {
            if (!arrayContains(inSlots, slot)) {
                return stack;
            }
        }
        if (!this.inventory.containsKey(slot)) {
            return stack; //Shouldn't happen anymore here tho
        }

        SlotStackHolder holder = this.inventory.get(slot);
        ItemStack toInsert = copyWithSize(stack, stack.getCount());
        if (!holder.itemStack.isEmpty()) {
            ItemStack existing = copyWithSize(holder.itemStack, holder.itemStack.getCount());
            int max = Math.min(existing.getMaxStackSize(), getSlotLimit(slot));
            if (existing.getCount() >= max || !canMergeItemStacks(existing, toInsert)) {
                return stack;
            }
            int movable = Math.min(max - existing.getCount(), stack.getCount());
            if (!simulate) {
                holder.itemStack.grow(movable);
                owner.markForUpdateSync();
                if (listener != null) {
                    listener.onChange();
                }
            }
            if (movable >= stack.getCount()) {
                return ItemStack.EMPTY;
            } else {
                ItemStack copy = stack.copy();
                copy.shrink(movable);
                return copy;
            }
        } else {
            int max = Math.min(stack.getMaxStackSize(), getSlotLimit(slot));
            if (max >= stack.getCount()) {
                if (!simulate) {
                    holder.itemStack = stack.copy();
                    owner.markForUpdateSync();
                    if (listener != null) {
                        listener.onChange();
                    }
                }
                return ItemStack.EMPTY;
            } else {
                ItemStack copy = stack.copy();
                copy.setCount(max);
                if (!simulate) {
                    holder.itemStack = copy;
                    owner.markForUpdateSync();
                    if (listener != null) {
                        listener.onChange();
                    }
                }
                copy = stack.copy();
                copy.shrink(max);
                return copy;
            }
        }
    }

    @Override
    @Nonnull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        AtomicReference<ItemStack> stackRef = new AtomicReference<>(ItemStack.EMPTY);
        Sync.doSyncAction(() -> stackRef.set(extractItemInternal(slot, amount, simulate)));
        return stackRef.get();
    }

    private ItemStack extractItemInternal(int slot, int amount, boolean simulate) {
        if (!allowAnySlots) {
            if (!arrayContains(outSlots, slot)) {
                return ItemStack.EMPTY;
            }
        }
        if (!this.inventory.containsKey(slot)) {
            return ItemStack.EMPTY; //Shouldn't happen anymore here tho
        }
        SlotStackHolder holder = this.inventory.get(slot);
        if (holder.itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack extract = copyWithSize(holder.itemStack, Math.min(amount, holder.itemStack.getCount()));
        if (extract.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (!simulate) {
            holder.itemStack = copyWithSize(holder.itemStack, holder.itemStack.getCount() - extract.getCount());
            if (listener != null) {
                listener.onChange();
            }
        }
        owner.markForUpdateSync();
        return extract;
    }

    public NBTTagCompound writeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setIntArray("inSlots", this.inSlots);
        tag.setIntArray("outSlots", this.outSlots);
        tag.setIntArray("miscSlots", this.miscSlots);

        NBTTagList inv = new NBTTagList();
        for (Integer slot : this.inventory.keySet()) {
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

    private static class SlotStackHolder {

        private final int slotId;
        @Nonnull
        private volatile ItemStack itemStack = ItemStack.EMPTY;

        private SlotStackHolder(int slotId) {
            this.slotId = slotId;
        }

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
