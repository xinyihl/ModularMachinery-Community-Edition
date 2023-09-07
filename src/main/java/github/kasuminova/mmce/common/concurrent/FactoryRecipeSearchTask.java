package github.kasuminova.mmce.common.concurrent;

import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftingStatus;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.factory.FactoryRecipeThread;
import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class FactoryRecipeSearchTask extends RecipeSearchTask {
    private final FactoryRecipeThread thread;
    private final List<MachineRecipe> filtered = new ArrayList<>();
    private final ActiveMachineRecipe[] running;
    private final TileFactoryController factory;

    public FactoryRecipeSearchTask(
            TileFactoryController controller,
            DynamicMachine currentMachine,
            int maxParallelism,
            Iterable<MachineRecipe> recipeList,
            FactoryRecipeThread thread,
            ActiveMachineRecipe[] running)
    {
        super(controller, currentMachine, maxParallelism, recipeList);
        this.factory = controller;
        this.running = running;
        this.thread = thread;
    }

    @Override
    protected RecipeCraftingContext computeTask() {
        filterRecipe();

        TileFactoryController factory = this.factory;
        DynamicMachine foundMachine = factory.getFoundMachine();
        if (foundMachine == null) return null;

        MachineRecipe highestValidity = null;
        RecipeCraftingContext.CraftingCheckResult highestValidityResult = null;
        float validity = 0F;

        for (MachineRecipe recipe : filtered) {
            ActiveMachineRecipe activeRecipe = new ActiveMachineRecipe(recipe, maxParallelism);
            RecipeCraftingContext context = factory.createContext(activeRecipe);
            RecipeCraftingContext.CraftingCheckResult result = factory.onCheck(context);

            if (result.isSuccess()) {
                //并发检查
                foundMachine = factory.getFoundMachine();
                if (foundMachine == null || !foundMachine.equals(currentMachine)) {
                    RecipeCraftingContextPool.returnCtx(context);
                    return null;
                }
                return context;
            } else if (highestValidity == null ||
                    (result.getValidity() >= 0.5F && result.getValidity() > validity)) {
                highestValidity = recipe;
                highestValidityResult = result;
                validity = result.getValidity();
            }
            RecipeCraftingContextPool.returnCtx(context);
        }

        if (highestValidity != null) {
            status = CraftingStatus.failure(
                    highestValidityResult.getFirstErrorMessage(""));
        } else {
            status = CraftingStatus.failure(
                    TileMultiblockMachineController.Type.NO_RECIPE.getUnlocalizedDescription());
        }

        return null;
    }

    private void filterRecipe() {
        Object2IntArrayMap<ResourceLocation> activeRecipeCountMap = new Object2IntArrayMap<>();
        activeRecipeCountMap.defaultReturnValue(0);

        for (ActiveMachineRecipe recipe : running) {
            ResourceLocation registryName = recipe.getRecipe().getRegistryName();
            int prevCount = activeRecipeCountMap.getInt(registryName);
            activeRecipeCountMap.put(registryName, prevCount + 1);
        }

        for (MachineRecipe recipe : recipeList) {
            // If the recipe specifies a thread name, determine if the thread name matches.
            String recipeRequiredName = recipe.getThreadName();
            if (!recipeRequiredName.isEmpty() && (thread == null || !thread.getThreadName().equals(recipeRequiredName))) {
                continue;
            }
            // If the number of running identical recipes in the factory exceeds
            // the maximum number defined by the recipe, then this recipe is not checked.
            int maxThreads = recipe.getMaxThreads();
            if (maxThreads != -1 && activeRecipeCountMap.getInt(recipe.getRegistryName()) >= maxThreads){
                continue;
            }

            filtered.add(recipe);
        }

        activeRecipeCountMap.clear();
    }
}
