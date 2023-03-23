package hellfirepvp.modularmachinery.common.integration.crafttweaker;

import com.google.gson.JsonParseException;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.block.IBlockState;
import crafttweaker.api.data.IData;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.util.IEventHandler;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentSelectorTag;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.client.ControllerGUIRenderEvent;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.machine.MachineStructureFormedEvent;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.machine.MachineTickEvent;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.helper.AdvancedBlockChecker;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.RecipeFailureActions;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import hellfirepvp.modularmachinery.common.modifier.ModifierReplacement;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.IBlockStateDescriptor;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceType;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import javax.annotation.Nullable;
import java.util.*;

@ZenRegister
@ZenClass("mods.modularmachinery.MachineBuilder")
public class MachineBuilder {
    public static final List<DynamicMachine> WAIT_FOR_LOAD = new ArrayList<>();
    public static final Map<ResourceLocation, MachineBuilder> PRE_LOAD_MACHINES = new HashMap<>();
    private final DynamicMachine machine;
    private final TaggedPositionBlockArray pattern;
    private BlockPos lastPos = null;
    private BlockArray.BlockInformation lastInformation = null;

    private MachineBuilder(String registryName, String localizedName) {
        this.machine = new DynamicMachine(registryName);
        this.pattern = this.machine.getPattern();

        this.machine.setLocalizedName(localizedName);
    }

    private MachineBuilder(String registryName, String localizedName, boolean requiresBlueprint, RecipeFailureActions failureAction, int color) {
        this.machine = new DynamicMachine(registryName);
        this.pattern = this.machine.getPattern();

        this.machine.setLocalizedName(localizedName);
        this.machine.setFailureAction(failureAction);
        this.machine.setDefinedColor(color);
        this.machine.setRequiresBlueprint(requiresBlueprint);
    }

    /**
     * 注册一个新的机械构建器。
     * 此方法应在 preInit 阶段调用！
     *
     * @param registryName  注册名
     * @param localizedName 译名
     */
    @ZenMethod
    public static void registerMachine(String registryName, String localizedName) {
        if (PRE_LOAD_MACHINES.containsKey(new ResourceLocation(ModularMachinery.MODID, registryName))) {
            CraftTweakerAPI.logError("[ModularMachinery] " + registryName + " is already exists!");
            return;
        }
        MachineBuilder builder = new MachineBuilder(registryName, localizedName);
        PRE_LOAD_MACHINES.put(builder.machine.getRegistryName(), builder);
    }

    /**
     * 注册一个新的机械构建器。
     * 此方法应在 preInit 阶段调用！
     *
     * @param registryName      注册名
     * @param localizedName     译名
     * @param requiresBlueprint 是否需要蓝图
     * @param failureAction     失败操作
     * @param color             颜色
     */
    @ZenMethod
    public static void registerMachine(String registryName, String localizedName, boolean requiresBlueprint, RecipeFailureActions failureAction, int color) {
        if (PRE_LOAD_MACHINES.containsKey(new ResourceLocation(ModularMachinery.MODID, registryName))) {
            CraftTweakerAPI.logError("[ModularMachinery] " + registryName + " is already exists!");
            return;
        }
        MachineBuilder builder = new MachineBuilder(registryName, localizedName, requiresBlueprint, failureAction, color);
        PRE_LOAD_MACHINES.put(builder.machine.getRegistryName(), builder);
    }

    /**
     * 获取在 preInit 注册的机械构建器。
     *
     * @param registryName 注册名
     * @return 机械构建器，若先前未注册则返回 null。
     */
    @ZenMethod
    public static MachineBuilder getBuilder(String registryName) {
        return PRE_LOAD_MACHINES.get(new ResourceLocation(ModularMachinery.MODID, registryName));
    }

    /**
     * 设置此机械是否受并行控制器影响。
     */
    @ZenMethod
    public MachineBuilder setParallelizable(boolean isParallelizable) {
        machine.setParallelizable(isParallelizable);
        return this;
    }

    /**
     * 设置此机械的最大并行数。
     * @param maxParallelism 并行数
     */
    @ZenMethod
    public MachineBuilder setMaxParallelism(int maxParallelism) {
        machine.setMaxParallelism(maxParallelism);
        return this;
    }

    /**
     * 添加配方修改器。
     *
     * @param x            X
     * @param y            Y
     * @param z            Z
     * @param ctBlockState BlockState，写法请参照 <a href="https://docs.blamejared.com/1.12/en/Vanilla/Blocks/IBlockState">CT Wiki 页面</a>
     * @param description  描述
     * @param modifiers    修改器列表
     */
    @ZenMethod
    public MachineBuilder addModifier(int x, int y, int z, IBlockState ctBlockState, String description, RecipeModifier... modifiers) {
        List<IBlockStateDescriptor> stateDescriptorList = new ArrayList<>();
        stateDescriptorList.add(new IBlockStateDescriptor(CraftTweakerMC.getBlockState(ctBlockState)));
        addModifier(new BlockPos(x, y, z), new BlockArray.BlockInformation(stateDescriptorList), description, modifiers);

        return this;
    }

    /**
     * 添加配方修改器，判断方块为传入的物品所属的方块。
     *
     * @param x           X
     * @param y           Y
     * @param z           Z
     * @param ctItemStack 物品
     * @param description 描述
     * @param modifiers   修改器列表
     */
    @ZenMethod
    public MachineBuilder addModifier(int x, int y, int z, IItemStack ctItemStack, String description, RecipeModifier... modifiers) {
        ItemStack item = CraftTweakerMC.getItemStack(ctItemStack);
        Block block = Block.getBlockFromItem(item.getItem());
        if (block != Blocks.AIR) {
            List<IBlockStateDescriptor> stateDescriptorList = new ArrayList<>();
            stateDescriptorList.add(new IBlockStateDescriptor(block));
            addModifier(new BlockPos(x, y, z), new BlockArray.BlockInformation(stateDescriptorList), description, modifiers);
        } else {
            CraftTweakerAPI.logError("[ModularMachinery] " + item.getDisplayName() + " cannot convert to Block!");
        }

        return this;
    }

    /**
     * 添加配方修改器，判断方块为传入的方块名称。方块名称写法等同于 JSON 格式的方块名。
     *
     * @param x           X
     * @param y           Y
     * @param z           Z
     * @param blockName   名称，例如：modularmachinery:blockinputbus@1
     * @param description 描述
     * @param modifiers   修改器列表
     */
    @ZenMethod
    public MachineBuilder addModifier(int x, int y, int z, String blockName, String description, RecipeModifier... modifiers) {
        List<IBlockStateDescriptor> stateDescriptorList = new ArrayList<>();

        try {
            stateDescriptorList.add(BlockArray.BlockInformation.getDescriptor(blockName));
            addModifier(new BlockPos(x, y, z), new BlockArray.BlockInformation(stateDescriptorList), description, modifiers);
        } catch (JsonParseException e) {
            CraftTweakerAPI.logError("[ModularMachinery] " + blockName + " is invalid block!", e);
        }

        return this;
    }

    /**
     * 添加智能数据接口类型。
     *
     * @param type 类型
     */
    @ZenMethod
    public MachineBuilder addSmartInterfaceType(SmartInterfaceType type) {
        if (!machine.hasSmartInterfaceType(type.getType())) {
            machine.addSmartInterfaceType(type);
        } else {
            CraftTweakerAPI.logWarning("[ModularMachinery] DynamicMachine `" + machine.getRegistryName() + "` is already has SmartInterfaceType `" + type.getType() + "`!");
        }
        return this;
    }

    /**
     * 添加结构形成事件监听器。
     */
    @ZenMethod
    public MachineBuilder addStructureFormedHandler(IEventHandler<MachineStructureFormedEvent> function) {
        machine.addMachineEventHandler(MachineStructureFormedEvent.class, function);
        return this;
    }

    /**
     * 添加机器事件监听器。
     */
    @ZenMethod
    public MachineBuilder addTickHandler(IEventHandler<MachineTickEvent> function) {
        machine.addMachineEventHandler(MachineTickEvent.class, function);
        return this;
    }

    /**
     * 添加控制器 GUI 渲染事件监听器。
     */
    @ZenMethod
    public MachineBuilder addGUIRenderHandler(IEventHandler<ControllerGUIRenderEvent> function) {
        machine.addMachineEventHandler(ControllerGUIRenderEvent.class, function);
        return this;
    }

    /**
     * 添加方块至结构。
     *
     * @param x             X
     * @param y             Y
     * @param z             Z
     * @param ctBlockStates BlockState，写法请参照 <a href="https://docs.blamejared.com/1.12/en/Vanilla/Blocks/IBlockState">CT Wiki 页面</a>
     */
    @ZenMethod
    public MachineBuilder addBlock(int x, int y, int z, IBlockState... ctBlockStates) {
        List<net.minecraft.block.state.IBlockState> stateList = new ArrayList<>();

        for (IBlockState ctBlockState : ctBlockStates) {
            stateList.add(CraftTweakerMC.getBlockState(ctBlockState));
        }
        List<IBlockStateDescriptor> stateDescriptorList = new ArrayList<>();
        for (net.minecraft.block.state.IBlockState blockState : stateList) {
            stateDescriptorList.add(new IBlockStateDescriptor(blockState));
        }

        addBlock(new BlockPos(x, y, z), new BlockArray.BlockInformation(stateDescriptorList));
        return this;
    }

    @ZenMethod
    public MachineBuilder addBlock(int[] xList, int[] yList, int[] zList,
                                   @Nullable IData nbt, @Nullable IData previewNBT, @Nullable AdvancedBlockChecker checker,
                                   IBlockState... ctBlockStates) {
        for (int x : xList) {
            for (int y : yList) {
                for (int z : zList) {
                    addBlock(x, y, z, ctBlockStates);
                    if (nbt != null) setNBT(nbt);
                    if (previewNBT != null) setPreviewNBT(previewNBT);
                    if (checker != null) setBlockChecker(checker);
                }
            }
        }
        return this;
    }

    @ZenMethod
    public MachineBuilder addBlock(int[] xList, int[] yList, int[] zList,
                                   IData nbt, IData previewNBT,
                                   IBlockState... ctBlockStates) {
        addBlock(xList, yList, zList, nbt, previewNBT, null, ctBlockStates);
        return this;
    }

    @ZenMethod
    public MachineBuilder addBlock(int[] xList, int[] yList, int[] zList,
                                   AdvancedBlockChecker checker,
                                   IBlockState... ctBlockStates) {
        addBlock(xList, yList, zList, null, null, checker, ctBlockStates);
        return this;
    }

    @ZenMethod
    public MachineBuilder addBlock(int[] xList, int[] yList, int[] zList, IBlockState... ctBlockStates) {
        return addBlock(xList, yList, zList, null, null, null, ctBlockStates);
    }

    /**
     * 添加一个物品所属的方块至结构。
     *
     * @param x            X
     * @param y            Y
     * @param z            Z
     * @param ctItemStacks 物品
     */
    @ZenMethod
    public MachineBuilder addBlock(int x, int y, int z, IItemStack... ctItemStacks) {
        List<ItemStack> stackList = new ArrayList<>();
        for (IItemStack ctItemStack : ctItemStacks) {
            stackList.add(CraftTweakerMC.getItemStack(ctItemStack));
        }
        List<IBlockStateDescriptor> stateDescriptorList = new ArrayList<>();
        for (ItemStack stack : stackList) {
            Block block = Block.getBlockFromItem(stack.getItem());
            if (block != Blocks.AIR) {
                stateDescriptorList.add(new IBlockStateDescriptor(block));
            } else {
                CraftTweakerAPI.logError("[ModularMachinery] " + stack.getDisplayName() + " cannot convert to Block!");
            }
        }

        if (!stateDescriptorList.isEmpty()) {
            addBlock(new BlockPos(x, y, z), new BlockArray.BlockInformation(stateDescriptorList));
        }

        return this;
    }

    @ZenMethod
    public MachineBuilder addBlock(int[] xList, int[] yList, int[] zList,
                                   @Nullable IData nbt, @Nullable IData previewNBT, @Nullable AdvancedBlockChecker checker,
                                   IItemStack... ctItemStacks) {
        for (int x : xList) {
            for (int y : yList) {
                for (int z : zList) {
                    addBlock(x, y, z, ctItemStacks);
                    if (nbt != null) setNBT(nbt);
                    if (previewNBT != null) setPreviewNBT(previewNBT);
                    if (checker != null) setBlockChecker(checker);
                }
            }
        }
        return this;
    }

    @ZenMethod
    public MachineBuilder addBlock(int[] xList, int[] yList, int[] zList,
                                   IData nbt, IData previewNBT,
                                   IItemStack... ctItemStacks) {
        addBlock(xList, yList, zList, nbt, previewNBT, null, ctItemStacks);
        return this;
    }

    @ZenMethod
    public MachineBuilder addBlock(int[] xList, int[] yList, int[] zList,
                                   AdvancedBlockChecker checker,
                                   IItemStack... ctItemStacks) {
        addBlock(xList, yList, zList, null, null, checker, ctItemStacks);
        return this;
    }

    @ZenMethod
    public MachineBuilder addBlock(int[] xList, int[] yList, int[] zList, IItemStack... ctItemStacks) {
        return addBlock(xList, yList, zList, null, null, null, ctItemStacks);
    }

    /**
     * 添加方块至结构，方块名称写法等同于 JSON 格式的方块名。
     *
     * @param x          X
     * @param y          Y
     * @param z          Z
     * @param blockNames 名称，例如：modularmachinery:blockinputbus@1
     */
    @ZenMethod
    public MachineBuilder addBlock(int x, int y, int z, String... blockNames) {
        List<IBlockStateDescriptor> stateDescriptorList = new ArrayList<>();
        for (String blockName : blockNames) {
            try {
                stateDescriptorList.add(BlockArray.BlockInformation.getDescriptor(blockName));
            } catch (JsonParseException e) {
                CraftTweakerAPI.logError("[ModularMachinery] " + blockName + " is invalid block!", e);
            }
        }

        if (!stateDescriptorList.isEmpty()) {
            addBlock(new BlockPos(x, y, z), new BlockArray.BlockInformation(stateDescriptorList));
        }

        return this;
    }

    @ZenMethod
    public MachineBuilder addBlock(int[] xList, int[] yList, int[] zList,
                                   @Nullable IData nbt, @Nullable IData previewNBT, @Nullable AdvancedBlockChecker checker,
                                   String... blockNames) {
        for (int x : xList) {
            for (int y : yList) {
                for (int z : zList) {
                    addBlock(x, y, z, blockNames);
                    if (nbt != null) setNBT(nbt);
                    if (previewNBT != null) setPreviewNBT(previewNBT);
                    if (checker != null) setBlockChecker(checker);
                }
            }
        }
        return this;
    }

    @ZenMethod
    public MachineBuilder addBlock(int[] xList, int[] yList, int[] zList,
                                   IData nbt, IData previewNBT,
                                   String... blockNames) {
        addBlock(xList, yList, zList, nbt, previewNBT, null, blockNames);
        return this;
    }

    @ZenMethod
    public MachineBuilder addBlock(int[] xList, int[] yList, int[] zList,
                                   AdvancedBlockChecker checker,
                                   String... blockNames) {
        addBlock(xList, yList, zList, null, null, checker, blockNames);
        return this;
    }

    @ZenMethod
    public MachineBuilder addBlock(int[] xList, int[] yList, int[] zList, String... blockNames) {
        return addBlock(xList, yList, zList, null, null, null, blockNames);
    }

    /**
     * 设置 ComponentSelectorTag，用于配方寻找组件时的额外条件。
     * @param tag Tag 名称
     */
    @ZenMethod
    public MachineBuilder setTag(String tag) {
        if (lastInformation != null && lastPos != null) {
            pattern.setTag(lastPos, new ComponentSelectorTag(tag));
        }
        return this;
    }

    /**
     * 设置方块的 NBT 判断，在 addBlock() 之后调用。
     *
     * @param data 要判断相同的 NBT
     */
    @ZenMethod
    public MachineBuilder setNBT(IData data) {
        if (lastInformation != null) {
            NBTTagCompound tag = CraftTweakerMC.getNBTCompound(data);
            lastInformation.matchingTag = tag;
            if (lastInformation.previewTag == null) {
                lastInformation.previewTag = tag;
            }
        }
        return this;
    }

    /**
     * 设置方块在结构预览时的 NBT 信息，在 addBlock() 之后调用。
     *
     * @param data 结构预览时的 NBT
     */
    @ZenMethod
    public MachineBuilder setPreviewNBT(IData data) {
        if (lastInformation != null) {
            lastInformation.previewTag = CraftTweakerMC.getNBTCompound(data);
        }
        return this;
    }

    /**
     * 设置方块高级检查器，由魔改员自行编写逻辑（非异步）。
     *
     * @param checker 函数
     */
    @ZenMethod
    public MachineBuilder setBlockChecker(AdvancedBlockChecker checker) {
        if (lastInformation != null) {
            lastInformation.nbtChecker = checker;
        }
        return this;
    }

    /**
     * 控制器是否需要蓝图
     */
    @ZenMethod
    public MachineBuilder setRequiresBlueprint(boolean requiresBlueprint) {
        this.machine.setRequiresBlueprint(requiresBlueprint);
        return this;
    }

    /**
     * 设置当此机器配方运行失败时的操作
     *
     * @param failureAction Action 可以通过 RecipeFailureActions.getFailureAction(String key) 获得
     */
    @ZenMethod
    public MachineBuilder setFailureAction(RecipeFailureActions failureAction) {
        this.machine.setFailureAction(failureAction);
        return this;
    }

    /**
     * 设置机械颜色，该结构内的其他组件也将会变为此颜色。
     *
     * @param color 颜色，例如：0xFFFFFF
     */
    @ZenMethod
    public MachineBuilder setColor(int color) {
        this.machine.setDefinedColor(color);
        return this;
    }

    /**
     * 注册此机械。
     */
    @ZenMethod
    public void build() {
        WAIT_FOR_LOAD.add(machine);
    }

    private void addBlock(BlockPos pos, BlockArray.BlockInformation information) {
        pattern.addBlock(pos, information);
        lastInformation = information;
        lastPos = pos;
    }

    private void addModifier(BlockPos pos, BlockArray.BlockInformation information, String description, RecipeModifier... modifiers) {
        this.machine.getModifiers().putIfAbsent(pos, new ArrayList<>());
        this.machine.getModifiers().get(pos).add(new ModifierReplacement(information, Arrays.asList(modifiers), description));
        lastInformation = information;
        lastPos = pos;
    }

    public DynamicMachine getMachine() {
        return machine;
    }
}
