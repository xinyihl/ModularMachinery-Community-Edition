package kport.modularmagic.common.tile;

import com.rwtema.extrautils2.power.PowerManager;
import com.rwtema.extrautils2.tile.TileRainbowGenerator;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import kport.modularmagic.common.tile.machinecomponent.MachineComponentRainbowProvider;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.Collection;

public class TileRainbowProvider extends TileColorableMachineComponent implements MachineComponentTile {

    private int frequency;

    public TileRainbowProvider(int frequency) {
        this.frequency = frequency;
    }

    public boolean rainbow() {
        PowerManager.PowerFreq freq = PowerManager.instance.getPowerFreqRaw(this.frequency);
        if (freq == null) {
            return false;
        }

        Collection<TileRainbowGenerator> c = freq.getSubTypes(TileRainbowGenerator.rainbowGenerators);
        if (c == null) {
            return false;
        }

        for (final TileRainbowGenerator power : c) {
            if (power.providing) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public MachineComponent<?> provideComponent() {
        return new MachineComponentRainbowProvider(this, IOType.INPUT);
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);

        compound.setInteger("frequency", this.frequency);
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);

        this.frequency = compound.getInteger("frequency");
    }
}
