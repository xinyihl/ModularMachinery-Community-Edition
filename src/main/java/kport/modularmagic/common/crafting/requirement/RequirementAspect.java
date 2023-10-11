package kport.modularmagic.common.crafting.requirement;

import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftCheck;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
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
import kport.modularmagic.common.utils.AspectJarProxy;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RequirementAspect extends ComponentRequirement.ParallelizableRequirement<AspectList, RequirementTypeAspect>
        implements Asyncable, ComponentRequirement.Parallelizable {

    public int amount;
    public Aspect aspect;

    public RequirementAspect(IOType actionType, int amount, Aspect aspect) {
        super((RequirementTypeAspect) RegistriesMM.REQUIREMENT_TYPE_REGISTRY.getValue(ModularMagicRequirements.KEY_REQUIREMENT_ASPECT), actionType);

        this.amount = amount;
        this.aspect = aspect;
    }

    @Nonnull
    private static List<AspectJarProxy> convertJars(final List<ProcessingComponent<?>> components) {
        List<AspectJarProxy> jars = new ArrayList<>();
        for (final ProcessingComponent<?> component : components) {
            jars.add((AspectJarProxy) component.providedComponent());
        }
        return jars;
    }

    @Override
    public boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx) {
        MachineComponent<?> cpn = component.getComponent();
        return cpn.getContainerProvider() instanceof AspectJarProxy &&
                cpn.getComponentType() instanceof ComponentAspect &&
                cpn.ioType == getActionType();
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public List<ProcessingComponent<?>> copyComponents(final List<ProcessingComponent<?>> components) {
        List<ProcessingComponent<?>> list = new ArrayList<>();
        for (final ProcessingComponent<?> component : components) {
            list.add(new ProcessingComponent<>((
                    MachineComponent<Object>) component.component(),
                    new AspectJarProxy(((AspectJarProxy) component.providedComponent()).getOriginal()),
                    component.tag())
            );
        }
        return list;
    }

    @Nonnull
    @Override
    public CraftCheck canStartCrafting(final List<ProcessingComponent<?>> components, final RecipeCraftingContext context) {
        List<AspectJarProxy> jars = convertJars(components);

        switch (getActionType()) {
            case INPUT -> {
                int taken = takeAll(jars, context, parallelism, true);
                if (taken < parallelism) {
                    return CraftCheck.failure("error.modularmachinery.requirement.aspect.less");
                }
            }
            case OUTPUT -> {
                if (!ignoreOutputCheck) {
                    int added = addAll(jars, context, parallelism, true);
                    if (added < parallelism) {
                        return CraftCheck.failure("error.modularmachinery.requirement.aspect.out");
                    }
                }
            }
        }

        return CraftCheck.success();
    }

    @Override
    public void startCrafting(final List<ProcessingComponent<?>> components, final RecipeCraftingContext context, final ResultChance chance) {
        if (actionType == IOType.INPUT) {
            List<AspectJarProxy> jars = convertJars(components);
            takeAll(jars, context, parallelism, false);
        }
    }

    @Override
    public void finishCrafting(final List<ProcessingComponent<?>> components, final RecipeCraftingContext context, final ResultChance chance) {
        if (actionType == IOType.OUTPUT) {
            List<AspectJarProxy> jars = convertJars(components);
            addAll(jars, context, parallelism, false);
        }
    }

    @Override
    public int getMaxParallelism(final List<ProcessingComponent<?>> components, final RecipeCraftingContext context, final int maxParallelism) {
        if (parallelizeUnaffected || (ignoreOutputCheck && actionType == IOType.OUTPUT)) {
            return maxParallelism;
        }

        List<AspectJarProxy> jars = convertJars(components);
        switch (actionType) {
            case INPUT -> {
                return takeAll(jars, context, maxParallelism, true);
            }
            case OUTPUT -> {
                return addAll(jars, context, maxParallelism, true);
            }
        }

        return 0;
    }

    private int addAll(final List<AspectJarProxy> jars,
                       final RecipeCraftingContext context,
                       final float maxMultiplier,
                       final boolean simulate) {
        int toAdd = Math.round(RecipeModifier.applyModifiers(context, this, (float) this.amount, false));
        int maxAdd = (int) (toAdd * maxMultiplier);

        int totalAdded = 0;
        for (final AspectJarProxy jar : jars) {
            if (simulate) {
                totalAdded += jar.addToContainer(aspect, maxAdd - totalAdded);
            } else {
                totalAdded += jar.getOriginal().addToContainer(aspect, maxAdd - totalAdded);
            }
            if (totalAdded >= maxAdd) {
                break;
            }
        }

        if (totalAdded < maxAdd) {
            return totalAdded / toAdd;
        }
        return totalAdded;
    }

    private int takeAll(final List<AspectJarProxy> jars,
                        final RecipeCraftingContext context,
                        final float maxMultiplier,
                        final boolean simulate) {
        int toTake = Math.round(RecipeModifier.applyModifiers(context, this, (float) this.amount, false));
        int maxTake = (int) (toTake * maxMultiplier);

        int totalTaken = 0;
        for (final AspectJarProxy jar : jars) {
            if (simulate) {
                int jarAmount = jar.getAmount();
                if (jar.takeFromContainer(aspect, Math.min(jarAmount, maxTake - totalTaken))) {
                    totalTaken += jarAmount;
                }
            } else {
                int jarAmount = jar.getOriginal().amount;
                if (jar.getOriginal().takeFromContainer(aspect, Math.min(jarAmount, maxTake - totalTaken))) {
                    totalTaken += jarAmount;
                }
            }
            if (totalTaken >= maxTake) {
                break;
            }
        }

        if (totalTaken < maxTake) {
            return totalTaken / toTake;
        }
        return totalTaken;
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
        req.parallelizeUnaffected = this.parallelizeUnaffected;
        return req;
    }

    @Override
    public JEIComponentAspect provideJEIComponent() {
        return new JEIComponentAspect(this);
    }
}
