# 无限库存

[English](https://github.com/A-G-guy/infinite-inventory/blob/main/README.md)

![无限库存Logo](https://raw.githubusercontent.com/A-G-guy/infinite-inventory/main/.github/assets/infiniteinventory-logo.png)

[![GitHub Release](https://img.shields.io/github/v/release/A-G-guy/infinite-inventory?style=flat-square)](https://github.com/A-G-guy/infinite-inventory/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=flat-square&logo=minecraft)](https://minecraft.net)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.x-orange?style=flat-square&logo=java)](https://neoforged.net)
[![License](https://img.shields.io/github/license/A-G-guy/infinite-inventory?style=flat-square)](https://github.com/A-G-guy/infinite-inventory/blob/main/LICENSE)
[![Modrinth](https://img.shields.io/badge/Modrinth-审核中-1DBF7A?style=flat-square&logo=modrinth)](https://modrinth.com/mod/agguy-infinite-inventory)

**再也不用为背包空间发愁。**

无限库存是一个面向 Minecraft 1.21.1 NeoForge 的模组。它将狭小的玩家背包升级为**个人专属的、可无限扩展的、数据库式仓储系统**。你不再需要频繁整理箱子、潜影盒和散布各处的储物室 —— 只需一个可检索的终端，就能携带全部物品收藏，按页签分类管理，支持双语智能搜索，并配备自动安全备份机制。

![Hero Banner](https://raw.githubusercontent.com/A-G-guy/infinite-inventory/main/.github/assets/infiniteinventory-hero-v2.png)

## 下载

- **[Modrinth](https://modrinth.com/mod/agguy-infinite-inventory)** — 审核中，通过后即可下载。
- **[GitHub Releases](https://github.com/A-G-guy/infinite-inventory/releases)** — 所有稳定版本与更新日志。

## 为什么要下载这个模组？

如果你曾经遇到过以下情况：
- 挖矿或建筑时快捷栏和背包格子不够用
- 整理箱子的时间比实际游玩还长
- 找不到某个物品放在哪个箱子里
- 希望不用记住储物布局就能瞬间找到物品

那么无限库存将以上所有繁琐操作替换为一个全屏数据库界面。按名称、拼音、物品 ID、标签或模组搜索 —— 几秒钟内精准提取所需物品。

## 核心功能

- **无限个人仓储** —— 一个数据库，零格子限制。可存储任意数量的独立物品堆叠。
- **全屏数据库界面** —— 多面板视图：个人库、公共库（服务器全服共享池）或混合模式。
- **双语智能搜索** —— 识别中文显示名、英文别名、物品 ID、命名空间和拼音（全拼 + 首字母）。
- **类 JEI 高级搜索语法** —— 按模组（`@`）、物品标签（`#`）、注册名（`&`）、创造标签页（`%`）筛选，支持 AND/OR/NOT 逻辑。
- **页签管理** —— 创建、重命名、排序、分配自定义图标。一键搬运整个页签内容。
- **稳定排序** —— 按最近变更、最近新增、名称、数量、命名空间或物品 ID 排序。
- **星标与备注系统** —— 将常用物品标记为星标，或为任意物品附加自定义备注。
- **操作日志查看器** —— 查阅存入、提取等数据库操作历史。
- **自动入库增强** —— 拾取的物品自动存入指定的目标页签。
- **数据安全** —— 每 15 分钟自动滚动备份、手动备份/恢复命令、模组临时缺失时保留未解析物品。
- **可选 Accessories / Curios 集成** —— 将数据库终端作为背部饰品穿戴，通过快捷键快速打开数据库界面。

## 运行环境

| 项目 | 值 |
|---|---|
| **运行端** | 客户端 + 服务器端（完整功能需两端同时安装；服务端负责数据持久化和公共库） |
| **Minecraft** | 1.21.1 |
| **模组加载器** | NeoForge |
| **支持的 NeoForge** | 21.1.222+ |
| **Java** | 21 |
| **可选依赖** | Accessories 1.1.0-beta.53+1.21.1、Curios 9.5.1+1.21.1 |

## 安装方式

1. 安装 Minecraft **1.21.1**。
2. 安装兼容的 **NeoForge** **21.1.x** 构建版本。
3. 将 `无限库存` JAR 放入 `mods/` 文件夹。
4. *(可选)* 如需从背部槽位穿戴终端并通过快捷键快速访问，安装 **Accessories** 或 **Curios**。

> **注意：** 多人联机时，模组需同时存在于**客户端和服务端**。服务端负责数据持久化和公共库；客户端负责界面渲染和搜索输入。

## 合成获取

在生存模式中合成**数据库终端**：

![数据库终端](https://raw.githubusercontent.com/A-G-guy/infinite-inventory/main/.github/assets/database_access_item.png)

```text
D E D
R C R
D G D
```

| 符号 | 物品 |
|---|---|
| D | 钻石块 |
| E | 末影珍珠 |
| R | 红石块 |
| C | 箱子 |
| G | 金块 |

物品 ID: `infiniteinventory:database_access_item`

## 使用方法

### 打开数据库
- **手持**：手持数据库终端并右键点击，即可打开全屏界面。
- **已装备（Accessories / Curios）**：在「控制 → 按键绑定 → Infinite Inventory → 打开已装备的数据库」中绑定快捷键，当终端穿戴在背部饰品槽时可直接一键打开。

### 切换视图
使用顶部工具栏切换：
- **个人库** —— 你的私人物品数据库
- **公共库** —— 服务器全服共享存储（如启用）
- **混合** —— 两者合并视图

### 存入物品
- **左键单击** 背包中的物品存入 1 个
- **Shift+单击** 将整组物品存入指定页签
- **全部存入** 按钮 —— 将玩家背包中的所有物品一次性存入数据库
- **存入已有** 按钮 —— 仅存入数据库中已有对应条目的物品（适合批量补货）

> **存入冲突**：若该物品已存在于其他页签中，会弹出冲突处理窗口，可选择存入原始页签或将所有相关物品转移到目标页签。

### 提取物品
- **左键单击** 数据库条目提取 1 个
- **Shift+单击** 直接提取整组到背包
- **右键单击** 打开上下文选项：
  - 提取整组到光标
  - 提取半组到背包
  - 自定义数量提取（弹出数字输入框）

### 搜索
在搜索栏输入内容过滤物品。搜索支持：
- 中文物品名和拼音（全拼 + 首字母）
- 英文显示名和别名
- 精确物品 ID 和命名空间

**特殊搜索语法：**

| 前缀 | 含义 | 示例 |
|---|---|---|
| `@` | 按模组 ID 筛选 | `@infiniteinventory` |
| `#` | 按物品标签筛选 | `#minecraft:logs` |
| `&` | 按注册名筛选 | `&diamond` |
| `%` | 按创造标签页筛选 | `%building blocks` |
| `\|` | OR 运算符 | `diamond \| emerald` |
| `-` | 排除 | `-stone` |

多个关键词之间的空格表示 **AND**。

### 管理页签
- **创建** 新页签对物品进行分类
- **拖拽** 页签调整顺序
- **右键** 页签进行重命名、更换图标、转移整页内容或删除
- **隐藏** 不常用的页签以保持界面整洁
- **批量转移**：多选条目（拖拽框选或 Shift+单击），然后右键选择「移动选中项到另一分类」

### 星标与备注
- **星标** 常用物品以便快速定位
- **右键** →「编辑备注」为任意物品附加自定义备注
- 备注可批量应用到所有选中的物品

### 数据库设置（齿轮图标）
设置面板提供 5 个导航标签页：
- **高级搜索** —— 配置搜索行为与默认筛选条件
- **加强** —— 开关可选增强功能：
  - *拾取物品自动入库* —— 新拾取的物品自动存入指定的目标页签
  - *悬浮提示显示数量* —— 在物品悬浮提示中显示该物品的总存储数量
  - *关键变更强制保存* —— 重要变更后立即持久化数据库
- **视图** —— 预览并切换个人库、公共库、混合视图
- **管理** —— 批量创建、删除、重命名、重新排序和整理页签
- **日志** —— 按时间顺序查看所有存入、提取、转移等操作记录

## 数据安全

你的数据存储在主世界的 `SavedData` 系统中，而非终端物品本身。这意味着：

- **终端可替换** —— 丢失或损坏终端不会删除你的数据库。
- **未解析物品保留** —— 模组临时移除时，其物品以未解析条目保留，模组恢复后自动还原。
- **自动备份** —— 数据库每 15 分钟自动备份一次。
- **手动备份/恢复** —— 服务器管理员可随时创建快照并从中恢复。

### 备份命令

```
/infiniteinventory database backup now       # 创建手动备份
/infiniteinventory database backup list      # 列出可用备份
/infiniteinventory database restore <名称>    # 从指定备份恢复
```

## 参考文档

- [架构说明](https://github.com/A-G-guy/infinite-inventory/blob/main/docs/long-term/architecture/personal-database-architecture.md)
- [安全与恢复指南](https://github.com/A-G-guy/infinite-inventory/blob/main/docs/long-term/operations/database-safety-and-recovery.md)
- [公共发布检查清单](https://github.com/A-G-guy/infinite-inventory/blob/main/docs/long-term/operations/public-release-checklist.md)

## 链接

- **源代码：** <https://github.com/A-G-guy/infinite-inventory>
- **问题追踪：** <https://github.com/A-G-guy/infinite-inventory/issues>
- **Modrinth：** <https://modrinth.com/mod/agguy-infinite-inventory>

## 许可证

- 项目许可证：[Apache-2.0](https://github.com/A-G-guy/infinite-inventory/blob/main/LICENSE)
- 第三方声明：[THIRD_PARTY_NOTICES.md](https://github.com/A-G-guy/infinite-inventory/blob/main/THIRD_PARTY_NOTICES.md)
