package kport.modularmagic.common.crafting.requirement;

import com.google.common.collect.Lists;
import hellfirepvp.modularmachinery.common.crafting.helper.*;
import hellfirepvp.modularmachinery.common.lib.RegistriesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.Asyncable;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import kport.modularmagic.common.crafting.component.ComponentAspect;
import kport.modularmagic.common.crafting.requirement.types.ModularMagicRequirements;
import kport.modularmagic.common.crafting.requirement.types.RequirementTypeAspect;
import kport.modularmagic.common.integration.jei.component.JEIComponentAspect;
import kport.modularmagic.common.tile.TileAspectProvider;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.tiles.essentia.TileJarFillable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class RequirementAspect extends ComponentRequirement<AspectList, RequirementTypeAspect> implements Asyncable {

    public int amount;
    public int countAmount;
    public Aspect aspect;

    public RequirementAspect(IOType actionType, int amount, Aspect aspect) {
        super((RequirementTypeAspect) RegistriesMM.REQUIREMENT_TYPE_REGISTRY.getValue(ModularMagicRequirements.KEY_REQUIREMENT_ASPECT), actionType);

        this.amount = amount;
        this.aspect = aspect;
    }

    @Override
    public boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx) {
        MachineComponent<?> cpn = component.getComponent();
        return cpn.getContainerProvider() instanceof TileAspectProvider &&
                cpn.getComponentType() instanceof ComponentAspect &&
                cpn.ioType == getActionType();
    }

    @Override
    public boolean startCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        if (!canStartCrafting(component, context, Lists.newArrayList()).isSuccess())
            return false;

        if (getActionType() == IOType.INPUT) {
            TileAspectProvider provider = (TileAspectProvider) component.getComponent().getContainerProvider();
            return provider.takeFromContainer(this.aspect, this.amount);
        }
        return false;
    }

    @Nonnull
    @Override
    public CraftCheck finishCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        if (getActionType() == IOType.OUTPUT) {
            TileAspectProvider provider = (TileAspectProvider) component.getComponent().getContainerProvider();
            provider.addToContainer(this.aspect, this.amount);
        }
        return CraftCheck.success();
    }

    @Nonnull
    @Override
    public CraftCheck canStartCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, List<ComponentOutputRestrictor> restrictions) {
        TileAspectProvider provider = (TileAspectProvider) component.getComponent().getContainerProvider();
        switch (getActionType()) {
            case INPUT -> {
                if (provider.doesContainerContainAmount(this.aspect, this.amount)) {
                    return CraftCheck.success();
                } else {
                    return CraftCheck.failure("error.modularmachinery.requirement.aspect.less");
                }
            }
            case OUTPUT -> {
                if (ignoreOutputCheck || provider.amount == 0 || provider.aspect == this.aspect && TileJarFillable.CAPACITY >= provider.amount + this.amount)
                    return CraftCheck.success();
                else
                    return CraftCheck.failure("error.modularmachinery.requirement.aspect.out");
            }
        }
        return CraftCheck.skipComponent();
    }

    @Nonnull
    @Override
    public String getMissingComponentErrorMessage(IOType ioType) {
        return "error.modularmachinery.component.invalid";
    }

    @Override
    public RequirementAspect deepCopy() {
        return deepCopyModified(Collections.emptyList());
    }

    @Override
    public RequirementAspect deepCopyModified(List<RecipeModifier> list) {
        int amount = Math.round(RecipeModifier.applyModifiers(list, this, this.amount, false));
        RequirementAspect req = new RequirementAspect(actionType, amount, aspect);
        req.tag = this.tag;
        req.ignoreOutputCheck = this.ignoreOutputCheck;
        return req;
    }

    @Override
    public JEIComponentAspect provideJEIComponent() {
        return new JEIComponentAspect(this);
    }
}
