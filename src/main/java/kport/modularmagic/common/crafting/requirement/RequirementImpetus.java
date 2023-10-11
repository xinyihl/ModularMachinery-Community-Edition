package kport.modularmagic.common.crafting.requirement;

import com.google.common.collect.Lists;
import hellfirepvp.modularmachinery.common.crafting.helper.*;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.Asyncable;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import kport.modularmagic.common.crafting.component.ComponentImpetus;
import kport.modularmagic.common.crafting.requirement.types.RequirementTypeImpetus;
import kport.modularmagic.common.integration.jei.component.JEIComponentImpetus;
import kport.modularmagic.common.integration.jei.ingredient.Impetus;
import kport.modularmagic.common.tile.TileImpetusComponent;
import kport.modularmagic.common.tile.machinecomponent.MachineComponentImpetus;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;

/**
 * @author youyihj
 */
public class RequirementImpetus extends ComponentRequirement<Impetus, RequirementTypeImpetus> implements Asyncable {
    private final int impetus;

    public RequirementImpetus(IOType actionType, int impetus) {
        super(RequirementTypeImpetus.INSTANCE, actionType);
        this.impetus = impetus;
    }

    @Override
    public boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx) {
        MachineComponent<?> cmp = component.getComponent();
        return cmp.getComponentType() instanceof ComponentImpetus &&
                cmp instanceof MachineComponentImpetus &&
                cmp.getIOType() == this.getActionType();
    }

    @Override
    public boolean startCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        if (!this.canStartCrafting(component, context, Lists.newArrayList()).isSuccess()) {
            return false;
        } else if (this.getActionType() == IOType.INPUT) {
            TileImpetusComponent.Input tileComponent = (TileImpetusComponent.Input) component.getComponent().getContainerProvider();
            tileComponent.consumeImpetus(this.impetus);
        }
        return true;
    }

    @Nonnull
    @Override
    public CraftCheck finishCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        if (this.getActionType() == IOType.OUTPUT) {
            TileImpetusComponent.Output tileComponent = (TileImpetusComponent.Output) component.getComponent().getContainerProvider();
            tileComponent.supplyImpetus(this.impetus);
        }
        return CraftCheck.success();
    }

    @Nonnull
    @Override
    public CraftCheck canStartCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, List<ComponentOutputRestrictor> restrictions) {
        switch (this.getActionType()) {
            case INPUT -> {
                TileImpetusComponent.Input tileComponent = (TileImpetusComponent.Input) component.getComponent().getContainerProvider();
                if (tileComponent.hasEnoughImpetus(this.impetus)) {
                    return CraftCheck.success();
                } else {
                    return CraftCheck.failure("error.modularmachinery.impetus.less");
                }
            }
            case OUTPUT -> {
                TileImpetusComponent.Output tileComponent1 = (TileImpetusComponent.Output) component.getComponent().getContainerProvider();
                if (tileComponent1.hasEnoughCapacity(this.impetus)) {
                    return CraftCheck.success();
                } else {
                    return CraftCheck.failure("error.modularmachinery.impetus.space");
                }
            }
            default -> {
                return CraftCheck.failure("?");
            }
        }
    }

    @Override
    public RequirementImpetus deepCopy() {
        return new RequirementImpetus(getActionType(), this.impetus);
    }

    @Override
    public RequirementImpetus deepCopyModified(List<RecipeModifier> modifiers) {
        float newAmount = RecipeModifier.applyModifiers(modifiers, this, this.impetus, false);
        return new RequirementImpetus(getActionType(), MathHelper.ceil(newAmount));
    }

    @Nonnull
    @Override
    public String getMissingComponentErrorMessage(IOType ioType) {
        return "component.missing.impetus." + ioType.name().toLowerCase(Locale.ENGLISH);
    }

    public Impetus getImpetus() {
        return new Impetus(impetus);
    }

    @Override
    public JEIComponent<Impetus> provideJEIComponent() {
        return new JEIComponentImpetus(this);
    }
}
