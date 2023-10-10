package kport.modularmagic.common.integration.jei.component;

import com.google.common.collect.Lists;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.integration.recipe.RecipeLayoutPart;
import kport.modularmagic.common.integration.jei.ingredient.Rainbow;
import kport.modularmagic.common.integration.jei.recipelayoutpart.LayoutRainbow;

import java.awt.*;
import java.util.List;

public class JEIComponentRainbow extends ComponentRequirement.JEIComponent<Rainbow> {

    @Override
    public Class<Rainbow> getJEIRequirementClass() {
        return Rainbow.class;
    }

    @Override
    public List<Rainbow> getJEIIORequirements() {
        return Lists.newArrayList(new Rainbow(true));
    }

    @Override
    public RecipeLayoutPart<Rainbow> getLayoutPart(Point offset) {
        return new LayoutRainbow(offset);
    }

    @Override
    public void onJEIHoverTooltip(int slotIndex, boolean input, Rainbow ingredient, List<String> tooltip) {

    }
}
