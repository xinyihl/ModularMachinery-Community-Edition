/**
 * Example Code For ModularMachinery-CE
 * This sample script only lists the common code usage of MMCE, for other usage, please refer to the source code.
 */

import crafttweaker.item.IItemStack;
import crafttweaker.liquid.ILiquidStack;
import crafttweaker.data.IData;

import mods.modularmachinery.IngredientArrayBuilder;
import mods.modularmachinery.IngredientArrayPrimer;
import mods.modularmachinery.RecipePrimer;
import mods.modularmachinery.RecipeBuilder;
import mods.modularmachinery.RecipeCheckEvent;
import mods.modularmachinery.RecipeStartEvent;
import mods.modularmachinery.RecipePreTickEvent;
import mods.modularmachinery.RecipeTickEvent;
import mods.modularmachinery.RecipeFinishEvent;
import mods.modularmachinery.RecipeModifierBuilder;

import mods.modularmachinery.MMEvents;
import mods.modularmachinery.MachineStructureFormedEvent;
import mods.modularmachinery.MachineTickEvent;
import mods.modularmachinery.ControllerGUIRenderEvent;

import mods.modularmachinery.IMachineController;
import mods.modularmachinery.SmartInterfaceData;
import mods.modularmachinery.MachineModifier;
import mods.modularmachinery.SmartInterfaceType;

//========== RecipeBuilder / RecipePrimer Usages ==========
val workTime = 5; // Recipe worktime.
val recipeSortPriority = 5; // The smaller the value, the higher the priority, affecting the order of mechanical scanning recipes before and after.
val doesVoidPerTickFaliure = false; // If there is a problem during operation (e.g. lack of energy input), will the recipe be destroyed and stop working.

// Use RecipeBuilder to create a RecipePrimer.
// RecipeBuilder.newBuilder("recipe_registryname", "machine_registryname", workTime) // Another usage.
// RecipeBuilder.newBuilder("recipe_registryname", "machine_registryname", workTime, recipeSortPriority) // Another usage.
RecipeBuilder.newBuilder("recipe_registryname", "machine_registryname", workTime, recipeSortPriority, doesVoidPerTickFaliure)
    //========== Energy PerTick Input / Outputs ==========
    .addEnergyPerTickInput(3000000000) //Energy Input, The maximum value is no longer 2147483647, There can only be one EnergyPerTickInput per recipe!
    .addEnergyPerTickOutput(3000000000) // Energy Output, The maximum value is no longer 2147483647, There can only be one EnergyPerTickOutput per recipe!

    //========== Item Input / Inputs / Output / Outputs ==========
    //Also suitable for Item Output / Outputs
    .addItemInput(<minecraft:diamond> * 1) // Item Input
    .addItemInput(<ore:ingotIron> * 1) // Also supports oreDict Input
    .addItemInput(<ore:ingotIron>, 1) // It's deprecated! Use the method above.
    .addItemInputs(
        <minecraft:diamond> * 1,
        <ore:ingotIron> * 1
    ) // Add multiple item inputs at once!
    .addItemInputs([
        <minecraft:diamond> * 1,
        <ore:ingotIron> * 1,
    ]) // Also support array.
    // Advanced Functions

    //========== Fluid Input / Inputs / Output / Outputs ==========
    //Also suitable for Fluid Output / Outputs
    .addFluidInput(<liquid:water> * 1000) // Fluid / Liquid Input.
    .addFluidInputs(
        <liquid:water> * 1000,
        <liquid:lava> * 1000
    ) // Add multiple item inputs at once!
    .addFluidInputs([
        <liquid:water> * 1000,
        <liquid:lava> * 1000,
    ]) // Also support array.

    //========== Gas Input / Output ==========
    .addGasInput("fusionfuel", 1000) // Gas Name And Amount, Need Mekanism Mod Installed.
    .addGasOutput("fusionfuel", 500) // Gas Name And Amount, Need Mekanism Mod Installed.

    //========== General Input / Output ==========
    .addInput(<minecraft:diamond> * 4) // Support Item Input.
    .addInput(<liquid:water> * 1000) // Also Support Fluid / Liquid Input!
    .addInputs([
        <minecraft:diamond> * 4,
        <liquid:water> * 1000,
    ]) // Permission to put them together!

    //========== Catalyst Input (Advanced) ==========
    .addCatalystInput(
        <minecraft:diamond> * 4,
        ["This tips will be shown JEI",],
        [RecipeModifierBuilder.create(  "modularmachinery:energy", "input",        0.5,   1,                         false).build(),]
                                      // ^                          ^               ^      ^                          ^
                                     // Requirement Type,          Operation Type, Value, Operation(0 is +, 1 is *), Affect Chance
    ).setChance(0.5) // Adds a catalyst input, which is optional and applies the parameters within RecipeModifier when the machine detects a catalyst input. Also allows to set the chance.

    //========== IngredientArray Input (Advanced) ==========
    .addIngredientArrayInput(IngredientArrayBuilder.newBuilder()
        .addIngredient(<ore:ingotIron> * 8) // Allow IItemStack and oreDict.
        .addIngredients([
            <ore:gemDiamond> * 1,
            <minecraft:dye:4> * 8
        ]) // Multiple types, allowing oreDict.
        .addIngredient(<minecraft:gold_ingot> * 8).setChance(0.5) // Set individual probabilities for each input type.
        .addChancedIngredient(<minecraft:dye:4> * 16, 0.75) // Like above!
        .build() // This function is optional.
    ) // Add a set of inputs and the machine will only match and consume one of the items in the set, with the priority depending on the order in which they are added in the script.

    //========== Single Requirement Modifier Functions (Advanced) ==========
    // Must be called after addXXXInput() / addXXXOutput()!
    .setTag("tag") // This is not NBTTag! This is ComponentSelectorTag. You can use <modid:name>.withTag() to add nbt check.
    .setChance(0.11) // Set the Input / Output Chance (0 ~ 1 (0% ~ 100%)).

    // Only Support Parallelizable Inputs/ Outputs.
    // Supported Input / Outputs:
    // energyPerTick Input / Output
    // item Input / Output
    // fluid Input / Output
    // gas Input / Output
    // ingredientArray Input / Output
    .setParallelizeUnaffected(true) // Set whether it is independent of the parallel multiplier (e.g. item input always consumes a fixed amount and does not vary with the number of parallels).

    // Only Support Item Input / Output.
    .addItemInput(<mekanism:machineblock2:11>).setPreViewNBT({tier: 3}) // You can set preview nbt in JEI, This does not affect the NBT determination in real situations.
    .addItemInput(<mekanism:machineblock2:11>).setNBTChecker(function(controller as IMachineController, item as IItemStack) as bool {
        val tagMap = item.tag.asMap();
        val tier = tagMap["tier"];
        if (isNull(tier)) {
            return false;
        }
        val value = tier.asInt();
        if (value >= 1 && value <= 4) {
            return true;
        }
        return false;
    }) // An example of an advanced check item NBT for people with special needs, return boolean.
    .addItemInput(<mekanism:machineblock2:11>).addItemModifier(function(controller as IMachineController, item as IItemStack) as IItemStack {
        if (item.amount >= 32) {
            return item * 32;
        } else {
            return item;
        }
    }) // An example of modify the type or quantity of items as they are input or output, for those with certain special needs. return IItemStack.

    //========== Event Handlers (Advanced) ==========
    // See the source code in hellfirepvp.modularmachinery.common.integration.crafttweaker.event.recipe.RecipeEvent;
    .addCheckHandler(function(event as RecipeCheckEvent) {
        // It will trigger when the recipe is scanned mechanically.
        // Handle RecipeCheckEvent, And do something...
    })
    .addStartHandler(function(event as RecipeStartEvent) {
        // It will trigger when the machine starts running this recipe.
        // Handle RecipeStartEvent, And do something...
    })
    .addPreTickHandler(function(event as RecipePreTickEvent) {
        // Triggered when the machine is running this recipe, it will trigger multiple times, before the Tick starts (e.g., before energy consumption).
        // Handle RecipePreTickEvent, And do something...
    })
    .addTickHandler(function(event as RecipeTickEvent) {
        // Triggered when the machine is running this recipe, will trigger multiple times, triggered after Tick is completed (e.g., energy has been consumed).
        // Handle RecipeTickEvent, And do something...
    })
    .addFinishHandler(function(event as RecipeFinishEvent) {
        // Triggered when machinery finished this recipe.
        // Handle RecipeFinishEvent, And do something...
    })

    //========== Smart Interface (Advanced) ==========
    .addSmartInterfaceDataInput("speed", 0, 1) // type, minValue, maxValue.
    // .addSmartInterfaceDataInput("speed", 1) // type, min & max value, will be supported in future version.

    //========== Misc ==========
    .addRecipeTooltip("Add tooltips, It will be shown in JEI.", "Multiple lines can be written at once.")
    .setParallelized(true) // Is this recipe allowed to be parallelized? You can set the default value in `modularmachinery.cfg`.

    .build(); // At the end, this function must be called.

//========== MMEvents ==========
// See the source code in github.kasuminova.mmce.common.event.machine.MachineEvent;
MMEvents.onStructureFormed("machine_registryname", function(event as MachineStructureFormedEvent) {
    // Triggered when the machine forms a structure.
    // Handle MachineStructureFormedEvent, And do something...
});

MMEvents.onMachineTick("machine_registryname", function(event as MachineTickEvent) {
    // Only the machinery forming the structure can trigger, once every Tick, before the recipe is run.
    // Handle MachineTickEvent, And do something...
});

MMEvents.onControllerGUIRender("machine_registryname", function(event as ControllerGUIRenderEvent) {
    // Client only, triggered when the player opens the controller GUI corresponding to the mechanical registration name, allowing additional information to be added to the controller GUI within this event.
    // Handle ControllerGUIRenderEvent, And do something...
});
