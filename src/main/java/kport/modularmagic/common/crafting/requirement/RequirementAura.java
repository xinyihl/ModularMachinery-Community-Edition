package kport.modularmagic.common.crafting.requirement;

import com.google.common.collect.Lists;
import hellfirepvp.modularmachinery.common.crafting.helper.*;
import hellfirepvp.modularmachinery.common.lib.RegistriesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.Asyncable;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import kport.modularmagic.common.crafting.component.ComponentAura;
import kport.modularmagic.common.crafting.requirement.types.ModularMagicRequirements;
import kport.modularmagic.common.crafting.requirement.types.RequirementTypeAura;
import kport.modularmagic.common.integration.jei.component.JEIComponentAura;
import kport.modularmagic.common.integration.jei.ingredient.Aura;
import kport.modularmagic.common.tile.TileAuraProvider;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class RequirementAura extends ComponentRequirement<Aura, RequirementTypeAura> implements Asyncable {

    public Aura aura;
    public int max;
    public int min;

    public RequirementAura(IOType type, Aura aura, int max, int min) {
        super((RequirementTypeAura) RegistriesMM.REQUIREMENT_TYPE_REGISTRY.getValue(ModularMagicRequirements.KEY_REQUIREMENT_AURA), type);
        this.aura = aura;
        this.max = max;
        this.min = min;
    }

    @Override
    public boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx) {
        MachineComponent<?> cpn = component.getComponent();
        return cpn.getContainerProvider() instanceof TileAuraProvider &&
                cpn.getComponentType() instanceof ComponentAura &&
                cpn.ioType == getActionType();
    }

    @Override
    public boolean startCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        if (!canStartCrafting(component, context, Lists.newArrayList()).isSuccess())
            return false;

        if (getActionType() == IOType.INPUT) {
            TileAuraProvider provider = (TileAuraProvider) component.getComponent().getContainerProvider();
            provider.removeAura(this.aura);
        }
        return true;
    }

    @Nonnull
    @Override
    public CraftCheck finishCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        if (getActionType() == IOType.OUTPUT) {
            TileAuraProvider provider = (TileAuraProvider) component.getComponent().getContainerProvider();
            provider.addAura(this.aura);
        }
        return CraftCheck.success();
    }

    @Nonnull
    @Override
    public CraftCheck canStartCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, List<ComponentOutputRestrictor> restrictions) {
        TileAuraProvider provider = (TileAuraProvider) component.getComponent().getContainerProvider();
        if (provider.getAura().getType() != this.aura.getType()) {
            return CraftCheck.failure("error.modularmachinery.requirement.aura.invalid");
        }

        switch (getActionType()) {
            case INPUT:
                if (provider.getAura().getAmount() - this.aura.getAmount() < this.min)
                    return CraftCheck.failure("error.modularmachinery.requirement.aura.less");

            case OUTPUT:
                if (ignoreOutputCheck || provider.getAura().getAmount() + this.aura.getAmount() > this.max)
                    return CraftCheck.failure("error.modularmachinery.requirement.aura.more");
        }
        return CraftCheck.success();
    }

    @Override
    public ComponentRequirement<Aura, RequirementTypeAura> deepCopy() {
        return deepCopyModified(Collections.emptyList());
    }

    @Override
    public ComponentRequirement<Aura, RequirementTypeAura> deepCopyModified(List<RecipeModifier> modifiers) {
        Aura aura = new Aura(Math.round(
                RecipeModifier.applyModifiers(modifiers, this, this.aura.getAmount(), false)),
                this.aura.getType()
        );
        RequirementAura req = new RequirementAura(actionType, aura, this.max, this.min);
        req.tag = this.tag;
        req.ignoreOutputCheck = this.ignoreOutputCheck;
        return req;
    }

    @Nonnull
    @Override
    public String getMissingComponentErrorMessage(IOType ioType) {
        return "error.modularmachinery.component.invalid";
    }

    @Override
    public JEIComponent<Aura> provideJEIComponent() {
        return new JEIComponentAura(this);
    }
}
