package hellfirepvp.modularmachinery.common.util;

import hellfirepvp.modularmachinery.common.tiles.base.TileEntitySynchronized;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;

public class IOInventoryRedstone extends IOInventory {
    public IOInventoryRedstone(TileEntitySynchronized owner, int[] inSlots, int[] outSlots) {
        super(owner, inSlots, outSlots);
    }

    public IOInventoryRedstone(TileEntitySynchronized owner, int[] inSlots, int[] outSlots, EnumFacing... accessibleFrom) {
        super(owner, inSlots, outSlots, accessibleFrom);
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        return stack;
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean hasCapability(EnumFacing facing) {
        return false;
    }

}
