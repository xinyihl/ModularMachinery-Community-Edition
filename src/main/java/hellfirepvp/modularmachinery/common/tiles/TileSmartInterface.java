package hellfirepvp.modularmachinery.common.tiles;

import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

public class TileSmartInterface extends TileColorableMachineComponent implements ITickable, MachineComponentTile {
    //LEFT = MachinePos, MIDDLE = DataType, RIGHT = InputValue
    private final ArrayList<Triple<BlockPos, String, Float>> boundData = new ArrayList<>();
    private final SmartInterfaceProvider provider = new SmartInterfaceProvider(this);

    public static NBTTagCompound serializeBoundData(Triple<BlockPos, String, Float> boundData) {
        NBTTagCompound subData = new NBTTagCompound();

        subData.setLong("pos", boundData.getLeft().toLong());
        subData.setString("type", boundData.getMiddle());
        subData.setFloat("numIn", boundData.getRight());

        return subData;
    }

    public static Triple<BlockPos, String, Float> deserializeBoundData(NBTTagCompound tag) {
        return new MutableTriple<>(
                BlockPos.fromLong(tag.getLong("pos")),
                tag.getString("type"),
                tag.getFloat("numIn"));
    }

    @Override
    @Nonnull
    public SmartInterfaceProvider provideComponent() {
        return provider;
    }

    @Override
    public void update() {
        World world = getWorld();
        if (world.isRemote) {
            return;
        }

    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        if (!compound.hasKey("boundData")) {
            return;
        }

        NBTTagList data = compound.getTagList("boundData", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < data.tagCount(); i++) {
            NBTTagCompound subData = data.getCompoundTagAt(i);
            Triple<BlockPos, String, Float> triple = deserializeBoundData(subData);
            boundData.add(triple);
        }
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        NBTTagList data = new NBTTagList();
        for (Triple<BlockPos, String, Float> boundDatum : boundData) {
            data.appendTag(serializeBoundData(boundDatum));
        }
        if (!data.isEmpty()) {
            compound.setTag("boundData", data);
        }
    }

    public static class SmartInterfaceProvider extends MachineComponent<SmartInterfaceProvider> {
        private final TileSmartInterface parent;

        public SmartInterfaceProvider(TileSmartInterface parent) {
            super(IOType.INPUT);
            this.parent = parent;
        }

        @Nullable
        public Triple<BlockPos, String, Float> getMachineData(String type) {
            for (Triple<BlockPos, String, Float> boundDatum : parent.boundData) {
                if (boundDatum.getMiddle().equals(type)) {
                    return boundDatum;
                }
            }
            return null;
        }

        @Nullable
        public Triple<BlockPos, String, Float> getMachineData(BlockPos pos) {
            for (Triple<BlockPos, String, Float> boundDatum : parent.boundData) {
                if (boundDatum.getLeft().equals(pos)) {
                    return boundDatum;
                }
            }
            return null;
        }

        @Nullable
        public Triple<BlockPos, String, Float> getMachineData(int index) {
            return parent.boundData.size() >= index ? parent.boundData.get(index) : null;
        }

        public boolean hasMachineData(BlockPos pos) {
            return getMachineData(pos) != null;
        }

        public boolean hasMachineData(BlockPos pos, String type) {
            Triple<BlockPos, String, Float> data = getMachineData(pos);
            if (data == null) {
                return false;
            }
            return data.getMiddle().equals(type);
        }

        public void addMachineData(BlockPos pos, String type, float defaultValue) {
            Triple<BlockPos, String, Float> data = getMachineData(pos);
            if (data != null) {
                return;
            }
            parent.boundData.add(new MutableTriple<>(pos, type, defaultValue));
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
