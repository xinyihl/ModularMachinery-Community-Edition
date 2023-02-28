package hellfirepvp.modularmachinery.common.block.prop;

import net.minecraft.util.IStringSerializable;

public enum SmartInterfaceTypeEnum implements IStringSerializable {
    NUMBER;

    @Override
    public String getName() {
        return name().toLowerCase();
    }
}
