package hellfirepvp.modularmachinery.common.block.prop;

import net.minecraft.util.IStringSerializable;

public enum SmartInterfaceType implements IStringSerializable {
    NUMBER,
    STRING;

    @Override
    public String getName() {
        return name().toLowerCase();
    }
}
