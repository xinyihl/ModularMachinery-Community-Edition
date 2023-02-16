package github.kasuminova.mmce.common.itemtype;

import net.minecraft.item.ItemStack;

public class ChancedIngredientStack extends IngredientStack {
    public float chance = 1.0F;

    public ChancedIngredientStack(ItemStack itemStack) {
        super(itemStack);
    }

    public ChancedIngredientStack(String oreDictName, int count) {
        super(oreDictName, count);
    }

    public ChancedIngredientStack(ItemStack itemStack, float chance) {
        super(itemStack);
        this.chance = chance;
    }

    public ChancedIngredientStack(String oreDictName, int count, float chance) {
        super(oreDictName, count);
        this.chance = chance;
    }
}
