package kport.modularmagic.common.integration.jei.render;

import kport.modularmagic.common.integration.jei.ingredient.Impetus;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import thecodex6824.thaumicaugmentation.api.TAItems;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * @author youyihj
 */
public class ImpetusRender implements IIngredientRenderer<Impetus> {
    @Override
    public void render(Minecraft minecraft, int xPosition, int yPosition, @Nullable Impetus ingredient) {
        ItemStack itemStack = new ItemStack(TAItems.AUGMENT_CASTER_RIFT_ENERGY_STORAGE, 1, 0);
        minecraft.getRenderItem().renderItemIntoGUI(itemStack, xPosition, yPosition);
    }

    @Override
    public List<String> getTooltip(Minecraft minecraft, Impetus ingredient, ITooltipFlag tooltipFlag) {
        return Collections.singletonList(I18n.format("info.modularmachinery.impetus", ingredient.getAmount()));
    }
}
