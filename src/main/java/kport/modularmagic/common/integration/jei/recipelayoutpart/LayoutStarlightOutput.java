package kport.modularmagic.common.integration.jei.recipelayoutpart;

import hellfirepvp.modularmachinery.common.integration.recipe.RecipeLayoutPart;
import kport.modularmagic.common.integration.jei.ingredient.StarlightOutput;
import kport.modularmagic.common.integration.jei.render.StarlightOutputRenderer;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.Minecraft;

import java.awt.*;

public class LayoutStarlightOutput extends RecipeLayoutPart<StarlightOutput> {

    public LayoutStarlightOutput(Point offset) {
        super(offset);
    }

    @Override
    public int getComponentWidth() {
        return 16;
    }

    @Override
    public int getComponentHeight() {
        return 16;
    }

    @Override
    public Class<StarlightOutput> getLayoutTypeClass() {
        return StarlightOutput.class;
    }

    @Override
    public IIngredientRenderer<StarlightOutput> provideIngredientRenderer() {
        return new StarlightOutputRenderer();
    }

    @Override
    public int getRendererPaddingX() {
        return 0;
    }

    @Override
    public int getRendererPaddingY() {
        return 0;
    }

    @Override
    public int getMaxHorizontalCount() {
        return 1;
    }

    @Override
    public int getComponentHorizontalGap() {
        return 0;
    }

    @Override
    public int getComponentVerticalGap() {
        return 0;
    }

    @Override
    public int getComponentHorizontalSortingOrder() {
        return 0;
    }

    @Override
    public boolean canBeScaled() {
        return false;
    }

    @Override
    public void drawBackground(Minecraft mc) {

    }
}
