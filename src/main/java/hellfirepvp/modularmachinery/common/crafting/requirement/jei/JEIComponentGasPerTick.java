package hellfirepvp.modularmachinery.common.crafting.requirement.jei;

import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGasPerTick;
import hellfirepvp.modularmachinery.common.integration.ingredient.HybridFluid;
import hellfirepvp.modularmachinery.common.integration.ingredient.HybridFluidGas;
import hellfirepvp.modularmachinery.common.integration.recipe.RecipeLayoutPart;
import hellfirepvp.modularmachinery.common.machine.IOType;
import net.minecraft.client.resources.I18n;

import java.awt.*;
import java.util.Collections;
import java.util.List;

public class JEIComponentGasPerTick extends ComponentRequirement.JEIComponent<HybridFluid> {
    private final RequirementGasPerTick requirement;

    public JEIComponentGasPerTick(RequirementGasPerTick requirement) {
        this.requirement = requirement;
    }

    @Override
    public Class<HybridFluid> getJEIRequirementClass() {
        return HybridFluid.class;
    }

    @Override
    public List<HybridFluid> getJEIIORequirements() {
        return Collections.singletonList(new HybridFluidGas(requirement.required));
    }

    @Override
    public RecipeLayoutPart<HybridFluid> getLayoutPart(Point offset) {
        return new RecipeLayoutPart.Tank(offset);
    }

    @Override
    public void onJEIHoverTooltip(int slotIndex, boolean input, HybridFluid ingredient, List<String> tooltip) {
        IOType ioType = requirement.getActionType();
        if (ioType == IOType.INPUT) {
            tooltip.add(I18n.format("tooltip.fluid_pertick.in", ingredient.getAmount()));
        } else if (ioType == IOType.OUTPUT) {
            tooltip.add(I18n.format("tooltip.fluid_pertick.out", ingredient.getAmount()));
        }
    }
}
