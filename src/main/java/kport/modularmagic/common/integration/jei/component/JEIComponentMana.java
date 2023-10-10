package kport.modularmagic.common.integration.jei.component;

import com.google.common.collect.Lists;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.integration.recipe.RecipeLayoutPart;
import kport.modularmagic.common.crafting.requirement.RequirementMana;
import kport.modularmagic.common.integration.jei.ingredient.Mana;
import kport.modularmagic.common.integration.jei.recipelayoutpart.LayoutMana;

import java.awt.*;
import java.util.List;

public class JEIComponentMana extends ComponentRequirement.JEIComponent<Mana> {

    private RequirementMana requirementMana;

    public JEIComponentMana(RequirementMana requirementMana) {
        this.requirementMana = requirementMana;
    }

    @Override
    public Class<Mana> getJEIRequirementClass() {
        return Mana.class;
    }

    @Override
    public List<Mana> getJEIIORequirements() {
        Mana mana = new Mana(requirementMana.manaAmount);
        return Lists.newArrayList(mana);
    }

    @Override
    public RecipeLayoutPart<Mana> getLayoutPart(Point offset) {
        return new LayoutMana(offset);
    }

    @Override
    public void onJEIHoverTooltip(int slotIndex, boolean input, Mana ingredient, List<String> tooltip) {

    }


}
