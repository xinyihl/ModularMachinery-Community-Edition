/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.crafting.requirement.jei;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.integration.ingredient.IngredientItemStack;
import hellfirepvp.modularmachinery.common.integration.recipe.RecipeLayoutPart;
import hellfirepvp.modularmachinery.common.util.FuelItemHelper;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class is part of the Fracture Mod
 * The complete source code for this mod can be found on github.
 * Class: JEIComponentItem
 * Created by HellFirePvP
 * Date: 08.04.2018 / 12:44
 */
public class JEIComponentItem extends ComponentRequirement.JEIComponent<IngredientItemStack> {

    private final RequirementItem requirement;

    public JEIComponentItem(RequirementItem requirement) {
        this.requirement = requirement;
    }

    @Override
    public Class<IngredientItemStack> getJEIRequirementClass() {
        return IngredientItemStack.class;
    }

    @Override
    public Class<ItemStack> getTrueJEIRequirementClass() {
        return ItemStack.class;
    }

    @Override
    public List<ItemStack> getTrueJEIIORequirements() {
        return Lists.transform(getJEIIORequirements(), ingredientItemStack -> ingredientItemStack == null ? null : ingredientItemStack.stack());
    }

    @Override
    public List<IngredientItemStack> getJEIIORequirements() {
        switch (requirement.requirementType) {
            case ITEMSTACKS -> {
                ItemStack stack = ItemUtils.copyStackWithSize(requirement.required, requirement.required.getCount());
                if (requirement.previewDisplayTag != null) {
                    stack.setTagCompound(requirement.previewDisplayTag);
                } else if (requirement.tag != null) {
                    requirement.previewDisplayTag = requirement.tag.copy();
                    stack.setTagCompound(requirement.previewDisplayTag.copy());
                }
                if (requirement.minAmount != requirement.maxAmount) {
                    stack.setCount(requirement.maxAmount);
                }
                return Collections.singletonList(requirement.asIngredientItemStack(stack));
            }
            case OREDICT -> {
                NonNullList<ItemStack> stacks = OreDictionary.getOres(requirement.oreDictName);
                NonNullList<ItemStack> out = NonNullList.create();
                for (ItemStack oreDictIn : stacks) {
                    if (oreDictIn.getItemDamage() == OreDictionary.WILDCARD_VALUE && !oreDictIn.isItemStackDamageable() && oreDictIn.getItem().getCreativeTab() != null) {
                        oreDictIn.getItem().getSubItems(oreDictIn.getItem().getCreativeTab(), out);
                    } else {
                        out.add(oreDictIn);
                    }
                }
                NonNullList<IngredientItemStack> stacksOut = NonNullList.create();
                for (ItemStack itemStack : out) {
                    ItemStack copy = itemStack.copy();
                    if (requirement.minAmount != requirement.maxAmount) {
                        copy.setCount(requirement.maxAmount);
                    } else {
                        copy.setCount(requirement.oreDictItemAmount);
                    }
                    stacksOut.add(requirement.asIngredientItemStack(copy));
                }
                return stacksOut;
            }
            case FUEL -> {
                return Lists.transform(FuelItemHelper.getFuelItems(), IngredientItemStackTransformer.INSTANCE);
            }
        }
        return new ArrayList<>(0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public RecipeLayoutPart<IngredientItemStack> getLayoutPart(Point offset) {
        return new RecipeLayoutPart.Item(offset);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onJEIHoverTooltip(int slotIndex, boolean input, IngredientItemStack ingredient, List<String> tooltip) {
        if (requirement.requirementType == RequirementItem.ItemRequirementType.FUEL) {
            addFuelTooltip(ingredient, tooltip);
        }
        addChanceTooltip(input, tooltip, requirement.chance);
        addMinMaxTooltip(input, ingredient, tooltip);
    }

    public static void addFuelTooltip(final IngredientItemStack ingredient, final List<String> tooltip) {
        int burn = TileEntityFurnace.getItemBurnTime(ingredient.stack());
        if (burn > 0) {
            tooltip.add(TextFormatting.GRAY + I18n.format("tooltip.machinery.fuel.item", burn));
        }
        tooltip.add(I18n.format("tooltip.machinery.fuel"));
    }

    public static void addMinMaxTooltip(final boolean input, final IngredientItemStack ingredient, final List<String> tooltip) {
        if (ingredient.min() != ingredient.max()) {
            String key = input ? "tooltip.machinery.min_max_amount.input" : "tooltip.machinery.min_max_amount.output";
            tooltip.add(I18n.format(key, ingredient.min(), ingredient.max()));
        }
    }

    public static void addChanceTooltip(final boolean input, final List<String> tooltip, final float chance) {
        if (chance < 1F && chance >= 0F) {
            String keyNever = input ? "tooltip.machinery.chance.in.never" : "tooltip.machinery.chance.out.never";
            String keyChance = input ? "tooltip.machinery.chance.in" : "tooltip.machinery.chance.out";

            if (chance == 0F) {
                tooltip.add(I18n.format(keyNever));
            } else {
                String chanceStr = chance < 0.0001F ? "< 0.01%" : MiscUtils.formatFloat(chance * 100F, 2) + "%";
                tooltip.add(I18n.format(keyChance, chanceStr));
            }
        }
    }

    public static class IngredientItemStackTransformer implements Function<ItemStack, IngredientItemStack> {

        public static final IngredientItemStackTransformer INSTANCE = new IngredientItemStackTransformer();

        private IngredientItemStackTransformer() {
        }

        @Nullable
        @Override
        public IngredientItemStack apply(@Nullable final ItemStack input) {
            return input == null ? null : new IngredientItemStack(input, input.getCount(), input.getCount(), 1.0F);
        }
    }
}
