# 贡献指南

感谢你对 Infinite Inventory / 无限库存 的兴趣！本指南帮助你快速搭建环境并参与贡献。

## 开发环境搭建

### 前置要求

- **Java 21**（Minecraft 1.21.1 与 NeoForge 的运行时目标版本）
- **Gradle**（使用仓库自带的 `gradlew` 即可，无需单独安装）

### 克隆与构建

```bash
git clone https://github.com/agguy/minecraft-infinite-inventory-mod-neoforge.git
cd minecraft-infinite-inventory-mod-neoforge
./gradlew build
```

首次构建会自动下载 NeoForge MDK 与依赖，耗时较长，请耐心等待。

### IDE 导入

- **IntelliJ IDEA**：直接打开项目根目录，Gradle 导入完成后运行 `genIntellijRuns` 生成运行配置。
- **VS Code**：安装 Extension Pack for Java，打开项目根目录即可。

### 常用 Gradle 任务

| 任务 | 说明 |
|------|------|
| `./gradlew build` | 完整构建，包含编译、测试、行数检查 |
| `./gradlew test` | 运行全部单元测试 |
| `./gradlew check` | 编译 + 测试 + 行数限制检查 + 版本治理检查 |
| `./gradlew javadoc` | 生成 API 文档（排除 client/compat 包） |
| `./gradlew runClient` | 启动客户端开发环境 |
| `./gradlew runServer` | 启动服务端开发环境 |
| `./gradlew runGameTestServer` | 运行 GameTest |

## 分支策略

- `main`：唯一主分支，始终可构建通过，受保护。
- 功能开发：从 `main` 切出 `feat/<short-description>` 分支。
- Bug 修复：从 `main` 切出 `fix/<short-description>` 分支。
- 重构/工程改进：从 `main` 切出 `refactor/<short-description>` 分支。
- **禁止直接向 `main` 提交**，所有改动通过 Pull Request 合并。

## 提交规范

本项目严格遵循 [Conventional Commits](https://www.conventionalcommits.org/)。

允许的 `<type>` 仅限以下 7 种：

| 类型 | 用途 | 版本影响 |
|------|------|----------|
| `feat` | 新功能 | minor |
| `fix` | Bug 修复 | patch |
| `docs` | 文档变更 | 无 |
| `style` | 代码格式调整（不影响功能） | 无 |
| `refactor` | 重构（不改变外部行为） | 无 |
| `test` | 测试相关 | 无 |
| `chore` | 工程杂项（构建脚本、依赖升级等） | 无 |

### 提交格式

```text
<type>: <简短描述，小写开头，不超过50字符>

- <动作动词> <详细变更 1>
- <动作动词> <详细变更 2>
```

示例：

```text
feat: 为数据库页签添加图标自定义功能

- 实现 TabIconSelectScreen 供玩家选择页签图标
- 在 DatabaseTab 中持久化图标 NBT 数据
- 更新页签渲染器以显示自定义图标
```

### 破坏性变更

如果改动破坏向后兼容性，提交类型后加 `!`，或在正文添加 `BREAKING CHANGE:` 段落：

```text
feat!: 移除旧版 NBT 序列化兼容层

BREAKING CHANGE: 1.0.x 版本的存档将无法直接加载，需先升级到 1.2.x 过渡版本。
```

## Pull Request 流程

1. **切分支**：按上述分支策略创建功能分支。
2. **本地验证**：提交前确保 `./gradlew check` 全部通过。
3. **提交信息**：每个提交都遵循 Conventional Commits 格式。
4. **PR 描述**：说明改动背景、具体变更、测试方式。
5. **版本检查**：如果改动影响玩家体验，确保 `mod_version` 已按 [版本治理规则](docs/long-term/engineering/version-governance.md) 正确推进。
6. **审查与合并**：维护者审查通过后，使用 `Squash and Merge` 合并到 `main`。

## 代码风格要求

详细规范请参阅项目内文档：

- [Java 文件行数约束](docs/long-term/engineering/java-file-line-limit.md)：单文件不得超过 500 行。
- [版本治理与推进规则](docs/long-term/engineering/version-governance.md)：版本号推进策略与 CI 校验。
- [项目文档索引与治理说明](docs/README.md)：文档分类、命名规范与维护规则。

### 核心约束速查

- **函数 < 50 行，类 < 300 行**（硬上限 500 行）。
- **卫语句优先**：使用提前返回，避免 `if-else` 嵌套超过 3 层。
- **异常不静默吞没**：所有 `catch` 块必须附带上下文日志。
- **命名即文档**：禁止无意义单字母、缩写或拼音。
- **注释解释 Why，不解释 What**。
- **Public 接口必须包含标准 JavaDoc**（参数、返回值、异常）。
- **测试文件放入 `tests/` 子目录**，与源码命名严格对应。
- **逢变必测**：核心逻辑增改必须同步覆盖测试用例。

## 问题与讨论

- 发现 Bug 或有功能建议：请提交 [GitHub Issue](https://github.com/agguy/minecraft-infinite-inventory-mod-neoforge/issues)。
- 安全漏洞：请遵循 [SECURITY.md](./SECURITY.md) 中的私密报告流程，不要在公开 Issue 中披露。
