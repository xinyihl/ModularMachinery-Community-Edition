/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common;

import github.kasuminova.mmce.common.capability.CapabilityUpgrade;
import github.kasuminova.mmce.common.concurrent.TaskExecutor;
import github.kasuminova.mmce.common.container.ContainerMEFluidInputBus;
import github.kasuminova.mmce.common.container.ContainerMEFluidOutputBus;
import github.kasuminova.mmce.common.container.ContainerMEItemInputBus;
import github.kasuminova.mmce.common.container.ContainerMEItemOutputBus;
import github.kasuminova.mmce.common.handler.EventHandler;
import github.kasuminova.mmce.common.handler.UpgradeEventHandler;
import github.kasuminova.mmce.common.handler.UpgradeMachineEventHandler;
import github.kasuminova.mmce.common.integration.ModIntegrationAE2;
import github.kasuminova.mmce.common.tile.MEFluidInputBus;
import github.kasuminova.mmce.common.tile.MEFluidOutputBus;
import github.kasuminova.mmce.common.tile.MEItemInputBus;
import github.kasuminova.mmce.common.tile.MEItemOutputBus;
import github.kasuminova.mmce.common.tile.base.MEMachineComponent;
import github.kasuminova.mmce.common.util.concurrent.Action;
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
import hellfirepvp.modularmachinery.common.machine.factory.FactoryRecipeThread;
import hellfirepvp.modularmachinery.common.registry.internal.InternalRegistryPrimer;
import hellfirepvp.modularmachinery.common.registry.internal.PrimerEventHandler;
import hellfirepvp.modularmachinery.common.tiles.*;
import hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch;
import hellfirepvp.modularmachinery.common.tiles.base.TileFluidTank;
import hellfirepvp.modularmachinery.common.tiles.base.TileItemBus;
import hellfirepvp.modularmachinery.common.util.BlockArrayCache;
import hellfirepvp.modularmachinery.common.util.FuelItemHelper;
import ink.ikx.mmce.core.AssemblyEventHandler;
import kport.modularmagic.common.container.ContainerLifeEssence;
import kport.modularmagic.common.crafting.component.ModularMagicComponents;
import kport.modularmagic.common.crafting.requirement.types.ModularMagicRequirements;
import kport.modularmagic.common.item.ModularMagicItems;
import kport.modularmagic.common.tile.*;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;

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

        CapabilityUpgrade.register();

        MinecraftForge.EVENT_BUS.register(ModularMachinery.EXECUTE_MANAGER);
        MinecraftForge.EVENT_BUS.register(AssemblyEventHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(new EventHandler());
        MinecraftForge.EVENT_BUS.register(new UpgradeEventHandler());
        ModularMachinery.EVENT_BUS.register(new UpgradeMachineEventHandler());

        ModularMachinery.EXECUTE_MANAGER.init();
        ModularMachinery.log.info(String.format("[ModularMachinery-CE] Parallel executor is ready (%s Threads), Let's get started!!!", TaskExecutor.THREAD_COUNT));

        ModularMagicItems.initItems();
        ModularMagicComponents.initComponents();
        ModularMagicRequirements.initRequirements();

        //ModularMagic
        if(Mods.BM2.isPresent()) {
            GameRegistry.registerTileEntity(TileWillProvider.Input.class, new ResourceLocation(ModularMachinery.MODID, "tilewillproviderinput"));
            GameRegistry.registerTileEntity(TileWillProvider.Output.class, new ResourceLocation(ModularMachinery.MODID, "tilewillprovideroutput"));
            GameRegistry.registerTileEntity(TileLifeEssenceProvider.Input.class, new ResourceLocation(ModularMachinery.MODID, "tilelifeessenceproviderinput"));
            GameRegistry.registerTileEntity(TileLifeEssenceProvider.Output.class, new ResourceLocation(ModularMachinery.MODID, "tilelifeessenceprovideroutput"));
        }
        if(Mods.TC6.isPresent()) {
            GameRegistry.registerTileEntity(TileAspectProvider.Input.class, new ResourceLocation(ModularMachinery.MODID, "tileaspectproviderinput"));
            GameRegistry.registerTileEntity(TileAspectProvider.Output.class, new ResourceLocation(ModularMachinery.MODID, "tileaspectprovideroutput"));
        }
        if(Mods.EXU2.isPresent()) {
            GameRegistry.registerTileEntity(TileGridProvider.Input.class, new ResourceLocation(ModularMachinery.MODID, "tilegridproviderinput"));
            GameRegistry.registerTileEntity(TileGridProvider.Output.class, new ResourceLocation(ModularMachinery.MODID, "tilegridprovideroutput"));
            GameRegistry.registerTileEntity(TileRainbowProvider.class, new ResourceLocation(ModularMachinery.MODID, "tilerainbowprovider"));
        }
        if(Mods.ASTRAL.isPresent()) {
            GameRegistry.registerTileEntity(TileStarlightInput.class, new ResourceLocation(ModularMachinery.MODID, "tilestarlightinput"));
            GameRegistry.registerTileEntity(TileStarlightOutput.class, new ResourceLocation(ModularMachinery.MODID, "tilestarlightoutput"));
            GameRegistry.registerTileEntity(TileConstellationProvider.class, new ResourceLocation(ModularMachinery.MODID, "tileconstellationprovider"));
        }
        if(Mods.NATURESAURA.isPresent()) {
            GameRegistry.registerTileEntity(TileAuraProvider.Input.class, new ResourceLocation(ModularMachinery.MODID, "tileauraproviderinput"));
            GameRegistry.registerTileEntity(TileAuraProvider.Output.class, new ResourceLocation(ModularMachinery.MODID, "tileauraprovideroutput"));
        }
        if(Mods.BOTANIA.isPresent()) {
            GameRegistry.registerTileEntity(TileManaProvider.Input.class, new ResourceLocation(ModularMachinery.MODID, "tilemanainput"));
            GameRegistry.registerTileEntity(TileManaProvider.Output.class, new ResourceLocation(ModularMachinery.MODID, "tilemanaoutput"));
        }
        if(Mods.TA.isPresent()) {
            GameRegistry.registerTileEntity(TileImpetusComponent.Input.class, new ResourceLocation(ModularMachinery.MODID, "impetusinput"));
            GameRegistry.registerTileEntity(TileImpetusComponent.Output.class, new ResourceLocation(ModularMachinery.MODID, "impetusoutput"));
        }
    }

    public void init() {
        FuelItemHelper.initialize();
        IntegrationTypeHelper.filterModIdComponents();
        IntegrationTypeHelper.filterModIdRequirementTypes();

        if (Mods.TOP.isPresent()) {
            ModIntegrationTOP.registerProvider();
            ModularMachinery.log.info("[ModularMachinery-CE] TheOneProbe integration is enabled! Stop looking at the dark controller gui!");
        }
    }

    public void postInit() {
        if (Mods.AE2.isPresent()) {
            ModIntegrationAE2.registerUpgrade();
        }

        MachineRegistry.registerMachines(MachineRegistry.loadMachines(null));
        MachineRegistry.registerMachines(MachineBuilder.WAIT_FOR_LOAD);
        CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
                BlockArrayCache.buildCache(MachineRegistry.getLoadedMachines()));

        MachineModifier.loadAll();
        MMEvents.registryAll();
        RecipeAdapterRegistry.registerDynamicMachineAdapters();

        RecipeRegistry.getRegistry().loadRecipeRegistry(null, true);
        for (Action action : FactoryRecipeThread.WAIT_FOR_ADD) {
            action.doAction();
        }
        FactoryRecipeThread.WAIT_FOR_ADD.clear();

        future.join();
    }

    public void registerBlockModel(Block block) {
    }

    public void registerItemModel(Item item) {
    }

    public void registerItemModelWithCustomName(Item item) {
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
            case CONTROLLER -> {
                return new ContainerController((TileMachineController) present, player);
            }
            case FACTORY -> {
                return new ContainerFactoryController((TileFactoryController) present, player);
            }
            case BUS_INVENTORY -> {
                return new ContainerItemBus((TileItemBus) present, player);
            }
            case TANK_INVENTORY -> {
                return new ContainerFluidHatch((TileFluidTank) present, player);
            }
            case ENERGY_INVENTORY -> {
                return new ContainerEnergyHatch((TileEnergyHatch) present, player);
            }
            case SMART_INTERFACE -> {
                return new ContainerSmartInterface((TileSmartInterface) present, player);
            }
            case PARALLEL_CONTROLLER -> {
                return new ContainerParallelController((TileParallelController) present, player);
            }
            case UPGRADE_BUS -> {
                return new ContainerUpgradeBus((TileUpgradeBus) present, player);
            }
            case BLUEPRINT_PREVIEW -> {
            }
            case ME_ITEM_OUTPUT_BUS -> {
                if (aeSecurityCheck(player, present)) {
                    return null;
                }
                return new ContainerMEItemOutputBus((MEItemOutputBus) present, player);
            }
            case ME_ITEM_INPUT_BUS -> {
                if (aeSecurityCheck(player, present)) {
                    return null;
                }
                return new ContainerMEItemInputBus((MEItemInputBus) present, player);
            }
            case ME_FLUID_OUTPUT_BUS -> {
                if (aeSecurityCheck(player, present)) {
                    return null;
                }
                return new ContainerMEFluidOutputBus((MEFluidOutputBus) present, player);
            }
            case ME_FLUID_INPUT_BUS -> {
                if (aeSecurityCheck(player, present)) {
                    return null;
                }
                return new ContainerMEFluidInputBus((MEFluidInputBus) present, player);
            }

            case GUI_ESSENCE_PROVIDER -> {
                if (!Mods.BM2.isPresent()) {
                    return null;
                }
                return new ContainerLifeEssence((TileLifeEssenceProvider) present, player);
            }
        }

        return null;
    }

    private static boolean aeSecurityCheck(EntityPlayer player, TileEntity te) {
        if (!Mods.AE2.isPresent() || !(te instanceof MEMachineComponent)) {
            return true;
        }

        return ModIntegrationAE2.securityCheck(player, ((MEMachineComponent) te).getProxy());
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
        UPGRADE_BUS(TileUpgradeBus.class),
        BLUEPRINT_PREVIEW(null),
        ME_ITEM_OUTPUT_BUS(Mods.AE2.isPresent() ? MEItemOutputBus.class : null),
        ME_ITEM_INPUT_BUS(Mods.AE2.isPresent() ? MEItemInputBus.class : null),
        ME_FLUID_OUTPUT_BUS(Mods.AE2.isPresent() ? MEFluidOutputBus.class : null),
        ME_FLUID_INPUT_BUS(Mods.AE2.isPresent() ? MEFluidInputBus.class : null),
        GUI_ESSENCE_PROVIDER(Mods.BM2.isPresent() ? TileLifeEssenceProvider.class : null),
        ;

        public final Class<? extends TileEntity> requiredTileEntity;

        GuiType(@Nullable Class<? extends TileEntity> requiredTileEntity) {
            this.requiredTileEntity = requiredTileEntity;
        }
    }
}
