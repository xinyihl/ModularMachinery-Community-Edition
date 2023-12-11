package hellfirepvp.modularmachinery.common.crafting.requirement.jei;

import github.kasuminova.mmce.common.itemtype.ChancedIngredientStack;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementIngredientArray;
import hellfirepvp.modularmachinery.common.integration.ingredient.IngredientItemStack;
import hellfirepvp.modularmachinery.common.integration.recipe.RecipeLayoutPart;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.OreDictionary;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class JEIComponentIngredientArray extends ComponentRequirement.JEIComponent<IngredientItemStack> {
    public final RequirementIngredientArray requirement;

    public JEIComponentIngredientArray(RequirementIngredientArray requirement) {
        this.requirement = requirement;
    }

    @Override
    public Class<IngredientItemStack> getJEIRequirementClass() {
        return IngredientItemStack.class;
    }

    @Override
    public List<IngredientItemStack> getJEIIORequirements() {
        List<IngredientItemStack> copiedIngredients = new ArrayList<>();
        for (ChancedIngredientStack ingredient : requirement.getIngredients()) {
            switch (ingredient.ingredientType) {
                case ITEMSTACK -> {
                    ItemStack itemStack = ingredient.itemStack;
                    ItemStack copiedStack = ItemUtils.copyStackWithSize(itemStack, itemStack.getCount());
                    copiedIngredients.add(ingredient.asIngredientItemStack(copiedStack));
                }
                case ORE_DICT -> {
                    NonNullList<ItemStack> stacks = OreDictionary.getOres(ingredient.oreDictName);
                    NonNullList<ItemStack> out = NonNullList.create();
                    for (ItemStack oreDictIn : stacks) {
                        if (oreDictIn.getItemDamage() == OreDictionary.WILDCARD_VALUE && !oreDictIn.isItemStackDamageable() && oreDictIn.getItem().getCreativeTab() != null) {
                            oreDictIn.getItem().getSubItems(oreDictIn.getItem().getCreativeTab(), out);
                        } else {
                            out.add(oreDictIn);
                        }
                    }

                    for (ItemStack itemStack : out) {
                        ItemStack copied = itemStack.copy();
                        copied.setCount(ingredient.count);
                        copiedIngredients.add(ingredient.asIngredientItemStack(copied));
                    }
                }
            }
        }

        return copiedIngredients;
    }

    @Override
    public RecipeLayoutPart<IngredientItemStack> getLayoutPart(Point offset) {
        return new RecipeLayoutPart.Item(offset);
    }

    @Override
    public void onJEIHoverTooltip(int slotIndex, boolean input, IngredientItemStack ingredient, List<String> tooltip) {
        tooltip.add("");
        tooltip.add(I18n.format("tooltip.machinery.ingredient_array_input"));
        StringBuilder tooltipBuilder = new StringBuilder();
        for (ChancedIngredientStack stack : requirement.getIngredients()) {
            switch (stack.ingredientType) {
                case ITEMSTACK -> {
                    ItemStack itemStack = stack.itemStack;
                    tooltipBuilder.append(itemStack.getDisplayName());
                }
                case ORE_DICT -> {
                    tooltipBuilder.append(stack.oreDictName);
                }
            }

            if (stack.minCount != stack.maxCount) {
                tooltipBuilder.append(" * ").append(String.format("%d ~ %d", stack.minCount, stack.maxCount));
            } else {
                switch (stack.ingredientType) {
                    case ITEMSTACK -> tooltipBuilder.append(" * ").append(stack.itemStack.getCount());
                    case ORE_DICT -> tooltipBuilder.append(" * ").append(stack.count);
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
                    String chanceStr = chance < 0.0001F ? "< 0.01%" : MiscUtils.formatFloat(chance * 100F, 2) + "%";
                    tooltipBuilder.append(I18n.format(keyChance, chanceStr));
                }

                tooltipBuilder.append(")");
            }

            tooltip.add(tooltipBuilder.toString());
            tooltipBuilder.setLength(0);
        }
    }

}
