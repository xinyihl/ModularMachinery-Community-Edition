package github.kasuminova.mmce.common.capability;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CapabilityUpgradeProvider implements ICapabilitySerializable<NBTTagCompound> {
    private final CapabilityUpgrade upgrade = new CapabilityUpgrade();

    public CapabilityUpgrade getUpgrade() {
        return upgrade;
    }

    @Override
    public boolean hasCapability(@Nonnull final Capability<?> capability, @Nullable final EnumFacing facing) {
        return capability == CapabilityUpgrade.MACHINE_UPGRADE_CAPABILITY;
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull final Capability<T> capability, @Nullable final EnumFacing facing) {
        return capability == CapabilityUpgrade.MACHINE_UPGRADE_CAPABILITY ? CapabilityUpgrade.MACHINE_UPGRADE_CAPABILITY.cast(upgrade) : null;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        return upgrade.writeNBT();
    }

    @Override
    public void deserializeNBT(final NBTTagCompound nbt) {
        upgrade.readNBT(nbt);
    }
}
