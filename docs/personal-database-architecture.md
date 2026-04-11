# 个人数据库架构说明

## 目标

这个模组把玩家的物品管理从有限背包扩展为“跟随存档与玩家数据走”的个人数据库。

当前版本的核心目标如下：

- 每个玩家只有一个数据库实例，数据跟随玩家附件持久化。
- 数据库快捷物品只是入口，不承载真实存储内容。
- 玩家死亡后数据库内容不会掉落，也不会因为快捷物品丢失而丢库。
- 数据库页面采用全屏原版风格布局，左侧是玩家原版背包区，右侧是数据库区。
- 右侧支持分类页签、排序、名称搜索、分页以及一键入库。

## 领域划分

### `database`

负责数据库领域模型与序列化：

- `PlayerDatabaseAttachment`：玩家级持久化仓储。
- `StoredStackKey`：使用物品本体和组件作为唯一键。
- `StoredStackEntry`：记录数量、分类和最近修改序号。
- `DatabaseQuery` / `DatabaseViewState` / `DatabasePage`：查询与页面投影。

### `service`

`PersonalDatabaseService` 负责数据库打开、入库、提取、分页、排序与筛选，是服务层总入口。

### `menu`

`PersonalDatabaseMenu` 负责：

- 复制原版背包布局。
- 维护 2x2 crafting、盔甲、副手和主背包槽位。
- 将数据库交互转换为服务层操作。
- 在服务端生成数据库页面快照并同步给客户端。

### `client`

`PersonalDatabaseScreen` 负责全屏界面表现：

- 左侧使用 `VanillaPlayerInventoryPaneProvider` 复用原版背包背景和洋娃娃渲染。
- 右侧使用原版 `generic_54` 容器纹理承载数据库网格。
- 顶部提供分类页签、搜索框、排序下拉和翻页控件。

### `network`

自定义 payload 用于客户端与服务端同步：

- `DatabaseSnapshotPayload`：服务端下发页面快照。
- `DatabaseQueryPayload`：客户端提交分类、排序、搜索、分页查询。
- `DatabaseClickPayload`：数据库格子点击和 Shift 快速提取。
- `DepositAllPayload`：一键入库。

## 持久化与死亡语义

玩家数据库通过 NeoForge `AttachmentType` 绑定到玩家实体，并启用 `copyOnDeath()`。

为了保证入口物品不影响真实数据：

- `LivingDropsEvent` 会移除数据库快捷物品的掉落。
- `PlayerEvent.Clone` 在死亡且未启用 `keepInventory` 时补回快捷物品。

这样可以确保数据库内容和入口物品都符合“死亡不掉落”的预期，但数据库本体仍然与玩家数据解耦。

## 当前兼容策略

当前左侧界面已完整支持原版背包区、盔甲、副手和洋娃娃。

第三方饰品栏兼容暂时通过 `PlayerInventoryPaneProvider` 预留扩展点，后续可以在不重写数据库主页面的前提下接入更多玩家面板提供者。

## 已知边界

当前版本有意保持以下边界：

- 搜索首版以物品显示名和注册名为主。
- 分类页签为固定枚举，不支持玩家自定义标签。
- 右侧数据库区目前是自定义渲染，不是传统容器槽位。
- 快捷物品默认通过创造栏获取，后续可以增加更多入口。
