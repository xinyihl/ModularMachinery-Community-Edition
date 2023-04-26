package hellfirepvp.modularmachinery.common.integration.crafttweaker.event.recipe;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.IMachineController;
import hellfirepvp.modularmachinery.common.machine.factory.FactoryRecipeThread;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenSetter;

@ZenRegister
@ZenClass("mods.modularmachinery.FactoryRecipeFailureEvent")
public class FactoryRecipeFailureEvent extends FactoryRecipeEvent {
    private final String cause;
    private boolean destructRecipe;

    public FactoryRecipeFailureEvent(FactoryRecipeThread recipeThread, IMachineController controller, String cause, boolean destructRecipe) {
        super(recipeThread, recipeThread.getActiveRecipe(), controller);
        this.cause = cause;
        this.destructRecipe = destructRecipe;
    }

    @ZenGetter("cause")
    public String getCause() {
        return cause;
    }

    @ZenGetter("destructRecipe")
    public boolean isDestructRecipe() {
        return destructRecipe;
    }

    @ZenSetter("destructRecipe")
    public void setDestructRecipe(boolean destructRecipe) {
        this.destructRecipe = destructRecipe;
    }
}
