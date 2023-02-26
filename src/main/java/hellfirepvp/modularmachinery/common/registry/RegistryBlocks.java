/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.registry;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.block.*;
import hellfirepvp.modularmachinery.common.item.ItemBlockCustomName;
import hellfirepvp.modularmachinery.common.item.ItemBlockMachineComponent;
import hellfirepvp.modularmachinery.common.item.ItemBlockMachineComponentCustomName;
import hellfirepvp.modularmachinery.common.tiles.*;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static hellfirepvp.modularmachinery.common.lib.BlocksMM.*;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: RegistryBlocks
 * Created by HellFirePvP
 * Date: 28.06.2017 / 20:22
 */
public class RegistryBlocks {

    public static final List<BlockDynamicColor> pendingIBlockColorBlocks = new LinkedList<>();
    private static final List<Block> blockModelRegister = new ArrayList<>();

    public static void initialize() {
        registerBlocks();

        registerTiles();

        registerBlockModels();
    }

    private static void registerBlocks() {
        blockController = prepareRegister(new BlockController());
        prepareItemBlockRegister(blockController);

        blockCasing = prepareRegister(new BlockCasing());
        prepareItemBlockRegister(blockCasing);

        itemInputBus = prepareRegister(new BlockInputBus());
        prepareItemBlockRegister(itemInputBus);
        itemOutputBus = prepareRegister(new BlockOutputBus());
        prepareItemBlockRegister(itemOutputBus);
        fluidInputHatch = prepareRegister(new BlockFluidInputHatch());
        prepareItemBlockRegister(fluidInputHatch);
        fluidOutputHatch = prepareRegister(new BlockFluidOutputHatch());
        prepareItemBlockRegister(fluidOutputHatch);
        energyInputHatch = prepareRegister(new BlockEnergyInputHatch());
        prepareItemBlockRegister(energyInputHatch);
        energyOutputHatch = prepareRegister(new BlockEnergyOutputHatch());
        prepareItemBlockRegister(energyOutputHatch);
        smartInterface = prepareRegister(new BlockSmartInterface());
        prepareItemBlockRegister(smartInterface);
    }

    private static void registerTiles() {
        registerTile(TileColorableMachineComponent.class);

        registerTile(TileMachineController.class);

        registerTile(TileFluidInputHatch.class);
        registerTile(TileFluidOutputHatch.class);
        registerTile(TileItemOutputBus.class);
        registerTile(TileItemInputBus.class);
        registerTile(TileEnergyInputHatch.class);
        registerTile(TileEnergyOutputHatch.class);
    }

    private static void registerBlockModels() {
        for (Block block : blockModelRegister) {
            ModularMachinery.proxy.registerBlockModel(block);
        }
    }

    private static void registerTile(Class<? extends TileEntity> tile, String name) {
        GameRegistry.registerTileEntity(tile, name);
    }

    private static void registerTile(Class<? extends TileEntity> tile) {
        registerTile(tile, tile.getSimpleName().toLowerCase());
    }

    private static void prepareItemBlockRegister(Block block) {
        if (block instanceof BlockMachineComponent) {
            if (block instanceof BlockCustomName) {
                prepareItemBlockRegister(new ItemBlockMachineComponentCustomName(block));
            } else {
                prepareItemBlockRegister(new ItemBlockMachineComponent(block));
            }
        } else {
            if (block instanceof BlockCustomName) {
                prepareItemBlockRegister(new ItemBlockCustomName(block));
            } else {
                prepareItemBlockRegister(new ItemBlock(block));
            }
        }
    }

    private static <T extends ItemBlock> T prepareItemBlockRegister(T item) {
        String name = item.getBlock().getClass().getSimpleName().toLowerCase();
        item.setRegistryName(ModularMachinery.MODID, name).setTranslationKey(ModularMachinery.MODID + '.' + name);

        RegistryItems.itemBlocks.add(item);
        return item;
    }

    private static <T extends Block> T prepareRegister(T block) {
        String name = block.getClass().getSimpleName().toLowerCase();
        block.setRegistryName(ModularMachinery.MODID, name).setTranslationKey(ModularMachinery.MODID + '.' + name);

        blockModelRegister.add(block);
        CommonProxy.registryPrimer.register(block);
        if (block instanceof BlockDynamicColor) {
            pendingIBlockColorBlocks.add((BlockDynamicColor) block);
        }
        return block;
    }

}
