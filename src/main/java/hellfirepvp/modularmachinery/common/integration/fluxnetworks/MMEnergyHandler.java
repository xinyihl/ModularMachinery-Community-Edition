package hellfirepvp.modularmachinery.common.integration.fluxnetworks;

import hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import sonar.fluxnetworks.api.energy.ITileEnergyHandler;

import javax.annotation.Nonnull;

/**
 * <p>通量网络适配器</p>
 * <p>允许通量网络传输超过 Integer.MAX_VALUE 的能量值。</p>
 * <p>Flux Networks Adapter</p>
 * <p>Allows the flux network to transmit energy values that exceed Integer.MAX_VALUE.</p>
 */
public class MMEnergyHandler implements ITileEnergyHandler {
    public static final MMEnergyHandler INSTANCE = new MMEnergyHandler();

    private MMEnergyHandler() {
    }

    @Override
    public boolean hasCapability(@Nonnull TileEntity tile, EnumFacing side) {
        return !tile.isInvalid() && tile instanceof TileEnergyHatch;
    }

    @Override
    public boolean canAddEnergy(@Nonnull TileEntity tile, EnumFacing side) {
        if (tile instanceof final TileEnergyHatch energyHatch) {
            return energyHatch.canReceive();
        }
        return false;
    }

    @Override
    public boolean canRemoveEnergy(@Nonnull TileEntity tile, EnumFacing side) {
        if (tile instanceof final TileEnergyHatch energyHatch) {
            return energyHatch.canExtract();
        }
        return false;
    }

    @Override
    public long addEnergy(long amount, @Nonnull TileEntity tile, EnumFacing side, boolean simulate) {
        if (!(tile instanceof final TileEnergyHatch energyHatch)) {
            return 0;
        }

        long remainingCapacity = energyHatch.getRemainingCapacity();
        long transferLimit = Math.min(energyHatch.getTier().transferLimit, remainingCapacity);
        if (simulate) {
            return Math.min(transferLimit, amount);
        }

        if (transferLimit < amount) {
            return energyHatch.receiveEnergy(transferLimit) ? transferLimit : 0;
        } else {
            return energyHatch.receiveEnergy(amount) ? amount : 0;
        }
    }

    @Override
    public long removeEnergy(long amount, @Nonnull TileEntity tile, EnumFacing side) {
        if (!(tile instanceof final TileEnergyHatch energyHatch)) {
            return 0;
        }

        long currentEnergy = energyHatch.getCurrentEnergy();
        long transferLimit = Math.min(energyHatch.getTier().transferLimit, currentEnergy);

        if (transferLimit < amount) {
            return energyHatch.extractEnergy(transferLimit) ? transferLimit : 0;
        } else {
            return energyHatch.extractEnergy(amount) ? amount : 0;
        }
    }
}
