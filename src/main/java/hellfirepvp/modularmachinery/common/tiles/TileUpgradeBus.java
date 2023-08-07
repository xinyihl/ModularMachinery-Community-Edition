package hellfirepvp.modularmachinery.common.tiles;

import github.kasuminova.mmce.common.capability.CapabilityUpgrade;
import github.kasuminova.mmce.common.upgrade.MachineUpgrade;
import github.kasuminova.mmce.common.upgrade.UpgradeType;
import github.kasuminova.mmce.common.upgrade.registry.RegistryUpgrade;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.TileEntityRestrictedTick;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.IntStream;

public class TileUpgradeBus extends TileEntityRestrictedTick implements MachineComponentTile {
    private final UpgradeBusProvider provider = new UpgradeBusProvider();
    private final Map<BlockPos, DynamicMachine> boundedMachine = new HashMap<>();
    private NBTTagCompound upgradeCustomData = new NBTTagCompound();

    private IOInventory inventory = null;

    public TileUpgradeBus() {
    }

    public TileUpgradeBus(int maxUpgradeSlot) {
        int[] slots = new int[maxUpgradeSlot];
        for (int i = 0; i < maxUpgradeSlot; i++) {
            slots[i] = i;
        }
        this.inventory = new IOInventory(this, slots, new int[0]);
    }

    @Override
    public void doRestrictedTick() {
        if (world.isRemote || ticksExisted % 20 != 0) {
            return;
        }

        int prevSize = boundedMachine.size();
        Iterator<Map.Entry<BlockPos, DynamicMachine>> it = boundedMachine.entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry<BlockPos, DynamicMachine> entry = it.next();
            BlockPos key = entry.getKey();
            DynamicMachine machine = entry.getValue();

            TileEntity te = world.getTileEntity(key);
            if (!(te instanceof TileMultiblockMachineController)) {
                it.remove();
                continue;
            }

            TileMultiblockMachineController controller = (TileMultiblockMachineController) te;
            if (!machine.equals(controller.getFoundMachine())) {
                it.remove();
            }
        }
        if (prevSize != boundedMachine.size()) {
            markForUpdate();
        }
    }

    public IOInventory getInventory() {
        return inventory;
    }

    public Map<BlockPos, DynamicMachine> getBoundedMachine() {
        return boundedMachine;
    }

    @Nonnull
    @Override
    public UpgradeBusProvider provideComponent() {
        return provider;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) inventory;
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void readCustomNBT(final NBTTagCompound compound) {
        super.readCustomNBT(compound);
        if (compound.hasKey("inv")) {
            inventory = IOInventory.deserialize(this, compound.getCompoundTag("inv"));
        } else {
            inventory = new IOInventory(this, new int[0], new int[0]);
        }
        if (compound.hasKey("upgradeCustomData")) {
            upgradeCustomData = compound.getCompoundTag("upgradeCustomData");
        }

        boundedMachine.clear();
        if (compound.hasKey("boundedMachine", Constants.NBT.TAG_LIST)) {
            NBTTagList tagList = compound.getTagList("boundedMachine", Constants.NBT.TAG_COMPOUND);
            IntStream.range(0, tagList.tagCount()).mapToObj(tagList::getCompoundTagAt).forEach(tagCompound -> {
                BlockPos pos = BlockPos.fromLong(tagCompound.getLong("pos"));
                DynamicMachine machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation(tagCompound.getString("machine")));
                if (machine == null) {
                    return;
                }
                boundedMachine.put(pos, machine);
            });
        }
    }

    @Override
    public void writeCustomNBT(final NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setTag("inv", inventory.writeNBT());
        compound.setTag("upgradeCustomData", upgradeCustomData);
        if (!boundedMachine.isEmpty()) {
            NBTTagList tagList = new NBTTagList();
            boundedMachine.forEach((pos, machine) -> {
                NBTTagCompound tagCompound = new NBTTagCompound();
                tagCompound.setLong("pos", pos.toLong());
                tagCompound.setString("machine", machine.getRegistryName().toString());
                tagList.appendTag(tagCompound);
            });
            compound.setTag("boundedMachine", tagList);
        }
    }

    public class UpgradeBusProvider extends MachineComponent<UpgradeBusProvider> {

        public UpgradeBusProvider() {
            super(IOType.INPUT);
        }

        public void boundMachine(TileMultiblockMachineController controller) {
            BlockPos pos = controller.getPos();
            DynamicMachine foundMachine = controller.getFoundMachine();
            if (boundedMachine.containsKey(pos) || foundMachine == null) {
                return;
            }

            boundedMachine.put(pos, foundMachine);
            markForUpdateSync();
        }

        public Map<BlockPos, DynamicMachine> getBoundedMachine() {
            return boundedMachine;
        }

        public Map<UpgradeType, List<MachineUpgrade>> getUpgrades(@Nullable TileMultiblockMachineController controller) {
            IOInventory inventory = TileUpgradeBus.this.inventory;
            HashMap<UpgradeType, List<MachineUpgrade>> found = new HashMap<>();
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!RegistryUpgrade.supportsUpgrade(stack)) {
                    continue;
                }

                CapabilityUpgrade capability = stack.getCapability(CapabilityUpgrade.MACHINE_UPGRADE_CAPABILITY, null);
                if (capability == null) {
                    continue;
                }

                add:
                for (MachineUpgrade upgrade : capability.getUpgrades()) {
                    if (controller != null && !upgrade.getType().isCompatible(controller.getFoundMachine())) {
                        continue;
                    }

                    List<MachineUpgrade> upgrades = found.computeIfAbsent(upgrade.getType(), v -> new ArrayList<>());
                    for (final MachineUpgrade u : upgrades) {
                        if (u.equals(upgrade)) {
                            if (u.getStackSize() >= u.getType().getMaxStackSize()) {
                                continue add;
                            }

                            u.incrementStackSize(stack.getCount());
                            continue add;
                        }
                    }
                    upgrades.add(upgrade.copy().setParentBus(TileUpgradeBus.this));
                }
            }
            return found;
        }

        public NBTTagCompound getUpgradeCustomData(MachineUpgrade upgrade) {
            return upgradeCustomData.getCompoundTag(upgrade.getType().getName());
        }

        public void setUpgradeCustomData(MachineUpgrade upgrade, NBTTagCompound tagCompound) {
            upgradeCustomData.setTag(upgrade.getType().getName(), tagCompound);
            markForUpdateSync();
        }

        public int size() {
            return inventory.getSlots();
        }

        @Override
        public ComponentType getComponentType() {
            return ComponentTypesMM.COMPONENT_UPGRADE_BUS;
        }

        @Override
        public UpgradeBusProvider getContainerProvider() {
            return this;
        }
    }
}
