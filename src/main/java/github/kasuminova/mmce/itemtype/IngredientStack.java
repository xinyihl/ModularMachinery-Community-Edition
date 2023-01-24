package github.kasuminova.mmce.itemtype;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class IngredientStack {
    public final IngredientType ingredientType;
    public String oreDictName = "";
    public ItemStack itemStack;
    public int count;
    public NBTTagCompound tag = null;

    public IngredientStack(ItemStack itemStack) {
        this.itemStack = itemStack;
        this.count = itemStack.getCount();
        this.ingredientType = IngredientType.ITEMSTACK;
    }

    public IngredientStack(String oreDictName, int count) {
        this.itemStack = null;
        this.oreDictName = oreDictName;
        this.count = count;
        this.ingredientType = IngredientType.ORE_DICT;
    }

    public enum IngredientType {
        ITEMSTACK,
        ORE_DICT
    }
}
