package kport.modularmagic.common.crafting.component;

import hellfirepvp.modularmachinery.common.crafting.ComponentType;

import javax.annotation.Nullable;

/**
 * @author youyihj
 */
public class ComponentImpetus extends ComponentType {

    @Nullable
    @Override
    public String requiresModid() {
        return "thaumicaugmentation";
    }
}
