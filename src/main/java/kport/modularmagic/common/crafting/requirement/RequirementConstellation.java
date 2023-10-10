package kport.modularmagic.common.crafting.requirement;

import com.google.common.collect.Lists;
import hellfirepvp.astralsorcery.common.constellation.IConstellation;
import hellfirepvp.modularmachinery.common.crafting.helper.*;
import hellfirepvp.modularmachinery.common.lib.RegistriesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import kport.modularmagic.common.crafting.component.ComponentConstellation;
import kport.modularmagic.common.crafting.requirement.types.ModularMagicRequirements;
import kport.modularmagic.common.crafting.requirement.types.RequirementTypeConstellation;
import kport.modularmagic.common.integration.jei.component.JEIComponentConstellation;
import kport.modularmagic.common.integration.jei.ingredient.Constellation;
import kport.modularmagic.common.tile.TileConstellationProvider;

import javax.annotation.Nonnull;
import java.util.List;

public class RequirementConstellation extends ComponentRequirement<Constellation, RequirementTypeConstellation> {

    public IConstellation constellation;

    public RequirementConstellation(IOType actionType, IConstellation constellation) {
        super((RequirementTypeConstellation) RegistriesMM.REQUIREMENT_TYPE_REGISTRY.getValue(ModularMagicRequirements.KEY_REQUIREMENT_CONSTELLATION), actionType);
        this.constellation = constellation;
    }

    @Override
    public boolean isValidComponent(ProcessingComponent component, RecipeCraftingContext ctx) {
        MachineComponent cpn = component.getComponent();
        return cpn.getContainerProvider() instanceof TileConstellationProvider &&
                cpn.getComponentType() instanceof ComponentConstellation &&
                cpn.ioType == getActionType();
    }

    @Override
    public boolean startCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        return canStartCrafting(component, context, Lists.newArrayList()).isSuccess();
    }

    @Override
    public CraftCheck finishCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        return CraftCheck.success();
    }

    @Nonnull
    @Override
    public CraftCheck canStartCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, List<ComponentOutputRestrictor> restrictions) {
        if (getActionType() == IOType.OUTPUT)
            return CraftCheck.failure("error.modularmachinery.requirement.invalid");

        if (component.getComponent().getContainerProvider() == null || !(component.getComponent().getContainerProvider() instanceof TileConstellationProvider))
            return CraftCheck.failure("error.modularmachinery.requirement.constellation.missingprovider");

        TileConstellationProvider provider = (TileConstellationProvider) component.getComponent().getContainerProvider();
        if (provider.isConstellationInSky(constellation))
            return CraftCheck.success();
        else
            return CraftCheck.failure("error.modularmachinery.requirement.constellation.less");
    }

    @Nonnull
    @Override
    public String getMissingComponentErrorMessage(IOType ioType) {
        return "error.modularmachinery.component.invalid";
    }

    @Override
    public ComponentRequirement deepCopy() {
        return this;
    }

    @Override
    public ComponentRequirement deepCopyModified(List list) {
        return this;
    }

    @Override
    public void startRequirementCheck(ResultChance contextChance, RecipeCraftingContext context) {

    }

    @Override
    public void endRequirementCheck() {

    }

    @Override
    public JEIComponent provideJEIComponent() {
        return new JEIComponentConstellation(this);
    }
}
