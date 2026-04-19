# 无限库存

[English](./README.md)

![无限库存横幅](./.github/assets/infiniteinventory-hero.png)

`无限库存` 是一个面向 Minecraft 1.21.1 NeoForge 的模组。它把传统背包管理升级为数据库式库存系统，提供全屏检索、分类页签、快速提取，以及可选的公共库存储作用域。

## 核心特性

- 提供全屏数据库界面，支持个人库、公共库和混合多面板视图。
- 支持中英双语检索，可识别中文物品名、英文别名、物品 ID、命名空间与拼音。
- 支持自定义页签管理，包括重命名、图标选择、排序调整、整页转移和迁移式删除流程。
- 支持按最近变更、最近新增、名称、数量、命名空间和物品 ID 稳定排序。
- 提供未解析物品保留、自动滚动备份、手动备份和恢复命令等数据安全能力。
- 可选集成 `Accessories`，支持可穿戴数据库终端、兼容布局和快速打开。

## 兼容性

- Minecraft: `1.21.1`
- Loader: `NeoForge`
- 支持的 NeoForge 范围: `21.1.222+`
- Java: `21`
- 可选依赖: `Accessories 1.1.0-beta.53+1.21.1`

## 安装方式

1. 安装 Minecraft `1.21.1`。
2. 安装兼容的 NeoForge `21.1.x` 构建版本。
3. 将发布的 `Infinite Inventory` JAR 放入 `mods/` 目录。
4. 只有在你需要背部槽位穿戴访问和额外面板集成时，才需要安装 `Accessories`。

## 合成获取

数据库终端可以在生存模式中合成：

```text
D E D
R C R
D G D
```

- `D`: 钻石块
- `E`: 末影珍珠
- `R`: 红石块
- `C`: 箱子
- `G`: 金块

物品 ID 为 `infiniteinventory:database_access_item`。

## 玩法流程

- 打开数据库终端即可进入全屏库存数据库界面。
- 通过顶部工具栏切换个人库、公共库或混合视图。
- 可按显示名、物品 ID、命名空间、英文别名、中文别名和拼音首字母或全拼进行检索。
- 使用页签整理分类、批量搬运物品，让大规模存储仍保持清晰。
- 可直接把单个物品、半组、整组或自定义数量提取回玩家背包。
- 启用自动入库增强后，拾取的物品会自动存入指定目标页签。

## 搜索与排序

- 搜索支持 `zh_cn` 和 `en_us` 的双语物品名。
- 中文名称会额外生成拼音全拼和首字母，便于键盘快速检索。
- 高级搜索权重允许你重新平衡显示名、物品 ID、命名空间、拼音和数量加权。
- 非空搜索会结合精确匹配、前缀匹配、包含匹配、模糊匹配和排序规则，保持稳定结果顺序。

## 数据安全

- 实际数据库存储在主世界 `SavedData` 中，而不是保存在访问道具本体上。
- 缺失物品或临时不可用的模组物品会以未解析条目形式保留，不会被直接丢弃。
- 脏数据库状态每 15 分钟自动滚动备份一次。
- 管理员可以使用手动备份和恢复命令。

参考文档：

- [架构说明](./docs/long-term/architecture/personal-database-architecture.md)
- [安全与恢复](./docs/long-term/operations/database-safety-and-recovery.md)
- [公共发布检查清单](./docs/long-term/operations/public-release-checklist.md)

## 命令

- `/infiniteinventory database backup now`
- `/infiniteinventory database backup list`
- `/infiniteinventory database restore <snapshot>`

## 发布

- GitHub 源码与 Release：
  <https://github.com/agguy/minecraft-infinite-inventory-mod-neoforge>
- 计划公开分发平台：
  `Modrinth` 和 `CurseForge`
- 平台发布说明与元数据检查清单：
  [docs/long-term/operations/public-release-checklist.md](./docs/long-term/operations/public-release-checklist.md)

## 许可证

- 项目许可证：[Apache-2.0](./LICENSE)
- 第三方声明：[THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md)
