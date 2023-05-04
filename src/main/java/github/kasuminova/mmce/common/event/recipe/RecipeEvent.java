package github.kasuminova.mmce.common.event.recipe;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.util.IEventHandler;
import github.kasuminova.mmce.common.event.machine.MachineEvent;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;

import java.util.List;

@ZenRegister
@ZenClass("mods.modularmachinery.RecipeEvent")
public abstract class RecipeEvent extends MachineEvent {
    private final ActiveMachineRecipe activeRecipe;
    public RecipeEvent(TileMultiblockMachineController controller, ActiveMachineRecipe activeRecipe) {
        super(controller);
        this.activeRecipe = activeRecipe;
    }

    @ZenGetter("activeRecipe")
    public ActiveMachineRecipe getActiveRecipe() {
        return activeRecipe;
    }

    @Override
    public void postCrTEvent() {
        if (activeRecipe == null || activeRecipe.getRecipe() == null) {
            return;
        }
        List<IEventHandler<RecipeEvent>> handlers = activeRecipe.getRecipe().getRecipeEventHandlers().get(getClass());
        if (handlers == null) {
            return;
        }
        for (IEventHandler<RecipeEvent> handler : handlers) {
            handler.handle(this);
            if (isCanceled()) {
                break;
            }
        }
    }
}
