package hellfirepvp.modularmachinery.common.crafting;

import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import net.minecraft.util.ResourceLocation;

import java.util.List;
import java.util.function.Function;

public class ThreadRecipe extends MachineRecipe {
    private String threadName;
    private boolean singleThread = false;
    public ThreadRecipe(PreparedRecipe preparedRecipe) {
        super(preparedRecipe);
    }

    public ThreadRecipe(
            String path,
            ResourceLocation registryName,
            ResourceLocation owningMachine,
            int tickTime,
            int configuredPriority,
            boolean voidPerTickFailure,
            boolean isParallelized,
            String threadName,
            boolean singleThread)
    {
        super(path, registryName, owningMachine, tickTime, configuredPriority, voidPerTickFailure, isParallelized);
        this.threadName = threadName;
        this.singleThread = singleThread;
    }

    public String getThreadName() {
        return threadName;
    }

    public boolean isSingleThread() {
        return singleThread;
    }

    @Override
    public MachineRecipe copy(Function<ResourceLocation, ResourceLocation> registryNameChange, ResourceLocation newOwningMachineIdentifier, List<RecipeModifier> modifiers) {
        ThreadRecipe copy = new ThreadRecipe(
                this.recipeFilePath,
                registryNameChange.apply(this.registryName),
                newOwningMachineIdentifier,
                Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_DURATION, null, this.tickTime, false)),
                this.configuredPriority,
                this.doesCancelRecipeOnPerTickFailure(),
                this.isParallelized,
                this.threadName,
                this.singleThread);

        for (ComponentRequirement<?, ?> requirement : this.getCraftingRequirements()) {
            copy.addRequirement(requirement.deepCopyModified(modifiers));
        }

        return copy;
    }
}
