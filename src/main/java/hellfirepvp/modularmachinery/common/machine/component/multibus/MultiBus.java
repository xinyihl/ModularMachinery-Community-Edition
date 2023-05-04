package hellfirepvp.modularmachinery.common.machine.component.multibus;

import hellfirepvp.modularmachinery.common.util.HybridTank;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;

public class MultiBus extends HybridTank implements IItemHandlerModifiable {

    public MultiBus(int capacity) {
        super(capacity);
    }

    @Override
    public void setStackInSlot(int slot, @Nonnull ItemStack stack) {

    }

    @Override
    public int getSlots() {
        return 0;
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return null;
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        return null;
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return null;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 0;
    }
}
