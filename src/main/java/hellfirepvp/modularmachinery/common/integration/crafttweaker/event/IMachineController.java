package hellfirepvp.modularmachinery.common.integration.crafttweaker.event;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.block.IBlockState;
import crafttweaker.api.data.IData;
import crafttweaker.api.world.IBlockPos;
import crafttweaker.api.world.IWorld;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
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
     * 获取机器当前正在执行的配方
     *
     * @return 配方
     */
    @Nullable
    @ZenGetter("activeRecipe")
    ActiveMachineRecipe getActiveRecipe();

    /**
     * 机器是否在工作
     *
     * @return true 为工作，反之为闲置或未形成结构
     */
    @ZenGetter("isWorking")
    boolean isWorking();

    /**
     * 获取形成的机械结构名称
     *
     * @return 机械的注册名，如果未形成结构则返回 null
     */
    @Nullable
    @ZenGetter("formedMachineName")
    String getFormedMachineName();

    /**
     * 获取自定义 NBT 信息
     *
     * @return IData
     */
    @ZenGetter("customData")
    IData getCustomData();

    /**
     * 设置自定义 NBT 信息
     *
     * @param data IData
     */
    @ZenSetter("customData")
    void setCustomData(IData data);

    /**
     * 添加一个 RecipeModifier
     *
     * @param key      KEY，方便删除使用
     * @param modifier Modifier
     */
    @ZenMethod
    void addModifier(String key, RecipeModifier modifier);

    /**
     * 删除一个 RecipeModifier
     *
     * @param key KEY
     */
    @ZenMethod
    void removeModifier(String key);

    /**
     * 检查某个 RecipeModifier 是否已存在
     * @param key KEY
     * @return 存在返回 true，反之 false
     */
    @ZenMethod
    boolean hasModifier(String key);

    /**
     * 覆盖控制器的状态消息
     *
     * @param newInfo 新消息
     */
    @ZenSetter("statusMessage")
    void overrideStatusInfo(String newInfo);

    /**
     * 获取控制器绑定的指定智能数据接口数据
     * @param type 类型过滤
     * @return 智能数据接口的内部数据，如果没有则为 null
     */
    @Nullable
    @ZenMethod
    SmartInterfaceData getSmartInterfaceData(String type);

    /**
     * 获取控制器绑定的所有智能数据接口数据
     * @return 一组智能数据接口的内部数据，如果没有则为空数组
     */
    @ZenGetter("smartInterfaceDataList")
    SmartInterfaceData[] getSmartInterfaceDataList();

    TileMachineController getController();
}
