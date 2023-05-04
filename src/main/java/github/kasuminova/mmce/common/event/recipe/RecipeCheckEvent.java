package github.kasuminova.mmce.common.event.recipe;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.modularmachinery.RecipeCheckEvent")
public class RecipeCheckEvent extends RecipeEvent {
    private boolean isFailure = false;
    private String failureReason = null;

    public RecipeCheckEvent(TileMultiblockMachineController controller, ActiveMachineRecipe recipe) {
        super(controller, recipe);
    }

    @ZenMethod
    public void setFailed(String reason) {
        this.isFailure = true;
        this.failureReason = reason;
        setCanceled(true);
    }

    public boolean isFailure() {
        return isFailure;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
