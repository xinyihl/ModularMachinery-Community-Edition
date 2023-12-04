package hellfirepvp.modularmachinery.common.crafting.adapter;

import crafttweaker.util.IEventHandler;
import github.kasuminova.mmce.common.event.recipe.RecipeEvent;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementEnergy;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.Recipes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AdapterIC2Compressor extends RecipeAdapter {
    public static final int workTime = 300;

    public AdapterIC2Compressor() {
        super(new ResourceLocation("ic2", "te_compressor"));
    }

    @Nonnull
    @Override
    public Collection<MachineRecipe> createRecipesFor(ResourceLocation owningMachineName,
                                                      List<RecipeModifier> modifiers,
                                                      List<ComponentRequirement<?, ?>> additionalRequirements,
                                                      Map<Class<?>, List<IEventHandler<RecipeEvent>>> eventHandlers,
                                                      List<String> recipeTooltips) {
        Iterable<? extends ic2.api.recipe.MachineRecipe<IRecipeInput, Collection<ItemStack>>> machineRecipes = Recipes.compressor.getRecipes();

        List<MachineRecipe> recipes = new ArrayList<>(40);
        for (ic2.api.recipe.MachineRecipe<IRecipeInput, Collection<ItemStack>> machineRecipe : machineRecipes) {
            MachineRecipe recipe = createRecipeShell(
                    new ResourceLocation("ic2", "te_compressor_recipe_" + incId),
                    owningMachineName,
                    workTime, incId, false);

            int inAmount = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_ITEM, IOType.INPUT, machineRecipe.getInput().getAmount(), false));

            if (inAmount > 0) {
                for (ItemStack input : machineRecipe.getInput().getInputs()) {
                    recipe.addRequirement(new RequirementItem(IOType.INPUT, ItemUtils.copyStackWithSize(input, inAmount)));
                }
            }

            int outAmount = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_ITEM, IOType.OUTPUT, 1, false));
            if (outAmount > 0) {
                for (ItemStack output : machineRecipe.getOutput()) {
                    recipe.addRequirement(new RequirementItem(IOType.OUTPUT, ItemUtils.copyStackWithSize(output, outAmount)));
                }
            }

            int inEnergy = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_ENERGY, IOType.INPUT, 8, false));
            if (inEnergy > 0) {
                recipe.addRequirement(new RequirementEnergy(IOType.INPUT, inEnergy));
            }

            recipes.add(recipe);
            incId++;
        }

        return recipes;
    }
}
