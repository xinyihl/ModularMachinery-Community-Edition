package hellfirepvp.modularmachinery.common.crafting.adapter;

import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementEnergy;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import nc.recipe.BasicRecipe;
import nc.recipe.NCRecipes;
import nc.recipe.ingredient.IItemIngredient;
import nc.recipe.ingredient.ItemArrayIngredient;
import nc.recipe.ingredient.OreIngredient;
import nc.recipe.processor.AlloyFurnaceRecipes;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AdapterNCAlloyFurnace extends RecipeAdapter {
    public static final int WORK_TIME = 400;
    public static final int BASE_ENERGY_USAGE = 10;
    private int incId = 0;

    public AdapterNCAlloyFurnace() {
        super(new ResourceLocation("nuclearcraft", "alloy_furnace"));
    }

    @Nonnull
    @Override
    public Collection<MachineRecipe> createRecipesFor(ResourceLocation owningMachineName, List<RecipeModifier> modifiers, List<ComponentRequirement<?, ?>> additionalRequirements) {
        AlloyFurnaceRecipes alloyFurnaceRecipes = NCRecipes.alloy_furnace;

        List<BasicRecipe> recipeList = alloyFurnaceRecipes.getRecipeList();
        List<MachineRecipe> machineRecipeList = new ArrayList<>(recipeList.size());

        for (BasicRecipe basicRecipe : recipeList) {
            MachineRecipe recipe = createRecipeShell(new ResourceLocation("nuclearcraft", "alloy_furnace_" + incId),
                    owningMachineName, (int) basicRecipe.getBaseProcessTime(Math.round(RecipeModifier.applyModifiers(
                            modifiers, RequirementTypesMM.REQUIREMENT_DURATION, IOType.INPUT, WORK_TIME, false))),
                    incId, false
            );

            for (ComponentRequirement<?, ?> additionalRequirement : additionalRequirements) {
                recipe.addRequirement(additionalRequirement.deepCopy());
            }

            for (IItemIngredient iItemIngredient : basicRecipe.getItemIngredients()) {
                int inAmount = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_ITEM, IOType.INPUT, iItemIngredient.getStack().getCount(), false));
                if (inAmount > 0) {
                    if (iItemIngredient instanceof OreIngredient) {
                        OreIngredient oreIngredient = (OreIngredient) iItemIngredient;
                        recipe.addRequirement(new RequirementItem(IOType.INPUT, oreIngredient.oreName, inAmount));
                        continue;
                    }

                    if (iItemIngredient instanceof ItemArrayIngredient) {
                        ItemArrayIngredient arrayIngredient = (ItemArrayIngredient) iItemIngredient;
                        IItemIngredient oreIngredient = arrayIngredient.ingredientList.get(0);
                        if (oreIngredient instanceof OreIngredient){
                            recipe.addRequirement(new RequirementItem(IOType.INPUT, ((OreIngredient) oreIngredient).oreName, inAmount));
                            continue;
                        }
                    }
                    
                    recipe.addRequirement(new RequirementItem(IOType.INPUT, ItemUtils.copyStackWithSize(iItemIngredient.getStack(), inAmount)));
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
