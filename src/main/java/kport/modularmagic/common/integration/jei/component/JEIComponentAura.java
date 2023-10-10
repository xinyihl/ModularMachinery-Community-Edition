package kport.modularmagic.common.integration.jei.component;

import com.google.common.collect.Lists;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.integration.recipe.RecipeLayoutPart;
import kport.modularmagic.common.crafting.requirement.RequirementAura;
import kport.modularmagic.common.integration.jei.ingredient.Aura;
import kport.modularmagic.common.integration.jei.recipelayoutpart.LayoutAura;

import java.awt.*;
import java.util.List;

public class JEIComponentAura extends ComponentRequirement.JEIComponent<Aura> {

    private RequirementAura requirementAura;

    public JEIComponentAura(RequirementAura requirementAura) {
        this.requirementAura = requirementAura;
    }

    @Override
    public Class<Aura> getJEIRequirementClass() {
        return Aura.class;
    }

    @Override
    public List<Aura> getJEIIORequirements() {
        return Lists.newArrayList(requirementAura.aura);
    }

    @Override
    public RecipeLayoutPart<Aura> getLayoutPart(Point offset) {
        return new LayoutAura(offset);
    }

    @Override
    public void onJEIHoverTooltip(int slotIndex, boolean input, Aura ingredient, List<String> tooltip) {

    }
}
