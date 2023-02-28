package hellfirepvp.modularmachinery.common.tiles;

import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.TileEntityRestrictedTick;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TileSmartInterface extends TileEntityRestrictedTick implements MachineComponentTile {
    //LEFT = MachinePos, MIDDLE = DataType, RIGHT = InputValue
    private final List<SmartInterfaceData> boundData = new ArrayList<>();
    private final SmartInterfaceProvider provider = new SmartInterfaceProvider(this);

    @Override
    @Nonnull
    public SmartInterfaceProvider provideComponent() {
        return provider;
    }

    @Override
    public void doRestrictedTick() {
        World world = getWorld();
        if (world.isRemote) {
            return;
        }
        if (ticksExisted % 20 != 0) {
            return;
        }
        if (boundData.isEmpty()) {
            return;
        }

        //Check Parent Controller Exists
        int prevDataSize = boundData.size();
        for (int i = 0; i < boundData.size(); i++) {
            BlockPos pos = boundData.get(i).getPos();
            if (!world.isBlockLoaded(pos)) {
                continue;
            }
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity == null || tileEntity instanceof TileMachineController) {
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

    public static class SmartInterfaceProvider extends MachineComponent<SmartInterfaceProvider> {
        private final TileSmartInterface parent;

        public SmartInterfaceProvider(TileSmartInterface parent) {
            super(IOType.INPUT);
            this.parent = parent;
        }

        @Nullable
        public SmartInterfaceData getMachineData(String type) {
            for (SmartInterfaceData data : parent.boundData) {
                if (data.getType().equals(type)) {
                    return data;
                }
            }
            return null;
        }

        @Nullable
        public SmartInterfaceData getMachineData(BlockPos pos) {
            for (SmartInterfaceData data : parent.boundData) {
                if (data.getPos().equals(pos)) {
                    return data;
                }
            }
            return null;
        }

        @Nullable
        public SmartInterfaceData getMachineData(int index) {
            return parent.boundData.size() >= index ? parent.boundData.get(index) : null;
        }

        public boolean hasMachineData(BlockPos pos) {
            return getMachineData(pos) != null;
        }

        public boolean hasMachineData(BlockPos pos, String type) {
            SmartInterfaceData data = getMachineData(pos);
            if (data == null) {
                return false;
            }
            return data.getType().equals(type);
        }

        public void addMachineData(BlockPos pos, ResourceLocation parent, String type, float defaultValue) {
            SmartInterfaceData data = getMachineData(pos);
            if (data != null) {
                return;
            }
            this.parent.boundData.add(new SmartInterfaceData(pos, parent, type, defaultValue));
        }

        public int getBoundSize() {
            return parent.boundData.size();
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
