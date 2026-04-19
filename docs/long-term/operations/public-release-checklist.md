# 公共发布检查清单

## 目标

这份文档用于约束 `Infinite Inventory` 在 GitHub、Modrinth 与 CurseForge 的公开发布流程，避免每次发版时遗漏许可证、素材、兼容版本或元数据。

## 固定公开信息

- 项目名称：`Infinite Inventory`
- 中文副标题：`无限库存`
- 模组 ID：`infiniteinventory`
- Minecraft 版本：`1.21.1`
- Loader：`NeoForge`
- 许可证：`Apache-2.0`
- 源码地址：`https://github.com/agguy/minecraft-infinite-inventory-mod-neoforge`
- 问题反馈：`https://github.com/agguy/minecraft-infinite-inventory-mod-neoforge/issues`
- 作者展示名：`agguy`

## 素材清单

- 项目标识：`src/main/resources/infiniteinventory_logo.png`
  要求：`400x400 PNG`
- README/项目页横幅：`.github/assets/infiniteinventory-hero.png`
- 推荐补充的后续实机截图：
  - 主数据库界面
  - 混合视图或多面板视图
  - 高级搜索与排序
  - 页签管理面板
  - 备份恢复或自动入库演示

## GitHub 发版

- 确认 `README.md`、`README.zh-CN.md`、`LICENSE`、`NOTICE`、`THIRD_PARTY_NOTICES.md` 已同步到当前代码状态。
- 确认工作树不包含本地路径、密钥、私有配置和临时压缩包。
- 先运行 `./gradlew recommendModVersion`，确认当前版本治理报告符合预期。
- 再运行 `./gradlew verifyModVersionProgression`，确认 `mod_version` 已达到最低推荐值。
- 运行 `./gradlew check` 与 `./gradlew build`。
- 将发布 JAR 复制到你的共享交付目录或外部备份位置。
- 使用与 `gradle.properties` 一致的版本号创建标注 Git tag 与 GitHub Release，例如 `v1.2.0`。
- Release notes 至少说明：
  - 支持的 Minecraft / NeoForge 版本
  - 这次更新的核心功能或修复
  - 是否需要安装 `Accessories`

## Modrinth 元数据

- Project type：`mod`
- License：`Apache-2.0`
- Client side：`required`
- Server side：`required`
- Loaders：`NeoForge`
- Game versions：`1.21.1`
- Source URL：GitHub 仓库地址
- Issues URL：GitHub Issues 地址
- Recommended categories：
  - `storage`
  - `utility`
  - `technology`
- Optional dependency：
  - `Accessories`

推荐英文简介：

> A database-style infinite inventory mod for NeoForge 1.21.1 with full-screen search, tab organization, bilingual lookup, pinyin search, and personal/public storage scopes.

推荐中文补充简介：

> 一个面向 NeoForge 1.21.1 的数据库式无限库存模组，支持全屏搜索、页签整理、中英双语检索、拼音搜索，以及个人库/公共库双作用域。

## CurseForge 元数据

- 项目主描述必须英文在前，中文说明放在英文后面。
- 项目 Logo 使用 `400x400 PNG`。
- 文件上传使用编译后的 `.jar`，不要附带无关说明文件或压缩包。
- 建议分类方向：
  - `Storage`
  - `Utility`
- Summary 需要直接描述模组做什么，避免只写“adds items”之类模糊描述。
- Project License 选择 `Apache-2.0`。
- Source 字段填写 GitHub 仓库地址。

推荐英文 Summary：

> Turns Minecraft item storage into a searchable infinite inventory database with personal tabs, public storage, sorting, and safe backups.

## 许可证与第三方声明

- 根目录必须保留：
  - `LICENSE`
  - `NOTICE`
  - `THIRD_PARTY_NOTICES.md`
- 每次调整嵌入依赖后，都要重新核对：
  - `./gradlew dependencies --configuration embeddedLibrary`
- 当前没有外部 `embeddedLibrary` 依赖。
- 当前第三方源码声明：
  - NeoForged MDK 模板来源
  - TinyPinyin 最小字符拼音表来源

## 发版前最后核对

- `git status` 干净。
- 最近发布基线 tag 已存在，且命名符合 `v<mod_version>`。
- `recommendModVersion` 报告的最低推荐版本没有超过当前 `mod_version`。
- `./gradlew check` 通过。
- `./gradlew build` 通过。
- 最终 JAR 已复制到共享交付目录或外部备份位置。
- `src/main/resources/infiniteinventory_logo.png` 存在。
- `README.md` 与 `README.zh-CN.md` 的公开链接、互链和文案没有模板残留。
- 本地专用说明文件没有被重新加入 Git 索引。
