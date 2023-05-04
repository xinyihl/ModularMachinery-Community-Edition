package github.kasuminova.mmce.common.event.recipe;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenSetter;

@ZenRegister
@ZenClass("mods.modularmachinery.RecipeFailureEvent")
public class RecipeFailureEvent extends RecipeEvent {
    private final String cause;
    private boolean destructRecipe;

    public RecipeFailureEvent(TileMultiblockMachineController controller, String cause, boolean destructRecipe) {
        super(controller, controller.getActiveRecipeList()[0]);
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
