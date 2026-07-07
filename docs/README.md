---
last_modified: "2026-07-07 18:51"
---

# 项目文档索引与治理说明

## 目标

`docs/` 是项目级文档的唯一正式入口，用来沉淀可复用知识并约束临时文档的落位方式。

本目录只保留两类文档：

- 长期文档：稳定资产，服务后续开发、运维和架构演进。
- 短期文档：工作备忘，服务当前任务分析、方案制定、评审与复盘。

禁止在 `docs/` 根目录直接新增散落业务文档；新文档必须进入对应分类目录。

## 目录结构

```text
docs/
├── README.md
├── long-term/
│   ├── architecture/
│   ├── engineering/
│   └── operations/
└── short-term/
    ├── analysis/
    ├── archive/
    │   └── 2026/
    ├── plans/
    └── reviews/
```

各目录职责如下：

- `long-term/architecture/`：核心架构、领域模型、关键交互与长期设计说明。
- `long-term/engineering/`：工程规范、开发约束、工具链规则。
- `long-term/operations/`：运维、排障、数据安全、备份恢复等运行文档。
- `short-term/analysis/`：问题分析、现状调研、临时结论。
- `short-term/plans/`：实施计划、拆解方案、任务设计。
- `short-term/reviews/`：复盘、评审结论、验收记录。
- `short-term/archive/<year>/`：已完结短期文档归档区。

## 当前长期文档索引

- [数据库架构说明](./long-term/architecture/personal-database-architecture.md)：数据库领域划分、交互语义、持久化与兼容策略。
- [数据安全与恢复说明](./long-term/operations/database-safety-and-recovery.md)：备份、恢复、迁移与排障语义。
- [公共发布检查清单](./long-term/operations/public-release-checklist.md)：GitHub、Modrinth、CurseForge 的元数据、素材与发版核对项。
- [Java 文件行数约束](./long-term/engineering/java-file-line-limit.md)：Java 文件规模上限与校验方式。
- [版本治理与推进规则](./long-term/engineering/version-governance.md)：`mod_version` 的推进策略、CI 校验方式与发布基线规则。

## 短期文档工作流

1. 开始任务时，按用途写入 `analysis/`、`plans/` 或 `reviews/`。
2. 任务完成后，默认将对应短期文档移入 `archive/<year>/`。
3. 如果短期文档中的结论已经稳定，并且会持续指导后续开发或运维，应提炼后新增或更新到 `long-term/` 对应分类。
4. 归档后如果内容已经失效，后续可以在明确确认后清理；长期文档不应依赖短期文档才能被理解。

## 命名规范

长期文档：

- 使用稳定英文 `kebab-case.md` 文件名。
- 文件名应直接表达主题，避免 `misc`、`temp`、`notes` 这类模糊命名。

短期文档：

- 使用 `YYYY-MM-DD_HH-mm-<slug>.md`。
- `<slug>` 使用英文 `kebab-case`，描述任务主题。
- 示例：`2026-04-18_13-30-docs-governance-plan.md`

正文规范：

- 正文默认使用中文。
- 文件编码统一为 UTF-8。
- 文档重点说明“为什么”和“约束是什么”，避免把代码细节机械搬运进文档。

## 维护规则

- 长期文档必须保持可作为事实参考的稳定质量，过期内容需要及时修订或清理。
- 短期文档只承担沟通备忘职责，不写成长篇设计大全。
- 新增分类前，优先评估是否能归入现有目录，避免目录膨胀。
- 重要代码变更如果改变架构、运维流程或工程约束，应同步更新对应长期文档。
- `docs/README.md` 是文档治理的单一入口；不要在每个子目录重复维护另一套规则。
