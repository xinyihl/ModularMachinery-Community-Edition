/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.data;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.client.util.EnergyDisplayUtil;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.block.prop.EnergyHatchData;
import hellfirepvp.modularmachinery.common.block.prop.FluidHatchSize;
import hellfirepvp.modularmachinery.common.integration.ModIntegrationTOP;
import hellfirepvp.modularmachinery.common.machine.RecipeFailureActions;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import hellfirepvp.modularmachinery.common.util.CatalystNameUtil;
import ink.ikx.mmce.core.AssemblyConfig;
import net.minecraftforge.common.config.Configuration;

import java.io.File;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: Config
 * Created by HellFirePvP
 * Date: 26.06.2017 / 22:34
 */
public class Config {
    public static boolean mocCompatibleMode = false;
    public static int machineColor;
    private static File lastReadFile;
    private static Configuration lastReadConfig;

    public static void loadFrom(File file) {
        lastReadFile = file;
        lastReadConfig = new Configuration(file);

        load();

        if (lastReadConfig.hasChanged()) {
            lastReadConfig.save();
        }
    }

    private static void load() {
        FluidHatchSize.loadFromConfig(lastReadConfig);
        EnergyHatchData.loadFromConfig(lastReadConfig);
        EnergyDisplayUtil.loadFromConfig(lastReadConfig);
        CatalystNameUtil.loadFromConfig(lastReadConfig);
        RecipeFailureActions.loadFromConfig(lastReadConfig);
        TileMachineController.loadFromConfig(lastReadConfig);
        AssemblyConfig.loadFormConfig(lastReadConfig);
        if (Mods.TOP.isPresent()) {
            ModIntegrationTOP.loadFromConfig(lastReadConfig);
        }

        String strColor = lastReadConfig.getString("general-casing-color", "general", "FF4900", "Defines the _default_ color for machine casings as items or blocks. (Hex color without alpha) Has to be defined both server and clientside!");
        int col = 0xff921e; //TODO uh
        try {
            col = Integer.parseInt(strColor, 16);
        } catch (Exception exc) {
            ModularMachinery.log.error("Machine-Casing color defined in the config is not a hex color: " + strColor);
            ModularMachinery.log.error("Using default color instead...");
        }
        machineColor = col;
        mocCompatibleMode = lastReadConfig.getBoolean(
                "modular-controller-compatible-mode", "general", false,
                "When enabled, the mod registers a controller block under the name modularcontroller to prevent incompatibility with older saves.");
    }
}
