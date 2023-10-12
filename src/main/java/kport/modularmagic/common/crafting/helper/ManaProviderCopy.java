package kport.modularmagic.common.crafting.helper;

import kport.modularmagic.common.tile.TileManaProvider;
import net.minecraft.util.math.MathHelper;

public class ManaProviderCopy {
    private final TileManaProvider original;

    private int mana = 0;

    public ManaProviderCopy(final TileManaProvider original) {
        this.original = original;
    }

    public int getCurrentMana() {
        return mana;
    }

    public void recieveMana(int amount) {
        mana = MathHelper.clamp(mana + amount, 0, getManaCapacity());
    }

    public void reduceMana(int amount) {
        mana = MathHelper.clamp(mana - amount, 0, getManaCapacity());
    }

    public int getManaCapacity() {
        return original.getManaCapacity();
    }
}
