package kport.modularmagic.common.tile;

import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import de.ellpeck.naturesaura.api.aura.type.IAuraType;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import kport.modularmagic.common.integration.jei.ingredient.Aura;
import kport.modularmagic.common.tile.machinecomponent.MachineComponentAuraProvider;

import javax.annotation.Nullable;

public abstract class TileAuraProvider extends TileColorableMachineComponent implements MachineComponentTile {

    public void addAura(Aura aura) {
        IAuraChunk auraChunk = IAuraChunk.getAuraChunk(world, pos);
        if (aura.getType() != auraChunk.getType()) {
            return;
        }
        ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> auraChunk.storeAura(pos, aura.getAmount() * 100000));
    }

    public void removeAura(Aura aura) {
        IAuraChunk auraChunk = IAuraChunk.getAuraChunk(world, pos);
        if (aura.getType() != auraChunk.getType()) {
            return;
        }
        ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> auraChunk.drainAura(pos, aura.getAmount() * 100000));
    }

    public Aura getAura() {
        IAuraType type = IAuraChunk.getAuraChunk(world, pos).getType();
        int amount = IAuraChunk.getAuraInArea(world, pos, 1);
        return new Aura(amount, type);
    }

    public static class Input extends TileAuraProvider {

        @Nullable
        @Override
        public MachineComponentAuraProvider provideComponent() {
            return new MachineComponentAuraProvider(this, IOType.INPUT);
        }
    }

    public static class Output extends TileAuraProvider {

        @Nullable
        @Override
        public MachineComponentAuraProvider provideComponent() {
            return new MachineComponentAuraProvider(this, IOType.OUTPUT);
        }
    }
}
