package github.kasuminova.mmce.common.util;

import github.kasuminova.mmce.common.tile.MEPatternProvider;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasTankInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.IItemHandlerModifiable;
import mekanism.api.gas.GasStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MEPatternProviderHandler implements IItemHandlerModifiable, IFluidHandler, IExtendedGasHandler {

    private final MEPatternProvider owner;
    private final InfItemFluidHandler delegate;

    public MEPatternProviderHandler(MEPatternProvider owner, InfItemFluidHandler delegate) {
        this.owner = owner;
        this.delegate = delegate;
    }

    private boolean canPerformOperation() {
        MEPatternProvider.WorkModeSetting workMode = owner.getWorkMode();
        return switch (workMode) {
            case BLOCKING_MODE -> delegate.isEmpty();
            case CRAFTING_LOCK_MODE -> owner.isMachineCompleted();
            case ENHANCED_BLOCKING_MODE -> owner.getCurrentPattern() == null || delegate.isEmpty();
            default -> true;
        };
    }

    // === IItemHandlerModifiable ===
    @Override
    public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
        if (canPerformOperation()) {
            delegate.setStackInSlot(slot, stack);
        }
    }

    @Override
    public int getSlots() {
        return delegate.getSlots();
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return delegate.getStackInSlot(slot);
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (!canPerformOperation() && !simulate) {
            return stack;
        }
        return delegate.insertItem(slot, stack, simulate);
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return delegate.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return delegate.getSlotLimit(slot);
    }

    // === IFluidHandler ===
    @Override
    public IFluidTankProperties[] getTankProperties() {
        return delegate.getTankProperties();
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (!canPerformOperation() && doFill) {
            return 0;
        }
        return delegate.fill(resource, doFill);
    }

    @Nullable
    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        return delegate.drain(resource, doDrain);
    }

    @Nullable
    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        return delegate.drain(maxDrain, doDrain);
    }

    // === IExtendedGasHandler ===
    @Override
    public GasStack drawGas(GasStack toDraw, boolean doTransfer) {
        return delegate.drawGas(toDraw, doTransfer);
    }

    @Override
    public int receiveGas(EnumFacing side, GasStack toReceive, boolean doTransfer) {
        if (!canPerformOperation() && doTransfer) {
            return 0;
        }
        return delegate.receiveGas(side, toReceive, doTransfer);
    }

    @Override
    public GasStack drawGas(EnumFacing side, int drawAmount, boolean doTransfer) {
        return delegate.drawGas(side, drawAmount, doTransfer);
    }

    @Override
    public boolean canReceiveGas(EnumFacing side, Gas gas) {
        return canPerformOperation() && delegate.canReceiveGas(side, gas);
    }

    @Override
    public boolean canDrawGas(EnumFacing side, Gas gas) {
        return delegate.canDrawGas(side, gas);
    }

    @Nonnull
    @Override
    public GasTankInfo[] getTankInfo() {
        return delegate.getTankInfo();
    }
}