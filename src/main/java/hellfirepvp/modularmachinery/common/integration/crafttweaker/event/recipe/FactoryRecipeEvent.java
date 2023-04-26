package hellfirepvp.modularmachinery.common.integration.crafttweaker.event.recipe;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.IMachineController;
import hellfirepvp.modularmachinery.common.machine.factory.FactoryRecipeThread;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;

@ZenRegister
@ZenClass("mods.modularmachinery.FactoryRecipeEvent")
public abstract class FactoryRecipeEvent extends RecipeEvent {
    protected final FactoryRecipeThread recipeThread;
    public FactoryRecipeEvent(FactoryRecipeThread recipeThread, ActiveMachineRecipe activeRecipe, IMachineController controller) {
        super(controller, activeRecipe);
        this.recipeThread = recipeThread;
    }

    @ZenGetter("recipeThread")
    public FactoryRecipeThread getRecipeThread() {
        return recipeThread;
    }
}
