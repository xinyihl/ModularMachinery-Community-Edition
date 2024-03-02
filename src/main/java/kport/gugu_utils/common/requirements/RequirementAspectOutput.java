package kport.gugu_utils.common.requirements;

import hellfirepvp.modularmachinery.common.crafting.helper.ComponentOutputRestrictor;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftCheck;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import static hellfirepvp.modularmachinery.common.modifier.RecipeModifier.applyModifiers;
import kport.gugu_utils.common.requirements.types.RequirementTypeAdapter;
import kport.gugu_utils.jei.components.JEIComponentAspect;
import kport.gugu_utils.GuGuCompoments;
import kport.gugu_utils.GuGuRequirements;
import kport.gugu_utils.common.requirements.basic.RequirementConsumeOnce;
import thaumcraft.api.aspects.Aspect;

import java.util.List;

public class RequirementAspectOutput extends RequirementConsumeOnce<Integer, RequirementAspect.RT> {
    int amount;
    Aspect aspect;

    @SuppressWarnings("unchecked")
    public RequirementAspectOutput(int amount, Aspect aspect) {
        super((RequirementTypeAdapter) GuGuRequirements.REQUIREMENT_TYPE_ASPECT, IOType.OUTPUT);
        this.amount = amount;
        this.aspect = aspect;
    }

    @Override
    public CraftCheck canStartCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, List<ComponentOutputRestrictor> list) {
        if (!isCorrectHatch(component.getComponent())) return CraftCheck.skipComponent();
        return CraftCheck.success();
    }

    @Override
    public RequirementAspectOutput deepClone() {
        return new RequirementAspectOutput(amount, aspect);
    }

    @Override
    public RequirementAspectOutput deepCloneModified(List list) {
        return new RequirementAspectOutput((int) applyModifiers(list, this, amount, false), aspect);
    }

    @Override
    protected boolean isCorrectHatch(MachineComponent component) {
        return component.getComponentType().equals(GuGuCompoments.COMPONENT_ASPECT);
    }

    @Override
    protected RequirementAspect.RT emitConsumptionToken(RecipeCraftingContext context) {
        return new RequirementAspect.RT(amount, aspect);
    }

    @Override
    public JEIComponent provideJEIComponent() {
        return new JEIComponentAspect(amount, aspect);
    }

}
