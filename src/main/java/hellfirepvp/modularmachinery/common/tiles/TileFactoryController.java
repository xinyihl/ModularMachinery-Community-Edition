package hellfirepvp.modularmachinery.common.tiles;

import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;

public class TileFactoryController extends TileMultiblockMachineController {
    private final List<RecipeQueueElement> recipeQueue = new ArrayList<>();
    // Not really threads.
    private int maxThreads = 1;

    public TileFactoryController() {
    }

    @Override
    public void doRestrictedTick() {
        if (getWorld().isRemote) {
            return;
        }
        if (getWorld().getStrongPower(getPos()) > 0) {
            return;
        }

        if (!doStructureCheck() || !isStructureFormed()) {
            return;
        }


    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
    }

    public static class RecipeQueueElement {
        private ActiveMachineRecipe activeRecipe = null;
        private RecipeCraftingContext context = null;
        private CraftingStatus status = CraftingStatus.IDLE;

        public ActiveMachineRecipe getActiveRecipe() {
            return activeRecipe;
        }

        public RecipeQueueElement setActiveRecipe(ActiveMachineRecipe activeRecipe) {
            this.activeRecipe = activeRecipe;
            return this;
        }

        public RecipeCraftingContext getContext() {
            return context;
        }

        public RecipeQueueElement setContext(RecipeCraftingContext context) {
            this.context = context;
            return this;
        }

        public CraftingStatus getStatus() {
            return status;
        }

        public RecipeQueueElement setStatus(CraftingStatus status) {
            this.status = status;
            return this;
        }
    }
}
