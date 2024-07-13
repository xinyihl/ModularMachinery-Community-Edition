package hellfirepvp.modularmachinery.common.crafting.requirement;

import github.kasuminova.mmce.common.util.IExtendedGasHandler;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftCheck;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.requirement.jei.JEIComponentGasPerTick;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementTypeGasPerTick;
import hellfirepvp.modularmachinery.common.integration.ingredient.HybridFluid;
import hellfirepvp.modularmachinery.common.integration.ingredient.HybridFluidGas;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.HybridFluidUtils;
import mekanism.api.gas.GasStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class RequirementGasPerTick extends ComponentRequirement.PerTickParallelizable<HybridFluid, RequirementTypeGasPerTick>
        implements ComponentRequirement.Parallelizable {

    public final GasStack required;

    protected final HybridFluidGas requirementCheck;
    protected NBTTagCompound tagMatch = null, tagDisplay = null;
    protected boolean isSuccess = false;

    public RequirementGasPerTick(IOType actionType, GasStack required) {
        super(RequirementTypesMM.REQUIREMENT_GAS_PERTICK, actionType);
        this.required = required;
        this.requirementCheck = new HybridFluidGas(required);
    }

    @Override
    public boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx) {
        MachineComponent<?> cmp = component.component();
        return (cmp.getContainerProvider() instanceof IExtendedGasHandler) && cmp.ioType == this.actionType;
    }

    @Override
    public RequirementGasPerTick deepCopy() {
        return deepCopyModified(Collections.emptyList());
    }

    @Override
    public RequirementGasPerTick deepCopyModified(List<RecipeModifier> modifiers) {
        GasStack stack = this.required.copy();
        stack.amount = ((int) Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_GAS, actionType, (double) stack.amount, false)));
        RequirementGasPerTick gas = new RequirementGasPerTick(actionType, stack);
        gas.tagMatch = tagMatch;
        gas.tagDisplay = tagDisplay;
        return gas;
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
        return new JEIComponentGasPerTick(this);
    }

    @Nonnull
    @Override
    public CraftCheck canStartCrafting(final List<ProcessingComponent<?>> components, final RecipeCraftingContext context) {
        return doGasIO(components, context);
    }

    @Override
    public CraftCheck doIOTick(final List<ProcessingComponent<?>> components, final RecipeCraftingContext context, final float durationMultiplier) {
        return doGasIO(components, context);
    }

    @Nonnull
    @Override
    public List<ProcessingComponent<?>> copyComponents(final List<ProcessingComponent<?>> components) {
        return HybridFluidUtils.copyGasHandlerComponents(components);
    }

    @Override
    public int getMaxParallelism(final List<ProcessingComponent<?>> components, final RecipeCraftingContext context, final int maxParallelism) {
        if (ignoreOutputCheck && actionType == IOType.OUTPUT) {
            return maxParallelism;
        }
        if (parallelizeUnaffected) {
            if (doGasIOInternal(components, context, 1) >= 1) {
                return maxParallelism;
            }
            return 0;
        }
        return doGasIOInternal(components, context, maxParallelism);
    }

    private CraftCheck doGasIO(final List<ProcessingComponent<?>> components, final RecipeCraftingContext context) {
        int mul = doGasIOInternal(components, context, parallelism);
        if (mul < parallelism) {
            return switch (actionType) {
                case INPUT -> CraftCheck.failure("craftcheck.failure.gas.input");
                case OUTPUT -> {
                    if (ignoreOutputCheck) {
                        yield CraftCheck.success();
                    }
                    yield CraftCheck.failure("craftcheck.failure.gas.output.space");
                }
            };
        }
        return CraftCheck.success();
    }

    public int doGasIOInternal(final List<ProcessingComponent<?>> components, final RecipeCraftingContext context, final int maxMultiplier) {
        List<IExtendedGasHandler> gasHandlers = HybridFluidUtils.castGasHandlerComponents(components);

        long required = Math.round(RecipeModifier.applyModifiers(context, RequirementTypesMM.REQUIREMENT_GAS, actionType, (double) this.required.amount, false));
        if (required <= 0) {
            return maxMultiplier;
        }

        long maxRequired = required * maxMultiplier;

        GasStack stack = this.required.copy();
        long totalIO = HybridFluidUtils.doSimulateDrainOrFill(stack, gasHandlers, maxRequired, actionType);

        if (totalIO < required) {
            return 0;
        }

        HybridFluidUtils.doDrainOrFill(stack, totalIO, gasHandlers, actionType);

        return (int) (totalIO / required);
    }
}
