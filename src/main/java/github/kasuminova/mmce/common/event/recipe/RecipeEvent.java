package github.kasuminova.mmce.common.event.recipe;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.util.IEventHandler;
import github.kasuminova.mmce.common.event.machine.MachineEvent;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;

import java.util.List;

@ZenRegister
@ZenClass("mods.modularmachinery.RecipeEvent")
public class RecipeEvent extends MachineEvent {
    private final ActiveMachineRecipe activeRecipe;
    private final RecipeCraftingContext context;

    public RecipeEvent(TileMultiblockMachineController controller, RecipeCraftingContext context) {
        super(controller);
        this.activeRecipe = context.getActiveRecipe();
        this.context = context;
    }

    @ZenGetter("activeRecipe")
    public ActiveMachineRecipe getActiveRecipe() {
        return activeRecipe;
    }

    public RecipeCraftingContext getContext() {
        return context;
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
