/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.base;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: Mods
 * Created by HellFirePvP
 * Date: 02.03.2019 / 17:42
 */
public enum Mods {
    CRAFTTWEAKER("crafttweaker"),
    JEI("jei"),
    GREGTECH("gregtech"),
    DRACONICEVOLUTION("draconicevolution"),
    REDSTONEFLUXAPI("redstoneflux"),
    MEKANISM("mekanism"),
    IC2("ic2"),
    TOP("theoneprobe"),
    NUCLEARCRAFT_OVERHAULED("nuclearcraft"),
    RESOURCELOADER("resourceloader"),
    // TXLoader uses fully private variables and I can't get the correct resource folder.
    // Bad design.
    TX_LOADER("txloader"),
    FLUX_NETWORKS("fluxnetworks"),
    ZEN_UTILS("zenutils"),
    TCONSTRUCT("tconstruct"),
    AE2("appliedenergistics2"),
    AE2EL("appliedenergistics2") {
        @Override
        public boolean isPresent() {
            if (super.isPresent()) {
                ModContainer ae2 = Loader.instance().getIndexedModList().get("appliedenergistics2");
                return ae2.getName().equals("AE2 Unofficial Extended Life");
            } else {
                return false;
            }
        }
    },

    ;

    public final String modid;
    private final boolean loaded;

    Mods(String modName) {
        this.modid = modName;
        this.loaded = Loader.isModLoaded(this.modid);
    }

    public boolean isPresent() {
        return loaded;
    }

}
