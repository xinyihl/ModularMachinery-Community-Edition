/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.util;

import hellfirepvp.modularmachinery.common.base.Mods;
import net.minecraftforge.fluids.FluidTank;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: CopyHandlerHelper
 * Created by HellFirePvP
 * Date: 12.07.2017 / 21:37
 */
public class CopyHandlerHelper {

    public static HybridGasTank copyGasTank(FluidTank tank) {
        if (Mods.MEKANISM.isPresent() && tank instanceof HybridGasTank) {
            HybridGasTank newTank = new HybridGasTank(tank.getCapacity());
            newTank.setGas(((HybridGasTank) tank).getGas());
            return newTank;
        }

        return null;
    }

}
