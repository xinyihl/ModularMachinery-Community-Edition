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
import hellfirepvp.modularmachinery.common.util.HybridFluidUtils;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class RequirementFluidPerTick extends ComponentRequirement.PerTick<HybridFluid, RequirementTypeFluidPerTick> implements
        ComponentRequirement.Parallelizable,
        ComponentRequirement.MultiComponent {

    public final FluidStack required;

    protected final HybridFluid requirementCheck;
    protected NBTTagCompound tagMatch = null, tagDisplay = null;
    protected boolean isSuccess = false;

    private int parallelism = 1;
    private boolean parallelizeUnaffected = false;

    public RequirementFluidPerTick(IOType actionType, FluidStack required) {
        super(RequirementTypesMM.REQUIREMENT_FLUID_PERTICK, actionType);
        this.required = required;
        this.requirementCheck = new HybridFluid(required);
    }

    @Override
    public boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx) {
        MachineComponent<?> cmp = component.component();
        return cmp.getComponentType().equals(ComponentTypesMM.COMPONENT_FLUID) &&
                cmp instanceof MachineComponent.FluidHatch &&
                cmp.ioType == this.actionType;
    }

    @Override
    public RequirementFluidPerTick deepCopy() {
        return deepCopyModified(Collections.emptyList());
    }

    @Override
    public RequirementFluidPerTick deepCopyModified(List<RecipeModifier> modifiers) {
        FluidStack stack = this.required.copy();
        stack.amount = ((int) Math.round(RecipeModifier.applyModifiers(modifiers, this, (double) stack.amount, false)));
        RequirementFluidPerTick fluid = new RequirementFluidPerTick(actionType, stack);
        fluid.setTag(getTag());
        fluid.tagMatch = tagMatch;
        fluid.tagDisplay = tagDisplay;
        fluid.parallelizeUnaffected = parallelizeUnaffected;
        fluid.ignoreOutputCheck = ignoreOutputCheck;
        return fluid;
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
    public void startCrafting(List<ProcessingComponent<?>> components, RecipeCraftingContext context, ResultChance chance) {
        super.startCrafting(components, context, chance);
    }

    @Override
    public CraftCheck finishCrafting(final List<ProcessingComponent<?>> components, final RecipeCraftingContext context, final ResultChance chance) {
        return CraftCheck.success();
    }

    @Override
    public CraftCheck canStartCrafting(final List<ProcessingComponent<?>> components, final RecipeCraftingContext context) {
        return doFluidIO(components, context);
    }

    @Override
    public CraftCheck doIOTick(final List<ProcessingComponent<?>> components, final RecipeCraftingContext context, final float durationMultiplier) {
        return doFluidIO(components, context);
    }

    @Override
    public List<ProcessingComponent<?>> copyComponents(final List<ProcessingComponent<?>> components) {
        return HybridFluidUtils.copyFluidHandlerComponents(components);
    }

    @Override
    public int getMaxParallelism(final List<ProcessingComponent<?>> components, final RecipeCraftingContext context, final int maxParallelism) {
        if (parallelizeUnaffected || (ignoreOutputCheck && actionType == IOType.OUTPUT)) {
            return maxParallelism;
        }

        return doFluidIOInternal(components, context, maxParallelism);
    }

    private CraftCheck doFluidIO(final List<ProcessingComponent<?>> components, final RecipeCraftingContext context) {
        int mul = doFluidIOInternal(components, context, parallelism);
        if (mul < parallelism) {
            return switch (actionType) {
                case INPUT -> CraftCheck.failure("craftcheck.failure.fluid.input");
                case OUTPUT -> {
                    if (ignoreOutputCheck) {
                        yield CraftCheck.success();
                    }
                    yield CraftCheck.failure("craftcheck.failure.fluid.output.space");
                }
            };
        }
        return CraftCheck.success();
    }

    public int doFluidIOInternal(final List<ProcessingComponent<?>> components, final RecipeCraftingContext context, final int maxMultiplier) {
        List<IFluidHandler> fluidHandlers = HybridFluidUtils.castFluidHandlerComponents(components);

        long required = Math.round(RecipeModifier.applyModifiers(context, this, (double) this.required.amount, false));
        if (required <= 0) {
            return maxMultiplier;
        }

        long maxRequired = required * maxMultiplier;

        FluidStack stack = this.required.copy();
        long totalIO = HybridFluidUtils.doSimulateDrainOrFill(stack, fluidHandlers, maxRequired, actionType);

        if (totalIO < required) {
            return 0;
        }

        HybridFluidUtils.doDrainOrFill(stack, totalIO, fluidHandlers, actionType);

        return (int) (totalIO / required);
    }

    @Override
    public void setParallelism(final int parallelism) {
        if (!parallelizeUnaffected) {
            this.parallelism = parallelism;
        }
    }

    @Override
    public void setParallelizeUnaffected(final boolean unaffected) {
        this.parallelizeUnaffected = unaffected;
        if (parallelizeUnaffected) {
            this.parallelism = 1;
        }
    }

    // Noop

    @Override
    public void startRequirementCheck(ResultChance contextChance, RecipeCraftingContext context) {

    }

    @Override
    public void endRequirementCheck() {
    }

    @Nonnull
    @Override
    public CraftCheck canStartCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, List<ComponentOutputRestrictor> restrictions) {
        return CraftCheck.success();
    }

    @Override
    public boolean startCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        return true;
    }

    @Override
    @Nonnull
    public CraftCheck finishCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        return CraftCheck.success();
    }

    @Override
    public void startIOTick(RecipeCraftingContext context, float durationMultiplier) {
    }

    @Nonnull
    @Override
    public CraftCheck resetIOTick(RecipeCraftingContext context) {
        return CraftCheck.skipComponent();
    }

    @Nonnull
    @Override
    public CraftCheck doIOTick(ProcessingComponent<?> component, RecipeCraftingContext context) {
        return CraftCheck.skipComponent();
    }
}
