package github.kasuminova.mmce.common.upgrade;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.helper.UpgradeEventHandlerCT;
import hellfirepvp.modularmachinery.common.tiles.TileUpgradeBus;
import net.minecraft.nbt.NBTTagCompound;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;

import java.util.*;

@ZenRegister
@ZenClass("mods.modularmachinery.MachineUpgrade")
public abstract class MachineUpgrade {

    protected final UpgradeType type;
    protected final Map<Class<?>, List<UpgradeEventHandlerCT>> eventProcessor = new HashMap<>();
    protected int stackSize = 0;
    protected TileUpgradeBus parentBus = null;

    public MachineUpgrade(final UpgradeType type) {
        this.type = type;
    }


    public void readNBT(NBTTagCompound tag) {

    }

    public NBTTagCompound writeNBT() {
        return new NBTTagCompound();
    }

    public abstract MachineUpgrade copy();

    public abstract List<String> getDescriptions();

    public abstract List<String> getBusGUIDescriptions();

    public void addEventHandler(Class<?> eventClass, UpgradeEventHandlerCT handler) {
        eventProcessor.computeIfAbsent(eventClass, v -> new ArrayList<>()).add(handler);
    }

    public List<UpgradeEventHandlerCT> getEventHandlers(Class<?> eventClass) {
        return eventProcessor.getOrDefault(eventClass, Collections.emptyList());
    }

    public TileUpgradeBus getParentBus() {
        return parentBus;
    }

    public MachineUpgrade setParentBus(final TileUpgradeBus parentBus) {
        this.parentBus = parentBus;
        return this;
    }

    public int incrementStackSize(int increment) {
        return stackSize += increment;
    }

    public int decrementStackSize(int decrement) {
        return stackSize -= decrement;
    }

    @ZenGetter("stackSize")
    public int getStackSize() {
        return Math.min(type.getMaxStackSize(), stackSize);
    }

    public UpgradeType getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof MachineUpgrade)) {
            return false;
        }
        MachineUpgrade another = (MachineUpgrade) obj;
        return type.equals((another).type) && parentBus == another.parentBus;
    }
}
