package github.kasuminova.mmce.common.itemtype;

import github.kasuminova.mmce.common.helper.AdvancedItemChecker;
import github.kasuminova.mmce.common.helper.AdvancedItemModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;

public class IngredientStack {
    public final IngredientType ingredientType;
    public String oreDictName = "";
    public ItemStack itemStack;
    public int count;
    public NBTTagCompound tag = null;
    public AdvancedItemChecker itemChecker = null;
    public List<AdvancedItemModifier> itemModifierList = new ArrayList<>();

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
