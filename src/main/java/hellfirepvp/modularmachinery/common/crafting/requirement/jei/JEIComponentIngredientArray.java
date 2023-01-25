package hellfirepvp.modularmachinery.common.crafting.requirement.jei;

import com.google.common.collect.Lists;
import github.kasuminova.mmce.itemtype.ChancedIngredientStack;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementIngredientArray;
import hellfirepvp.modularmachinery.common.integration.recipe.RecipeLayoutPart;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.oredict.OreDictionary;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class JEIComponentIngredientArray extends ComponentRequirement.JEIComponent<ItemStack> {
    public final RequirementIngredientArray requirement;

    public JEIComponentIngredientArray(RequirementIngredientArray requirement) {
        this.requirement = requirement;
    }

    @Override
    public Class<ItemStack> getJEIRequirementClass() {
        return ItemStack.class;
    }

    @Override
    public List<ItemStack> getJEIIORequirements() {
        ArrayList<ItemStack> copiedItemArray = new ArrayList<>(requirement.itemArray.size());
        for (ChancedIngredientStack stack : requirement.itemArray) {
            switch (stack.ingredientType) {
                case ITEMSTACK: {
                    ItemStack itemStack = stack.itemStack;
                    ItemStack copiedStack = ItemUtils.copyStackWithSize(itemStack, itemStack.getCount());
                    copiedItemArray.add(copiedStack);
                    break;
                }
                case ORE_DICT: {
                    NonNullList<ItemStack> stacks = OreDictionary.getOres(stack.oreDictName);
                    NonNullList<ItemStack> out = NonNullList.create();
                    for (ItemStack oreDictIn : stacks) {
                        if (oreDictIn.getItemDamage() == OreDictionary.WILDCARD_VALUE && !oreDictIn.isItemStackDamageable() && oreDictIn.getItem().getCreativeTab() != null) {
                            oreDictIn.getItem().getSubItems(oreDictIn.getItem().getCreativeTab(), out);
                        } else {
                            out.add(oreDictIn);
                        }
                    }

                    for (ItemStack itemStack : out) {
                        ItemStack copy = itemStack.copy();
                        copy.setCount(stack.count);
                        copiedItemArray.add(copy);
                    }
                    break;
                }
            }
        }

        return Lists.newArrayList(copiedItemArray);
    }

    @Override
    public RecipeLayoutPart<ItemStack> getLayoutPart(Point offset) {
        return new RecipeLayoutPart.Item(offset);
    }

    @Override
    public void onJEIHoverTooltip(int slotIndex, boolean input, ItemStack ingredient, List<String> tooltip) {
        tooltip.add("");
        tooltip.add(I18n.format("tooltip.machinery.ingredient_array_input"));
        for (ChancedIngredientStack stack : requirement.itemArray) {
            StringBuilder tooltipBuilder = new StringBuilder(10);
            switch (stack.ingredientType) {
                case ITEMSTACK: {
                    ItemStack itemStack = stack.itemStack;
                    tooltipBuilder.append(itemStack.getDisplayName()).append(" * ").append(itemStack.getCount());
                    break;
                }
                case ORE_DICT: {
                    tooltipBuilder.append(stack.oreDictName).append(" * ").append(stack.count);
                    break;
                }
            }

            float chance = stack.chance * requirement.chance;
            if (chance < 1F && chance >= 0F) {
                tooltipBuilder.append(" (");

                String keyNever = input ? "tooltip.machinery.chance.in.never" : "tooltip.machinery.chance.out.never";
                String keyChance = input ? "tooltip.machinery.chance.in" : "tooltip.machinery.chance.out";

                if (chance == 0F) {
                    tooltipBuilder.append(I18n.format(keyNever));
                } else {
                    String chanceStr = String.valueOf(MathHelper.floor(chance * 100F));

                    if (chance < 0.01F) {
                        chanceStr = "< 1";
                    }
                    chanceStr += "%";
                    tooltipBuilder.append(I18n.format(keyChance, chanceStr));
                }

                tooltipBuilder.append(")");
            }

            tooltip.add(tooltipBuilder.toString());
        }
    }

}
