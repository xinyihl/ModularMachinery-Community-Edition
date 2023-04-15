package hellfirepvp.modularmachinery.common.crafting.adapter.nco;

import crafttweaker.util.IEventHandler;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.adapter.RecipeAdapter;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementEnergy;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluid;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.recipe.RecipeEvent;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import nc.recipe.BasicRecipe;
import nc.recipe.NCRecipes;
import nc.recipe.ingredient.IFluidIngredient;
import nc.recipe.ingredient.IItemIngredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AdapterNCOChemicalReactor extends AdapterNCOMachine {
    public static final int WORK_TIME = 400;
    public static final int BASE_ENERGY_USAGE = 5;

    public AdapterNCOChemicalReactor() {
        super(new ResourceLocation("nuclearcraft", "chemical_reactor"));
    }

    @Nonnull
    @Override
    public Collection<MachineRecipe> createRecipesFor(ResourceLocation owningMachineName, List<RecipeModifier> modifiers, List<ComponentRequirement<?, ?>> additionalRequirements, Map<Class<?>, List<IEventHandler<RecipeEvent>>> eventHandlers, List<String> recipeTooltips) {
        List<BasicRecipe> recipeList = NCRecipes.chemical_reactor.getRecipeList();
        List<MachineRecipe> machineRecipeList = new ArrayList<>(recipeList.size());

        for (BasicRecipe basicRecipe : recipeList) {
            MachineRecipe recipe = createRecipeShell(new ResourceLocation("nuclearcraft", "infuser_" + incId),
                    owningMachineName, (int) basicRecipe.getBaseProcessTime(Math.round(RecipeModifier.applyModifiers(
                            modifiers, RequirementTypesMM.REQUIREMENT_DURATION, IOType.INPUT, WORK_TIME, false))),
                    incId, false
            );

            RecipeAdapter.addAdditionalRequirements(recipe, additionalRequirements, eventHandlers, recipeTooltips);

            for (IFluidIngredient fluidIngredient : basicRecipe.getFluidIngredients()) {
                FluidStack stack = fluidIngredient.getStack().copy();
                int inAmount = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_FLUID, IOType.INPUT, stack.amount, false));
                if (inAmount > 0) {
                    stack.amount = inAmount;
                    recipe.addRequirement(new RequirementFluid(IOType.INPUT, stack));
                }
            }

            for (IItemIngredient itemProduct : basicRecipe.getItemProducts()) {
                int outputAmount = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_ITEM, IOType.INPUT, itemProduct.getStack().getCount(), false));
                if (outputAmount > 0) {
                    recipe.addRequirement(new RequirementItem(IOType.OUTPUT, ItemUtils.copyStackWithSize(itemProduct.getStack(), outputAmount)));
                }
            }

            recipe.addRequirement(new RequirementEnergy(IOType.INPUT, Math.round(RecipeModifier.applyModifiers(
                    modifiers, RequirementTypesMM.REQUIREMENT_ENERGY, IOType.INPUT, BASE_ENERGY_USAGE, false)))
            );

            machineRecipeList.add(recipe);
            incId++;

        }

        return machineRecipeList;
    }
}
