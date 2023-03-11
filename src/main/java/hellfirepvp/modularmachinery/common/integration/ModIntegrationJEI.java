/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.integration;

import com.google.common.collect.Lists;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.RecipeRegistry;
import hellfirepvp.modularmachinery.common.integration.ingredient.HybridFluid;
import hellfirepvp.modularmachinery.common.integration.ingredient.HybridFluidGas;
import hellfirepvp.modularmachinery.common.integration.ingredient.HybridFluidRenderer;
import hellfirepvp.modularmachinery.common.integration.ingredient.HybridStackHelper;
import hellfirepvp.modularmachinery.common.integration.preview.CategoryStructurePreview;
import hellfirepvp.modularmachinery.common.integration.preview.StructurePreviewWrapper;
import hellfirepvp.modularmachinery.common.integration.recipe.CategoryDynamicRecipe;
import hellfirepvp.modularmachinery.common.integration.recipe.DynamicRecipeWrapper;
import hellfirepvp.modularmachinery.common.integration.recipe.RecipeLayoutHelper;
import hellfirepvp.modularmachinery.common.item.ItemBlueprint;
import hellfirepvp.modularmachinery.common.lib.BlocksMM;
import hellfirepvp.modularmachinery.common.lib.ItemsMM;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import mezz.jei.api.*;
import mezz.jei.api.ingredients.IIngredientRegistry;
import mezz.jei.api.ingredients.IModIngredientRegistration;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import mezz.jei.api.recipe.IStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Optional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: ModIntegrationJEI
 * Created by HellFirePvP
 * Date: 10.07.2017 / 19:22
 */
@JEIPlugin
public class ModIntegrationJEI implements IModPlugin {

    public static final String CATEGORY_PREVIEW = "modularmachinery.preview";
    private static final Map<DynamicMachine, CategoryDynamicRecipe> recipeCategories = new HashMap<>();
    public static IStackHelper stackHelper;
    public static IJeiHelpers jeiHelpers;
    public static IIngredientRegistry ingredientRegistry;
    public static IRecipeRegistry recipeRegistry;

    public static String getCategoryStringFor(DynamicMachine machine) {
        return "modularmachinery.recipes." + machine.getRegistryName().getPath();
    }

    public static CategoryDynamicRecipe getCategory(DynamicMachine machine) {
        return recipeCategories.get(machine);
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistry subtypeRegistry) {
        subtypeRegistry.registerSubtypeInterpreter(ItemsMM.blueprint, (s) -> {
            DynamicMachine machine = ItemBlueprint.getAssociatedMachine(s);
            if (machine == null) {
                return ISubtypeRegistry.ISubtypeInterpreter.NONE;
            }
            return machine.getRegistryName().toString();
        });
    }

    @Override
    public void registerIngredients(IModIngredientRegistration registry) {
        try {
            registry.register(() -> HybridFluid.class, Lists.newArrayList(), new HybridStackHelper<>(), new HybridFluidRenderer<>());
            if (Mods.MEKANISM.isPresent()) {
                registerHybridGas(registry);
            }
        } catch (Exception exc) {
            ModularMachinery.log.warn("Error setting up HybridFluid JEI registration! Check the log after this for more details! Report this error!");
            exc.printStackTrace();
            throw exc;
        }
    }

    @Optional.Method(modid = "mekanism")
    private void registerHybridGas(IModIngredientRegistration registry) {
        registry.register(() -> HybridFluidGas.class, Lists.newArrayList(), new HybridStackHelper<>(), new HybridFluidRenderer<>());
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        jeiHelpers = registry.getJeiHelpers();
        RecipeLayoutHelper.init();

        registry.addRecipeCategories(new CategoryStructurePreview());

        for (DynamicMachine machine : MachineRegistry.getRegistry()) {
            CategoryDynamicRecipe recipe = new CategoryDynamicRecipe(machine);
            recipeCategories.put(machine, recipe);
            registry.addRecipeCategories(recipe);
        }
    }

    @Override
    public void register(IModRegistry registry) {
        jeiHelpers = registry.getJeiHelpers();
        ingredientRegistry = registry.getIngredientRegistry();
        RecipeLayoutHelper.init();

        registry.addRecipeCatalyst(new ItemStack(BlocksMM.blockController), CATEGORY_PREVIEW);
        for (DynamicMachine machine : MachineRegistry.getRegistry()) {
            ItemStack stack = new ItemStack(ItemsMM.blueprint);
            ItemBlueprint.setAssociatedMachine(stack, machine);
            String machineCategory = getCategoryStringFor(machine);
            registry.addRecipeCatalyst(stack, machineCategory);
        }

        List<StructurePreviewWrapper> previews = Lists.newArrayList();
        for (DynamicMachine machine : MachineRegistry.getRegistry()) {
            previews.add(new StructurePreviewWrapper(machine));
        }
        registry.addRecipes(previews, CATEGORY_PREVIEW);

        for (DynamicMachine machine : MachineRegistry.getRegistry()) {
            Iterable<MachineRecipe> recipes = RecipeRegistry.getRecipesFor(machine);
            List<DynamicRecipeWrapper> recipeWrappers = new ArrayList<>();
            for (MachineRecipe recipe : recipes) {
                recipeWrappers.add(new DynamicRecipeWrapper(recipe));
            }
            registry.addRecipes(recipeWrappers, getCategoryStringFor(machine));
        }

        BlockController.MACHINE_CONTROLLERS.values().forEach(controller ->
                registry.addRecipeCatalyst(new ItemStack(controller),
                        getCategoryStringFor(controller.getParentMachine()))
        );
        BlockController.MOC_MACHINE_CONTROLLERS.values().forEach(controller ->
                registry.addRecipeCatalyst(new ItemStack(controller),
                        getCategoryStringFor(controller.getParentMachine()))
        );
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        recipeRegistry = jeiRuntime.getRecipeRegistry();
    }

}
