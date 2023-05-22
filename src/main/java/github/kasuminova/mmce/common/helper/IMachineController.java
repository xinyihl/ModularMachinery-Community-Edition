package github.kasuminova.mmce.common.helper;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.block.IBlockState;
import crafttweaker.api.data.IData;
import crafttweaker.api.world.IBlockPos;
import crafttweaker.api.world.IWorld;
import github.kasuminova.mmce.common.upgrade.MachineUpgrade;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.machine.RecipeThread;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;
import stanhebben.zenscript.annotations.ZenSetter;

import javax.annotation.Nullable;

@ZenRegister
@ZenClass("mods.modularmachinery.IMachineController")
public interface IMachineController {
    /**
     * 获取控制器所在的世界。
     *
     * @return 世界
     */
    @ZenGetter("world")
    IWorld getIWorld();

    /**
     * 获取控制器方块。
     *
     * @return 方块
     */
    @ZenGetter("blockState")
    IBlockState getIBlockState();

    /**
     * 获取控制器所在的坐标
     *
     * @return 坐标
     */
    @ZenGetter("pos")
    IBlockPos getIPos();

    /**
     * 获取机械在当前世界运行的时间（非世界时间，进入退出世界会被重置）
     */
    @ZenGetter("ticksExisted")
    int getTicksExisted();

    /**
     * 获取机器当前正在执行的配方。
     *
     * @deprecated 已弃用，请使用 {@link ActiveMachineRecipe[] getActiveRecipeList}
     * @return 配方
     */
    @Nullable
    @Deprecated
    @ZenGetter("activeRecipe")
    ActiveMachineRecipe getActiveRecipe();

    /**
     * 获取机器当前正在执行的配方列表。
     *
     * @return 配方
     */
    @ZenGetter("activeRecipeList")
    ActiveMachineRecipe[] getActiveRecipeList();

    /**
     * 获取机器所有的配方线程。
     * @return 线程列表，所有元素均不为空，包括闲置线程。
     */
    @ZenGetter("recipeThreadList")
    RecipeThread[] getRecipeThreadList();

    /**
     * 机器是否在工作。
     *
     * @return true 为工作，反之为闲置或未形成结构
     */
    @ZenGetter("isWorking")
    boolean isWorking();

    /**
     * 获取形成的机械结构名称。
     *
     * @return 机械的注册名，如果未形成结构则返回 null
     */
    @Nullable
    @ZenGetter("formedMachineName")
    String getFormedMachineName();

    /**
     * 获取自定义 NBT 信息。
     *
     * @return IData
     */
    @ZenGetter("customData")
    IData getCustomData();

    /**
     * 设置自定义 NBT 信息。
     *
     * @param data IData
     */
    @ZenSetter("customData")
    void setCustomData(IData data);

    /**
     * 添加一个半永久 RecipeModifier，会在配方完成的时候自动删除。
     *
     * @param key      KEY，方便删除使用
     * @param modifier Modifier
     */

    @ZenMethod
    void addModifier(String key, RecipeModifier modifier);

    /**
     * 删除一个半永久 RecipeModifier。
     *
     * @param key KEY
     */

    @ZenMethod
    void removeModifier(String key);

    /**
     * 添加一个永久性的 RecipeModifier。
     *
     * @param key      KEY，方便删除使用
     * @param modifier Modifier
     */
    @ZenMethod
    void addPermanentModifier(String key, RecipeModifier modifier);

    /**
     * 删除一个永久性的 RecipeModifier。
     *
     * @param key KEY
     */
    @ZenMethod
    void removePermanentModifier(String key);

    /**
     * 检查某个 RecipeModifier 是否已存在。
     *
     * @param key KEY
     * @return 存在返回 true，反之 false
     */
    @ZenMethod
    boolean hasModifier(String key);

    /**
     * 覆盖控制器的状态消息。
     *
     * @param newInfo 新消息
     */
    @Deprecated
    @ZenSetter("statusMessage")
    void overrideStatusInfo(String newInfo);

    /**
     * 获取控制器绑定的指定智能数据接口数据。
     *
     * @param type 类型过滤
     * @return 智能数据接口的内部数据，如果没有则为 null
     */
    @Nullable
    @ZenMethod
    SmartInterfaceData getSmartInterfaceData(String type);

    /**
     * 获取控制器绑定的所有智能数据接口数据。
     *
     * @return 一组智能数据接口的内部数据，如果没有则为空数组，但不会为 null
     */
    @ZenGetter("smartInterfaceDataList")
    SmartInterfaceData[] getSmartInterfaceDataList();

    /**
     * 获取控制器检测到的配方修改器升级名称。
     *
     * @return 返回找到的所有升级名称，如果没有则为空数组，但不会为 null
     */
    @ZenGetter("foundModifiers")
    String[] getFoundModifierReplacements();

    /**
     * 获取机械中的某个 modifierReplacement 是否存在
     */
    @ZenMethod
    boolean hasModifierReplacement(String modifierName);

    /**
     * 机械是否存在给定名称的机械升级。
     * @param upgradeName 名称
     * @return 存在返回 true，否则返回 false
     */
    @ZenMethod
    boolean hasMachineUpgrade(String upgradeName);

    /**
     * 根据给定的名称，获取控制器中的一个机械升级。
     * 可为 null。
     *
     * @param upgradeName 名称
     * @return 机械升级，如果无则返回 null
     */
    @Nullable
    @ZenMethod
    MachineUpgrade getMachineUpgrade(String upgradeName);

    TileMultiblockMachineController getController();
}
