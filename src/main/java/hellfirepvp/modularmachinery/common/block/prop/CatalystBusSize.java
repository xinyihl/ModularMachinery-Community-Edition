package hellfirepvp.modularmachinery.common.block.prop;

import net.minecraft.util.IStringSerializable;

public enum CatalystBusSize implements IStringSerializable {

    catalyst00(32),
    catalyst01(32),
    catalyst02(32),
    catalyst03(32),
    catalyst04(32),
    catalyst05(32),
    catalyst06(32),
    catalyst07(32),
    catalyst08(32),
    catalyst09(32),
    catalyst10(32),
    catalyst11(32),
    catalyst12(32),
    catalyst13(32),
    catalyst14(32),
    catalyst15(32);

    private int slots;

    private final int defaultConfigSize;

    private CatalystBusSize(int defaultConfigSize) {
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
