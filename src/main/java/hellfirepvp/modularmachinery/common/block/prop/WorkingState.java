package hellfirepvp.modularmachinery.common.block.prop;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;

public enum WorkingState implements IStringSerializable {
    IDLE,
    WORKING,
    ;

    @Nonnull
    @Override
    public String getName() {
        return name().toLowerCase();
    }
}
