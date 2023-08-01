/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.crafting.requirement;

import github.kasuminova.util.MultiFluidTank;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.crafting.helper.*;
import hellfirepvp.modularmachinery.common.crafting.requirement.jei.JEIComponentHybridFluid;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementTypeFluid;
import hellfirepvp.modularmachinery.common.integration.ingredient.HybridFluid;
import hellfirepvp.modularmachinery.common.integration.ingredient.HybridFluidGas;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.*;
import hellfirepvp.modularmachinery.common.util.nbt.NBTMatchingHelper;
import mekanism.api.gas.GasStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: RequirementFluid
 * Created by HellFirePvP
 * Date: 24.02.2018 / 12:28
 * TODO: Split FluidStack and GasStack into two different Requirements, combining the two makes for terrible code.
 */
public class RequirementFluid extends ComponentRequirement<HybridFluid, RequirementTypeFluid>
        implements ComponentRequirement.ChancedRequirement, ComponentRequirement.Parallelizable, Asyncable {

    public final HybridFluid required;
    public float chance = 1F;
    private int parallelism = 1;
    private boolean parallelizeUnaffected = false;
    private HybridFluid requirementCheck;
    private boolean doesntConsumeInput;

    private NBTTagCompound tagMatch = null, tagDisplay = null;

    public RequirementFluid(IOType ioType, FluidStack fluid) {
        this(RequirementTypesMM.REQUIREMENT_FLUID, ioType, new HybridFluid(fluid));
    }

    private RequirementFluid(RequirementTypeFluid type, IOType ioType, HybridFluid required) {
        super(type, ioType);
        this.required = required.copy();
        this.requirementCheck = this.required.copy();
    }

    @net.minecraftforge.fml.common.Optional.Method(modid = "mekanism")
    public static RequirementFluid createMekanismGasRequirement(RequirementTypeFluid type, IOType ioType, GasStack gasStack) {
        return new RequirementFluid(type, ioType, new HybridFluidGas(gasStack));
    }

    @Override
    public int getSortingWeight() {
        return PRIORITY_WEIGHT_FLUID;
    }

    @Override
    public ComponentRequirement<HybridFluid, RequirementTypeFluid> deepCopy() {
        RequirementFluid fluid = new RequirementFluid(this.requirementType, this.actionType, this.required.copy());
        fluid.setTag(getTag());
        fluid.triggerTime = this.triggerTime;
        fluid.triggerRepeatable = this.triggerRepeatable;
        fluid.chance = this.chance;
        fluid.tagMatch = getTagMatch();
        fluid.tagDisplay = getTagDisplay();
        fluid.parallelizeUnaffected = this.parallelizeUnaffected;
        return fluid;
    }

    @Override
    public ComponentRequirement<HybridFluid, RequirementTypeFluid> deepCopyModified(List<RecipeModifier> modifiers) {
        HybridFluid hybrid = this.required.copy();
        hybrid.setAmount(Math.round(RecipeModifier.applyModifiers(modifiers, this, hybrid.getAmount(), false)));
        RequirementFluid fluid = new RequirementFluid(this.requirementType, this.actionType, hybrid);
        fluid.setTag(getTag());
        fluid.chance = RecipeModifier.applyModifiers(modifiers, this, this.chance, true);
        fluid.tagMatch = getTagMatch();
        fluid.tagDisplay = getTagDisplay();
        fluid.parallelizeUnaffected = this.parallelizeUnaffected;
        return fluid;
    }

    @Override
    public JEIComponent<HybridFluid> provideJEIComponent() {
        return new JEIComponentHybridFluid(this);
    }

    public void setMatchNBTTag(@Nullable NBTTagCompound tag) {
        this.tagMatch = tag;
    }

    @Nullable
    public NBTTagCompound getTagMatch() {
        if (tagMatch == null) {
            return null;
        }
        return tagMatch.copy();
    }

    public void setDisplayNBTTag(@Nullable NBTTagCompound tag) {
        this.tagDisplay = tag;
    }

    @Nullable
    public NBTTagCompound getTagDisplay() {
        if (tagDisplay == null) {
            return null;
        }
        return tagDisplay.copy();
    }

    @Override
    public void setChance(float chance) {
        this.chance = chance;
    }

    @Override
    public void startRequirementCheck(ResultChance contextChance, RecipeCraftingContext context) {
        this.requirementCheck = this.required.copy();
        this.requirementCheck.setAmount(Math.round(RecipeModifier.applyModifiers(context, this, this.requirementCheck.getAmount(), false) * parallelism));
        this.doesntConsumeInput = contextChance.canProduce(RecipeModifier.applyModifiers(context, this, this.chance, true));
    }

    @Override
    public void endRequirementCheck() {
        this.requirementCheck = this.required.copy();
        this.doesntConsumeInput = true;
    }

    @Nonnull
    @Override
    public String getMissingComponentErrorMessage(IOType ioType) {
        ResourceLocation compKey = this.requirementType.getRegistryName();
        return String.format("component.missing.%s.%s.%s",
                compKey.getNamespace(), compKey.getPath(), ioType.name().toLowerCase());
    }

    @Override
    public boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx) {
        MachineComponent<?> cmp = component.component;
        if (Mods.MEKANISM.isPresent() && required instanceof HybridFluidGas) {
            return  cmp instanceof MachineComponent.FluidHatch &&
                    cmp.ioType == this.actionType &&
                    cmp.getContainerProvider() instanceof HybridGasTank;
        } else {
            return  cmp instanceof MachineComponent.FluidHatch &&
                    cmp.ioType == this.actionType &&
                    cmp.getComponentType().equals(ComponentTypesMM.COMPONENT_FLUID);
        }
    }

    @Nonnull
    @Override
    public CraftCheck canStartCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, List<ComponentOutputRestrictor> restrictions) {
        IFluidHandler handler = (IFluidHandler) component.providedComponent;

        if (Mods.MEKANISM.isPresent() && required instanceof HybridFluidGas) {
            if (handler instanceof HybridGasTank) {
                Optional<CraftCheck> check = checkStartCraftingWithMekanism(component, context, (HybridGasTank) handler, restrictions);
                if (check.isPresent()) {
                    return check.get();
                }
            }

            return CraftCheck.skipComponent();
        }

        switch (actionType) {
            case INPUT:
                //If it doesn't consume the item, we only need to see if it's actually there.
                FluidStack drained = handler.drain(this.requirementCheck.copy().asFluidStack(), false);
                if (drained == null) {
                    return CraftCheck.failure("craftcheck.failure.fluid.input");
                }
                if (!NBTMatchingHelper.matchNBTCompound(this.tagMatch, drained.tag)) {
                    return CraftCheck.failure("craftcheck.failure.fluid.input");
                }
                this.requirementCheck.setAmount(Math.max(this.requirementCheck.getAmount() - drained.amount, 0));
                if (this.requirementCheck.getAmount() <= 0) {
                    return CraftCheck.success();
                }
                return CraftCheck.failure("craftcheck.failure.fluid.input");
            case OUTPUT:
                handler = new MultiFluidTank(handler);

                for (ComponentOutputRestrictor restrictor : restrictions) {
                    if (restrictor instanceof ComponentOutputRestrictor.RestrictionTank) {
                        ComponentOutputRestrictor.RestrictionTank tank = (ComponentOutputRestrictor.RestrictionTank) restrictor;

                        if (tank.exactComponent.equals(component)) {
                            handler.fill(tank.inserted == null ? null : tank.inserted.copy().asFluidStack(), true);
                        }
                    }
                }
                int filled = handler.fill(this.requirementCheck.copy().asFluidStack(), false); //True or false doesn't really matter tbh
                boolean didFill = filled >= this.requirementCheck.getAmount();
                if (didFill) {
                    context.addRestriction(new ComponentOutputRestrictor.RestrictionTank(this.requirementCheck.copy(), component));
                }
                if (didFill) {
                    return CraftCheck.success();
                }
                return CraftCheck.failure("craftcheck.failure.fluid.output.space");
        }
        return CraftCheck.skipComponent();
    }

    @net.minecraftforge.fml.common.Optional.Method(modid = "mekanism")
    private Optional<CraftCheck> checkStartCraftingWithMekanism(ProcessingComponent<?> component,
                                                                RecipeCraftingContext context,
                                                                HybridGasTank handler,
                                                                List<ComponentOutputRestrictor> restrictions) {
        HybridGasTank gasTank = handler;
        switch (actionType) {
            case INPUT:
                GasStack drained = gasTank.drawGas(EnumFacing.UP, this.requirementCheck.getAmount(), false);
                if (drained == null) {
                    return Optional.of(CraftCheck.failure("craftcheck.failure.gas.input"));
                }
                if (drained.getGas() != ((HybridFluidGas) this.requirementCheck).asGasStack().getGas()) {
                    return Optional.of(CraftCheck.failure("craftcheck.failure.gas.input"));
                }
                this.requirementCheck.setAmount(Math.max(this.requirementCheck.getAmount() - drained.amount, 0));
                if (this.requirementCheck.getAmount() <= 0) {
                    return Optional.of(CraftCheck.success());
                }
                return Optional.of(CraftCheck.failure("craftcheck.failure.gas.input"));
            case OUTPUT:
                gasTank = (HybridGasTank) CopyHandlerHelper.copyTank(gasTank);

                for (ComponentOutputRestrictor restrictor : restrictions) {
                    if (restrictor instanceof ComponentOutputRestrictor.RestrictionTank) {
                        ComponentOutputRestrictor.RestrictionTank tank = (ComponentOutputRestrictor.RestrictionTank) restrictor;

                        if (tank.exactComponent.equals(component) && tank.inserted instanceof HybridFluidGas) {
                            gasTank.receiveGas(EnumFacing.UP, ((HybridFluidGas) this.requirementCheck).asGasStack(), true);
                        }
                    }
                }

                int gasFilled = gasTank.receiveGas(EnumFacing.UP, ((HybridFluidGas) this.requirementCheck).asGasStack(), false);
                boolean didFill = gasFilled >= this.requirementCheck.getAmount();
                if (didFill) {
                    context.addRestriction(new ComponentOutputRestrictor.RestrictionTank(this.requirementCheck.copy(), component));
                }
                if (didFill) {
                    return Optional.of(CraftCheck.success());
                }
                return Optional.of(CraftCheck.failure("craftcheck.failure.gas.output.space"));
        }
        return Optional.empty();
    }

    @Override
    public boolean startCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        IFluidHandler handler = (IFluidHandler) component.providedComponent;
        if (actionType == IOType.INPUT) {
            if (Mods.MEKANISM.isPresent() && required instanceof HybridFluidGas) {
                if (handler instanceof HybridGasTank) {
                    return startCraftingWithMekanismHandling((HybridGasTank) handler, chance);
                }
                return false;
            }

            //If it doesn't consume the item, we only need to see if it's actually there.
            FluidStack drainedSimulated = handler.drain(this.requirementCheck.copy().asFluidStack(), false);
            if (drainedSimulated == null) {
                return false;
            }
            if (!NBTMatchingHelper.matchNBTCompound(this.tagMatch, drainedSimulated.tag)) {
                return false;
            }
            if (this.doesntConsumeInput) {
                this.requirementCheck.setAmount(Math.max(this.requirementCheck.getAmount() - drainedSimulated.amount, 0));
                return this.requirementCheck.getAmount() <= 0;
            }
            FluidStack actualDrained = handler.drain(this.requirementCheck.copy().asFluidStack(), true);
            if (actualDrained == null) {
                return false;
            }
            if (!NBTMatchingHelper.matchNBTCompound(this.tagMatch, actualDrained.tag)) {
                return false;
            }
            this.requirementCheck.setAmount(Math.max(this.requirementCheck.getAmount() - actualDrained.amount, 0));
            return this.requirementCheck.getAmount() <= 0;
        }
        return false;
    }

    @net.minecraftforge.fml.common.Optional.Method(modid = "mekanism")
    private boolean startCraftingWithMekanismHandling(HybridGasTank handler, ResultChance chance) {
        GasStack drainSimulated = handler.drawGas(EnumFacing.UP, this.requirementCheck.getAmount(), false);
        if (drainSimulated == null) {
            return false;
        }
        if (drainSimulated.getGas() != ((HybridFluidGas) this.requirementCheck).asGasStack().getGas()) {
            return false;
        }
        if (this.doesntConsumeInput) {
            this.requirementCheck.setAmount(Math.max(this.requirementCheck.getAmount() - drainSimulated.amount, 0));
            return this.requirementCheck.getAmount() <= 0;
        }
        GasStack actualDrain = handler.drawGas(EnumFacing.UP, this.requirementCheck.getAmount(), true);
        if (actualDrain == null) {
            return false;
        }
        if (actualDrain.getGas() != ((HybridFluidGas) this.requirementCheck).asGasStack().getGas()) {
            return false;
        }
        this.requirementCheck.setAmount(Math.max(this.requirementCheck.getAmount() - actualDrain.amount, 0));
        return this.requirementCheck.getAmount() <= 0;
    }

    @Override
    @Nonnull
    public CraftCheck finishCrafting(ProcessingComponent<?> component, RecipeCraftingContext context, ResultChance chance) {
        IFluidHandler handler = (IFluidHandler) component.providedComponent;
        if (Objects.requireNonNull(actionType) != IOType.OUTPUT) {
            return CraftCheck.skipComponent();
        }

        if (Mods.MEKANISM.isPresent() && required instanceof HybridFluidGas) {
            return handler instanceof HybridGasTank
                    ? finishWithMekanismHandling((HybridGasTank) handler, context, chance)
                    : CraftCheck.skipComponent();
        }

        FluidStack outStack = this.requirementCheck.asFluidStack();
        if (outStack != null) {
            int filled = handler.fill(outStack.copy(), false);
            if (chance.canProduce(RecipeModifier.applyModifiers(context, this, this.chance, true))) {
                if (filled >= outStack.amount) {
                    return CraftCheck.success();
                }
                return CraftCheck.failure("craftcheck.failure.fluid.output.space");
            }
            FluidStack copyOut = outStack.copy();
            if (this.tagDisplay != null) {
                copyOut.tag = this.tagDisplay.copy();
            }
            if (filled >= outStack.amount && handler.fill(copyOut.copy(), true) >= copyOut.amount) {
                return CraftCheck.success();
            }
            return CraftCheck.failure("craftcheck.failure.fluid.output.space");
        }

        return CraftCheck.skipComponent();
    }

    @net.minecraftforge.fml.common.Optional.Method(modid = "mekanism")
    @Nonnull
    private CraftCheck finishWithMekanismHandling(HybridGasTank handler, RecipeCraftingContext context, ResultChance chance) {
        GasStack gasOut = ((HybridFluidGas) this.requirementCheck).asGasStack();
        int filledGas = handler.receiveGas(EnumFacing.UP, gasOut, false);
        if (filledGas < gasOut.amount) {
            return CraftCheck.failure("craftcheck.failure.gas.output.space");
        }
        if (chance.canProduce(RecipeModifier.applyModifiers(context, this, this.chance, true))) {
            return CraftCheck.success();
        }
        if (handler.receiveGas(EnumFacing.UP, gasOut, true) >= gasOut.amount) {
            return CraftCheck.success();
        }
        return CraftCheck.failure("craftcheck.failure.gas.output.space");
    }

    @Override
    public int maxParallelism(ProcessingComponent<?> component, RecipeCraftingContext context, int maxParallelism) {
        if (parallelizeUnaffected) {
            return maxParallelism;
        }
        IFluidHandler handler = (IFluidHandler) component.providedComponent;
        switch (actionType) {
            case INPUT: {
                if (Mods.MEKANISM.isPresent() && this.required instanceof HybridFluidGas && handler instanceof HybridGasTank) {
                    GasStack gas = ((HybridFluidGas) requirementCheck).asGasStack().copy();
                    return HybridFluidUtils.maxGasInputParallelism(
                            (HybridGasTank) handler, gas, maxParallelism);
                } else {
                    FluidStack fluid = requirementCheck.asFluidStack().copy();
                    return HybridFluidUtils.maxFluidInputParallelism(
                            handler, fluid, maxParallelism);
                }
            }
            case OUTPUT: {
                if (Mods.MEKANISM.isPresent() && this.required instanceof HybridFluidGas && handler instanceof HybridGasTank) {
                    GasStack gas = ((HybridFluidGas) requirementCheck).asGasStack().copy();
                    return HybridFluidUtils.maxGasOutputParallelism(
                            (HybridGasTank) handler, gas, maxParallelism);
                } else {
                    FluidStack fluid = requirementCheck.asFluidStack().copy();
                    return HybridFluidUtils.maxFluidOutputParallelism(
                            handler, fluid, maxParallelism);
                }
            }
        }
        return maxParallelism;
    }

    @Override
    public void setParallelism(int parallelism) {
        if (!parallelizeUnaffected) {
            this.parallelism = parallelism;
        }
    }

    @Override
    public void setParallelizeUnaffected(boolean unaffected) {
        this.parallelizeUnaffected = unaffected;
        if (parallelizeUnaffected) {
            this.parallelism = 1;
        }
    }
}
