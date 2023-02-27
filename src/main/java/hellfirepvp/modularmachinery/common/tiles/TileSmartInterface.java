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
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

public class TileSmartInterface extends TileColorableMachineComponent implements ITickable, MachineComponentTile {
    //LEFT = MachineName, MIDDLE = MachinePos, RIGHT = InterfaceInputValue
    private final ArrayList<Triple<BlockPos, ResourceLocation, Float>> boundData = new ArrayList<>();
    private final SmartInterfaceProvider provider = new SmartInterfaceProvider(this);

    public static NBTTagCompound serializeBoundData(Triple<BlockPos, ResourceLocation, Float> boundData) {
        NBTTagCompound subData = new NBTTagCompound();

        subData.setLong("pos", boundData.getLeft().toLong());
        subData.setString("machine", boundData.getMiddle().toString());
        subData.setFloat("numIn", boundData.getRight());

        return subData;
    }

    public static Triple<BlockPos, ResourceLocation, Float> deserializeBoundData(NBTTagCompound tag) {
        return new MutableTriple<>(
                BlockPos.fromLong(tag.getLong("pos")),
                new ResourceLocation(tag.getString("machine")),
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
            Triple<BlockPos, ResourceLocation, Float> triple = deserializeBoundData(subData);
            boundData.add(triple);
        }
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        NBTTagList data = new NBTTagList();
        for (Triple<BlockPos, ResourceLocation, Float> boundDatum : boundData) {
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
        public Triple<BlockPos, ResourceLocation, Float> getMachineData(ResourceLocation machineName) {
            for (Triple<BlockPos, ResourceLocation, Float> boundDatum : parent.boundData) {
                if (boundDatum.getMiddle().equals(machineName)) {
                    return boundDatum;
                }
            }
            return null;
        }

        @Nullable
        public Triple<BlockPos, ResourceLocation, Float> getMachineData(BlockPos pos) {
            for (Triple<BlockPos, ResourceLocation, Float> boundDatum : parent.boundData) {
                if (boundDatum.getLeft().equals(pos)) {
                    return boundDatum;
                }
            }
            return null;
        }

        @Nullable
        public Triple<BlockPos, ResourceLocation, Float> getMachineData(int index) {
            return parent.boundData.size() >= index ? parent.boundData.get(index) : null;
        }

        public boolean hasMachineData(BlockPos pos) {
            return getMachineData(pos) != null;
        }

        public boolean hasMachineData(ResourceLocation machineName) {
            return getMachineData(machineName) != null;
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
