package github.kasuminova.mmce.common.util;

import io.netty.util.collection.IntObjectHashMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.registries.IRegistryDelegate;

import javax.annotation.Nonnull;

public class OredictCache {
    private static final IntObjectHashMap<IntObjectHashMap<int[]>> ORE_ID_CACHE_MAP = new IntObjectHashMap<>();

    public static int[] getOreIDsFast(@Nonnull ItemStack stack) {
        if (stack.isEmpty()) {
            return new int[]{};
        }

        IRegistryDelegate<Item> delegate = stack.getItem().delegate;
        if (delegate.name() == null) {
            FMLLog.log.debug("Attempted to find the oreIDs for an unregistered object ({}). This won't work very well.", stack);
            return new int[0];
        }

        int id = Item.REGISTRY.getIDForObject(delegate.get());
        int damageOffset = id | ((stack.getItemDamage() + 1) << 16);

        return ORE_ID_CACHE_MAP.computeIfAbsent(id, v -> new IntObjectHashMap<>())
                .computeIfAbsent(damageOffset, v -> OreDictionary.getOreIDs(stack));
    }
}
