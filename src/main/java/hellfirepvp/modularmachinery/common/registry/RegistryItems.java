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
import hellfirepvp.modularmachinery.common.item.*;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static hellfirepvp.modularmachinery.common.lib.ItemsMM.*;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: RegistryItems
 * Created by HellFirePvP
 * Date: 28.06.2017 / 18:40
 */
public class RegistryItems {

    public static final List<ItemDynamicColor> pendingDynamicColorItems = new LinkedList<>();
    static final List<Item> itemBlocks = new ArrayList<>();
    private static final List<Item> itemModelRegister = new ArrayList<>();

    public static void initialize() {
        blueprint = prepareRegister(new ItemBlueprint());
        modularium = prepareRegister(new ItemModularium());
        constructTool = prepareRegister(new ItemConstructTool());

        registerItemBlocks();
        registerItemModels();
    }

    private static <T extends Item> T prepareRegister(T item) {
        String name = item.getClass().getSimpleName().toLowerCase();
        item.setRegistryName(ModularMachinery.MODID, name).setTranslationKey(ModularMachinery.MODID + '.' + name);

        return register(item);
    }

    private static <T extends Item> T register(T item) {
        itemModelRegister.add(item);
        CommonProxy.registryPrimer.register(item);
        if (item instanceof ItemDynamicColor) {
            pendingDynamicColorItems.add((ItemDynamicColor) item);
        }
        return item;
    }

    private static void registerItemBlocks() {
        itemBlocks.forEach(RegistryItems::register);
    }

    private static void registerItemModels() {
        itemModelRegister.stream()
                .filter(item -> !(item instanceof ItemBlockCustomName))
                .forEach(ModularMachinery.proxy::registerItemModel);
    }

}
