package hellfirepvp.modularmachinery.common.crafting.adapter.tc6;

import crafttweaker.util.IEventHandler;
import github.kasuminova.mmce.common.event.recipe.RecipeEvent;
import github.kasuminova.mmce.common.itemtype.ChancedIngredientStack;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.adapter.RecipeAdapter;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementIngredientArray;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import kport.modularmagic.common.crafting.requirement.RequirementAspect;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.ResourceLocation;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.crafting.IThaumcraftRecipe;
import thaumcraft.api.crafting.InfusionRecipe;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AdapterTC6InfusionMatrix extends RecipeAdapter {
    public static final int BASE_WORK_TIME = 300;

    public AdapterTC6InfusionMatrix() {
        super(new ResourceLocation("thaumcraft", "infusion_matrix"));
    }

    @Nonnull
    @Override
    public Collection<MachineRecipe> createRecipesFor(ResourceLocation owningMachineName, List<RecipeModifier> modifiers, List<ComponentRequirement<?, ?>> additionalRequirements, Map<Class<?>, List<IEventHandler<RecipeEvent>>> eventHandlers, List<String> recipeTooltips) {

        List<MachineRecipe> machineRecipeList = new ArrayList<>();

        for (ResourceLocation string : ThaumcraftApi.getCraftingRecipes().keySet()) {
            IThaumcraftRecipe recipe = ThaumcraftApi.getCraftingRecipes().get(string);
            if (recipe instanceof InfusionRecipe) {
                if (((InfusionRecipe) recipe).getRecipeInput() != null && ((InfusionRecipe) recipe).recipeOutput != null)
                {
                    MachineRecipe machineRecipe = createRecipeShell(new ResourceLocation("thaumcraft", "auto_infusion" + incId), owningMachineName, ((InfusionRecipe) recipe).instability == 0 ? BASE_WORK_TIME : ((InfusionRecipe) recipe).instability * 1000, incId, false);

                    int inAmount = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_ITEM, IOType.INPUT, ((InfusionRecipe) recipe).getRecipeInput().getMatchingStacks().length, false));
                    if (inAmount > 0) {
                        ItemStack[] inputsMiddle = ((InfusionRecipe) recipe).getRecipeInput().getMatchingStacks();
                        List<ChancedIngredientStack> ingredientStackListMiddle = new ArrayList<>(inputsMiddle.length);
                        for(ItemStack stack : inputsMiddle)
                            ingredientStackListMiddle.add(new ChancedIngredientStack(stack));
                        if(!ingredientStackListMiddle.isEmpty())
                            machineRecipe.addRequirement(new RequirementIngredientArray(ingredientStackListMiddle));

                        for (Ingredient iigredient : ((InfusionRecipe) recipe).getComponents()) {
                            ItemStack[] inputs = iigredient.getMatchingStacks();
                            List<ChancedIngredientStack> ingredientStackList = new ArrayList<>(inputs.length);

                            for (ItemStack input : inputs) {
                                ingredientStackList.add(new ChancedIngredientStack(input));

                                /*
                                if(input.getItem() != ItemsTC.primordialPearl) {
                                    input.setItemDamage(input.getItemDamage() - 1);
                                    if(input.getItemDamage() > 0)
                                        machineRecipe.addRequirement(new RequirementItem(IOType.OUTPUT, ItemUtils.copyStackWithSize(input, inAmount)));
                                }
                                */
                            }
                            if(!ingredientStackList.isEmpty())
                                machineRecipe.addRequirement(new RequirementIngredientArray(ingredientStackList));
                        }

                        Object output = ((InfusionRecipe) recipe).recipeOutput;
                        if(output instanceof ItemStack) {
                            int outAmount = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_ITEM, IOType.OUTPUT, ((ItemStack) output).getCount(), false));
                            if (outAmount > 0) {
                                machineRecipe.addRequirement(new RequirementItem(IOType.OUTPUT, ItemUtils.copyStackWithSize((ItemStack) output, outAmount)));
                            }
                        }else if (output != null) {
                            for (ItemStack stack : ((InfusionRecipe) recipe).getRecipeInput().getMatchingStacks()) {
                                if (stack != null) {
                                    Object[] objects = (Object[]) output;
                                    ItemStack copied = stack.copy();
                                    copied.setTagInfo((String) objects[0], (NBTBase) objects[1]);
                                    machineRecipe.addRequirement(new RequirementItem(IOType.OUTPUT, copied));
                                }
                            }
                        }

                        for(Aspect aspect : ((InfusionRecipe) recipe).getAspects().getAspects()){
                            machineRecipe.addRequirement(new RequirementAspect(IOType.INPUT, ((InfusionRecipe) recipe).getAspects().getAmount(aspect), aspect));
                        }
                    }
                    machineRecipeList.add(machineRecipe);
                    incId++;
                }
            }
        }

        return machineRecipeList;
    }
}
