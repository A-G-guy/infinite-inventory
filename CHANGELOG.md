# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- 系统页签支持移动换图标，所有页签可设顶部可见性
- 为右键菜单每个选项添加物品图标
- 新增测试覆盖：registry、command、event、item、localization 包的基础单元测试
- 新增 Jacoco 覆盖率报告配置
- 新增 Mockito 测试依赖

### Changed
- 优化右键菜单图标选择，使用语义更贴近的原版物品
- 替换数据库物品图标为64x64高清像素风格版本
- 替换模组logo为高清像素风格终端设备图标
- 调整模组logo尺寸为256x256并修复比例

### Fixed
- 阻止背包快捷键在数据库页面文本输入框获焦点时关闭GUI
- 补充系统页签与隐藏页签的单元测试，修复 ensureDefaultConcreteTab 边界值
- 多选备注不统一提示与星标右键菜单智能选项
- 修复4处静默吞没异常，补充上下文日志记录

### Removed
- 移除 JEI crafting auto-extract 功能及相关代码

## [1.3.0] - 2025-03-15

### Added
- JEI 集成：支持数量显示和物品提取
- 星标/收藏系统：支持标记常用物品
- 物品备注系统：可为任意物品添加自定义备注
- 数据库操作日志查看器：记录存入、提取等操作历史
- JEI 风格高级搜索语法：支持按名称、ID、标签等字段搜索

### Changed
- 中英文 README 分离，文档更易于维护
- 新增版本演进治理规则

### Fixed
- 修复日志条目序列化问题
- 修复未授权请求未正确清除缓存的问题
- 修复日志面板渲染、页签名和关闭按钮表现
- 修复星标系统与收藏页签的交互问题
- 修复跨混合范围选择的备注提交问题
- 修复默认增强配置和 PersonalDatabaseMenu JEI 处理器注册

## [1.0.0] - 2025-01-20

### Added
- 无限个人数据库：将玩家背包升级为可无限扩展的数据库式仓储系统
- 全屏数据库 UI：支持多面板、多页签浏览
- 双语搜索：支持中文拼音和英文物品名称搜索
- 页签管理：支持创建、重命名、排序、隐藏页签
- 多种排序方式：按最近变更、名称、数量、ID 等排序
- 数据安全：自动备份和手动备份机制
- Accessories 模组集成
- 数据持久化：基于 NBT 的数据存储和同步
