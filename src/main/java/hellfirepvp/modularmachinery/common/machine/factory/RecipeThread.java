package hellfirepvp.modularmachinery.common.machine.factory;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.nbt.NBTTagCompound;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * <p>TileFactoryController 的一部分，存储单独的配方运行数据。</p>
 * <p>不是真正意义上的线程。</p>
 * <p>Part of the TileFactoryController that stores data for separate recipe runs.</p>
 * <p>Not a thread in the true sense of the word.</p>
 */
@ZenRegister
@ZenClass("mods.modularmachinery.RecipeThread")
public class RecipeThread {
    private final TileFactoryController factory;
    private final boolean isDaemon;
    private final String threadName;
    private ActiveMachineRecipe activeRecipe = null;
    private RecipeCraftingContext context = null;
    private TileMultiblockMachineController.CraftingStatus status = TileMultiblockMachineController.CraftingStatus.IDLE;
    private boolean waitForFinish = false;

    public RecipeThread(TileFactoryController factory) {
        this(factory, false, "");
    }

    public RecipeThread(TileFactoryController factory, boolean isDaemon, String threadName) {
        this.factory = factory;
        this.isDaemon = isDaemon;
        this.threadName = threadName;
    }

    @ZenMethod
    public static RecipeThread createDaemonRecipeThread(String threadName) {
        return new RecipeThread(null, true, threadName);
    }

    public TileMultiblockMachineController.CraftingStatus onTick() {
        if (activeRecipe == null) {
            return new TileMultiblockMachineController.CraftingStatus(TileMultiblockMachineController.Type.NO_RECIPE, "");
        }
        if (context == null) {
            context = factory.createContext(activeRecipe);
        }

        return (status = activeRecipe.tick(factory, context));
    }

    public TileMultiblockMachineController.CraftingStatus onFinished() {
        if (activeRecipe == null) {
            return new TileMultiblockMachineController.CraftingStatus(TileMultiblockMachineController.Type.NO_RECIPE, "");
        }
        if (context == null) {
            context = factory.createContext(activeRecipe);
        }

        RecipeCraftingContext.CraftingCheckResult checkResult = context.canFinishCrafting();
        if (checkResult.isFailure()) {
            waitForFinish = true;
            return (status = TileMultiblockMachineController.CraftingStatus.failure(checkResult.getFirstErrorMessage("")));
        } else {
            waitForFinish = false;
        }

        context.finishCrafting();
        factory.onThreadRecipeFinished(this);

        activeRecipe.reset();
        activeRecipe.setMaxParallelism(factory.getAvailableParallelism());
        context = factory.createContext(activeRecipe);

        RecipeCraftingContext.CraftingCheckResult result = factory.onCheck(context);
        if (result.isSuccess()) {
            factory.onThreadRecipeStart(this);
            return TileMultiblockMachineController.CraftingStatus.SUCCESS;
        } else {
            return (status = TileMultiblockMachineController.CraftingStatus.IDLE);
        }
    }

    public NBTTagCompound serialize() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("activeRecipe", activeRecipe.serialize());
        tag.setTag("status", status.serialize());
        return tag;
    }

    public static RecipeThread deserialize(NBTTagCompound tag, TileFactoryController factory) {
        if (!tag.hasKey("activeRecipe") || !tag.hasKey("status")) {
            return null;
        }
        return new RecipeThread(factory)
                .setActiveRecipe(new ActiveMachineRecipe(tag.getCompoundTag("activeRecipe")))
                .setStatus(TileMultiblockMachineController.CraftingStatus.deserialize(tag.getCompoundTag("status")));
    }

    @ZenGetter("activeRecipe")
    public ActiveMachineRecipe getActiveRecipe() {
        return activeRecipe;
    }

    public RecipeThread setActiveRecipe(ActiveMachineRecipe activeRecipe) {
        this.activeRecipe = activeRecipe;
        return this;
    }

    public RecipeCraftingContext getContext() {
        return context;
    }

    public RecipeThread setContext(RecipeCraftingContext context) {
        this.context = context;
        return this;
    }

    public TileMultiblockMachineController.CraftingStatus getStatus() {
        return status;
    }

    public RecipeThread setStatus(TileMultiblockMachineController.CraftingStatus status) {
        this.status = status;
        return this;
    }

    public boolean isWaitForFinish() {
        return waitForFinish;
    }

    public RecipeThread setWaitForFinish(boolean waitForFinish) {
        this.waitForFinish = waitForFinish;
        return this;
    }

    public TileFactoryController getFactory() {
        return factory;
    }

    @ZenGetter("isWorking")
    public boolean isWorking() {
        if (activeRecipe == null) {
            return false;
        }
        return status != TileMultiblockMachineController.CraftingStatus.IDLE;
    }

    @ZenGetter("isDaemon")
    public boolean isDaemon() {
        return isDaemon;
    }

    @ZenGetter("threadName")
    public String getThreadName() {
        return threadName;
    }

    public RecipeThread copy(TileFactoryController ctrl) {
        return new RecipeThread(ctrl, isDaemon, threadName);
    }
}
