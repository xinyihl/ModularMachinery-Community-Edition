package hellfirepvp.modularmachinery.common.util;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

public class SmartInterfaceData {
    private final BlockPos pos;
    private final ResourceLocation parent;
    private final String type;
    private float value = 0;

    public SmartInterfaceData(BlockPos pos, ResourceLocation parent, String type) {
        this.pos = pos;
        this.parent = parent;
        this.type = type;
    }

    public SmartInterfaceData(BlockPos pos, ResourceLocation parent, String type, float value) {
        this.pos = pos;
        this.parent = parent;
        this.type = type;
        this.value = value;
    }

    public BlockPos getPos() {
        return pos;
    }

    public ResourceLocation getParent() {
        return parent;
    }

    public String getType() {
        return type;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public NBTTagCompound serialize() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setLong("pos", pos.toLong());
        compound.setString("parent", parent.toString());
        compound.setString("type", type);
        compound.setFloat("value", value);

        return compound;
    }

    public static SmartInterfaceData deserialize(NBTTagCompound compound) {
        return new SmartInterfaceData(
                BlockPos.fromLong(compound.getLong("pos")),
                new ResourceLocation(compound.getString("parent")),
                compound.getString("type"),
                compound.getFloat("value"));
    }
}
