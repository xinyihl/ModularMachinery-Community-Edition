/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.util;

import hellfirepvp.modularmachinery.common.base.Mods;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fml.common.Optional;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: CopyHandlerHelper
 * Created by HellFirePvP
 * Date: 12.07.2017 / 21:37
 */
public class CopyHandlerHelper {

    public static HybridTank copyTank(FluidTank tank) {
        NBTTagCompound cmp = new NBTTagCompound();
        tank.writeToNBT(cmp);
        if (Mods.MEKANISM.isPresent() && tank instanceof HybridGasTank) {
            writeGasTag((HybridGasTank) tank, cmp);
        }
        HybridTank newTank = new HybridTank(tank.getCapacity());
        if (Mods.MEKANISM.isPresent() && tank instanceof HybridGasTank) {
            newTank = buildMekGasTank(tank.getCapacity());
        }
        newTank.readFromNBT(cmp);
        if (Mods.MEKANISM.isPresent() && tank instanceof HybridGasTank) {
            readGasTag((HybridGasTank) newTank, cmp);
        }
        return newTank;
    }

    @Optional.Method(modid = "mekanism")
    private static HybridTank buildMekGasTank(int capacity) {
        return new HybridGasTank(capacity);
    }

    @Optional.Method(modid = "mekanism")
    private static void writeGasTag(HybridGasTank tank, NBTTagCompound compound) {
        tank.writeGasToNBT(compound);
    }

    @Optional.Method(modid = "mekanism")
    private static void readGasTag(HybridGasTank tank, NBTTagCompound compound) {
        tank.readGasFromNBT(compound);
    }

    public static IOInventory copyInventory(IOInventory inventory) {
        return IOInventory.deserialize(inventory.getOwner(), inventory.writeNBT());
    }

}
