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
        if (tile instanceof TileEnergyHatch) {
            TileEnergyHatch energyHatch = (TileEnergyHatch) tile;
            return energyHatch.canReceive();
        }
        return false;
    }

    @Override
    public boolean canRemoveEnergy(@Nonnull TileEntity tile, EnumFacing side) {
        if (tile instanceof TileEnergyHatch) {
            TileEnergyHatch energyHatch = (TileEnergyHatch) tile;
            return energyHatch.canExtract();
        }
        return false;
    }

    @Override
    public long addEnergy(long amount, @Nonnull TileEntity tile, EnumFacing side, boolean simulate) {
        if (!(tile instanceof TileEnergyHatch)) {
            return 0;
        }

        TileEnergyHatch energyHatch = (TileEnergyHatch) tile;
        long remainingCapacity = energyHatch.getRemainingCapacity();
        if (simulate) {
            return Math.min(remainingCapacity, amount);
        }

        if (remainingCapacity < amount) {
            return energyHatch.receiveEnergy(remainingCapacity) ? remainingCapacity : 0;
        } else {
            return energyHatch.receiveEnergy(amount) ? amount : 0;
        }
    }

    @Override
    public long removeEnergy(long amount, @Nonnull TileEntity tile, EnumFacing side) {
        if (!(tile instanceof TileEnergyHatch)) {
            return 0;
        }

        TileEnergyHatch energyHatch = (TileEnergyHatch) tile;
        long currentEnergy = energyHatch.getCurrentEnergy();

        if (currentEnergy < amount) {
            return energyHatch.extractEnergy(amount) ? amount : 0;
        } else {
            return energyHatch.extractEnergy(currentEnergy) ? currentEnergy : 0;
        }
    }
}
