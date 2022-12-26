/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.util;

import com.google.common.collect.ImmutableList;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: FuelItemHelper
 * Created by HellFirePvP
 * Date: 23.08.2017 / 16:43
 */
public class FuelItemHelper {

    private static List<ItemStack> knownFuelStacks = null;

    public static void initialize() {
        NonNullList<ItemStack> stacks = NonNullList.create();
        for (Item item : ForgeRegistries.ITEMS) {
            CreativeTabs tab = item.getCreativeTab();
            if (tab != null) {
                item.getSubItems(tab, stacks);
            }
        }
        List<ItemStack> out = new LinkedList<>();
        for (ItemStack stack : stacks) {
            try {
                int burn = TileEntityFurnace.getItemBurnTime(stack); //Respects vanilla values.
                if (burn > 0) {
                    out.add(stack);
                }
            } catch (Exception exc) {
            }
        }
        knownFuelStacks = ImmutableList.copyOf(out);
    }

    public static List<ItemStack> getFuelItems() {
        if (knownFuelStacks == null) {
            return new ArrayList<>(0);
        }
        return knownFuelStacks;
    }

}
