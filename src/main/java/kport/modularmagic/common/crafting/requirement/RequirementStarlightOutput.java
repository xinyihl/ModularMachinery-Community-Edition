package kport.modularmagic.common.crafting.requirement;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.crafting.helper.*;
import hellfirepvp.modularmachinery.common.lib.RegistriesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.Asyncable;
import kport.modularmagic.common.crafting.component.ComponentStarLightOutput;
import kport.modularmagic.common.crafting.requirement.types.ModularMagicRequirements;
import kport.modularmagic.common.crafting.requirement.types.RequirementTypeStarlightOutput;
import kport.modularmagic.common.integration.jei.component.JEIComponentStarlightOutput;
import kport.modularmagic.common.integration.jei.ingredient.StarlightOutput;
import kport.modularmagic.common.tile.TileStarlightOutput;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class RequirementStarlightOutput extends ComponentRequirement.PerTick<StarlightOutput, RequirementTypeStarlightOutput> implements Asyncable {

    public float starlightAmount;

    public RequirementStarlightOutput(IOType actionType, float starlightAmount) {
        super((RequirementTypeStarlightOutput) RegistriesMM.REQUIREMENT_TYPE_REGISTRY.getValue(ModularMagicRequirements.KEY_REQUIREMENT_STARLIGHT_OUTPUT), actionType);
        this.starlightAmount = starlightAmount;
    }

    @Override
    public boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx) {
        MachineComponent<?> cpn = component.getComponent();
        return cpn.getContainerProvider() instanceof TileStarlightOutput &&
                cpn.getComponentType() instanceof ComponentStarLightOutput &&
                cpn.ioType == getActionType();
    }

    @Nonnull
    @Override
    public CraftCheck doIOTick(ProcessingComponent<?> component, RecipeCraftingContext context) {
        if (getActionType() == IOType.OUTPUT) {
            ModularMachinery.EXECUTE_MANAGER.addSyncTask(() ->
                    ((TileStarlightOutput) component.getComponent().getContainerProvider())
                            .setStarlightProduced(this.starlightAmount / 4000));
        }
        return CraftCheck.success();
    }

    @Nonnull
    @Override
    public CraftCheck canStartCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, List<ComponentOutputRestrictor> restrictions) {
        return CraftCheck.success();
    }

    @Nonnull
    @Override
    public String getMissingComponentErrorMessage(IOType ioType) {
        return "error.modularmachinery.component.invalid";
    }

    @Override
    public RequirementStarlightOutput deepCopy() {
        return deepCopyModified(Collections.emptyList());
    }

    @Override
    public RequirementStarlightOutput deepCopyModified(List<RecipeModifier> list) {
        float starlightAmount = RecipeModifier.applyModifiers(list, this, this.starlightAmount, false);
        return new RequirementStarlightOutput(actionType, starlightAmount);
    }

    @Override
    public JEIComponentStarlightOutput provideJEIComponent() {
        return new JEIComponentStarlightOutput(this);
    }
}
