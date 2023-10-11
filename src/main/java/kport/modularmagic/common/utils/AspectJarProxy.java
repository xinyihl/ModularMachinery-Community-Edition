package kport.modularmagic.common.utils;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.tiles.essentia.TileJarFillable;

public class AspectJarProxy {
    private final TileJarFillable original;

    private Aspect aspect;
    private int amount;

    public AspectJarProxy(TileJarFillable jar) {
        this.original = jar;
        this.aspect = jar.aspect;
        this.amount = jar.amount;
    }

    public boolean takeFromContainer(Aspect tt, int am) {
        if (this.amount >= am && tt == this.aspect) {
            this.amount -= am;
            if (this.amount <= 0) {
                this.aspect = null;
                this.amount = 0;
            }
            return true;
        } else {
            return false;
        }
    }

    public int addToContainer(Aspect tt, int am) {
        if (am != 0) {
            if (this.amount < 250 && tt == this.aspect || this.amount == 0) {
                this.aspect = tt;
                int added = Math.min(am, 250 - this.amount);
                this.amount += added;
                am -= added;
            }
        }
        return am;
    }

    public TileJarFillable getOriginal() {
        return original;
    }

    public Aspect getAspect() {
        return aspect;
    }

    public AspectJarProxy setAspect(Aspect aspect) {
        this.aspect = aspect;
        return this;
    }

    public int getAmount() {
        return amount;
    }

    public AspectJarProxy setAmount(int amount) {
        this.amount = amount;
        return this;
    }
}
