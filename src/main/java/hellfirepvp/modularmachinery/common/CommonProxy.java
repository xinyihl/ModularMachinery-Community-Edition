/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common;

import github.kasuminova.mmce.common.concurrent.Action;
import github.kasuminova.mmce.common.concurrent.TaskExecutor;
import github.kasuminova.mmce.common.event.EventHandler;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.container.*;
import hellfirepvp.modularmachinery.common.crafting.IntegrationTypeHelper;
import hellfirepvp.modularmachinery.common.crafting.RecipeRegistry;
import hellfirepvp.modularmachinery.common.crafting.adapter.RecipeAdapterRegistry;
import hellfirepvp.modularmachinery.common.data.Config;
import hellfirepvp.modularmachinery.common.data.ModDataHolder;
import hellfirepvp.modularmachinery.common.integration.ModIntegrationCrafttweaker;
import hellfirepvp.modularmachinery.common.integration.ModIntegrationTOP;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.MachineBuilder;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.MachineModifier;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.MMEvents;
import hellfirepvp.modularmachinery.common.integration.fluxnetworks.ModIntegrationFluxNetworks;
import hellfirepvp.modularmachinery.common.lib.BlocksMM;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.machine.factory.RecipeThread;
import hellfirepvp.modularmachinery.common.registry.internal.InternalRegistryPrimer;
import hellfirepvp.modularmachinery.common.registry.internal.PrimerEventHandler;
import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import hellfirepvp.modularmachinery.common.tiles.TileParallelController;
import hellfirepvp.modularmachinery.common.tiles.TileSmartInterface;
import hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch;
import hellfirepvp.modularmachinery.common.tiles.base.TileFluidTank;
import hellfirepvp.modularmachinery.common.tiles.base.TileItemBus;
import hellfirepvp.modularmachinery.common.util.BlockArrayCache;
import hellfirepvp.modularmachinery.common.util.FuelItemHelper;
import ink.ikx.mmce.core.AssemblyEventHandler;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import javax.annotation.Nullable;
import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: CommonProxy
 * Created by HellFirePvP
 * Date: 26.06.2017 / 21:00
 */
public class CommonProxy implements IGuiHandler {

    public static final ModDataHolder dataHolder = new ModDataHolder();
    public static CreativeTabs creativeTabModularMachinery;
    public static InternalRegistryPrimer registryPrimer;

    public CommonProxy() {
        registryPrimer = new InternalRegistryPrimer();
        MinecraftForge.EVENT_BUS.register(new PrimerEventHandler(registryPrimer));
    }

    public static void loadModData(File configDir) {
        dataHolder.setup(configDir);
        if (dataHolder.requiresDefaultMachinery()) {
            dataHolder.copyDefaultMachinery();
        }
    }

    private static void checkThirdPartyServer() {
        if (isClassExist("catserver.server.CatServer") || isClassExist("com.mohistmc.MohistMC")) {
            ModularMachinery.log.warn("//////// Plugin Server Detected! ////////");
            ModularMachinery.log.warn("Plugin server will break MMCE's asynchronous functionality.");
            ModularMachinery.log.warn("Plugin server compatibility mode is enabled!");
            ModularMachinery.log.warn("This will cause asynchronous effects to drop and raise the overhead of the main thread!");
            ModularMachinery.pluginServerCompatibleMode = true;
        } else {
            ModularMachinery.pluginServerCompatibleMode = false;
        }
    }

    private static boolean isClassExist(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void preInit() {
        creativeTabModularMachinery = new CreativeTabs(ModularMachinery.MODID) {
            @Override
            public ItemStack createIcon() {
                return new ItemStack(BlocksMM.blockController);
            }
        };

        NetworkRegistry.INSTANCE.registerGuiHandler(ModularMachinery.MODID, this);

        if (Mods.CRAFTTWEAKER.isPresent()) {
            MinecraftForge.EVENT_BUS.register(new ModIntegrationCrafttweaker());
        }
        if (Mods.FLUX_NETWORKS.isPresent() && Config.enableFluxNetworksIntegration) {
            ModIntegrationFluxNetworks.preInit();
            ModularMachinery.log.info("[ModularMachinery-CE] Flux Networks integration is enabled! Lets your network to transmit more power!");
        }

        MachineRegistry.preloadMachines();

        MinecraftForge.EVENT_BUS.register(AssemblyEventHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(new EventHandler());

        MinecraftForge.EVENT_BUS.register(ModularMachinery.EXECUTE_MANAGER);
        ModularMachinery.EXECUTE_MANAGER.init();
        checkThirdPartyServer();
        ModularMachinery.log.info(String.format("[ModularMachinery-CE] Parallel executor is ready (%s Threads), Let's get started!!!", TaskExecutor.FORK_JOIN_POOL.getParallelism()));
    }

    public void init() {
        FuelItemHelper.initialize();
        IntegrationTypeHelper.filterModIdComponents();
        IntegrationTypeHelper.filterModIdRequirementTypes();

        MachineRegistry.registerMachines(MachineRegistry.loadMachines(null));
        MachineRegistry.registerMachines(MachineBuilder.WAIT_FOR_LOAD);
        CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
                BlockArrayCache.buildCache(MachineRegistry.getLoadedMachines()));

        MachineModifier.loadAll();
        MMEvents.registryAll();
        RecipeAdapterRegistry.registerDynamicMachineAdapters();

        RecipeRegistry.getRegistry().loadRecipeRegistry(null, true);
        for (Action action : RecipeThread.WAIT_FOR_ADD) {
            action.doAction();
        }
        RecipeThread.WAIT_FOR_ADD.clear();

        future.join();

        if (Mods.TOP.isPresent()) {
            ModIntegrationTOP.registerProvider();
            ModularMachinery.log.info("[ModularMachinery-CE] TheOneProbe integration is enabled! Stop looking at the dark controller gui!");
        }
    }

    public void postInit() {
    }

    public void registerBlockModel(Block block) {
    }

    public void registerItemModel(Item item) {
    }

    @Nullable
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        GuiType type = GuiType.values()[MathHelper.clamp(ID, 0, GuiType.values().length - 1)];
        Class<? extends TileEntity> required = type.requiredTileEntity;
        TileEntity present = null;
        if (required != null) {
            TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
            if (te != null && required.isAssignableFrom(te.getClass())) {
                present = te;
            } else {
                return null;
            }
        }
        switch (type) {
            case CONTROLLER:
                return new ContainerController((TileMachineController) present, player);
            case FACTORY:
                return new ContainerFactoryController((TileFactoryController) present, player);
            case BUS_INVENTORY:
                return new ContainerItemBus((TileItemBus) present, player);
            case TANK_INVENTORY:
                return new ContainerFluidHatch((TileFluidTank) present, player);
            case ENERGY_INVENTORY:
                return new ContainerEnergyHatch((TileEnergyHatch) present, player);
            case SMART_INTERFACE:
                return new ContainerSmartInterface((TileSmartInterface) present, player);
            case PARALLEL_CONTROLLER:
                return new ContainerParallelController((TileParallelController) present, player);
            case BLUEPRINT_PREVIEW:
                break;
        }
        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

    public enum GuiType {

        CONTROLLER(TileMachineController.class),
        FACTORY(TileFactoryController.class),
        BUS_INVENTORY(TileItemBus.class),
        TANK_INVENTORY(TileFluidTank.class),
        ENERGY_INVENTORY(TileEnergyHatch.class),
        SMART_INTERFACE(TileSmartInterface.class),
        PARALLEL_CONTROLLER(TileParallelController.class),
        BLUEPRINT_PREVIEW(null);

        public final Class<? extends TileEntity> requiredTileEntity;

        GuiType(@Nullable Class<? extends TileEntity> requiredTileEntity) {
            this.requiredTileEntity = requiredTileEntity;
        }
    }
}
