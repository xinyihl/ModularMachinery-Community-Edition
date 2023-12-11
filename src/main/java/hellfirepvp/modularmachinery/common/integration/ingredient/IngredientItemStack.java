package hellfirepvp.modularmachinery.common.integration.ingredient;

import com.github.bsideup.jabel.Desugar;
import net.minecraft.item.ItemStack;

import java.util.Objects;

@Desugar
public final class IngredientItemStack {
    private final ItemStack stack;
    private final int min;
    private final int max;
    private final float chance;

    public IngredientItemStack(ItemStack stack, int min, int max, float chance) {
        this.stack = stack;
        this.min = min;
        this.max = max;
        this.chance = chance;
    }

    public IngredientItemStack copy() {
        return new IngredientItemStack(stack.copy(), min, max, chance);
    }

    public ItemStack stack() {
        return stack;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    public float chance() {
        return chance;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (IngredientItemStack) obj;
        return Objects.equals(this.stack, that.stack) &&
                this.min == that.min &&
                this.max == that.max &&
                Float.floatToIntBits(this.chance) == Float.floatToIntBits(that.chance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stack, min, max, chance);
    }

    @Override
    public String toString() {
        return "IngredientItemStack[" +
                "stack=" + stack + ", " +
                "min=" + min + ", " +
                "max=" + max + ", " +
                "chance=" + chance + ']';
    }

}
