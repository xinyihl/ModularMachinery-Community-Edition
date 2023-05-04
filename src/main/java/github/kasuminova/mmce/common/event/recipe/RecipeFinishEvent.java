package github.kasuminova.mmce.common.event.recipe;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import stanhebben.zenscript.annotations.ZenClass;

@ZenRegister
@ZenClass("mods.modularmachinery.RecipeFinishEvent")
public class RecipeFinishEvent extends RecipeEvent {
    public RecipeFinishEvent(TileMultiblockMachineController controller) {
        super(controller, controller.getActiveRecipeList()[0]);
    }
}
