package hellfirepvp.modularmachinery.common.tiles;

import crafttweaker.util.IEventHandler;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.machine.MachineEvent;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.machine.SmartInterfaceUpdateEvent;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.TileEntityRestrictedTick;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TileSmartInterface extends TileEntityRestrictedTick implements MachineComponentTile {
    private final List<SmartInterfaceData> boundData = new ArrayList<>();
    private final SmartInterfaceProvider provider = new SmartInterfaceProvider(this);

    @Override
    @Nonnull
    public SmartInterfaceProvider provideComponent() {
        return provider;
    }

    @Override
    public void doRestrictedTick() {
        if (getWorld().isRemote) {
            return;
        }
        if (ticksExisted % 20 != 0) {
            return;
        }
        if (boundData.isEmpty()) {
            return;
        }

        // Check Parent Controller Exists
        int prevDataSize = boundData.size();
        for (int i = 0; i < boundData.size(); i++) {
            BlockPos pos = boundData.get(i).getPos();
            if (!getWorld().isBlockLoaded(pos)) {
                continue;
            }
            TileEntity tileEntity = getWorld().getTileEntity(pos);
            if (!(tileEntity instanceof TileMultiblockMachineController)) {
                boundData.remove(i);
                i--;
            }
        }

        if (boundData.size() != prevDataSize) {
            markForUpdate();
        }
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        boundData.clear();

        if (!compound.hasKey("boundData")) {
            return;
        }

        NBTTagList tagList = compound.getTagList("boundData", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < tagList.tagCount(); i++) {
            NBTTagCompound subCompound = tagList.getCompoundTagAt(i);
            boundData.add(SmartInterfaceData.deserialize(subCompound));
        }
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        NBTTagList tagList = new NBTTagList();
        for (SmartInterfaceData data : boundData) {
            tagList.appendTag(data.serialize());
        }
        if (!tagList.isEmpty()) {
            compound.setTag("boundData", tagList);
        }
    }

    public static void onDataUpdate(TileSmartInterface owner, SmartInterfaceData newData) {
        TileEntity te = owner.getWorld().getTileEntity(newData.getPos());
        if (!(te instanceof TileMultiblockMachineController)) {
            return;
        }
        TileMultiblockMachineController ctrl = (TileMultiblockMachineController) te;

        if (ctrl.getFoundMachine() == null) {
            return;
        }

        List<IEventHandler<MachineEvent>> handlerList = ctrl.getFoundMachine().getMachineEventHandlers(SmartInterfaceUpdateEvent.class);
        if (handlerList == null || handlerList.isEmpty()) return;

        for (IEventHandler<MachineEvent> handler : handlerList) {
            SmartInterfaceUpdateEvent event = new SmartInterfaceUpdateEvent(ctrl, owner.getPos(), newData);
            handler.handle(event);
        }
    }

    public static class SmartInterfaceProvider extends MachineComponent<SmartInterfaceProvider> {
        private final TileSmartInterface parent;
        private final List<SmartInterfaceData> boundData;

        public SmartInterfaceProvider(TileSmartInterface parent) {
            super(IOType.INPUT);
            this.parent = parent;
            this.boundData = parent.boundData;
        }

        @Nullable
        public SmartInterfaceData getMachineData(String type) {
            for (SmartInterfaceData data : boundData) {
                if (data.getType().equals(type)) {
                    return data;
                }
            }
            return null;
        }

        @Nullable
        public SmartInterfaceData getMachineData(BlockPos pos) {
            for (SmartInterfaceData data : boundData) {
                if (data.getPos().equals(pos)) {
                    return data;
                }
            }
            return null;
        }

        @Nullable
        public SmartInterfaceData getMachineData(int index) {
            return boundData.size() > index ? boundData.get(index) : null;
        }

        public void addMachineData(BlockPos pos, ResourceLocation parent, String type, float defaultValue, boolean override) {
            SmartInterfaceData data = getMachineData(pos);
            if (data != null) {
                if (override) {
                    removeMachineData(pos);
                } else {
                    return;
                }
            }

            SmartInterfaceData newData = new SmartInterfaceData(pos, parent, type, defaultValue);
            this.boundData.add(newData);
            onDataUpdate(this.parent, newData);

            this.parent.markForUpdateSync();
        }

        public void removeMachineData(BlockPos pos) {
            for (int i = 0; i < this.boundData.size(); i++) {
                SmartInterfaceData data = this.boundData.get(i);
                if (data.getPos().equals(pos)) {
                    this.boundData.remove(i);
                    this.parent.markForUpdateSync();
                    return;
                }
            }
        }

        public int getBoundSize() {
            return boundData.size();
        }

        @Override
        public ComponentType getComponentType() {
            return ComponentTypesMM.COMPONENT_SMART_INTERFACE;
        }

        @Override
        public SmartInterfaceProvider getContainerProvider() {
            return this;
        }
    }
}
