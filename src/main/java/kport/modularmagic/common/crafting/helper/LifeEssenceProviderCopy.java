package kport.modularmagic.common.crafting.helper;

import kport.modularmagic.common.tile.TileLifeEssenceProvider;

public class LifeEssenceProviderCopy {
    private final TileLifeEssenceProvider original;

    private int lifeEssenceCache;

    public LifeEssenceProviderCopy(final TileLifeEssenceProvider original) {
        this.original = original;
        this.lifeEssenceCache = original.getLifeEssenceCache();
    }

    public TileLifeEssenceProvider getOriginal() {
        return original;
    }

    public int getLifeEssenceCache() {
        return lifeEssenceCache;
    }

    public int getOrbCapacity() {
        return original.getOrbCapacity();
    }

    public int addLifeEssenceCache(int amount) {
        int orbCapacity = getOrbCapacity();
        if (orbCapacity <= 0) {
            return 0;
        }

        int maxCapacity = orbCapacity / 10;
        int maxCanAdd = maxCapacity - lifeEssenceCache;
        if (maxCanAdd <= 0) {
            return 0;
        }

        int added = Math.min(amount, maxCanAdd);
        lifeEssenceCache += added;
        return added;
    }

    public int removeLifeEssenceCache(int amount) {
        int maxCanConsume = Math.min(lifeEssenceCache, amount);
        lifeEssenceCache -= maxCanConsume;
        return maxCanConsume;
    }
}
