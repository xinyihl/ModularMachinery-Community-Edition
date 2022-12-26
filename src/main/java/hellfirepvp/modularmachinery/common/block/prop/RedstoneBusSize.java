package hellfirepvp.modularmachinery.common.block.prop;

import net.minecraft.util.IStringSerializable;

public enum RedstoneBusSize implements IStringSerializable {

    TINY(1);

    private final int slots;

    private final int defaultConfigSize;

    RedstoneBusSize(int defaultConfigSize) {
        this.defaultConfigSize = defaultConfigSize;
        this.slots = this.defaultConfigSize;
    }

    public int getSlotCount() {
        return slots;
    }

    @Override
    public String getName() {
        return name().toLowerCase();
    }

}
