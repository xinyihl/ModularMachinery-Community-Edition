/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.tiles.base;

import com.brandon3055.draconicevolution.DEFeatures;
import com.brandon3055.draconicevolution.blocks.tileentity.TileEnergyStorageCore;
import github.kasuminova.mmce.common.concurrent.Sync;
import gregtech.api.capability.GregtechCapabilities;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.block.prop.EnergyHatchData;
import hellfirepvp.modularmachinery.common.data.Config;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.util.IEnergyHandlerAsync;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

import static hellfirepvp.modularmachinery.common.block.prop.EnergyHatchData.*;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: TileEnergyHatch
 * Created by HellFirePvP
 * Date: 08.07.2017 / 10:14
 */
@Optional.Interface(iface = "cofh.redstoneflux.api.IEnergyStorage", modid = "redstoneflux")
public abstract class TileEnergyHatch extends TileColorableMachineComponent implements ITickable, IEnergyStorage, IEnergyHandlerAsync, MachineComponentTile, cofh.redstoneflux.api.IEnergyStorage, SelectiveUpdateTileEntity {

    protected long energy = 0;
    protected EnergyHatchData size;
    protected BlockPos foundCore = null;
    protected int energyCoreSearchFailedCount = 0;
    private GTEnergyContainer energyContainer;

    public TileEnergyHatch() {
    }

    public TileEnergyHatch(EnergyHatchData size, IOType ioType) {
        this.size = size;
        this.energyContainer = new GTEnergyContainer(this, ioType);
    }

    @Optional.Method(modid = "draconicevolution")
    protected long attemptDECoreTransfer(long maxCanReceive) {
        return 0;
    }

    @Optional.Method(modid = "draconicevolution")
    protected void findCore() {
        if (!(world.getTotalWorldTime() % currentFoundCoreDelay() == 0)) {
            return;
        }

        TileEnergyStorageCore core = null;
        Iterable<BlockPos> positions = BlockPos.getAllInBox(pos.add(-searchRange, -searchRange, -searchRange), pos.add(searchRange, searchRange, searchRange));

        for (BlockPos blockPos : positions) {
            if (world.getBlockState(blockPos).getBlock() == DEFeatures.energyStorageCore) {
                TileEntity tile = world.getTileEntity(blockPos);
                if (tile instanceof TileEnergyStorageCore && ((TileEnergyStorageCore) tile).active.value) {
                    core = (TileEnergyStorageCore) tile;
                    break;
                }
            }
        }

        if (core == null) {
            energyCoreSearchFailedCount++;
        } else {
            foundCore = core.getPos();
            energyCoreSearchFailedCount = 0;
        }
    }

    protected int currentFoundCoreDelay() {
        return energyCoreSearchDelay + (delayedEnergyCoreSearch
                ? (Math.min(energyCoreSearchFailedCount * 20, maxEnergyCoreSearchDelay - energyCoreSearchDelay))
                : 0);
    }

    @Optional.Method(modid = "gregtech")
    private static Capability<?> getGTEnergyCapability() {
        return GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER;
    }

    protected static int convertDownEnergy(long energy) {
        return energy >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) energy;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive()) {
            return 0;
        }
        int insertable = this.energy + maxReceive > this.size.maxEnergy ? convertDownEnergy(this.size.maxEnergy - this.energy) : maxReceive;
        insertable = Math.min(insertable, convertDownEnergy(size.transferLimit));
        if (!simulate) {
            this.energy = MiscUtils.clamp(this.energy + insertable, 0, this.size.maxEnergy);
            markForUpdate();
        }
        return insertable;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!canExtract()) {
            return 0;
        }
        int extractable = this.energy - maxExtract < 0 ? convertDownEnergy(this.energy) : maxExtract;
        extractable = Math.min(extractable, convertDownEnergy(size.transferLimit));
        if (!simulate) {
            this.energy = MiscUtils.clamp(this.energy - extractable, 0, this.size.maxEnergy);
            markForUpdate();
        }
        return extractable;
    }

    @Override
    public int getEnergyStored() {
        return convertDownEnergy(this.energy);
    }

    @Override
    public int getMaxEnergyStored() {
        return convertDownEnergy(this.size.maxEnergy);
    }

    @Override
    public abstract boolean canExtract();

    @Override
    public abstract boolean canReceive();

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return true;
        }

        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return (T) this;
        }
        if (Mods.GREGTECH.isPresent() && capability == getGTEnergyCapability()) {
            return (T) this.energyContainer;
        }

        return super.getCapability(capability, facing);
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return Config.selectiveUpdateTileEntity ? null : super.getUpdatePacket();
    }

    @Override
    public SPacketUpdateTileEntity getTrueUpdatePacket() {
        return super.getUpdatePacket();
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);

        NBTBase energyTag = compound.getTag("energy");
        if (energyTag instanceof NBTPrimitive) {
            this.energy = ((NBTPrimitive) energyTag).getLong();
        }
        this.size = EnergyHatchData.values()[compound.getInteger("hatchSize")];
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);

        compound.setLong("energy", this.energy);
        compound.setInteger("hatchSize", this.size.ordinal());
    }

    //MM stuff

    public EnergyHatchData getTier() {
        return size;
    }

    @Override
    public long getCurrentEnergy() {
        return this.energy;
    }

    @Override
    public void setCurrentEnergy(long energy) {
        this.energy = MiscUtils.clamp(energy, 0, getMaxEnergy());
        markForUpdateSync();
    }

    @Override
    public boolean extractEnergy(long extract) {
        AtomicBoolean success = new AtomicBoolean(false);
        Sync.doSyncAction(() -> {
            if (this.energy >= extract) {
                this.energy -= extract;
                success.set(true);
            }
        });
        if (success.get()) {
            markForUpdateSync();
        }
        return success.get();
    }

    @Override
    public boolean receiveEnergy(long receive) {
        AtomicBoolean success = new AtomicBoolean(false);
        Sync.doSyncAction(() -> {
            if (getRemainingCapacity() >= receive) {
                this.energy += receive;
                success.set(true);
            }
        });
        if (success.get()) {
            markForUpdateSync();
        }
        return success.get();
    }

    @Override
    public long getMaxEnergy() {
        return this.size.maxEnergy;
    }

}
