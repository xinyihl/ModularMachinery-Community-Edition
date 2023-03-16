/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.registry;

import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.crafting.adapter.*;

import static hellfirepvp.modularmachinery.common.lib.RecipeAdaptersMM.*;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: RegistryRecipeAdapters
 * Created by HellFirePvP
 * Date: 13.07.2019 / 09:05
 */
public class RegistryRecipeAdapters {

    private RegistryRecipeAdapters() {
    }

    public static void initialize() {
        MINECRAFT_FURNACE = registerAdapter(new AdapterMinecraftFurnace());
        if (Mods.IC2.isPresent()) {
            IC2_COMPRESSOR = registerAdapter(new AdapterICCompressor());
        }
        if (Mods.NUCLEARCRAFT_OVERHAULED.isPresent()) {
            registerAdapter(new AdapterNCOAlloyFurnace());
            registerAdapter(new AdapterNCOInfuser());
        }
    }

    public static <T extends RecipeAdapter> T registerAdapter(T adapter) {
        CommonProxy.registryPrimer.register(adapter);
        return adapter;
    }

}
