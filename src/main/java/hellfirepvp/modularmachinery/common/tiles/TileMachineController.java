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
import github.kasuminova.mmce.common.concurrent.TaskExecutor;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.RecipeRegistry;
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
import hellfirepvp.modularmachinery.common.machine.*;
import hellfirepvp.modularmachinery.common.modifier.ModifierReplacement;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.tiles.base.ColorableMachineTile;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.TileEntityRestrictedTick;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceType;
import hellfirepvp.modularmachinery.common.util.nbt.NBTHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
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
import java.util.concurrent.RecursiveTask;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: TileMachineController
 * Created by HellFirePvP
 * Date: 28.06.2017 / 17:14
 */
public class TileMachineController extends TileEntityRestrictedTick implements IMachineController {
    public static final int BLUEPRINT_SLOT = 0;
    public static final int ACCELERATOR_SLOT = 1;
    public static int structureCheckDelay = 30;
    public static boolean delayedStructureCheck = true;
    public static int maxStructureCheckDelay = 100;
    private final Map<BlockPos, List<ModifierReplacement>> foundModifiers = new HashMap<>();
    private final Map<String, RecipeModifier> customModifiers = new HashMap<>();
    private final Map<String, BlockPos> foundSmartInterfaces = new HashMap<>();
    private final List<Tuple<MachineComponent<?>, ComponentSelectorTag>> foundComponents = new ArrayList<>();
    private NBTTagCompound customData = new NBTTagCompound();
    private CraftingStatus craftingStatus = CraftingStatus.MISSING_STRUCTURE;
    private DynamicMachine.ModifierReplacementMap foundReplacements = null;
    private IOInventory inventory;
    private DynamicMachine foundMachine = null;
    private TaggedPositionBlockArray foundPattern = null;
    private EnumFacing patternRotation = null;
    private ActiveMachineRecipe activeRecipe = null;
    private RecipeCraftingContext context = null;
    private RecipeSearchTask searchTask = null;
    private int recipeResearchRetryCount = 0;
    private int structureCheckFailedCount = 0;

    public TileMachineController() {
        this.inventory = buildInventory();
        this.inventory.setStackLimit(1, BLUEPRINT_SLOT);
    }

    public static void loadFromConfig(Configuration config) {
        //最短结构检查间隔
        structureCheckDelay = config.getInt("structure-check-delay", "general",
                30, 1, 1200,
                "The multiblock structure checks the structural integrity at how often? (TimeUnit: Tick)");
        //延迟结构检查
        delayedStructureCheck = config.getBoolean("delayed-structure-check", "general",
                true, "When enabled, the interval between structure checks is gradually increased when multiblock structure checks fail.");
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
    }

    @Override
    public void doRestrictedTick() {
        if (getWorld().isRemote) {
            return;
        }
        if (getWorld().getStrongPower(getPos()) > 0) {
            return;
        }

        checkStructure();
        updateComponents();

        if (!isStructureFormed()) {
            if (craftingStatus != CraftingStatus.MISSING_STRUCTURE) {
                craftingStatus = CraftingStatus.MISSING_STRUCTURE;
                markForUpdate();
            }
            return;
        }

        boolean updateRequired = onMachineTick();

        if (this.activeRecipe == null) {
            searchAndStartRecipe();
            if (updateRequired) {
                markForUpdate();
            }
            return;
        }

        if (this.context == null) {
            //context preInit
            context = createContext(this.activeRecipe);
        }

        CraftingStatus statusTmp = this.craftingStatus;
        MachineRecipe machineRecipe = this.activeRecipe.getRecipe();
        if (!onPreTick()) {
            //Handle perTick IO and tick progression
            this.activeRecipe.tick(this, this.context);
            this.activeRecipe.setTick(Math.max(this.activeRecipe.getTick() - 1, 0));
        } else {
            //Handle perTick IO and tick progression
            if (statusTmp != this.craftingStatus) {
                this.activeRecipe.tick(this, this.context);
            } else {
                this.craftingStatus = this.activeRecipe.tick(this, this.context);
            }
            if (machineRecipe.doesCancelRecipeOnPerTickFailure() && !this.craftingStatus.isCrafting()) {
                this.activeRecipe = null;
            } else if (this.activeRecipe.isCompleted()) {
                onFinished();
            } else {
                onTick();
            }
        }

        markForUpdate();
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

        activeRecipe.start(context);
        List<IEventHandler<RecipeEvent>> handlerList = this.activeRecipe.getRecipe().getRecipeEventHandlers(RecipeStartEvent.class);
        if (handlerList != null && !handlerList.isEmpty()) {
            for (IEventHandler<RecipeEvent> handler : handlerList) {
                RecipeStartEvent event = new RecipeStartEvent(this);
                handler.handle(event);
            }
        }

        markForUpdate();
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
                craftingStatus = CraftingStatus.failure(event.getPreventReason());
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
        this.activeRecipe.reset();
        this.context.overrideModifier(MiscUtils.flatten(this.foundModifiers.values()));
        ModularMachinery.EXECUTE_MANAGER.addPostTickTask(() -> tryStartRecipe(activeRecipe, null));
    }

    /**
     * <p>机器尝试开始执行一个配方。</p>
     *
     * @param activeRecipe ActiveMachineRecipe
     * @param context      RecipeCraftingContext
     */
    private void tryStartRecipe(@Nonnull ActiveMachineRecipe activeRecipe, @Nullable RecipeCraftingContext context) {
        RecipeCraftingContext finalContext;
        finalContext = context == null ? createContext(activeRecipe) : context;

        RecipeCraftingContext.CraftingCheckResult tryResult = onCheck(finalContext);

        if (tryResult.isSuccess()) {
            ModularMachinery.EXECUTE_MANAGER.addMainThreadTask(() -> onStart(activeRecipe, finalContext));
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

    private RecipeCraftingContext createContext(ActiveMachineRecipe activeRecipe) {
        RecipeCraftingContext context = this.foundMachine.createContext(activeRecipe, this, this.foundComponents, MiscUtils.flatten(this.foundModifiers.values()));
        for (RecipeModifier modifier : customModifiers.values()) {
            context.addModifier(modifier);
        }
        return context;
    }

    private void searchAndStartRecipe() {
        if (searchTask != null) {
            if (!searchTask.isDone()) {
                return;
            }

            //并发检查
            if (searchTask.currentMachine == foundMachine) {
                try {
                    RecipeCraftingContext context = searchTask.get();
                    if (context != null) {
                        ModularMachinery.EXECUTE_MANAGER.addPostTickTask(() -> tryStartRecipe(context.getActiveRecipe(), context));
                    }
                } catch (Exception e) {
                    ModularMachinery.log.warn(e);
                }
            }

            searchTask = null;
        } else if (this.ticksExisted % currentRecipeSearchDelay() == 0) {
            createRecipeSearchTask();
        }
    }

    private void createRecipeSearchTask() {
        searchTask = new RecipeSearchTask(foundMachine);
        TaskExecutor.FORK_JOIN_POOL.submit(searchTask);
    }

    private void resetMachine(boolean resetAll) {
        if (resetAll) {
            activeRecipe = null;
            craftingStatus = CraftingStatus.MISSING_STRUCTURE;
            structureCheckFailedCount++;
            recipeResearchRetryCount = 0;

            customData = new NBTTagCompound();
            customModifiers.clear();
        }
        foundMachine = null;
        foundPattern = null;
        patternRotation = null;
        foundReplacements = null;
    }

    private int currentStructureCheckDelay() {
        if (!delayedStructureCheck) {
            return structureCheckDelay;
        }
        if (isStructureFormed()) {
            return Math.min(structureCheckDelay + currentRecipeSearchDelay(), maxStructureCheckDelay - structureCheckDelay);
        } else {
            return Math.min(structureCheckDelay + this.structureCheckFailedCount * 10, maxStructureCheckDelay - structureCheckDelay);
        }
    }

    private int currentRecipeSearchDelay() {
        return Math.min(20 + this.recipeResearchRetryCount * 10, 80);
    }

    public boolean isStructureFormed() {
        return this.foundMachine != null && this.foundPattern != null && this.patternRotation != null;
    }

    private void onStructureFormed() {
        List<IEventHandler<MachineEvent>> handlerList = this.foundMachine.getMachineEventHandlers(MachineStructureFormedEvent.class);
        if (handlerList != null && !handlerList.isEmpty()) {
            for (IEventHandler<MachineEvent> handler : handlerList) {
                MachineStructureFormedEvent event = new MachineStructureFormedEvent(this);
                handler.handle(event);
            }
        }
    }

    private void checkStructure() {
        if (ticksExisted % currentStructureCheckDelay() != 0) {
            return;
        }

        if (isStructureFormed()) {
            if (this.foundMachine.requiresBlueprint() && !this.foundMachine.equals(getBlueprintMachine())) {
                resetMachine(true);
            } else if (!foundPattern.matches(getWorld(), getPos(), true, this.foundReplacements)) {
                resetMachine(true);
            }
        }

        if (this.foundMachine != null && this.foundPattern != null && this.patternRotation != null && this.foundReplacements != null) {
            return;
        }

        resetMachine(false);

        DynamicMachine blueprint = getBlueprintMachine();
        if (blueprint != null) {
            if (matchesRotation(blueprint.getPattern(), blueprint)) {
                this.world.setBlockState(pos, BlocksMM.blockController.getDefaultState().withProperty(BlockController.FACING, this.patternRotation));

                if (this.foundMachine.getMachineColor() != Config.machineColor) {
                    distributeCasingColor();
                }

                onStructureFormed();
                structureCheckFailedCount = 0;
                markForUpdate();
            }
        } else {
            for (DynamicMachine machine : MachineRegistry.getRegistry()) {
                if (machine.requiresBlueprint()) continue;
                if (matchesRotation(machine.getPattern(), machine)) {
                    this.world.setBlockState(pos, BlocksMM.blockController.getDefaultState().withProperty(BlockController.FACING, this.patternRotation));

                    if (this.foundMachine.getMachineColor() != Config.machineColor) {
                        distributeCasingColor();
                    }

                    onStructureFormed();
                    structureCheckFailedCount = 0;
                    markForUpdate();
                    break;
                }
            }
        }
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
        EnumFacing face = EnumFacing.NORTH;
        DynamicMachine.ModifierReplacementMap replacements = machine.getModifiersAsMatchingReplacements();
        do {
            if (pattern.matches(getWorld(), getPos(), false, replacements)) {
                this.foundPattern = pattern;
                this.patternRotation = face;
                this.foundMachine = machine;
                this.foundReplacements = replacements;
                return true;
            }
            face = face.rotateYCCW();
            pattern = pattern.rotateYCCW();
            replacements = replacements.rotateYCCW();
        } while (face != EnumFacing.NORTH);
        resetMachine(false);
        return false;
    }

    private void updateComponents() {
        if (this.foundMachine == null || this.foundPattern == null || this.patternRotation == null || this.foundReplacements == null) {
            this.foundComponents.clear();
            this.foundModifiers.clear();
            this.foundSmartInterfaces.clear();

            resetMachine(false);
            return;
        }
        if (ticksExisted % currentStructureCheckDelay() != 0) {
            return;
        }

        this.foundComponents.clear();
        this.foundSmartInterfaces.clear();
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

            if (!(component instanceof TileSmartInterface.SmartInterfaceProvider) || foundMachine.smartInterfaceTypesIsEmpty()) {
                continue;
            }

            TileSmartInterface.SmartInterfaceProvider smartInterface = ((TileSmartInterface.SmartInterfaceProvider) component);
            SmartInterfaceData data = smartInterface.getMachineData(getPos());
            Map<String, SmartInterfaceType> notFoundInterface = foundMachine.getFilteredType(foundSmartInterfaces.keySet());

            if (notFoundInterface.isEmpty()) {
                Optional<SmartInterfaceType> firstOpt = foundMachine.getFirstSmartInterfaceType();
                if (firstOpt.isPresent()) {
                    SmartInterfaceType first = firstOpt.get();
                    smartInterface.addMachineData(getPos(), foundMachine.getRegistryName(), first.getType(), 0);
                    foundSmartInterfaces.put(first.getType(), realPos);
                }
            } else if (data != null) {
                String type = data.getType();

                if (notFoundInterface.containsKey(type)) {
                    foundSmartInterfaces.put(type, realPos);
                }
            } else {
                SmartInterfaceType type = notFoundInterface.values().stream().sorted().findFirst().get();
                smartInterface.addMachineData(getPos(), foundMachine.getRegistryName(), type.getType(), 0);
                foundSmartInterfaces.put(type.getType(), realPos);
            }
        }

        int rotations = 0;
        EnumFacing rot = EnumFacing.NORTH;
        while (rot != this.patternRotation) {
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

    @Deprecated
    public float getCurrentActiveRecipeProgress(float partial) {
        if (activeRecipe == null) return 0F;
        float tick = activeRecipe.getTick() + partial;
        float maxTick = activeRecipe.getTotalTick();
        return MathHelper.clamp(tick / maxTick, 0F, 1F);
    }

    public boolean hasActiveRecipe() {
        return this.activeRecipe != null;
    }

    @Override
    public boolean shouldRefresh(@Nonnull World world, @Nonnull BlockPos pos, IBlockState oldState, IBlockState newSate) {
        return oldState.getBlock() != newSate.getBlock();
    }

    @Nullable
    public DynamicMachine getFoundMachine() {
        return foundMachine;
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

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        this.inventory = IOInventory.deserialize(this, compound.getCompoundTag("items"));
        this.inventory.setStackLimit(1, BLUEPRINT_SLOT);

        if (compound.hasKey("status")) { //Legacy support
            this.craftingStatus = new CraftingStatus(Type.values()[compound.getInteger("status")], "");
        } else {
            this.craftingStatus = CraftingStatus.deserialize(compound.getCompoundTag("statusTag"));
        }

        if (compound.hasKey("machine") && compound.hasKey("rotation")) {
            ResourceLocation rl = new ResourceLocation(compound.getString("machine"));
            DynamicMachine machine = MachineRegistry.getRegistry().getMachine(rl);
            if (machine == null) {
                ModularMachinery.log.info("Couldn't find machine named " + rl + " for controller at " + getPos());
                resetMachine(false);
            } else {
                EnumFacing rot = EnumFacing.byHorizontalIndex(compound.getInteger("rotation"));
                EnumFacing offset = EnumFacing.NORTH;
                TaggedPositionBlockArray pattern = machine.getPattern();
                DynamicMachine.ModifierReplacementMap replacements = machine.getModifiersAsMatchingReplacements();
                while (offset != rot) {
                    replacements = replacements.rotateYCCW();
                    pattern = pattern.rotateYCCW();
                    offset = offset.rotateY();
                }
                this.patternRotation = rot;
                this.foundPattern = pattern;
                this.foundMachine = machine;
                this.foundReplacements = replacements;

                if (compound.hasKey("modifierOffsets")) {
                    NBTTagList list = compound.getTagList("modifierOffsets", Constants.NBT.TAG_COMPOUND);
                    for (int i = 0; i < list.tagCount(); i++) {
                        NBTTagCompound posTag = list.getCompoundTagAt(i);
                        BlockPos modOffset = NBTUtil.getPosFromTag(posTag.getCompoundTag("position"));
                        IBlockState state = NBTHelper.getBlockState(posTag, "state");
                        if (state != null) {
                            for (ModifierReplacement mod : this.foundMachine.getModifiers().getOrDefault(modOffset, Lists.newArrayList())) {
                                if (mod.getBlockInformation().matchesState(state)) {
                                    this.foundModifiers.putIfAbsent(modOffset, Lists.newArrayList());
                                    this.foundModifiers.get(modOffset).add(mod);
                                }
                            }
                        }
                    }
                }

                if (compound.hasKey("customData")) {
                    this.customData = compound.getCompoundTag("customData");
                }
                if (compound.hasKey("customModifier")) {
                    NBTTagList tagList = compound.getTagList("customModifier", Constants.NBT.TAG_COMPOUND);
                    for (int i = tagList.tagCount(); i > 0; i--) {
                        NBTTagCompound modifierTag = tagList.getCompoundTagAt(i);
                        this.customModifiers.put(modifierTag.getString("key"), RecipeModifier.deserialize(modifierTag.getCompoundTag("modifier")));
                    }
                }
            }
        } else {
            resetMachine(false);
        }
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

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setTag("items", this.inventory.writeNBT());
        compound.setTag("statusTag", this.craftingStatus.serialize());

        if (this.foundMachine != null && this.patternRotation != null) {
            compound.setString("machine", this.foundMachine.getRegistryName().toString());
            compound.setInteger("rotation", this.patternRotation.getHorizontalIndex());

            NBTTagList listModifierOffsets = new NBTTagList();
            for (BlockPos offset : this.foundModifiers.keySet()) {
                NBTTagCompound tag = new NBTTagCompound();

                tag.setTag("position", NBTUtil.createPosTag(offset));
                NBTHelper.setBlockState(tag, "state", world.getBlockState(getPos().add(offset)));

                listModifierOffsets.appendTag(tag);
            }
            compound.setTag("modifierOffsets", listModifierOffsets);

            if (customData != null) {
                compound.setTag("customData", customData);
            }
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
        if (this.activeRecipe != null) {
            compound.setTag("activeRecipe", this.activeRecipe.serialize());
        }
    }

    public enum Type {

        MISSING_STRUCTURE,
        NO_RECIPE,
        CRAFTING;

        public String getUnlocalizedDescription() {
            return "gui.controller.status." + this.name().toLowerCase();
        }

    }

    public static class CraftingStatus {

        private static final CraftingStatus SUCCESS = new CraftingStatus(Type.CRAFTING, "");
        private static final CraftingStatus MISSING_STRUCTURE = new CraftingStatus(Type.MISSING_STRUCTURE, "");

        private final Type status;
        private String unlocalizedMessage;

        private CraftingStatus(Type status, String unlocalizedMessage) {
            this.status = status;
            this.unlocalizedMessage = unlocalizedMessage;
        }

        public static CraftingStatus working() {
            return SUCCESS;
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
    }

    public class RecipeSearchTask extends RecursiveTask<RecipeCraftingContext> {
        private final DynamicMachine currentMachine;

        public RecipeSearchTask(DynamicMachine currentMachine) {
            this.currentMachine = currentMachine;
        }

        @Override
        protected RecipeCraftingContext compute() {
            Iterable<MachineRecipe> availableRecipes = RecipeRegistry.getRecipesFor(foundMachine);

            MachineRecipe highestValidity = null;
            RecipeCraftingContext.CraftingCheckResult highestValidityResult = null;
            float validity = 0F;

            for (MachineRecipe recipe : availableRecipes) {
                ActiveMachineRecipe activeRecipe = new ActiveMachineRecipe(recipe);
                RecipeCraftingContext context = createContext(activeRecipe);
                RecipeCraftingContext.CraftingCheckResult result = onCheck(context);
                if (result.isSuccess()) {
                    //并发检查
                    if (foundMachine == null || !foundMachine.equals(currentMachine)) return null;

                    recipeResearchRetryCount = 0;

                    return context;
                } else if (highestValidity == null ||
                           (result.getValidity() >= 0.5F && result.getValidity() > validity)) {
                    highestValidity = recipe;
                    highestValidityResult = result;
                    validity = result.getValidity();
                }
            }

            //并发检查
            if (foundMachine == null || !foundMachine.equals(currentMachine)) return null;

            if (highestValidity != null) {
                craftingStatus = CraftingStatus.failure(highestValidityResult.getFirstErrorMessage(""));
            } else {
                craftingStatus = CraftingStatus.failure(Type.NO_RECIPE.getUnlocalizedDescription());
            }
            recipeResearchRetryCount++;

            return null;
        }
    }
}
