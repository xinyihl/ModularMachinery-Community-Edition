/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.tiles;

import com.google.common.collect.Lists;
import crafttweaker.api.data.IData;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.api.world.IBlockPos;
import crafttweaker.api.world.IWorld;
import crafttweaker.util.IEventHandler;
import github.kasuminova.mmce.common.concurrent.RecipeSearchTask;
import github.kasuminova.mmce.common.concurrent.Sync;
import github.kasuminova.mmce.common.concurrent.TaskExecutor;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentSelectorTag;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.data.Config;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.IMachineController;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.machine.MachineEvent;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.machine.MachineStructureFormedEvent;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.machine.MachineTickEvent;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.recipe.*;
import hellfirepvp.modularmachinery.common.item.ItemBlueprint;
import hellfirepvp.modularmachinery.common.lib.BlocksMM;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import hellfirepvp.modularmachinery.common.modifier.ModifierReplacement;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.tiles.base.ColorableMachineTile;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.SelectiveUpdateTileEntity;
import hellfirepvp.modularmachinery.common.tiles.base.TileEntityRestrictedTick;
import hellfirepvp.modularmachinery.common.util.*;
import io.netty.util.internal.ThrowableUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>完全重构的社区版机械控制器，拥有强大的异步逻辑和极低的性能消耗。</p>
 * <p>Completely refactored community edition mechanical controller with powerful asynchronous logic and extremely low performance consumption.</p>
 * TODO: This class is too large, consider improving readability.
 */
public class TileMachineController extends TileEntityRestrictedTick implements IMachineController, SelectiveUpdateTileEntity {
    public static final int BLUEPRINT_SLOT = 0, ACCELERATOR_SLOT = 1;
    public static int structureCheckDelay = 30, maxStructureCheckDelay = 100;
    public static boolean delayedStructureCheck = true;
    public static boolean cleanCustomDataOnStructureCheckFailed = false;
    private final Map<BlockPos, List<ModifierReplacement>> foundModifiers = new HashMap<>();
    private final Map<String, RecipeModifier> customModifiers = new HashMap<>();
    private final Map<TileSmartInterface.SmartInterfaceProvider, String> foundSmartInterfaces = new HashMap<>();
    private final List<Tuple<MachineComponent<?>, ComponentSelectorTag>> foundComponents = new ArrayList<>();
    private final List<TileParallelController.ParallelControllerProvider> foundParallelControllers = new ArrayList<>();
    private EnumFacing controllerRotation = null;
    private BlockController parentController = null;
    private NBTTagCompound customData = new NBTTagCompound();
    private CraftingStatus craftingStatus = CraftingStatus.MISSING_STRUCTURE;
    private DynamicMachine.ModifierReplacementMap foundReplacements = null;
    private IOInventory inventory;
    private DynamicMachine foundMachine = null;
    private DynamicMachine parentMachine = null;
    private TaggedPositionBlockArray foundPattern = null;
    private ActiveMachineRecipe activeRecipe = null;
    private RecipeCraftingContext context = null;
    private RecipeSearchTask searchTask = null;
    private int recipeResearchRetryCounter = 0;
    private int structureCheckCounter = 0;

    public TileMachineController() {
        this.inventory = buildInventory();
        this.inventory.setStackLimit(1, BLUEPRINT_SLOT);
    }

    public TileMachineController(IBlockState state) {
        this();
        if (state.getBlock() instanceof BlockController) {
            this.parentController = (BlockController) state.getBlock();
            this.parentMachine = parentController.getParentMachine();
            this.controllerRotation = state.getValue(BlockController.FACING);
        } else {
            // wtf, where is the controller?
            ModularMachinery.log.warn("Invalid controller block at " + getPos() + " !");
            controllerRotation = EnumFacing.NORTH;
        }
    }

    public static void loadFromConfig(Configuration config) {
        //最短结构检查间隔
        structureCheckDelay = config.getInt("structure-check-delay", "general",
                30, 1, 1200,
                "The multiblock structure checks the structural integrity at how often? (TimeUnit: Tick)");
        //延迟结构检查
        delayedStructureCheck = config.getBoolean("delayed-structure-check", "general",
                true, "When enabled, the structure check interval in the idle state is incrementally increased to ease the performance footprint.");
        //最长结构检查间隔
        maxStructureCheckDelay = config.getInt("max-structure-check-delay", "general",
                100, 2, 1200,
                "When delayed-structure-check is enabled, what is the maximum check interval? (TimeUnit: Tick)");

        //检查最短结构检查间隔是否大于最长结构检查间隔
        if (structureCheckDelay >= maxStructureCheckDelay) {
            ModularMachinery.log.warn("structure-check-delay is bigger than or equal max-structure-check-delay!, use default value...");
            structureCheckDelay = 40;
            maxStructureCheckDelay = 100;
        }

        //当结构检查失败时，是否清空自定义数据
        cleanCustomDataOnStructureCheckFailed = config.getBoolean("clean-custom-data-on-structure-check-failed", "general",
                false, "When enabled, the customData will be cleared when multiblock structure check failed.");
    }

    @Override
    public void doRestrictedTick() {
        if (getWorld().isRemote) {
            return;
        }
        if (getWorld().getStrongPower(getPos()) > 0) {
            return;
        }

        // Use async check for large structure
        if (isStructureFormed() && !ModularMachinery.pluginServerCompatibleMode && this.foundPattern.getPattern().size() >= 1000) {
            ModularMachinery.EXECUTE_MANAGER.addParallelAsyncTask(() -> {
                onMachineTick();
                if (doStructureCheck() && isStructureFormed()) {
                    if (activeRecipe != null || searchAndStartRecipe()) {
                        doRecipeTick();
                    }
                }
            });
            return;
        }

        // Normal logic
        if (!doStructureCheck() || !isStructureFormed()) {
            return;
        }

        if (hasMachineTickEventHandlers()) {
            ModularMachinery.EXECUTE_MANAGER.addParallelAsyncTask(() -> {
                onMachineTick();
                if (activeRecipe != null || searchAndStartRecipe()) {
                    doRecipeTick();
                }
            });
            return;
        }

        if (activeRecipe != null || searchAndStartRecipe()) {
            ModularMachinery.EXECUTE_MANAGER.addParallelAsyncTask(this::doRecipeTick);
        }
    }

    private void checkRotation() {
        if (controllerRotation != null) {
            return;
        }
        IBlockState state = getWorld().getBlockState(getPos());
        if (state.getBlock() instanceof BlockController) {
            this.parentController = (BlockController) state.getBlock();
            this.parentMachine = parentController.getParentMachine();
            this.controllerRotation = state.getValue(BlockController.FACING);
        } else {
            // wtf, where is the controller?
            ModularMachinery.log.warn("Invalid controller block at " + getPos() + " !");
            controllerRotation = EnumFacing.NORTH;
        }
    }

    private boolean doStructureCheck() {
        if (!canCheckStructure()) {
            return true;
        }
        checkRotation();
        //检查多方块结构中的某个方块的区块是否被卸载，以免一些重要的配方运行失败。
        //可能会提高一些性能开销，但是玩家体验更加重要。
        //Check if a block of a chunk in a multiblock structure is unloaded, so that some important recipes do not fail to run.
        //It may raise some performance overhead, but the player experience is more important.
        if (!checkStructure()) {
            if (craftingStatus != CraftingStatus.CHUNK_UNLOADED) {
                craftingStatus = CraftingStatus.CHUNK_UNLOADED;
                markForUpdateSync();
            }
            return false;
        }
        if (!isStructureFormed()) {
            if (craftingStatus != CraftingStatus.MISSING_STRUCTURE) {
                craftingStatus = CraftingStatus.MISSING_STRUCTURE;
                markForUpdateSync();
            }
            return false;
        }
        updateComponents();
        return true;
    }

    private void doRecipeTick() {
        resetStructureCheckCounter();

        if (this.context == null) {
            //context preInit
            context = createContext(this.activeRecipe);
            context.setParallelism(this.activeRecipe.getParallelism());
        }

        CraftingStatus prevStatus = this.craftingStatus;
        MachineRecipe machineRecipe = this.activeRecipe.getRecipe();

        //检查预 Tick 事件是否阻止进一步运行。
        //Check if the PreTick event prevents further runs.
        if (!onPreTick()) {
            if (this.activeRecipe != null) {
                this.activeRecipe.tick(this, this.context);
                if (this.craftingStatus.isCrafting()) {
                    this.activeRecipe.setTick(Math.max(this.activeRecipe.getTick() - 1, 0));
                }
            }
            markForUpdateSync();
            return;
        }

        //当脚本修改了运行状态时，内部不再覆盖运行状态。
        //When scripts changed craftingStatus, it is no longer modified internally.
        if (prevStatus.equals(this.craftingStatus)) {
            this.craftingStatus = this.activeRecipe.tick(this, this.context);
        } else {
            this.activeRecipe.tick(this, this.context);
        }
        if (this.craftingStatus.isCrafting()) {
            onTick();
            if (this.activeRecipe.isCompleted()) {
                if (ModularMachinery.pluginServerCompatibleMode) {
                    ModularMachinery.EXECUTE_MANAGER.addSyncTask(this::onFinished);
                } else {
                    onFinished();
                }
            }
        } else if (machineRecipe.doesCancelRecipeOnPerTickFailure()) {
            this.activeRecipe = null;
            this.context = null;
        }
        markForUpdateSync();
    }

    @SuppressWarnings("unused")
    public BlockController getParentController() {
        return parentController;
    }

    @SuppressWarnings("unused")
    public DynamicMachine getParentMachine() {
        return parentMachine;
    }

    /**
     * <p>机器开始执行逻辑。</p>
     */
    public boolean onMachineTick() {
        List<IEventHandler<MachineEvent>> handlerList = this.foundMachine.getMachineEventHandlers(MachineTickEvent.class);
        if (handlerList == null || handlerList.isEmpty()) return false;
        for (IEventHandler<MachineEvent> handler : handlerList) {
            MachineTickEvent event = new MachineTickEvent(this);
            handler.handle(event);
        }
        return true;
    }

    public boolean hasMachineTickEventHandlers() {
        List<IEventHandler<MachineEvent>> handlerList = this.foundMachine.getMachineEventHandlers(MachineTickEvent.class);
        return handlerList != null && !handlerList.isEmpty();
    }

    /**
     * <p>机器开始检查配方能否工作。</p>
     *
     * @param context RecipeCraftingContext
     * @return CraftingCheckResult
     */
    public RecipeCraftingContext.CraftingCheckResult onCheck(RecipeCraftingContext context) {
        RecipeCraftingContext.CraftingCheckResult result = context.canStartCrafting();
        if (result.isSuccess()) {
            List<IEventHandler<RecipeEvent>> handlerList = context.getActiveRecipe().getRecipe().getRecipeEventHandlers(RecipeCheckEvent.class);
            if (handlerList == null || handlerList.isEmpty()) return result;
            for (IEventHandler<RecipeEvent> handler : handlerList) {
                RecipeCheckEvent event = new RecipeCheckEvent(this);
                handler.handle(event);
                if (event.isFailure()) {
                    result.overrideError(event.getFailureReason());
                    return result;
                }
            }
        }

        return result;
    }

    /**
     * <p>机器开始执行一个配方。</p>
     *
     * @param activeRecipe ActiveMachineRecipe
     * @param context      RecipeCraftingContext
     */
    public void onStart(ActiveMachineRecipe activeRecipe, RecipeCraftingContext context) {
        this.activeRecipe = activeRecipe;
        this.context = context;

        List<IEventHandler<RecipeEvent>> handlerList = this.activeRecipe.getRecipe().getRecipeEventHandlers(RecipeStartEvent.class);
        if (handlerList != null && !handlerList.isEmpty()) {
            for (IEventHandler<RecipeEvent> handler : handlerList) {
                RecipeStartEvent event = new RecipeStartEvent(this);
                handler.handle(event);
            }
        }
        activeRecipe.start(context);
    }

    /**
     * <p>机器在完成配方 Tick 后执行</p>
     *
     * @return 如果为 false，则进度停止增加，并在控制器状态栏输出原因
     */
    public boolean onPreTick() {
        List<IEventHandler<RecipeEvent>> handlerList = this.activeRecipe.getRecipe().getRecipeEventHandlers(RecipePreTickEvent.class);
        if (handlerList == null || handlerList.isEmpty()) return true;

        for (IEventHandler<RecipeEvent> handler : handlerList) {
            RecipePreTickEvent event = new RecipePreTickEvent(this);
            handler.handle(event);

            if (event.isPreventProgressing()) {
                craftingStatus = CraftingStatus.working(event.getReason());
                return false;
            }
            if (event.isFailure()) {
                if (event.isDestructRecipe()) {
                    this.activeRecipe = null;
                    this.context = null;
                }
                craftingStatus = CraftingStatus.failure(event.getReason());
                return false;
            }
        }

        return true;
    }

    /**
     * <p>与 {@code onPreTick()} 相似，但是可以销毁配方。</p>
     */
    public void onTick() {
        List<IEventHandler<RecipeEvent>> handlerList = this.activeRecipe.getRecipe().getRecipeEventHandlers(RecipeTickEvent.class);
        if (handlerList == null || handlerList.isEmpty()) return;

        for (IEventHandler<RecipeEvent> handler : handlerList) {
            RecipeTickEvent event = new RecipeTickEvent(this);
            handler.handle(event);
            if (event.isFailure()) {
                if (event.isDestructRecipe()) {
                    this.activeRecipe = null;
                    this.context = null;
                }
                craftingStatus = CraftingStatus.failure(event.getFailureReason());
                return;
            }
        }
    }

    /**
     * <p>机械完成一个配方。</p>
     */
    public void onFinished() {
        List<IEventHandler<RecipeEvent>> handlerList = this.activeRecipe.getRecipe().getRecipeEventHandlers(RecipeFinishEvent.class);
        if (handlerList != null && !handlerList.isEmpty()) {
            for (IEventHandler<RecipeEvent> handler : handlerList) {
                RecipeFinishEvent event = new RecipeFinishEvent(this);
                handler.handle(event);
            }
        }

        this.context.finishCrafting();

        this.activeRecipe = new ActiveMachineRecipe(this.activeRecipe.getRecipe(), getMaxParallelism());
        this.context = createContext(this.activeRecipe);
        tryStartRecipe(context);
    }

    /**
     * <p>机器尝试开始执行一个配方。</p>
     *
     * @param context RecipeCraftingContext
     */
    private void tryStartRecipe(@Nonnull RecipeCraftingContext context) {
        RecipeCraftingContext.CraftingCheckResult tryResult = onCheck(context);

        if (tryResult.isSuccess()) {
            Sync.doSyncAction(() -> onStart(context.getActiveRecipe(), context));
            markForUpdateSync();
        } else {
            this.craftingStatus = CraftingStatus.failure(tryResult.getFirstErrorMessage(""));
            this.activeRecipe = null;
            this.context = null;

            createRecipeSearchTask();
        }
    }

    @Override
    public IWorld getIWorld() {
        return CraftTweakerMC.getIWorld(getWorld());
    }

    @Override
    public crafttweaker.api.block.IBlockState getIBlockState() {
        return CraftTweakerMC.getBlockState(getWorld().getBlockState(getPos()));
    }

    @Override
    public IBlockPos getIPos() {
        return CraftTweakerMC.getIBlockPos(getPos());
    }

    @Override
    public ActiveMachineRecipe getActiveRecipe() {
        return activeRecipe;
    }

    @Override
    public boolean isWorking() {
        return craftingStatus.isCrafting();
    }

    @Override
    public String getFormedMachineName() {
        return isStructureFormed() ? foundMachine.getRegistryName().toString() : null;
    }

    @Override
    public IData getCustomData() {
        return CraftTweakerMC.getIDataModifyable(customData);
    }

    @Override
    public void setCustomData(IData data) {
        customData = CraftTweakerMC.getNBTCompound(data);
    }

    public void cancelCrafting(String reason) {
        this.activeRecipe = null;
        this.context = null;
        this.craftingStatus = CraftingStatus.failure(reason);
    }

    @Override
    public void addModifier(String key, RecipeModifier newModifier) {
        if (newModifier != null) {
            customModifiers.put(key, newModifier);
            flushContextModifier();
        }
    }

    @Override
    public void removeModifier(String key) {
        if (customModifiers.containsKey(key)) {
            customModifiers.remove(key);
            flushContextModifier();
        }
    }

    @Override
    public boolean hasModifier(String key) {
        return customModifiers.containsKey(key);
    }

    @Override
    public void overrideStatusInfo(String newInfo) {
        this.craftingStatus.overrideStatusMessage(newInfo);
    }

    @Override
    @Nullable
    public SmartInterfaceData getSmartInterfaceData(String requiredType) {
        AtomicReference<TileSmartInterface.SmartInterfaceProvider> reference = new AtomicReference<>(null);
        foundSmartInterfaces.forEach((provider, type) -> {
            if (type.equals(requiredType)) {
                reference.set(provider);
            }
        });
        TileSmartInterface.SmartInterfaceProvider smartInterface = reference.get();
        if (smartInterface != null) {
            return smartInterface.getMachineData(getPos());
        } else {
            return null;
        }
    }

    @Override
    public SmartInterfaceData[] getSmartInterfaceDataList() {
        List<SmartInterfaceData> dataList = new ArrayList<>();
        BlockPos ctrlPos = getPos();
        foundSmartInterfaces.forEach((provider, type) -> {
            SmartInterfaceData data = provider.getMachineData(ctrlPos);
            if (data != null) {
                dataList.add(data);
            }
        });
        return dataList.toArray(new SmartInterfaceData[0]);
    }

    @Override
    public TileMachineController getController() {
        return this;
    }

    public void flushContextModifier() {
        if (context != null) {
            this.context.overrideModifier(MiscUtils.flatten(this.foundModifiers.values()));
            for (RecipeModifier modifier : customModifiers.values()) {
                this.context.addModifier(modifier);
            }
        }
    }

    private IOInventory buildInventory() {
        return new IOInventory(this, new int[0], new int[0]).setMiscSlots(BLUEPRINT_SLOT, ACCELERATOR_SLOT);
    }

    public IOInventory getInventory() {
        return inventory;
    }

    public RecipeCraftingContext createContext(ActiveMachineRecipe activeRecipe) {
        RecipeCraftingContext context = this.foundMachine.createContext(activeRecipe, this, this.foundComponents, MiscUtils.flatten(this.foundModifiers.values()));
        for (RecipeModifier modifier : customModifiers.values()) {
            context.addModifier(modifier);
        }
        return context;
    }

    /**
     * 搜索并运行配方。
     *
     * @return 如果有配方正在运行，返回 true，否则 false。
     */
    private boolean searchAndStartRecipe() {
        if (searchTask != null) {
            if (!searchTask.isDone()) {
                return false;
            }

            //并发检查
            if (searchTask.getCurrentMachine() == foundMachine) {
                try {
                    RecipeCraftingContext context = searchTask.get();
                    if (context != null) {
                        tryStartRecipe(context);
                        searchTask = null;
                        return true;
                    }
                } catch (Exception e) {
                    ModularMachinery.log.warn(ThrowableUtil.stackTraceToString(e));
                }
            }

            searchTask = null;
        } else if (this.ticksExisted % currentRecipeSearchDelay() == 0) {
            createRecipeSearchTask();
        }
        return false;
    }

    private void createRecipeSearchTask() {
        searchTask = new RecipeSearchTask(this, foundMachine);
        TaskExecutor.FORK_JOIN_POOL.submit(searchTask);
    }

    private void resetMachine(boolean clearData) {
        if (clearData) {
            activeRecipe = null;
            craftingStatus = CraftingStatus.MISSING_STRUCTURE;
            incrementStructureCheckCounter();
            resetRecipeSearchRetryCount();

            if (cleanCustomDataOnStructureCheckFailed) {
                customData = new NBTTagCompound();
                customModifiers.clear();
            }
        }
        foundMachine = null;
        foundPattern = null;
        foundReplacements = null;
    }

    public int incrementRecipeSearchRetryCount() {
        recipeResearchRetryCounter++;
        return recipeResearchRetryCounter;
    }

    public void resetRecipeSearchRetryCount() {
        this.recipeResearchRetryCounter = 0;
    }

    public int incrementStructureCheckCounter() {
        structureCheckCounter++;
        return structureCheckCounter;
    }

    public void resetStructureCheckCounter() {
        this.structureCheckCounter = 0;
    }

    private boolean canCheckStructure() {
        if (isStructureFormed() && foundComponents.isEmpty()) {
            return true;
        }
        if (!delayedStructureCheck) {
            return ticksExisted % structureCheckDelay == 0;
        }
        if (isStructureFormed()) {
            return ticksExisted % Math.min(structureCheckDelay + currentRecipeSearchDelay(), maxStructureCheckDelay - structureCheckDelay) == 0;
        } else {
            return ticksExisted % Math.min(structureCheckDelay + this.structureCheckCounter * 5, maxStructureCheckDelay - structureCheckDelay) == 0;
        }
    }

    private int currentRecipeSearchDelay() {
        return Math.min(10 + this.recipeResearchRetryCounter * 5, 80);
    }

    public boolean isStructureFormed() {
        return this.foundMachine != null && this.foundPattern != null;
    }

    private void onStructureFormed() {
        Sync.doSyncAction(() -> {
            if (parentController != null) {
                this.world.setBlockState(pos, parentController.getDefaultState().withProperty(BlockController.FACING, this.controllerRotation));
            } else {
                this.world.setBlockState(pos, BlocksMM.blockController.getDefaultState().withProperty(BlockController.FACING, this.controllerRotation));
            }
        });

        if (this.foundMachine.getMachineColor() != Config.machineColor) {
            Sync.doSyncAction(this::distributeCasingColor);
        }

        List<IEventHandler<MachineEvent>> handlerList = this.foundMachine.getMachineEventHandlers(MachineStructureFormedEvent.class);
        if (handlerList != null && !handlerList.isEmpty()) {
            for (IEventHandler<MachineEvent> handler : handlerList) {
                MachineStructureFormedEvent event = new MachineStructureFormedEvent(this);
                handler.handle(event);
            }
        }

        resetStructureCheckCounter();
        markForUpdateSync();
    }

    /**
     * <p>检查机械结构。</p>
     * <p>Check machine structure.</p>
     *
     * @return Returns false when there is a square block in the structure that is not loaded, and true in all other cases.
     */
    private boolean checkStructure() {
        if (!canCheckStructure()) {
            return true;
        }

        if (isStructureFormed()) {
            if (foundComponents.isEmpty()) {
                for (BlockPos potentialPosition : foundPattern.getPattern().keySet()) {
                    BlockPos realPos = getPos().add(potentialPosition);
                    //Is Chunk Loaded? Prevention of unanticipated consumption of something.
                    if (!getWorld().isBlockLoaded(realPos)) {
                        return false;
                    }
                }
            }
            if (this.foundMachine.isRequiresBlueprint() && !this.foundMachine.equals(getBlueprintMachine())) {
                resetMachine(true);
            } else if (!foundPattern.matches(getWorld(), getPos(), true, this.foundReplacements)) {
                resetMachine(true);
            } else {
                incrementStructureCheckCounter();
            }
        }

        if (this.foundMachine != null && this.foundPattern != null && this.controllerRotation != null && this.foundReplacements != null) {
            return true;
        }
        resetMachine(false);

        // First, check blueprint machine.
        DynamicMachine blueprint = getBlueprintMachine();
        if (blueprint != null) {
            if (matchesRotation(BlockArrayCache.getBlockArrayCache(blueprint.getPattern().traitNum, controllerRotation), blueprint)) {
                onStructureFormed();
                return true;
            }
        }

        // After, check parentMachine.
        if (parentMachine != null) {
            if (parentMachine.isRequiresBlueprint() && !parentMachine.equals(blueprint)) {
                // ParentMachine needs blueprint, but controller not has that, end check.
                return true;
            }
            if (matchesRotation(BlockArrayCache.getBlockArrayCache(parentMachine.getPattern().traitNum, controllerRotation), parentMachine)) {
                onStructureFormed();
                return true;
            }
            // This controller is dedicated to parentMachine, it cannot become other, end check.
            return true;
        }

        // Finally, check all registered machinery.
        // TODO It needs to be optimized, it takes up too much performance.
        for (DynamicMachine machine : MachineRegistry.getRegistry()) {
            if (machine.isRequiresBlueprint()) continue;
            if (matchesRotation(BlockArrayCache.getBlockArrayCache(machine.getPattern().traitNum, controllerRotation), machine)) {
                onStructureFormed();
                break;
            }
        }

        return true;
    }

    private void distributeCasingColor() {
        if (this.foundMachine != null && this.foundPattern != null) {
            int color = this.foundMachine.getMachineColor();
            tryColorize(getPos(), color);
            for (BlockPos pos : this.foundPattern.getPattern().keySet()) {
                tryColorize(this.getPos().add(pos), color);
            }
        }
    }

    /**
     * 尝试对方块上色。
     * 此方法内容不应被修改，因为内容已被 gugu_utils 所覆盖。
     *
     * @param pos   位置
     * @param color 颜色
     */
    private void tryColorize(BlockPos pos, int color) {
        TileEntity te = this.getWorld().getTileEntity(pos);
        if (te instanceof ColorableMachineTile) {
            ((ColorableMachineTile) te).setMachineColor(color);
            getWorld().addBlockEvent(pos, getWorld().getBlockState(pos).getBlock(), 1, 1);
        }
    }

    private boolean matchesRotation(TaggedPositionBlockArray pattern, DynamicMachine machine) {
        if (pattern == null) {
            return false;
        }
        DynamicMachine.ModifierReplacementMap replacements = machine.getModifiersAsMatchingReplacements();
        if (pattern.matches(getWorld(), getPos(), false, replacements)) {
            this.foundPattern = pattern;
            this.foundMachine = machine;
            this.foundReplacements = replacements;
            return true;
        }
        resetMachine(false);
        return false;
    }

    public int getMaxParallelism() {
        int parallelism = 0;
        int maxParallelism = foundMachine.getMaxParallelism();
        for (TileParallelController.ParallelControllerProvider provider : foundParallelControllers) {
            parallelism += provider.getParallelism();

            if (parallelism >= maxParallelism) {
                return maxParallelism;
            }
        }
        return Math.max(1, parallelism);
    }

    private void updateComponents() {
        if (this.foundMachine == null || this.foundPattern == null || this.controllerRotation == null || this.foundReplacements == null) {
            this.foundComponents.clear();
            this.foundModifiers.clear();
            this.foundSmartInterfaces.clear();

            resetMachine(false);
            return;
        }
        if (!canCheckStructure()) {
            return;
        }

        this.foundComponents.clear();
        this.foundSmartInterfaces.clear();
        this.foundParallelControllers.clear();
        for (BlockPos potentialPosition : this.foundPattern.getPattern().keySet()) {
            BlockPos realPos = getPos().add(potentialPosition);
            TileEntity te = getWorld().getTileEntity(realPos);
            if (!(te instanceof MachineComponentTile)) {
                continue;
            }

            ComponentSelectorTag tag = this.foundPattern.getTag(potentialPosition);
            MachineComponent<?> component = ((MachineComponentTile) te).provideComponent();
            if (component == null) {
                continue;
            }

            this.foundComponents.add(new Tuple<>(component, tag));

            if (component instanceof TileParallelController.ParallelControllerProvider) {
                this.foundParallelControllers.add((TileParallelController.ParallelControllerProvider) component);
                continue;
            }

            checkAndAddSmartInterface(component, realPos);
        }

        updateModifiers();
    }

    private void updateModifiers() {
        int rotations = 0;
        EnumFacing rot = EnumFacing.NORTH;
        while (rot != this.controllerRotation) {
            rot = rot.rotateYCCW();
            rotations++;
        }

        this.foundModifiers.clear();
        for (Map.Entry<BlockPos, List<ModifierReplacement>> offsetModifiers : this.foundMachine.getModifiers().entrySet()) {
            BlockPos at = offsetModifiers.getKey();
            for (int i = 0; i < rotations; i++) {
                at = new BlockPos(at.getZ(), at.getY(), -at.getX());
            }
            BlockPos realAt = this.getPos().add(at);
            for (ModifierReplacement mod : offsetModifiers.getValue()) {
                if (mod.getBlockInformation().matches(this.world, realAt, true)) {
                    this.foundModifiers.putIfAbsent(offsetModifiers.getKey(), Lists.newArrayList());
                    this.foundModifiers.get(offsetModifiers.getKey()).add(mod);
                }
            }
        }
    }

    private void checkAndAddSmartInterface(MachineComponent<?> component, BlockPos realPos) {
        if (!(component instanceof TileSmartInterface.SmartInterfaceProvider) || foundMachine.smartInterfaceTypesIsEmpty()) {
            return;
        }
        TileSmartInterface.SmartInterfaceProvider smartInterface = (TileSmartInterface.SmartInterfaceProvider) component;
        SmartInterfaceData data = smartInterface.getMachineData(getPos());
        Map<String, SmartInterfaceType> notFoundInterface = foundMachine.getFilteredType(foundSmartInterfaces.values());

        if (data != null) {
            String type = data.getType();

            if (notFoundInterface.containsKey(type)) {
                foundSmartInterfaces.put(smartInterface, type);
            } else {
                smartInterface.removeMachineData(realPos);
            }
        } else {
            if (notFoundInterface.isEmpty()) {
                Optional<SmartInterfaceType> typeOpt = foundMachine.getFirstSmartInterfaceType();
                if (typeOpt.isPresent()) {
                    SmartInterfaceType type = typeOpt.get();
                    smartInterface.addMachineData(getPos(), foundMachine.getRegistryName(), type.getType(), type.getDefaultValue(), true);
                    foundSmartInterfaces.put(smartInterface, type.getType());
                }
            } else {
                SmartInterfaceType type = notFoundInterface.values().stream().sorted().findFirst().get();
                smartInterface.addMachineData(getPos(), foundMachine.getRegistryName(), type.getType(), type.getDefaultValue(), true);
                foundSmartInterfaces.put(smartInterface, type.getType());
            }
        }
    }

    @Deprecated
    public float getCurrentActiveRecipeProgress(float partial) {
        if (activeRecipe == null) return 0F;
        float tick = activeRecipe.getTick() + partial;
        float maxTick = activeRecipe.getTotalTick();
        return MathHelper.clamp(tick / maxTick, 0F, 1F);
    }

    @Override
    public boolean shouldRefresh(@Nonnull World world, @Nonnull BlockPos pos, IBlockState oldState, IBlockState newSate) {
        return oldState.getBlock() != newSate.getBlock();
    }

    @Nullable
    public DynamicMachine getFoundMachine() {
        return foundMachine;
    }

    public boolean isParallelized() {
        if (foundMachine != null) {
            return foundMachine.isParallelizable() && getMaxParallelism() > 1;
        } else {
            return false;
        }
    }

    @Nullable
    public DynamicMachine getBlueprintMachine() {
        ItemStack blueprintSlotted = this.inventory.getStackInSlot(BLUEPRINT_SLOT);
        if (!blueprintSlotted.isEmpty()) {
            return ItemBlueprint.getAssociatedMachine(blueprintSlotted);
        }
        return null;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) inventory;
        }
        return super.getCapability(capability, facing);
    }

    public CraftingStatus getCraftingStatus() {
        return craftingStatus;
    }

    public void setCraftingStatus(CraftingStatus status) {
        this.craftingStatus = status;
    }

    /**
     * <p>一定程度上缓解过大的带宽占用问题。</p>
     * <p>如果你想知道更多原因，请查看<a href="https://github.com/HellFirePvP/ModularMachinery/issues/228">此链接</a>。</p>
     * <p>Alleviate excessive bandwidth usage to a certain extent</p>
     * <p>If you want to know more about why, please <a href="https://github.com/HellFirePvP/ModularMachinery/issues/228">See This Link</a>.</p>
     */
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return Config.selectiveUpdateTileEntity ? null : super.getUpdatePacket();
    }

    @Override
    public SPacketUpdateTileEntity getTrueUpdatePacket() {
        return super.getUpdatePacket();
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        this.inventory = IOInventory.deserialize(this, compound.getCompoundTag("items"));
        this.inventory.setStackLimit(1, BLUEPRINT_SLOT);

        if (compound.hasKey("status")) {
            // Legacy support
            // TODO: How old a version is it compatible with?
            this.craftingStatus = new CraftingStatus(Type.values()[compound.getInteger("status")], "");
        } else {
            this.craftingStatus = CraftingStatus.deserialize(compound.getCompoundTag("statusTag"));
        }

        readMachineNBT(compound);

        if (compound.hasKey("activeRecipe")) {
            NBTTagCompound tag = compound.getCompoundTag("activeRecipe");
            ActiveMachineRecipe recipe = new ActiveMachineRecipe(tag);
            if (recipe.getRecipe() == null) {
                ModularMachinery.log.info("Couldn't find recipe named " + tag.getString("recipeName") + " for controller at " + getPos());
                this.activeRecipe = null;
            } else {
                this.activeRecipe = recipe;
            }
        } else {
            this.activeRecipe = null;
        }
    }

    private void readMachineNBT(NBTTagCompound compound) {
        if (compound.hasKey("parentMachine")) {
            ResourceLocation rl = new ResourceLocation(compound.getString("parentMachine"));
            parentMachine = MachineRegistry.getRegistry().getMachine(rl);
            if (parentMachine != null) {
                parentController = BlockController.MACHINE_CONTROLLERS.get(parentMachine);
            } else {
                ModularMachinery.log.info("Couldn't find machine named " + rl + " for controller at " + getPos());
            }
        }

        if (!compound.hasKey("machine") || !compound.hasKey("rotation")) {
            resetMachine(false);
            return;
        }

        ResourceLocation rl = new ResourceLocation(compound.getString("machine"));
        DynamicMachine machine = MachineRegistry.getRegistry().getMachine(rl);
        if (machine == null) {
            ModularMachinery.log.info("Couldn't find machine named " + rl + " for controller at " + getPos());
            resetMachine(false);
            return;
        }
        this.foundMachine = machine;
        this.controllerRotation = EnumFacing.byHorizontalIndex(compound.getInteger("rotation"));

        TaggedPositionBlockArray pattern = BlockArrayCache.getBlockArrayCache(machine.getPattern().traitNum, this.controllerRotation);
        if (pattern == null) {
            ModularMachinery.log.info(rl + " has a empty pattern cache! Please report this to the mod author.");
            resetMachine(false);
            return;
        }
        this.foundPattern = pattern;

        if (parentMachine == null) {
            parentController = BlockController.MACHINE_CONTROLLERS.get(machine);
        }

        DynamicMachine.ModifierReplacementMap replacements = machine.getModifiersAsMatchingReplacements();
        EnumFacing offset = EnumFacing.NORTH;
        while (offset != this.controllerRotation) {
            replacements = replacements.rotateYCCW();
            offset = offset.rotateY();
        }
        this.foundReplacements = replacements;

        if (compound.hasKey("customData")) {
            this.customData = compound.getCompoundTag("customData");
        }
        if (compound.hasKey("customModifier")) {
            NBTTagList tagList = compound.getTagList("customModifier", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < tagList.tagCount(); i++) {
                NBTTagCompound modifierTag = tagList.getCompoundTagAt(i);
                this.customModifiers.put(modifierTag.getString("key"), RecipeModifier.deserialize(modifierTag.getCompoundTag("modifier")));
            }
        }
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setTag("items", this.inventory.writeNBT());
        compound.setTag("statusTag", this.craftingStatus.serialize());

        if (this.parentMachine != null) {
            compound.setString("parentMachine", this.parentMachine.getRegistryName().toString());
        }
        if (this.controllerRotation != null) {
            compound.setInteger("rotation", this.controllerRotation.getHorizontalIndex());
        }
        if (this.foundMachine != null) {
            compound.setString("machine", this.foundMachine.getRegistryName().toString());

            if (!customData.isEmpty()) {
                compound.setTag("customData", customData);
            }
            if (!customModifiers.isEmpty()) {
                NBTTagList tagList = new NBTTagList();
                customModifiers.forEach((key, modifier) -> {
                    if (key != null && modifier != null) {
                        NBTTagCompound modifierTag = new NBTTagCompound();
                        modifierTag.setString("key", key);
                        modifierTag.setTag("modifier", modifier.serialize());
                        tagList.appendTag(modifierTag);
                    }
                });
                compound.setTag("customModifier", tagList);
            }
        }
        if (this.activeRecipe != null) {
            compound.setTag("activeRecipe", this.activeRecipe.serialize());
        }
    }

    public enum Type {
        MISSING_STRUCTURE,
        CHUNK_UNLOADED,
        NO_RECIPE,
        CRAFTING;

        public String getUnlocalizedDescription() {
            return "gui.controller.status." + this.name().toLowerCase();
        }

    }

    public static class CraftingStatus {

        private static final CraftingStatus SUCCESS = new CraftingStatus(Type.CRAFTING, "");
        private static final CraftingStatus MISSING_STRUCTURE = new CraftingStatus(Type.MISSING_STRUCTURE, "");
        private static final CraftingStatus CHUNK_UNLOADED = new CraftingStatus(Type.CHUNK_UNLOADED, "");
        private final Type status;
        private String unlocalizedMessage;

        private CraftingStatus(Type status, String unlocalizedMessage) {
            this.status = status;
            this.unlocalizedMessage = unlocalizedMessage;
        }

        public static CraftingStatus working() {
            return SUCCESS;
        }

        public static CraftingStatus working(String unlocMessage) {
            return new CraftingStatus(Type.CRAFTING, unlocMessage);
        }

        public static CraftingStatus failure(String unlocMessage) {
            return new CraftingStatus(Type.NO_RECIPE, unlocMessage);
        }

        private static CraftingStatus deserialize(NBTTagCompound tag) {
            Type type = Type.values()[tag.getInteger("type")];
            String unlocMessage = tag.getString("message");
            return new CraftingStatus(type, unlocMessage);
        }

        public Type getStatus() {
            return status;
        }

        public String getUnlocMessage() {
            return !unlocalizedMessage.isEmpty() ? unlocalizedMessage : this.status.getUnlocalizedDescription();
        }

        public void overrideStatusMessage(String unlocalizedMessage) {
            this.unlocalizedMessage = unlocalizedMessage;
        }

        public boolean isCrafting() {
            return this.status == Type.CRAFTING;
        }

        private NBTTagCompound serialize() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("type", this.status.ordinal());
            tag.setString("message", this.unlocalizedMessage);
            return tag;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof CraftingStatus)) return false;
            CraftingStatus another = (CraftingStatus) obj;
            if (status != another.status) return false;
            return unlocalizedMessage.equals(another.unlocalizedMessage);
        }
    }
}
