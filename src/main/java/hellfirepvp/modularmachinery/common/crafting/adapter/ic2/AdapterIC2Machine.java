package hellfirepvp.modularmachinery.common.crafting.adapter.ic2;

import crafttweaker.util.IEventHandler;
import github.kasuminova.mmce.common.event.recipe.RecipeEvent;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.adapter.RecipeAdapter;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementEnergy;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import ic2.api.recipe.IRecipeInput;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class AdapterIC2Machine extends RecipeAdapter {
    public AdapterIC2Machine(@Nonnull final ResourceLocation registryName) {
        super(registryName);
    }

    protected void convertIC2RecipeToMMRecipe(final ResourceLocation owningMachineName,
                                              final List<RecipeModifier> modifiers,
                                              final Iterable<? extends ic2.api.recipe.MachineRecipe<IRecipeInput, Collection<ItemStack>>> machineRecipes,
                                              final List<MachineRecipe> recipes,
                                              final String recipeRegistryNamePrefix,
                                              final int workTime) {
        for (ic2.api.recipe.MachineRecipe<IRecipeInput, Collection<ItemStack>> machineRecipe : machineRecipes) {
            MachineRecipe recipe = createRecipeShell(
                    new ResourceLocation("ic2", recipeRegistryNamePrefix + incId),
                    owningMachineName,
                    workTime, incId, false);

            int inAmount = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_ITEM, IOType.INPUT, machineRecipe.getInput().getAmount(), false));

            if (inAmount > 0) {
                for (ItemStack input : machineRecipe.getInput().getInputs()) {
                    recipe.addRequirement(new RequirementItem(IOType.INPUT, ItemUtils.copyStackWithSize(input, inAmount)));
                }
            }

            for (final ItemStack output : machineRecipe.getOutput()) {
                int outAmount = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_ITEM, IOType.OUTPUT, output.getCount(), false));
                if (outAmount > 0) {
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
    }
}
