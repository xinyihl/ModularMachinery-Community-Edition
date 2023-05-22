package github.kasuminova.mmce.common.upgrade;

import net.minecraft.nbt.NBTTagCompound;

public abstract class DynamicMachineUpgrade extends MachineUpgrade {

    public DynamicMachineUpgrade(final UpgradeType type) {
        super(type);
    }

    public abstract void readItemNBT(NBTTagCompound tag);
    public abstract NBTTagCompound writeItemNBT();
}
