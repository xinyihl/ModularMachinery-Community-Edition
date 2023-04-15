package hellfirepvp.modularmachinery.common.crafting.adapter.tconstruct;

import crafttweaker.util.IEventHandler;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.adapter.RecipeAdapter;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluid;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.recipe.RecipeCheckEvent;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.recipe.RecipeEvent;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.smeltery.MeltingRecipe;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AdapterSmelteryMeltingRecipe extends RecipeAdapter {
    public static final int WORK_TIME = 4;

    public AdapterSmelteryMeltingRecipe() {
        super(new ResourceLocation("tconstruct", "smeltery_melting"));
    }

    @Nonnull
    @Override
    public Collection<MachineRecipe> createRecipesFor(ResourceLocation owningMachineName, List<RecipeModifier> modifiers, List<ComponentRequirement<?, ?>> additionalRequirements, Map<Class<?>, List<IEventHandler<RecipeEvent>>> eventHandlers, List<String> recipeTooltips) {
        List<MeltingRecipe> meltingRecipes = TinkerRegistry.getAllMeltingRecipies();
        List<MachineRecipe> machineRecipeList = new ArrayList<>(meltingRecipes.size());

        for (MeltingRecipe meltingRecipe : meltingRecipes) {
            MachineRecipe recipe = createRecipeShell(new ResourceLocation("tconstruct", "smeltery_melting_" + incId),
                    owningMachineName, Math.round(RecipeModifier.applyModifiers(
                            modifiers, RequirementTypesMM.REQUIREMENT_DURATION, IOType.INPUT, WORK_TIME, false)),
                    incId, false
            );
            recipe.addRecipeEventHandler(RecipeCheckEvent.class, (IEventHandler<RecipeCheckEvent>) event -> {
                ActiveMachineRecipe machineRecipe = event.getRecipe();
                machineRecipe.getDataCompound().setInteger("temperatureRequired", meltingRecipe.temperature);
            });
            RecipeAdapter.addAdditionalRequirements(recipe, additionalRequirements, eventHandlers, recipeTooltips);

            // Item Input
            List<ItemStack> inputs = meltingRecipe.input.getInputs();
            for (ItemStack stack : inputs) {
                int inAmount = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_ITEM, IOType.INPUT, stack.getCount(), false));
                if (inAmount > 0) {
                    ItemStack input = ItemUtils.copyStackWithSize(stack, inAmount);
                    recipe.addRequirement(new RequirementItem(IOType.INPUT, input));
                }
            }

            // Fluid Output
            FluidStack output = meltingRecipe.getResult().copy();
            int inAmount = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_FLUID, IOType.INPUT, output.amount, false));
            if (inAmount > 0) {
                output.amount = inAmount;
                recipe.addRequirement(new RequirementFluid(IOType.INPUT, output));
            }

            machineRecipeList.add(recipe);
        }

        return machineRecipeList;
    }
}
