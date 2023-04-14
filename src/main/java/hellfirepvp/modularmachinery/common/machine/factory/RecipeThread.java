package hellfirepvp.modularmachinery.common.machine.factory;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import github.kasuminova.mmce.common.concurrent.Action;
import github.kasuminova.mmce.common.concurrent.FactoryRecipeSearchTask;
import github.kasuminova.mmce.common.concurrent.TaskExecutor;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.RecipeRegistry;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftingStatus;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import io.netty.util.internal.ThrowableUtil;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.*;

/**
 * <p>TileFactoryController 的一部分，存储单独的配方运行数据。</p>
 * <p>不是真正意义上的线程。</p>
 * <p>Part of the TileFactoryController that stores data for separate recipe runs.</p>
 * <p>Not a thread in the true sense of the word.</p>
 */
@ZenRegister
@ZenClass("mods.modularmachinery.RecipeThread")
public class RecipeThread {
    public static final int RECIPE_SEARCH_DELAY = 20;
    public static final int IDLE_TIME_OUT = 200;
    public static final List<Action> WAIT_FOR_ADD = new ArrayList<>();
    private final TreeSet<MachineRecipe> recipeSet = new TreeSet<>();
    private final Map<String, RecipeModifier> permanentModifiers = new HashMap<>();
    private final Map<String, RecipeModifier> semiPermanentModifiers = new HashMap<>();
    public int idleTime = 0;
    private TileFactoryController factory;
    private boolean isDaemon;
    private String threadName;
    private FactoryRecipeSearchTask searchTask;
    private ActiveMachineRecipe activeRecipe = null;
    private RecipeCraftingContext context = null;
    private CraftingStatus status = CraftingStatus.IDLE;
    private boolean waitForFinish = false;

    public RecipeThread(TileFactoryController factory) {
        this(factory, false, "", Collections.emptySet(), Collections.emptyMap());
    }

    public RecipeThread(
            TileFactoryController factory,
            boolean isDaemon,
            String threadName,
            Set<MachineRecipe> recipeSet,
            Map<String, RecipeModifier> permanentModifiers) {
        this.factory = factory;
        this.isDaemon = isDaemon;
        this.threadName = threadName;
        this.recipeSet.addAll(recipeSet);
        this.permanentModifiers.putAll(permanentModifiers);
    }

    @ZenMethod
    public static RecipeThread createDaemonThread(String threadName) {
        return new RecipeThread(null, true, threadName, Collections.emptySet(), Collections.emptyMap());
    }

    public CraftingStatus onTick() {
        if (activeRecipe == null) {
            return new CraftingStatus(TileMultiblockMachineController.Type.NO_RECIPE, "");
        }
        if (context == null) {
            context = createContext(activeRecipe);
        }
        idleTime = 0;
        return (status = activeRecipe.tick(factory, context));
    }

    public void onFinished() {
        if (activeRecipe == null) {
            waitForFinish = false;
            return;
        }
        if (context == null) {
            context = createContext(activeRecipe);
        }

        RecipeCraftingContext.CraftingCheckResult checkResult = context.canFinishCrafting();
        if (checkResult.isFailure()) {
            waitForFinish = true;
            status = CraftingStatus.failure(checkResult.getFirstErrorMessage(""));
            return;
        }

        waitForFinish = false;
        context.finishCrafting();
        factory.onThreadRecipeFinished(this);
        semiPermanentModifiers.clear();

        activeRecipe.reset();
        activeRecipe.setMaxParallelism(factory.getAvailableParallelism());
        context = createContext(activeRecipe);

        RecipeCraftingContext.CraftingCheckResult result = factory.onCheck(context);
        if (result.isSuccess()) {
            factory.onThreadRecipeStart(this);
        } else {
            activeRecipe = null;
            context = null;
            status = CraftingStatus.IDLE;
            if (isDaemon) {
                createRecipeSearchTask();
            }
        }
    }

    public RecipeCraftingContext createContext(ActiveMachineRecipe activeRecipe) {
        RecipeCraftingContext context = factory.createContext(activeRecipe);
        context.addModifier(semiPermanentModifiers.values());
        context.addModifier(permanentModifiers.values());
        return context;
    }

    public void searchAndStartRecipe() {
        if (searchTask != null) {
            if (!searchTask.isDone()) {
                return;
            }

            RecipeCraftingContext context = null;
            try {
                context = searchTask.get();
                status = searchTask.getStatus();
            } catch (Exception e) {
                ModularMachinery.log.warn(ThrowableUtil.stackTraceToString(e));
            }
            searchTask = null;

            if (context == null) {
                return;
            }

            if (context.canStartCrafting().isSuccess()) {
                this.context = context;
                this.activeRecipe = context.getActiveRecipe();
                this.status = CraftingStatus.SUCCESS;
                factory.onThreadRecipeStart(this);
            }
        } else {
            if (factory.getTicksExisted() % RECIPE_SEARCH_DELAY == 0) {
                createRecipeSearchTask();
            }
        }
    }

    private void createRecipeSearchTask() {
        TileFactoryController factory = this.factory;
        Iterable<MachineRecipe> recipeSet = this.recipeSet.isEmpty() ? RecipeRegistry.getRecipesFor(factory.getFoundMachine()) : this.recipeSet;
        searchTask = new FactoryRecipeSearchTask(
                factory,
                factory.getFoundMachine(),
                factory.getAvailableParallelism(),
                recipeSet,
                this,
                factory.getActiveRecipeList());
        TaskExecutor.FORK_JOIN_POOL.submit(searchTask);
    }

    public NBTTagCompound serialize() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("status", status.serialize());
        if (activeRecipe != null && activeRecipe.getRecipe() != null) {
            tag.setTag("activeRecipe", activeRecipe.serialize());
        }
        if (isDaemon) {
            tag.setString("daemonThreadName", threadName);
        }
        if (!permanentModifiers.isEmpty()) {
            NBTTagList tagList = new NBTTagList();
            permanentModifiers.forEach((key, modifier) -> {
                if (key != null && modifier != null) {
                    NBTTagCompound modifierTag = new NBTTagCompound();
                    modifierTag.setString("key", key);
                    modifierTag.setTag("modifier", modifier.serialize());
                    tagList.appendTag(modifierTag);
                }
            });
            tag.setTag("permanentModifiers", tagList);
        }
        if (!semiPermanentModifiers.isEmpty()) {
            NBTTagList tagList = new NBTTagList();
            semiPermanentModifiers.forEach((key, modifier) -> {
                if (key != null && modifier != null) {
                    NBTTagCompound modifierTag = new NBTTagCompound();
                    modifierTag.setString("key", key);
                    modifierTag.setTag("modifier", modifier.serialize());
                    tagList.appendTag(modifierTag);
                }
            });
            tag.setTag("semiPermanentModifiers", tagList);
        }
        return tag;
    }

    public static RecipeThread deserialize(NBTTagCompound tag, TileFactoryController factory) {
        if (!tag.hasKey("status")) {
            return null;
        }

        Map<String, RecipeModifier> permanentModifiers = new HashMap<>();
        if (tag.hasKey("customModifier")) {
            NBTTagList tagList = tag.getTagList("customModifier", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < tagList.tagCount(); i++) {
                NBTTagCompound modifierTag = tagList.getCompoundTagAt(i);
                permanentModifiers.put(modifierTag.getString("key"), RecipeModifier.deserialize(modifierTag.getCompoundTag("modifier")));
            }
        }

        Map<String, RecipeModifier> semiPermanentModifiers = new HashMap<>();
        if (tag.hasKey("customModifier")) {
            NBTTagList tagList = tag.getTagList("customModifier", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < tagList.tagCount(); i++) {
                NBTTagCompound modifierTag = tagList.getCompoundTagAt(i);
                semiPermanentModifiers.put(modifierTag.getString("key"), RecipeModifier.deserialize(modifierTag.getCompoundTag("modifier")));
            }
        }

        ActiveMachineRecipe activeRecipe = deserializeRecipe(tag, factory);
        RecipeThread thread = new RecipeThread(factory)
                .setActiveRecipe(activeRecipe)
                .setStatus(CraftingStatus.deserialize(tag.getCompoundTag("status")));
        thread.permanentModifiers.putAll(permanentModifiers);
        thread.semiPermanentModifiers.putAll(semiPermanentModifiers);

        // Daemon Thread
        if (tag.hasKey("daemonThreadName")) {
            Map<String, RecipeThread> threads = factory.getFoundMachine().getDaemonThreads();
            RecipeThread daemonThread = threads.get(tag.getString("daemonThreadName"));
            if (daemonThread == null) {
                return thread;
            }
            return daemonThread.copyDataToAnother(factory, thread);
        }
        // Simple Thread
        return thread;
    }

    private static ActiveMachineRecipe deserializeRecipe(NBTTagCompound tag, TileFactoryController factory) {
        ActiveMachineRecipe activeRecipe = null;
        NBTTagCompound recipeTag = tag.getCompoundTag("activeRecipe");
        if (tag.hasKey("activeRecipe")) {
            activeRecipe = new ActiveMachineRecipe(recipeTag);
        }
        if (activeRecipe != null && activeRecipe.getRecipe() == null) {
            activeRecipe = null;
            ModularMachinery.log.info("Couldn't find recipe named " + recipeTag.getString("recipeName") + " for controller at " + factory.getPos());
        }
        return activeRecipe;
    }

    public void flushContextModifier() {
        if (context == null) {
            return;
        }
        context.overrideModifier(MiscUtils.flatten(factory.getFoundModifiers().values()));
        context.addModifier(factory.getCustomModifiers().values());
        context.addModifier(semiPermanentModifiers.values());
        context.addModifier(permanentModifiers.values());
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

    public CraftingStatus getStatus() {
        return status;
    }

    public RecipeThread setStatus(CraftingStatus status) {
        this.status = status;
        return this;
    }

    public boolean isWaitForFinish() {
        return waitForFinish;
    }

    public TileFactoryController getFactory() {
        return factory;
    }

    /**
     * 当前线程是否在工作。
     */
    @ZenGetter("isWorking")
    public boolean isWorking() {
        return status.getStatus() == TileMultiblockMachineController.Type.CRAFTING;
    }

    /**
     * 当前线程是否闲置
     */
    @ZenGetter("isIdle")
    public boolean isIdle() {
        return status == CraftingStatus.IDLE;
    }

    /**
     * 当前线程是否为守护线程（即始终存在）
     */
    @ZenGetter("isDaemon")
    public boolean isDaemon() {
        return isDaemon;
    }

    /**
     * 当前线程的名称（仅限守护线程）
     */
    @ZenGetter("threadName")
    public String getThreadName() {
        return threadName;
    }

    public Map<String, RecipeModifier> getPermanentModifiers() {
        return permanentModifiers;
    }

    public Map<String, RecipeModifier> getSemiPermanentModifiers() {
        return semiPermanentModifiers;
    }

    /**
     * 添加一个半永久配方修改器，它会在完成配方后被清空。
     *
     * @param name     名称
     * @param modifier 修改器
     */
    @ZenMethod
    public void addModifier(String name, RecipeModifier modifier) {
        semiPermanentModifiers.put(name, modifier);
        flushContextModifier();
    }

    /**
     * 删除一个半永久配方修改器。
     *
     * @param name 名称
     */
    @ZenMethod
    public void removeModifier(String name) {
        semiPermanentModifiers.remove(name);
        flushContextModifier();
    }

    /**
     * 线程内是否存在指定名称的半永久配方修改器。
     *
     * @param name 名称
     */
    @ZenMethod
    public boolean hasModifier(String name) {
        return semiPermanentModifiers.containsKey(name);
    }

    /**
     * 添加一个永久配方修改器，它将永远存在这个线程中。
     * @param name     名称
     * @param modifier 修改器
     */
    @ZenMethod
    public void addPermanentModifier(String name, RecipeModifier modifier) {
        permanentModifiers.put(name, modifier);
        flushContextModifier();
    }

    /**
     * 线程内是否存在指定名称的永久配方修改器。
     *
     * @param name 名称
     */
    @ZenMethod
    public boolean hasPermanentModifier(String name) {
        return permanentModifiers.containsKey(name);
    }

    /**
     * 删除一个永久配方修改器。
     *
     * @param name 名称
     */
    @ZenMethod
    public void removePermanentModifier(String name) {
        permanentModifiers.remove(name);
        flushContextModifier();
    }

    /**
     * 设置当前线程的状态信息。
     * @param info 信息
     */
    @ZenMethod
    public void setStatusInfo(String info) {
        status = new CraftingStatus(status.getStatus(), info);
    }

    /**
     * 为当前线程添加固定配方（仅守护线程生效）。
     */
    @ZenMethod
    public RecipeThread addRecipe(String recipeName) {
        WAIT_FOR_ADD.add(() -> {
            MachineRecipe recipe = RecipeRegistry.getRecipe(new ResourceLocation(ModularMachinery.MODID, recipeName));
            if (recipe != null) {
                addRecipe(recipe);
            } else {
                CraftTweakerAPI.logError("[ModularMachinery] Cloud not found recipe by name " + recipeName + "!");
            }
        });

        return this;
    }

    public RecipeThread addRecipe(MachineRecipe recipe) {
        recipeSet.add(recipe);
        return this;
    }

    public TreeSet<MachineRecipe> getRecipeSet() {
        return recipeSet;
    }

    public RecipeThread copyDaemonThread(TileFactoryController factory) {
        return new RecipeThread(factory, true, threadName, recipeSet, permanentModifiers);
    }

    public RecipeThread copyDataToAnother(TileFactoryController factory, RecipeThread another) {
        another.factory = factory;
        another.isDaemon = isDaemon;
        another.threadName = threadName;
        another.recipeSet.addAll(recipeSet);
        another.permanentModifiers.putAll(permanentModifiers);

        return another;
    }
}
