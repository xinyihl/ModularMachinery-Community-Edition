package hellfirepvp.modularmachinery.common.crafting.requirement.jei;

import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluidPerTick;
import hellfirepvp.modularmachinery.common.integration.ingredient.HybridFluid;
import hellfirepvp.modularmachinery.common.integration.recipe.RecipeLayoutPart;
import net.minecraft.client.resources.I18n;

import java.awt.*;
import java.util.Collections;
import java.util.List;

public class JEIComponentHybridFluidPerTick extends ComponentRequirement.JEIComponent<HybridFluid> {
    private final RequirementFluidPerTick requirement;

    public JEIComponentHybridFluidPerTick(RequirementFluidPerTick requirement) {
        this.requirement = requirement;
    }

    @Override
    public Class<HybridFluid> getJEIRequirementClass() {
        return HybridFluid.class;
    }

    @Override
    public List<HybridFluid> getJEIIORequirements() {
        return Collections.singletonList(requirement.required);
    }

    @Override
    public RecipeLayoutPart<HybridFluid> getLayoutPart(Point offset) {
        return new RecipeLayoutPart.Tank(offset);
    }

    @Override
    public void onJEIHoverTooltip(int slotIndex, boolean input, HybridFluid ingredient, List<String> tooltip) {
        tooltip.add(I18n.format("tooltip.fluid_pertick", ingredient.getAmount()));
    }
}
