package kport.modularmagic.common.integration.jei.component;

import com.google.common.collect.Lists;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.integration.recipe.RecipeLayoutPart;
import kport.modularmagic.common.crafting.requirement.RequirementStarlightOutput;
import kport.modularmagic.common.integration.jei.ingredient.StarlightOutput;
import kport.modularmagic.common.integration.jei.recipelayoutpart.LayoutStarlightOutput;

import java.awt.*;
import java.util.List;

public class JEIComponentStarlightOutput extends ComponentRequirement.JEIComponent<StarlightOutput> {

    private RequirementStarlightOutput requirementStarlightOutput;

    public JEIComponentStarlightOutput(RequirementStarlightOutput requirementStarlightOutput) {
        this.requirementStarlightOutput = requirementStarlightOutput;
    }

    @Override
    public Class<StarlightOutput> getJEIRequirementClass() {
        return StarlightOutput.class;
    }

    @Override
    public List<StarlightOutput> getJEIIORequirements() {
        return Lists.newArrayList(new StarlightOutput(requirementStarlightOutput.starlightAmount));
    }

    @Override
    public RecipeLayoutPart<StarlightOutput> getLayoutPart(Point offset) {
        return new LayoutStarlightOutput(offset);
    }

    @Override
    public void onJEIHoverTooltip(int slotIndex, boolean input, StarlightOutput ingredient, List<String> tooltip) {

    }
}
