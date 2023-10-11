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
import kport.modularmagic.common.crafting.component.ComponentAura;
import kport.modularmagic.common.crafting.requirement.types.ModularMagicRequirements;
import kport.modularmagic.common.crafting.requirement.types.RequirementTypeAura;
import kport.modularmagic.common.integration.jei.component.JEIComponentAura;
import kport.modularmagic.common.integration.jei.ingredient.Aura;
import kport.modularmagic.common.tile.TileAuraProvider;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequirementAura extends ComponentRequirement.ParallelizableRequirement<Aura, RequirementTypeAura>
        implements Asyncable, ComponentRequirement.Parallelizable {

    public Aura aura;
    public int max;
    public int min;

    public RequirementAura(IOType type, Aura aura, int max, int min) {
        super((RequirementTypeAura) RegistriesMM.REQUIREMENT_TYPE_REGISTRY.getValue(ModularMagicRequirements.KEY_REQUIREMENT_AURA), type);
        this.aura = aura;
        this.max = max;
        this.min = min;
    }

    @Nonnull
    private static Map<BlockPos, TileAuraProvider> buildChunkAuraProviderMap(final List<ProcessingComponent<?>> components) {
        Map<BlockPos, TileAuraProvider> chunkAuraProviders = new HashMap<>();
        for (final ProcessingComponent<?> component : components) {
            TileAuraProvider provider = (TileAuraProvider) component.getComponent().getContainerProvider();
            chunkAuraProviders.putIfAbsent(provider.getChunkPos(), provider);
        }
        return chunkAuraProviders;
    }

    @Override
    public boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx) {
        MachineComponent<?> cpn = component.getComponent();
        return cpn.getContainerProvider() instanceof TileAuraProvider &&
                cpn.getComponentType() instanceof ComponentAura &&
                cpn.ioType == getActionType();
    }

    @Nonnull
    @Override
    public List<ProcessingComponent<?>> copyComponents(final List<ProcessingComponent<?>> components) {
        return components;
    }

    @Nonnull
    @Override
    public CraftCheck canStartCrafting(final List<ProcessingComponent<?>> components, final RecipeCraftingContext context) {
        Map<BlockPos, TileAuraProvider> chunkAuraProviders = buildChunkAuraProviderMap(components);
        switch (actionType) {
            case INPUT -> {
                int removed = removeAll(chunkAuraProviders, context, parallelism, true);
                if (removed < parallelism) {
                    return CraftCheck.failure("error.modularmachinery.requirement.aura.less");
                }
            }
            case OUTPUT -> {
                int added = addAll(chunkAuraProviders, context, parallelism, true);
                if (added < parallelism) {
                    return CraftCheck.failure("error.modularmachinery.requirement.aura.more");
                }
            }
        }
        return CraftCheck.success();
    }

    @Override
    public void startCrafting(final List<ProcessingComponent<?>> components, final RecipeCraftingContext context, final ResultChance chance) {
        if (actionType == IOType.INPUT) {
            Map<BlockPos, TileAuraProvider> chunkAuraProviders = buildChunkAuraProviderMap(components);
            removeAll(chunkAuraProviders, context, parallelism, false);
        }
    }

    @Override
    public void finishCrafting(final List<ProcessingComponent<?>> components, final RecipeCraftingContext context, final ResultChance chance) {
        if (actionType == IOType.OUTPUT) {
            Map<BlockPos, TileAuraProvider> chunkAuraProviders = buildChunkAuraProviderMap(components);
            addAll(chunkAuraProviders, context, parallelism, false);
        }
    }

    @Override
    public int getMaxParallelism(final List<ProcessingComponent<?>> components, final RecipeCraftingContext context, final int maxParallelism) {
        if (parallelizeUnaffected) {
            return maxParallelism;
        }

        Map<BlockPos, TileAuraProvider> chunkAuraProviders = buildChunkAuraProviderMap(components);
        switch (actionType) {
            case INPUT -> {
                return removeAll(chunkAuraProviders, context, parallelism, true);
            }
            case OUTPUT -> {
                if (ignoreOutputCheck) {
                    return maxParallelism;
                }
                return addAll(chunkAuraProviders, context, parallelism, true);
            }
        }

        return 0;
    }

    private int addAll(final Map<BlockPos, TileAuraProvider> chunkAuraProviders,
                       final RecipeCraftingContext context,
                       final float maxMultiplier,
                       final boolean simulate) {
        int toAdd = (int) Math.round(RecipeModifier.applyModifiers(context, this, (double) aura.getAmount(), false));
        int maxAdd = (int) (toAdd * maxMultiplier);

        int totalAdded = 0;
        for (final TileAuraProvider auraProvider : chunkAuraProviders.values()) {
            Aura aura = auraProvider.getAura();
            if (aura.getType() == this.aura.getType() && aura.getAmount() < max) {
                int added = max - aura.getAmount();
                totalAdded += added;
                if (!simulate) {
                    auraProvider.addAura(new Aura(added, this.aura.getType()));
                }
                if (totalAdded >= maxAdd) {
                    break;
                }
            }
        }

        if (totalAdded < maxAdd) {
            return totalAdded / toAdd;
        }
        return totalAdded;
    }

    private int removeAll(final Map<BlockPos, TileAuraProvider> chunkAuraProviders,
                          final RecipeCraftingContext context,
                          final float maxMultiplier,
                          final boolean simulate) {
        int toRemove = (int) Math.round(RecipeModifier.applyModifiers(context, this, (double) aura.getAmount(), false));
        int maxRemove = (int) (toRemove * maxMultiplier);

        int totalRemoved = 0;
        for (final TileAuraProvider auraProvider : chunkAuraProviders.values()) {
            Aura aura = auraProvider.getAura();
            if (aura.getType() == this.aura.getType() && aura.getAmount() > min) {
                int removed = aura.getAmount() - min;
                totalRemoved += removed;
                if (!simulate) {
                    auraProvider.removeAura(new Aura(removed, this.aura.getType()));
                }
                if (totalRemoved >= maxRemove) {
                    break;
                }
            }
        }

        if (totalRemoved < maxRemove) {
            return totalRemoved / toRemove;
        }
        return totalRemoved;
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
        req.parallelizeUnaffected = this.parallelizeUnaffected;
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
