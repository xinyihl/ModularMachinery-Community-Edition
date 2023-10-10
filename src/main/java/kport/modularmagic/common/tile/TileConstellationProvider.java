package kport.modularmagic.common.tile;

import hellfirepvp.astralsorcery.common.constellation.IConstellation;
import hellfirepvp.astralsorcery.common.constellation.distribution.ConstellationSkyHandler;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import kport.modularmagic.common.tile.machinecomponent.MachineComponentConstellationProvider;

import javax.annotation.Nullable;

public class TileConstellationProvider extends TileColorableMachineComponent implements MachineComponentTile {

    public boolean isConstellationInSky(IConstellation constellation) {
        if (ConstellationSkyHandler.getInstance().getWorldHandler(world) == null)
            return false;

        return ConstellationSkyHandler.getInstance().getWorldHandler(world).isActive(constellation);
    }

    @Nullable
    @Override
    public MachineComponent provideComponent() {
        return new MachineComponentConstellationProvider(this, IOType.INPUT);
    }
}
