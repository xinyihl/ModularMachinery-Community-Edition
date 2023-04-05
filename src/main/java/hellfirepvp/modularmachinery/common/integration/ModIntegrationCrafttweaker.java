/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.integration;

import crafttweaker.mc1120.events.ScriptRunEvent;
import hellfirepvp.modularmachinery.common.crafting.RecipeRegistry;
import hellfirepvp.modularmachinery.common.crafting.adapter.RecipeAdapter;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.MachineBuilder;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.MachineModifier;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.MMEvents;
import hellfirepvp.modularmachinery.common.lib.RegistriesMM;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.util.BlockArrayCache;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import youyihj.zenutils.api.reload.ScriptReloadEvent;

import java.util.concurrent.CompletableFuture;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: ModIntegrationCrafttweaker
 * Created by HellFirePvP
 * Date: 18.08.2017 / 10:44
 */
public class ModIntegrationCrafttweaker {
    @SubscribeEvent
    public void onScriptsLoading(ScriptRunEvent.Pre event) {
        RecipeRegistry.getRegistry().clearLingeringRecipes();
        MachineBuilder.WAIT_FOR_LOAD.clear();
        MachineModifier.WAIT_FOR_MODIFY.clear();
    }

    @SubscribeEvent
    public void onScriptsLoaded(ScriptRunEvent.Post event) {
        RecipeRegistry.reloadAdapters();
    }

    @SubscribeEvent
    @Optional.Method(modid = "zenutils")
    public void onScriptsReloading(ScriptReloadEvent.Pre event) {
        ICommandSender sender = event.getRequester();
        sender.sendMessage(new TextComponentTranslation("message.reloading"));

        MachineBuilder.WAIT_FOR_LOAD.clear();
        MachineModifier.WAIT_FOR_MODIFY.clear();
        MMEvents.WAIT_FOR_REGISTER_LIST.clear();

        sender.sendMessage(new TextComponentTranslation(
                "message.cleared.recipes", RecipeRegistry.registeredRecipeCount()));
        RecipeRegistry.getRegistry().clearAllRecipes();
        // Reset RecipeAdapterIncId
        RegistriesMM.ADAPTER_REGISTRY.getValuesCollection().forEach(RecipeAdapter::resetIncId);

        for (DynamicMachine loadedMachine : MachineRegistry.getLoadedMachines()) {
            loadedMachine.getMachineEventHandlers().clear();
            loadedMachine.getSmartInterfaceTypes().clear();
        }
        // Reload JSON Machine
        MachineRegistry.preloadMachines();
        // Reload All Machine
        MachineRegistry.reloadMachine(MachineRegistry.loadMachines(null));
        sender.sendMessage(new TextComponentTranslation(
                "message.reloaded.machines", MachineRegistry.getLoadedMachines().size()));
    }

    @SubscribeEvent
    @Optional.Method(modid = "zenutils")
    public void onScriptsReloaded(ScriptReloadEvent.Post event) {
        ICommandSender sender = event.getRequester();

        MachineRegistry.reloadMachine(MachineBuilder.WAIT_FOR_LOAD);
        // Flush the context to preview the changed structure.
        ModIntegrationJEI.reloadPreviewWrappers();

        CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
                BlockArrayCache.buildCache(MachineRegistry.getLoadedMachines()));

        MachineModifier.loadAll();
        MMEvents.registryAll();

        RecipeRegistry.getRegistry().loadRecipeRegistry(null, true);
        ModIntegrationJEI.reloadRecipeWrappers();
        future.join();

        sender.sendMessage(new TextComponentTranslation(
                "message.reloaded.recipes", RecipeRegistry.registeredRecipeCount()));

        sender.sendMessage(new TextComponentTranslation("message.reloaded"));
    }

}
