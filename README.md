# ModularMachinery: Community Edition

Language Select

1. [简体中文](#简体中文)
2. [English](#English)

## 简体中文

社区版皆在使模块化机械变得更加现代化，并提供更多的可自定义内容。

### 增强功能：
* 提供更多对能源输入仓和能源输出仓的能量核心的搜索配置
* 更大的控制器 GUI，供魔改员提供更多的信息显示

### 扩展功能：
* 自定义配方失败操作（默认为 still，来自 [咕咕工具](https://github.com/ParaParty/gugu-utils)）
* TheOneProbe 兼容（目前支持配方进度 / 机械状态显示，支持配置）
* 新的配方类型
    * 材料组输入
    * 催化剂输入
    * 每 Tick 流体输入输出
* 基于 CraftTweaker 的高级事件系统和扩展 API
    * 5 个配方事件
    * 2 个机械事件
    * 在 JEI 配方界面添加自定义提示
    * 动态物品 NBT 判断与动态物品修改器
    * 动态生成、添加和删除的 RecipeModifier
    * 动态修改和添加控制器 GUI 状态信息
    * 内建可持久化（退出游戏保存数据）的自定义数据储存，支持读取和保存
    * 机械控制器 API
* 智能数据接口
    * 智能数据接口可以使用 CraftTweaker 来添加自定义功能
    * 它可以实现诸如编程电路、速率控制等功能

### 实验性功能：
* 高性能异步实现
    * 原版模块化机械提供了很多强大的功能，但是它的运行性能令人堪忧，尤其是有大量机械的情况下。
    * 社区版添加了可异步接口，所有实现此接口的都可以安全的以异步形式完成大量工作。
    * 根据大量的整合包测试，最高性能提升可达近 **10** 倍。
    * 唯一的主线程消耗是检查结构，但是它是**不可能**异步的，因为它在一些第三方插件服务端是**不安全**的操作。

* 重绘 JEI 配方界面图标，支持塞入更多的配方元素

开发中的功能：
* 随机物品输出
* 单方块机械实现
* 并行配方处理
* 工厂实现（类似 Mekanism 中的工厂）
* 合并附属功能
* 为 TheOneProbe 提供更多支持

## English

The Community Edition is all about modernizing the Modular Machinery and providing more customizable content.

### Enhancements:
* More searchable configurations for the DE Energy Core of the Energy Input Hatch and Energy Output Hatch
* Larger controller GUI for more information display by the modpackers

### Expanded Features:
* Custom recipe failure operation (default is still, from [gugu-utils](https://github.com/ParaParty/gugu-utils))
* TheOneProbe compatible (currently supports recipe progress / mechanical status display, supports configuration)
* New recipe types
    * Material group input
    * Catalyst input
    * Per Tick fluid input and output
* Advanced event system and extended API based on CraftTweaker
    * 5 recipe events
    * 2 mechanical events
    * Add custom hints to JEI recipe screen
    * Dynamic item NBT determination and dynamic item modifier
    * RecipeModifier for dynamic generation, addition and deletion
    * Dynamic modification and addition of controller GUI status information
    * Built-in customizable data storage with persistence (save data on exit from game), read and save support
    * Mechanical controller API
* Smart Data Interface
    * Smart Data Interface can be used to add custom functionality using CraftTweaker
    * It enables features such as programming circuits, rate control, etc.

### Experimental features:
* High-performance asynchronous implementation
    * The original Modular Machinery offers a lot of powerful features, but it runs with worrisome performance, especially with a large number of controllers.
    * The community version adds an asynchronizable interface, and all implementations of this interface can safely do a lot of work in an asynchronous form.
    * Based on extensive modpack testing, the maximum performance improvement can be over **10** times.
    * The only main thread consumed is structure check, but it is **impossible** to do asynchronously because it is **unsafe** to operate on the side of some third-party plugin services.

* Redrawn JEI recipe interface icon to support stuffing more recipe elements

### Features under development.
* Random item output
* Single cube mechanical implementation
* Parallel recipe processing
* Factory implementation (similar to the factory in Mekanism)
* Merging of dependent functions
* Additional support for TheOneProbe
