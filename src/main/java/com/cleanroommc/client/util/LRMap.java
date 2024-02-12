package com.cleanroommc.client.util;

import java.util.Map;

public class LRMap<K, V> {

    private Map<K, V> left;
    private Map<K, V> right;

    private boolean useLeft = true;

    public LRMap(Map<K, V> left, Map<K, V> right) {
        this.left = left;
        this.right = right;
    }

    public Map<K, V> getMap() {
        return useLeft ? left : right;
    }

    public LRMap<K, V> setMap(final Map<K, V> world) {
        return useLeft ? setLeft(world) : setRight(world);
    }

    public Map<K, V> getAnotherMap() {
        return useLeft ? right : left;
    }

    public LRMap<K, V> setAnotherMap(final Map<K, V> world) {
        return useLeft ? setRight(world) : setLeft(world);
    }

    public boolean isUseLeft() {
        return useLeft;
    }

    public LRMap<K, V> setUseLeft(final boolean useLeft) {
        this.useLeft = useLeft;
        return this;
    }

    public boolean isUseRight() {
        return !useLeft;
    }

    public LRMap<K, V> setUseRight(final boolean useRight) {
        this.useLeft = !useRight;
        return this;
    }

    public Map<K, V> getLeft() {
        return left;
    }

    public LRMap<K, V> setLeft(final Map<K, V> left) {
        this.left = left;
        return this;
    }

    public Map<K, V> getRight() {
        return right;
    }

    public LRMap<K, V> setRight(final Map<K, V> right) {
        this.right = right;
        return this;
    }

}
