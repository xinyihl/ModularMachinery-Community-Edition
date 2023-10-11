package kport.modularmagic.common.crafting.requirement;

import hellfirepvp.modularmachinery.common.crafting.helper.*;
import hellfirepvp.modularmachinery.common.lib.RegistriesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import kport.modularmagic.common.crafting.component.ModularMagicComponents;
import kport.modularmagic.common.crafting.requirement.types.ModularMagicRequirements;
import kport.modularmagic.common.crafting.requirement.types.RequirementTypeLifeEssence;
import kport.modularmagic.common.integration.jei.component.JEIComponentLifeEssence;
import kport.modularmagic.common.integration.jei.ingredient.LifeEssence;
import kport.modularmagic.common.tile.TileLifeEssenceProvider;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class RequirementLifeEssence extends ComponentRequirement.PerTick<LifeEssence, RequirementTypeLifeEssence> {

    public int essenceAmount;
    public boolean isPerTick;

    public RequirementLifeEssence(IOType actionType, int essenceAmount, boolean perTick) {
        super((RequirementTypeLifeEssence) RegistriesMM.REQUIREMENT_TYPE_REGISTRY.getValue(ModularMagicRequirements.KEY_REQUIREMENT_LIFE_ESSENCE), actionType);

        this.essenceAmount = essenceAmount;
        this.isPerTick = perTick;
    }

    @Override
    public boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx) {
        MachineComponent<?> cpn = component.getComponent();
        return cpn.getContainerProvider() instanceof TileLifeEssenceProvider &&
                cpn.getComponentType().equals(RegistriesMM.COMPONENT_TYPE_REGISTRY.getValue(ModularMagicComponents.KEY_COMPONENT_LIFE_ESSENCE)) &&
                cpn.ioType == getActionType();
    }

    @Override
    public boolean startCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        if (!canStartCrafting(component, context, Collections.emptyList()).isSuccess()) {
            return false;
        }

        TileLifeEssenceProvider essenceProvider = (TileLifeEssenceProvider) component.getComponent().getContainerProvider();

        if (getActionType() == IOType.INPUT && !isPerTick) {
            essenceProvider.removeLifeEssenceCache(essenceAmount);
        }
        return true;
    }

    @Nonnull
    @Override
    public CraftCheck finishCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        if (getActionType() == IOType.OUTPUT && !isPerTick) {
            TileLifeEssenceProvider essenceProvider = (TileLifeEssenceProvider) component.getComponent().getContainerProvider();
            essenceProvider.addLifeEssenceCache(essenceAmount);
        }
        return CraftCheck.success();
    }

    @Nonnull
    @Override
    public CraftCheck canStartCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, List<ComponentOutputRestrictor> restrictions) {
        if (!isValidComponent(component, context)) {
            return CraftCheck.failure(getMissingComponentErrorMessage(getActionType()));
        }

        TileLifeEssenceProvider essenceProvider = (TileLifeEssenceProvider) component.getComponent().getContainerProvider();

        if (essenceProvider.getSoulNetwork() == null) {
            return CraftCheck.failure("error.modularmachinery.requirement.lifeessence.orb");
        }

        switch (getActionType()) {
            case INPUT -> {
                if (essenceProvider.getLifeEssenceCache() >= essenceAmount) {
                    return CraftCheck.success();
                } else {
                    return CraftCheck.failure("error.modularmachinery.requirement.lifeessence.lp");
                }
            }
            case OUTPUT -> {
                return CraftCheck.success();
            }
        }
        return CraftCheck.failure("error.modularmachinery.requirement.lifeessence");
    }

    @Nonnull
    @Override
    public String getMissingComponentErrorMessage(IOType ioType) {
        return "error.modularmachinery.component.invalid";
    }

    @Nonnull
    @Override
    public CraftCheck doIOTick(ProcessingComponent<?> component, RecipeCraftingContext context) {
        if (!isPerTick) {
            return CraftCheck.success();
        }

        TileLifeEssenceProvider essenceProvider = (TileLifeEssenceProvider) component.getComponent().getContainerProvider();
        switch (getActionType()) {
            case INPUT -> {
                if (essenceProvider.getLifeEssenceCache() >= essenceAmount) {
                    essenceProvider.removeLifeEssenceCache(essenceAmount);
                    return CraftCheck.success();
                } else {
                    return CraftCheck.failure("error.modularmachinery.requirement.lifeessence.lp");
                }
            }
            case OUTPUT -> {
                essenceProvider.addLifeEssenceCache(essenceAmount);
                return CraftCheck.success();
            }
        }
        return CraftCheck.skipComponent();
    }

    @Override
    public RequirementLifeEssence deepCopy() {
        return deepCopyModified(Collections.emptyList());
    }

    @Override
    public RequirementLifeEssence deepCopyModified(List<RecipeModifier> list) {
        return this;
    }

    @Override
    public JEIComponentLifeEssence provideJEIComponent() {
        return new JEIComponentLifeEssence(this);
    }
}
