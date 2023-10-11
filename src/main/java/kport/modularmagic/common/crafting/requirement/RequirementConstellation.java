package kport.modularmagic.common.crafting.requirement;

import hellfirepvp.astralsorcery.common.constellation.IConstellation;
import hellfirepvp.modularmachinery.common.crafting.helper.*;
import hellfirepvp.modularmachinery.common.lib.RegistriesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.Asyncable;
import kport.modularmagic.common.crafting.component.ComponentConstellation;
import kport.modularmagic.common.crafting.requirement.types.ModularMagicRequirements;
import kport.modularmagic.common.crafting.requirement.types.RequirementTypeConstellation;
import kport.modularmagic.common.integration.jei.component.JEIComponentConstellation;
import kport.modularmagic.common.integration.jei.ingredient.Constellation;
import kport.modularmagic.common.tile.TileConstellationProvider;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class RequirementConstellation extends ComponentRequirement<Constellation, RequirementTypeConstellation> implements Asyncable {

    public IConstellation constellation;

    public RequirementConstellation(IOType actionType, IConstellation constellation) {
        super((RequirementTypeConstellation) RegistriesMM.REQUIREMENT_TYPE_REGISTRY.getValue(ModularMagicRequirements.KEY_REQUIREMENT_CONSTELLATION), actionType);
        this.constellation = constellation;
    }

    @Override
    public boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx) {
        MachineComponent<?> cpn = component.getComponent();
        return cpn.getContainerProvider() instanceof TileConstellationProvider &&
                cpn.getComponentType() instanceof ComponentConstellation &&
                cpn.ioType == getActionType();
    }

    @Nonnull
    @Override
    public CraftCheck canStartCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, List<ComponentOutputRestrictor> restrictions) {
        if (getActionType() == IOType.OUTPUT) {
            return CraftCheck.failure("error.modularmachinery.requirement.invalid");
        }
        if (component.getComponent().getContainerProvider() == null || !(component.getComponent().getContainerProvider() instanceof final TileConstellationProvider provider)) {
            return CraftCheck.failure("error.modularmachinery.requirement.constellation.missingprovider");
        }
        if (!provider.isConstellationInSky(constellation)) {
            return CraftCheck.failure("error.modularmachinery.requirement.constellation.less");
        }
        return CraftCheck.success();
    }

    @Nonnull
    @Override
    public String getMissingComponentErrorMessage(IOType ioType) {
        return "error.modularmachinery.component.invalid";
    }

    @Override
    public RequirementConstellation deepCopy() {
        return deepCopyModified(Collections.emptyList());
    }

    @Override
    public RequirementConstellation deepCopyModified(List<RecipeModifier> list) {
        return new RequirementConstellation(actionType, constellation);
    }

    @Override
    public JEIComponentConstellation provideJEIComponent() {
        return new JEIComponentConstellation(this);
    }
}
