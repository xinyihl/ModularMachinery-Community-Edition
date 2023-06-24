package hellfirepvp.modularmachinery.common.tiles.base;

import crafttweaker.api.data.IData;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.api.world.IBlockPos;
import crafttweaker.api.world.IWorld;
import github.kasuminova.mmce.common.concurrent.ActionExecutor;
import github.kasuminova.mmce.common.event.Phase;
import github.kasuminova.mmce.common.event.machine.MachineStructureFormedEvent;
import github.kasuminova.mmce.common.event.machine.MachineTickEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeCheckEvent;
import github.kasuminova.mmce.common.helper.IMachineController;
import github.kasuminova.mmce.common.upgrade.MachineUpgrade;
import github.kasuminova.mmce.common.upgrade.UpgradeType;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.block.BlockStatedMachineComponent;
import hellfirepvp.modularmachinery.common.block.prop.WorkingState;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentSelectorTag;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftingStatus;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.data.Config;
import hellfirepvp.modularmachinery.common.item.ItemBlueprint;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import hellfirepvp.modularmachinery.common.modifier.MultiBlockModifierReplacement;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.modifier.SingleBlockModifierReplacement;
import hellfirepvp.modularmachinery.common.tiles.TileParallelController;
import hellfirepvp.modularmachinery.common.tiles.TileSmartInterface;
import hellfirepvp.modularmachinery.common.tiles.TileUpgradeBus;
import hellfirepvp.modularmachinery.common.util.*;
import net.minecraft.block.Block;
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
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import stanhebben.zenscript.annotations.ZenMethod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public abstract class TileMultiblockMachineController extends TileEntityRestrictedTick implements SelectiveUpdateTileEntity, IMachineController {
    public static final int BLUEPRINT_SLOT = 0, ACCELERATOR_SLOT = 1;
    public static int structureCheckDelay = 30, maxStructureCheckDelay = 100;
    public static boolean delayedStructureCheck = true;
    public static boolean cleanCustomDataOnStructureCheckFailed = false;
    public static int performanceCache = 0;
    protected final Map<String, List<RecipeModifier>> foundModifiers = new ConcurrentHashMap<>();
    protected final Map<String, RecipeModifier> customModifiers = new ConcurrentHashMap<>();
    protected final Map<TileSmartInterface.SmartInterfaceProvider, String> foundSmartInterfaces = new ConcurrentHashMap<>();
    protected final Map<String, MachineUpgrade> foundUpgrades = new ConcurrentHashMap<>();
    protected final List<TileUpgradeBus.UpgradeBusProvider> foundUpgradeBuses = new ArrayList<>();
    protected final List<TileParallelController.ParallelControllerProvider> foundParallelControllers = new ArrayList<>();
    protected final List<Tuple<MachineComponent<?>, ComponentSelectorTag>> foundComponents = new CopyOnWriteArrayList<>();
    protected EnumFacing controllerRotation = null;
    protected DynamicMachine.ModifierReplacementMap foundReplacements = null;
    protected IOInventory inventory;
    protected NBTTagCompound customData = new NBTTagCompound();
    protected DynamicMachine foundMachine = null;
    protected DynamicMachine parentMachine = null;
    protected TaggedPositionBlockArray foundPattern = null;
    protected ActionExecutor tickExecutor = null;
    protected LinkedList<Integer> usedTimeList = new LinkedList<>();
    protected int usedTimeCache = 0;
    protected int structureCheckCounter = 0;
    protected int recipeResearchRetryCounter = 0;
    protected int lastStrongPower = 0;

    public TileMultiblockMachineController() {
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
                true, "When enabled, the structure check interval in the idle state is incrementally increased to ease the performance footprint.");
        //最长结构检查间隔
        maxStructureCheckDelay = config.getInt("max-structure-check-delay", "general",
                100, 2, 1200,
                "When delayed-structure-check is enabled, what is the maximum check interval? (TimeUnit: Tick)");

        //检查最短结构检查间隔是否大于最长结构检查间隔
        if (structureCheckDelay >= maxStructureCheckDelay) {
            ModularMachinery.log.warn("structure-check-delay is bigger than or equal max-structure-check-delay!, use default value...");
            structureCheckDelay = 30;
            maxStructureCheckDelay = 100;
        }

        //当结构检查失败时，是否清空自定义数据
        cleanCustomDataOnStructureCheckFailed = config.getBoolean("clean-custom-data-on-structure-check-failed", "general",
                false, "When enabled, the customData will be cleared when multiblock structure check failed.");
    }

    @Override
    public final void doRestrictedTick() {
        if (getWorld().isRemote) {
            return;
        }
        updateUsedTime();

        final long tickStart = System.nanoTime();

        // Controller Tick
        doControllerTick();

        incrementUsedTime((int) TimeUnit.MICROSECONDS.convert(System.nanoTime() - tickStart, TimeUnit.NANOSECONDS));
    }

    public abstract void doControllerTick();

    protected IOInventory buildInventory() {
        return new IOInventory(this, new int[0], new int[0]).setMiscSlots(BLUEPRINT_SLOT, ACCELERATOR_SLOT);
    }

    protected int getStrongPower() {
        if (ticksExisted % 20 == 0) {
            lastStrongPower = getWorld().getStrongPower(getPos());
        }
        return lastStrongPower;
    }

    protected void updateUsedTime() {
        addUsedTime(tickExecutor == null ? 0 : tickExecutor.usedTime);
    }

    protected void incrementUsedTime(int add) {
        usedTimeCache += add;
        Integer first = usedTimeList.getFirst();
        if (first != null) {
            usedTimeList.set(0, first + add);
        } else {
            usedTimeList.addFirst(add);
        }
    }

    protected void addUsedTime(int time) {
        usedTimeCache += time;
        usedTimeList.addFirst(time);
        if (usedTimeList.size() > 100) {
            usedTimeCache -= usedTimeList.pollLast();
        }
    }

    public int usedTimeAvg() {
        return usedTimeCache / usedTimeList.size();
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

    public abstract CraftingStatus getControllerStatus();

    public abstract void setControllerStatus(CraftingStatus status);

    public IOInventory getInventory() {
        return inventory;
    }

    public EnumFacing getControllerRotation() {
        return controllerRotation;
    }

    protected boolean canCheckStructure() {
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

    public int currentRecipeSearchDelay() {
        return Math.min(20 + this.recipeResearchRetryCounter * 5, 100);
    }

    public boolean isStructureFormed() {
        return this.foundMachine != null && this.foundPattern != null;
    }

    protected void resetMachine(boolean clearData) {
        if (clearData) {
            setControllerStatus(CraftingStatus.MISSING_STRUCTURE);
            incrementStructureCheckCounter();
            resetRecipeSearchRetryCount();

            if (cleanCustomDataOnStructureCheckFailed) {
                customData = new NBTTagCompound();
                customModifiers.clear();
            }
        }
        updateStatedMachineComponentSync(false);

        foundMachine = null;
        foundPattern = null;
        foundReplacements = null;
    }

    public void resetRecipeSearchRetryCount() {
        this.recipeResearchRetryCounter = 0;
    }

    public int incrementStructureCheckCounter() {
        structureCheckCounter++;
        return structureCheckCounter;
    }

    protected boolean matchesRotation(TaggedPositionBlockArray pattern, DynamicMachine machine, EnumFacing ctrlRotation) {
        if (pattern == null) {
            return false;
        }
        if (!getWorld().isAreaLoaded(pattern.getPatternBoundingBox(getPos()))) {
            return false;
        }

        DynamicMachine.ModifierReplacementMap replacements = machine.getModifiersAsMatchingReplacements();

        EnumFacing rotation = EnumFacing.NORTH;
        while (rotation != ctrlRotation) {
            rotation = rotation.rotateYCCW();
            replacements = replacements.rotateYCCW();
        }

        if (pattern.matches(getWorld(), getPos(), false, replacements)) {
            this.foundPattern = pattern;
            this.foundMachine = machine;
            this.foundReplacements = replacements;
            return true;
        }
        resetMachine(false);
        return false;
    }

    protected void distributeCasingColor() {
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

    public void resetStructureCheckCounter() {
        this.structureCheckCounter = 0;
    }

    protected void checkRotation() {
    }

    protected boolean doStructureCheck() {
        if (!canCheckStructure()) {
            return true;
        }
        checkRotation();
        // 检查多方块结构中的某个方块的区块是否被卸载，以免一些重要的配方运行失败。
        // 可能会提高一些性能开销，但是玩家体验更加重要。
        // Check if a block of a chunk in a multiblock structure is unloaded, so that some important recipes do not fail to run.
        // It may raise some performance overhead, but the player experience is more important.
        if (!checkStructure()) {
            if (getControllerStatus() != CraftingStatus.CHUNK_UNLOADED) {
                setControllerStatus(CraftingStatus.CHUNK_UNLOADED);
                markForUpdateSync();
            }
            return false;
        }
        if (!isStructureFormed()) {
            if (getControllerStatus() != CraftingStatus.MISSING_STRUCTURE) {
                setControllerStatus(CraftingStatus.MISSING_STRUCTURE);
                markForUpdateSync();
            }
            return false;
        }
        updateComponents();
        return true;
    }

    protected void updateStatedMachineComponentSync(final boolean working) {
        if (foundPattern == null) {
            return;
        }

        ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> updateStatedMachineComponent(working));
    }

    protected void updateStatedMachineComponent(final boolean working) {
        if (foundPattern == null) {
            return;
        }
        foundPattern.getPattern().forEach((pos, blockInfo) -> {
            final long start = System.nanoTime() / 1000;

            if (!blockInfo.hasStatedMachineComponent()) {
                return;
            }

            BlockPos realPos = getPos().add(pos);
            IBlockState blockState = getWorld().getBlockState(realPos);
            Block block = blockState.getBlock();
            if (!(block instanceof BlockStatedMachineComponent)) {
                return;
            }

            getWorld().setBlockState(realPos, blockState.withProperty(
                    BlockStatedMachineComponent.WORKING_STATE,
                    working ? WorkingState.WORKING : WorkingState.IDLE));

            addUsedTime((int) (System.nanoTime() / 1000 - start));
        });
    }

    public RecipeCraftingContext createContext(ActiveMachineRecipe activeRecipe) {
        RecipeCraftingContext context = this.foundMachine.createContext(activeRecipe, this, this.foundComponents, MiscUtils.flatten(this.foundModifiers.values()));
        context.addModifier(customModifiers.values());
        return context;
    }

    protected void onStructureFormed() {
        new MachineStructureFormedEvent(this).postEvent();

        if (this.foundMachine.getMachineColor() != Config.machineColor) {
            ModularMachinery.EXECUTE_MANAGER.addSyncTask(this::distributeCasingColor);
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
    protected boolean checkStructure() {
        if (!canCheckStructure()) {
            return true;
        }

        if (isStructureFormed()) {
            BlockPos ctrlPos = getPos();
            //Is chunk area loaded? Prevention of unanticipated consumption of something.
            if (!getWorld().isAreaLoaded(foundPattern.getPatternBoundingBox(ctrlPos))) {
                return false;
            }
            if (this.foundMachine.isRequiresBlueprint() && !this.foundMachine.equals(getBlueprintMachine())) {
                resetMachine(true);
            } else if (!foundPattern.matches(getWorld(), ctrlPos, true, this.foundReplacements)) {
                resetMachine(true);
            }
        }

        if (this.foundMachine != null && this.foundPattern != null && this.controllerRotation != null && this.foundReplacements != null) {
            return true;
        }
        resetMachine(false);

        // First, check blueprint machine.
        DynamicMachine blueprint = getBlueprintMachine();
        if (blueprint != null) {
            if (matchesRotation(
                    BlockArrayCache.getBlockArrayCache(blueprint.getPattern(), controllerRotation),
                    blueprint, controllerRotation))
            {
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
            if (matchesRotation(
                    BlockArrayCache.getBlockArrayCache(parentMachine.getPattern(), controllerRotation),
                    parentMachine, controllerRotation))
            {
                onStructureFormed();
                return true;
            }
            // This controller is dedicated to parentMachine, it cannot become other, end check.
            return true;
        }

        // Finally, check all registered machinery.
        checkAllPatterns();
        return true;
    }

    protected void checkAllPatterns() {
        BlockPos ctrlPos = getPos();
        for (DynamicMachine machine : MachineRegistry.getRegistry()) {
            if (machine.isRequiresBlueprint()) continue;
            TaggedPositionBlockArray pattern = BlockArrayCache.getBlockArrayCache(
                    machine.getPattern(), controllerRotation);

            if (!getWorld().isAreaLoaded(pattern.getPatternBoundingBox(ctrlPos))) {
                continue;
            }

            if (matchesRotation(pattern, machine, controllerRotation)) {
                onStructureFormed();
                break;
            }
        }
    }

    protected void updateComponents() {
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

        this.foundUpgrades.clear();
        this.foundComponents.clear();
        this.foundSmartInterfaces.clear();
        this.foundParallelControllers.clear();
        ArrayList<Tuple<MachineComponent<?>, ComponentSelectorTag>> foundComponents = new ArrayList<>();
        this.foundPattern.getTileBlocksArray().forEach((pos, info) -> {
            BlockPos realPos = getPos().add(pos);
            if (!getWorld().isBlockLoaded(realPos)) {
                return;
            }
            TileEntity te = getWorld().getTileEntity(realPos);
            if (!(te instanceof MachineComponentTile)) {
                return;
            }

            ComponentSelectorTag tag = this.foundPattern.getTag(pos);
            MachineComponent<?> component = ((MachineComponentTile) te).provideComponent();
            if (component == null) {
                return;
            }

            foundComponents.add(new Tuple<>(component, tag));
            if (component instanceof TileParallelController.ParallelControllerProvider) {
                this.foundParallelControllers.add((TileParallelController.ParallelControllerProvider) component);
                return;
            }
            checkAndAddUpgrades(component);
            checkAndAddSmartInterface(component, realPos);
        });
        this.foundComponents.addAll(foundComponents);

        foundModifiers.clear();
        updateModifiers();
        updateMultiBlockModifiers();
    }

    private void checkAndAddUpgrades(final MachineComponent<?> component) {
        if (!(component instanceof TileUpgradeBus.UpgradeBusProvider)) {
            return;
        }
        TileUpgradeBus.UpgradeBusProvider upgradeBus = (TileUpgradeBus.UpgradeBusProvider) component;
        upgradeBus.boundMachine(this);
        foundUpgradeBuses.add(upgradeBus);

        Map<UpgradeType, MachineUpgrade> upgrades = upgradeBus.getUpgrades(this);
        upgrades.forEach((type, newUpgrade) -> {
            MachineUpgrade upgrade = foundUpgrades.get(type.getName());
            if (upgrade != null) {
                upgrade.incrementStackSize(newUpgrade.getStackSize());
            } else {
                foundUpgrades.put(type.getName(), newUpgrade);
            }
        });
    }

    protected void updateMultiBlockModifiers() {
        for (MultiBlockModifierReplacement mod : foundMachine.getMultiBlockModifiers()) {
            if (!mod.matches(this)) {
                return;
            }
            this.foundModifiers.put(mod.getModifierName(), mod.getModifiers());
        }
    }

    protected void updateModifiers() {
        int rotations = 0;
        EnumFacing rot = EnumFacing.NORTH;
        while (rot != this.controllerRotation) {
            rot = rot.rotateYCCW();
            rotations++;
        }

        for (Map.Entry<BlockPos, List<SingleBlockModifierReplacement>> offsetModifiers : foundMachine.getModifiers().entrySet()) {
            BlockPos at = offsetModifiers.getKey();
            for (int i = 0; i < rotations; i++) {
                at = new BlockPos(at.getZ(), at.getY(), -at.getX());
            }
            BlockPos realAt = this.getPos().add(at);
            for (SingleBlockModifierReplacement mod : offsetModifiers.getValue()) {
                BlockArray.BlockInformation info = mod.getBlockInformation();
                for (int i = 0; i < rotations; i++) {
                    info = info.copyRotateYCCW();
                }
                if (info.matches(getWorld(), realAt, true)) {
                    foundModifiers.put(mod.getModifierName(), mod.getModifiers());
                }
            }
        }
    }

    protected void checkAndAddSmartInterface(MachineComponent<?> component, BlockPos realPos) {
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

    public IWorld getIWorld() {
        return CraftTweakerMC.getIWorld(getWorld());
    }

    public crafttweaker.api.block.IBlockState getIBlockState() {
        return CraftTweakerMC.getBlockState(getWorld().getBlockState(getPos()));
    }

    public IBlockPos getIPos() {
        return CraftTweakerMC.getIBlockPos(getPos());
    }

    public String getFormedMachineName() {
        return isStructureFormed() ? foundMachine.getRegistryName().toString() : null;
    }

    public IData getCustomData() {
        return CraftTweakerMC.getIDataModifyable(customData);
    }

    public NBTTagCompound getCustomDataTag() {
        return customData;
    }

    public void setCustomData(IData data) {
        customData = CraftTweakerMC.getNBTCompound(data);
    }

    public boolean hasModifier(String key) {
        return customModifiers.containsKey(key);
    }

    /**
     * <p>机器开始检查配方能否工作。</p>
     *
     * @param context RecipeCraftingContext
     * @return CraftingCheckResult
     */
    public RecipeCraftingContext.CraftingCheckResult onCheck(RecipeCraftingContext context) {
        RecipeCraftingContext.CraftingCheckResult result = context.canStartCrafting();
        RecipeCheckEvent event = new RecipeCheckEvent(this, context);
        event.postEvent();

        if (event.isFailure()) {
            result.overrideError(event.getFailureReason());
            return result;
        }

        return result;
    }

    /**
     * <p>机器开始执行逻辑。</p>
     */
    public void onMachineTick(Phase phase) {
        new MachineTickEvent(this, phase).postEvent();
    }

    @Override
    public void addPermanentModifier(String key, RecipeModifier newModifier) {
        if (newModifier != null) {
            customModifiers.put(key, newModifier);
            flushContextModifier();
        }
    }

    @Override
    public void removePermanentModifier(String key) {
        if (hasModifier(key)) {
            customModifiers.remove(key);
            flushContextModifier();
        }
    }

    public abstract void flushContextModifier();

    public Map<TileSmartInterface.SmartInterfaceProvider, String> getFoundSmartInterfaces() {
        return foundSmartInterfaces;
    }

    public Map<String, MachineUpgrade> getFoundUpgrades() {
        return foundUpgrades;
    }

    public List<TileUpgradeBus.UpgradeBusProvider> getFoundUpgradeBuses() {
        return foundUpgradeBuses;
    }

    public List<TileParallelController.ParallelControllerProvider> getFoundParallelControllers() {
        return foundParallelControllers;
    }

    public List<Tuple<MachineComponent<?>, ComponentSelectorTag>> getFoundComponents() {
        return foundComponents;
    }

    public DynamicMachine.ModifierReplacementMap getFoundReplacements() {
        return foundReplacements;
    }

    public Map<String, RecipeModifier> getCustomModifiers() {
        return customModifiers;
    }

    public Map<String, List<RecipeModifier>> getFoundModifiers() {
        return foundModifiers;
    }

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

    public String[] getFoundModifierReplacements() {
        return foundModifiers.keySet().toArray(new String[0]);
    }

    public boolean hasModifierReplacement(String modifierName) {
        return foundModifiers.containsKey(modifierName);
    }

    @Override
    public boolean hasMachineUpgrade(final String upgradeName) {
        MachineUpgrade upgrade = foundUpgrades.get(upgradeName);
        return upgrade != null && upgrade.getParentBus() != null;
    }

    @Nullable
    @Override
    public MachineUpgrade getMachineUpgrade(final String upgradeName) {
        MachineUpgrade upgrade = foundUpgrades.get(upgradeName);
        if (upgrade == null) {
            return null;
        }

        TileUpgradeBus parentBus = upgrade.getParentBus();
        if (parentBus == null) {
            return null;
        }

        upgrade.readNBT(parentBus.provideComponent().getUpgradeCustomData(upgrade));
        return upgrade;
    }

    public TileMultiblockMachineController getController() {
        return this;
    }

    public void incrementRecipeSearchRetryCount() {
        recipeResearchRetryCounter++;
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        this.inventory = IOInventory.deserialize(this, compound.getCompoundTag("items"));
        this.inventory.setStackLimit(1, BLUEPRINT_SLOT);

        readMachineNBT(compound);
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);

        compound.setTag("items", this.inventory.writeNBT());

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
    }

    protected void readMachineNBT(NBTTagCompound compound) {
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

        TaggedPositionBlockArray pattern = BlockArrayCache.getBlockArrayCache(machine.getPattern(), this.controllerRotation);
        if (pattern == null) {
            ModularMachinery.log.info(rl + " has a empty pattern cache! Please report this to the mod author.");
            resetMachine(false);
            return;
        }
        this.foundPattern = pattern;

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
        this.customModifiers.clear();
        if (compound.hasKey("customModifier")) {
            NBTTagList tagList = compound.getTagList("customModifier", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < tagList.tagCount(); i++) {
                NBTTagCompound modifierTag = tagList.getCompoundTagAt(i);
                this.customModifiers.put(modifierTag.getString("key"), RecipeModifier.deserialize(modifierTag.getCompoundTag("modifier")));
            }
        }
    }

    /**
     * <p>一定程度上缓解过大的带宽占用问题。</p>
     * <p>如果你想知道更多原因，请查看<a href="https://github.com/HellFirePvP/ModularMachinery/issues/228">此链接</a>。</p>
     * <p>Alleviate excessive bandwidth usage to a certain extent</p>
     * <p>If you want to know more about why, please <a href="https://github.com/HellFirePvP/ModularMachinery/issues/228">See This Link</a>.</p>
     */
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return null;
    }

    @Override
    public SPacketUpdateTileEntity getTrueUpdatePacket() {
        return super.getUpdatePacket();
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

    @Override
    public boolean shouldRefresh(@Nonnull World world, @Nonnull BlockPos pos, IBlockState oldState, IBlockState newSate) {
        return oldState.getBlock() != newSate.getBlock();
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @ZenMethod
    public int getTicksExisted() {
        return ticksExisted;
    }

    public enum Type {
        MISSING_STRUCTURE,
        CHUNK_UNLOADED,
        NO_RECIPE,
        IDLE,
        CRAFTING;

        public String getUnlocalizedDescription() {
            return "gui.controller.status." + this.name().toLowerCase();
        }

    }
}
