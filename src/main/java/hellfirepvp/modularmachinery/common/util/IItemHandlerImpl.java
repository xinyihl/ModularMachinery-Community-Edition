package hellfirepvp.modularmachinery.common.util;

import io.netty.util.collection.IntObjectHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class IItemHandlerImpl implements IItemHandlerModifiable {
    protected final Int2IntOpenHashMap slotLimits; // Value not present means default, aka 64.
    protected final Map<Integer, SlotStackHolder> inventory;

    public boolean allowAnySlots = false;
    public List<EnumFacing> accessibleSides = new ArrayList<>();
    protected int[] inSlots = new int[0], outSlots = new int[0], miscSlots = new int[0];

    protected IItemHandlerImpl() {
        this.inventory = new IntObjectHashMap<>();
        this.slotLimits = new Int2IntOpenHashMap();
        this.slotLimits.defaultReturnValue(64);
    }

    public IItemHandlerImpl(int[] inSlots, int[] outSlots) {
        this(inSlots, outSlots, EnumFacing.VALUES);
    }

    public IItemHandlerImpl(int[] inSlots, int[] outSlots, EnumFacing... accessibleFrom) {
        this.inSlots = inSlots;
        this.outSlots = outSlots;

        this.inventory = new IntObjectHashMap<>((inSlots.length + outSlots.length) * 2);
        this.slotLimits = new Int2IntOpenHashMap((inSlots.length + outSlots.length) * 2);
        this.slotLimits.defaultReturnValue(64);
        for (int slot : inSlots) {
            this.inventory.put(slot, new IItemHandlerImpl.SlotStackHolder(slot));
        }
        for (int slot : outSlots) {
            this.inventory.put(slot, new IItemHandlerImpl.SlotStackHolder(slot));
        }
        this.accessibleSides = Arrays.asList(accessibleFrom);
    }

    public IItemHandlerImpl(int[] inSlots, int[] outSlots, List<EnumFacing> accessibleFrom) {
        this.inSlots = inSlots;
        this.outSlots = outSlots;

        this.inventory = new IntObjectHashMap<>((inSlots.length + outSlots.length) * 2);
        this.slotLimits = new Int2IntOpenHashMap((inSlots.length + outSlots.length) * 2);
        this.slotLimits.defaultReturnValue(64);
        for (int slot : inSlots) {
            this.inventory.put(slot, new IItemHandlerImpl.SlotStackHolder(slot));
        }
        for (int slot : outSlots) {
            this.inventory.put(slot, new IItemHandlerImpl.SlotStackHolder(slot));
        }

        this.accessibleSides = new ArrayList<>(accessibleFrom);
    }

    public IItemHandlerImpl copy() {
        IItemHandlerImpl copy = new IItemHandlerImpl(inSlots, outSlots, accessibleSides);
        for (Map.Entry<Integer, SlotStackHolder> entry : inventory.entrySet()) {
            Integer slot = entry.getKey();
            SlotStackHolder holder = entry.getValue();
            copy.inventory.put(slot, holder.copy());
        }
        copy.slotLimits.putAll(slotLimits);
        return copy;
    }

    protected static boolean arrayContains(int[] array, int i) {
        return Arrays.binarySearch(array, i) >= 0;
    }

    protected static boolean canMergeItemStacks(@Nonnull ItemStack stack, @Nonnull ItemStack other) {
        if (stack.isEmpty() || other.isEmpty() || !stack.isStackable() || !other.isStackable()) {
            return false;
        }
        return stack.isItemEqual(other) && ItemStack.areItemStackTagsEqual(stack, other);
    }

    public IItemHandlerImpl setMiscSlots(int... miscSlots) {
        this.miscSlots = miscSlots;
        for (int slot : miscSlots) {
            this.inventory.put(slot, new IItemHandlerImpl.SlotStackHolder(slot));
        }
        return this;
    }

    public IItemHandlerImpl setStackLimit(int limit, int... slots) {
        for (int slot : slots) {
            this.slotLimits.put(slot, limit);
        }
        return this;
    }

    public IItemHandlerModifiable asGUIAccess() {
        return new GuiAccess(this);
    }

    @Override
    public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
        SlotStackHolder holder = this.inventory.get(slot);
        if (holder != null) {
            holder.itemStack = stack;
        }
    }

    @Override
    public int getSlots() {
        return inventory.size();
    }

    @Override
    public int getSlotLimit(int slot) {
        return slotLimits.get(slot);
    }

    @Override
    @Nonnull
    public ItemStack getStackInSlot(int slot) {
        SlotStackHolder holder = inventory.get(slot);
        return holder != null ? holder.itemStack : ItemStack.EMPTY;
    }

    @Override
    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return stack;
        return insertItemInternal(slot, stack, simulate);
    }

    protected ItemStack insertItemInternal(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (!allowAnySlots) {
            if (!arrayContains(inSlots, slot)) {
                return stack;
            }
        }

        IItemHandlerImpl.SlotStackHolder holder = this.inventory.get(slot);
        if (holder == null) {
            return stack; // Shouldn't happen anymore here tho
        }
        ItemStack toInsert = ItemUtils.copyStackWithSize(stack, stack.getCount());
        if (!holder.itemStack.isEmpty()) {
            ItemStack existing = ItemUtils.copyStackWithSize(holder.itemStack, holder.itemStack.getCount());
            int max = Math.min(existing.getMaxStackSize(), getSlotLimit(slot));
            if (existing.getCount() >= max || !canMergeItemStacks(existing, toInsert)) {
                return stack;
            }
            int movable = Math.min(max - existing.getCount(), stack.getCount());
            if (!simulate) {
                holder.itemStack.grow(movable);
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
                }
                return ItemStack.EMPTY;
            } else {
                ItemStack copy = stack.copy();
                copy.setCount(max);
                if (!simulate) {
                    holder.itemStack = copy;
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
        return extractItemInternal(slot, amount, simulate);
    }

    protected ItemStack extractItemInternal(int slot, int amount, boolean simulate) {
        if (!allowAnySlots) {
            if (!arrayContains(outSlots, slot)) {
                return ItemStack.EMPTY;
            }
        }
        IItemHandlerImpl.SlotStackHolder holder = this.inventory.get(slot);
        if (holder == null) {
            return ItemStack.EMPTY; // Shouldn't happen anymore here tho
        }
        if (holder.itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack extract = ItemUtils.copyStackWithSize(holder.itemStack, Math.min(amount, holder.itemStack.getCount()));
        if (extract.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (!simulate) {
            holder.itemStack = ItemUtils.copyStackWithSize(holder.itemStack, holder.itemStack.getCount() - extract.getCount());
        }
        return extract;
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

    protected static class SlotStackHolder {

        protected final int slotId;

        @Nonnull
        protected volatile ItemStack itemStack = ItemStack.EMPTY;

        protected SlotStackHolder(int slotId) {
            this.slotId = slotId;
        }

        public SlotStackHolder copy() {
            SlotStackHolder copied = new SlotStackHolder(slotId);
            if (!itemStack.isEmpty()) {
                copied.itemStack = itemStack.copy();
            }
            return copied;
        }
    }

    public static class GuiAccess implements IItemHandlerModifiable {

        private final IItemHandlerImpl inventory;

        public GuiAccess(IItemHandlerImpl inventory) {
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

            ItemStack extract = inventory.extractItem(slot, Math.min(amount, 64), simulate);

            inventory.allowAnySlots = allowPrev;
            return extract;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }
    }
}
