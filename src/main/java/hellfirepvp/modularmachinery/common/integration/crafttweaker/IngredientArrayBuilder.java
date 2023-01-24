package hellfirepvp.modularmachinery.common.integration.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.modularmachinery.IngredientArrayBuilder")
public class IngredientArrayBuilder {
    @ZenMethod
    public static IngredientArrayPrimer newBuilder() {
        return new IngredientArrayPrimer();
    }
}
