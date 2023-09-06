/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.integration;

import hellfirepvp.modularmachinery.common.tiles.TileEnergyInputHatch;
import hellfirepvp.modularmachinery.common.tiles.TileEnergyOutputHatch;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Optional;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: IntegrationIC2EventHandlerHelper
 * Created by HellFirePvP
 * Date: 17.08.2017 / 00:22
 */
public class IntegrationIC2EventHandlerHelper {

//    public static void fireLoadEvent(World world, IEnergyTile tileEnergyInputHatch) {
//        MinecraftServer ms = FMLCommonHandler.instance().getMinecraftServerInstance();
//        if (ms != null) {
//            ms.addScheduledTask(() -> {
//                if (!world.isRemote) {
//                    MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(tileEnergyInputHatch));
//                }
//            });
//        }
//    }

    @Optional.Method(modid = "ic2")
    public static void onEnergyTileUnLoaded(TileEnergyInputHatch energyTile) {
        MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(energyTile));
    }

    @Optional.Method(modid = "ic2")
    public static void onEnergyTileLoaded(TileEnergyInputHatch energyTile) {
        MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(energyTile));
    }

    @Optional.Method(modid = "ic2")
    public static void onEnergyTileUnLoaded(TileEnergyOutputHatch energyTile) {
        MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(energyTile));
    }

    @Optional.Method(modid = "ic2")
    public static void onEnergyTileLoaded(TileEnergyOutputHatch energyTile) {
        MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(energyTile));
    }

}
