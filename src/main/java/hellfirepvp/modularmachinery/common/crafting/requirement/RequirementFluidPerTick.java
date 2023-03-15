package hellfirepvp.modularmachinery.common.crafting.requirement;

import hellfirepvp.modularmachinery.common.crafting.helper.*;
import hellfirepvp.modularmachinery.common.crafting.requirement.jei.JEIComponentHybridFluidPerTick;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementTypeFluidPerTick;
import hellfirepvp.modularmachinery.common.integration.ingredient.HybridFluid;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.CopyHandlerHelper;
import hellfirepvp.modularmachinery.common.util.HybridTank;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import hellfirepvp.modularmachinery.common.util.nbt.NBTMatchingHelper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class RequirementFluidPerTick extends ComponentRequirement.PerTick<HybridFluid, RequirementTypeFluidPerTick> {
    public final HybridFluid required;
    protected final HybridFluid requirementCheck;
    protected NBTTagCompound tagMatch = null, tagDisplay = null;
    protected boolean isSuccess = false;

    public RequirementFluidPerTick(IOType actionType, FluidStack required) {
        super(RequirementTypesMM.REQUIREMENT_FLUID_PERTICK, actionType);
        this.required = new HybridFluid(required);
        this.requirementCheck = this.required.copy();
    }

    @Override
    public boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx) {
        MachineComponent<?> cmp = component.component;
        return cmp.getComponentType().equals(ComponentTypesMM.COMPONENT_FLUID) &&
               cmp instanceof MachineComponent.FluidHatch &&
               cmp.ioType == this.actionType;
    }

    @Override
    public boolean startCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        return canStartCrafting(component, context, new ArrayList<>(0)).isSuccess();
    }

    @Nonnull
    @Override
    public CraftCheck finishCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        return CraftCheck.success();
    }

    @Nonnull
    @Override
    public CraftCheck canStartCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, List<ComponentOutputRestrictor> restrictions) {
        return doFluidIO(component, context, restrictions, false);
    }

    public CraftCheck doFluidIO(ProcessingComponent<?> component, RecipeCraftingContext context, List<ComponentOutputRestrictor> restrictions, boolean doFillOrDrain) {
        HybridTank handler = (HybridTank) component.providedComponent;

        switch (actionType) {
            case INPUT:
                //If it doesn't consume the fluid, we only need to see if it's actually there.
                FluidStack drained = handler.drainInternal(this.requirementCheck.copy().asFluidStack(), doFillOrDrain);
                if (drained == null || !NBTMatchingHelper.matchNBTCompound(this.tagMatch, drained.tag)) {
                    return CraftCheck.failure("craftcheck.failure.fluid.input");
                }
                this.requirementCheck.setAmount(Math.max(this.requirementCheck.getAmount() - drained.amount, 0));
                if (this.requirementCheck.getAmount() <= 0) {
                    return CraftCheck.success();
                }
                return CraftCheck.failure("craftcheck.failure.fluid.input");
            case OUTPUT:
                handler = CopyHandlerHelper.copyTank(handler);

                for (ComponentOutputRestrictor restrictor : restrictions) {
                    if (restrictor instanceof ComponentOutputRestrictor.RestrictionTank) {
                        ComponentOutputRestrictor.RestrictionTank tank = (ComponentOutputRestrictor.RestrictionTank) restrictor;

                        if (tank.exactComponent.equals(component)) {
                            handler.fillInternal(tank.inserted == null ? null : tank.inserted.copy().asFluidStack(), true);
                        }
                    }
                }
                int filled = handler.fillInternal(this.requirementCheck.copy().asFluidStack(), doFillOrDrain); //True or false doesn't really matter tbh
                boolean didFill = filled >= this.requirementCheck.getAmount();
                if (didFill) {
                    context.addRestriction(new ComponentOutputRestrictor.RestrictionTank(this.requirementCheck.copy(), component));
                    return CraftCheck.success();
                }
                return CraftCheck.failure("craftcheck.failure.fluid.output.space");
        }
        return CraftCheck.skipComponent();
    }

    @Override
    public RequirementFluidPerTick deepCopy() {
        RequirementFluidPerTick fluid = new RequirementFluidPerTick(actionType, required.asFluidStack());
        fluid.tagMatch = tagMatch;
        fluid.tagDisplay = tagDisplay;
        return fluid;
    }

    @Override
    public RequirementFluidPerTick deepCopyModified(List<RecipeModifier> modifiers) {
        HybridFluid hybrid = this.required.copy();
        hybrid.setAmount(Math.round(RecipeModifier.applyModifiers(modifiers, this, hybrid.getAmount(), false)));
        RequirementFluidPerTick fluid = new RequirementFluidPerTick(actionType, hybrid.asFluidStack());

        fluid.tagMatch = tagMatch;
        fluid.tagDisplay = tagDisplay;
        return fluid;
    }

    @Override
    public void startRequirementCheck(ResultChance contextChance, RecipeCraftingContext context) {
        this.requirementCheck.setAmount(Math.round(RecipeModifier.applyModifiers(context, this, this.required.getAmount(), false)));
    }

    @Override
    public void endRequirementCheck() {
    }

    @Nonnull
    @Override
    public String getMissingComponentErrorMessage(IOType ioType) {
        ResourceLocation compKey = this.requirementType.getRegistryName();
        return String.format("component.missing.%s.%s.%s",
                compKey.getNamespace(), compKey.getPath(), ioType.name().toLowerCase());
    }

    @Override
    public JEIComponent<HybridFluid> provideJEIComponent() {
        return new JEIComponentHybridFluidPerTick(this);
    }

    @Override
    public void startIOTick(RecipeCraftingContext context, float durationMultiplier) {
        this.requirementCheck.setAmount(Math.round(RecipeModifier.applyModifiers(context, this, this.required.getAmount(), false) * durationMultiplier));
    }

    @Nonnull
    @Override
    public CraftCheck resetIOTick(RecipeCraftingContext context) {
        switch (actionType) {
            case INPUT:
                return isSuccess ? CraftCheck.success() : CraftCheck.failure("craftcheck.failure.fluid.input");
            case OUTPUT:
                return isSuccess ? CraftCheck.success() : CraftCheck.failure("craftcheck.failure.fluid.output.space");
        }
        return CraftCheck.skipComponent();
    }

    @Nonnull
    @Override
    public CraftCheck doIOTick(ProcessingComponent<?> component, RecipeCraftingContext context) {
        CraftCheck craftCheck = doFluidIO(component, context, new ArrayList<>(0), true);
        this.isSuccess = craftCheck.isSuccess();

        return craftCheck;
    }
}
