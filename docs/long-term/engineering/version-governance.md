---
last_modified: "2026-07-07 18:51"
---

# 版本治理与推进规则

## 目标

这份文档用于保证 `mod_version` 会随着玩家可见改动持续推进，避免功能开发完成后仍沿用旧版本号，导致 GitHub Release、CurseForge、Modrinth 与问题排查出现版本歧义。

## 单一事实源

- 模组版本只认 `gradle.properties` 中的 `mod_version`。
- 版本格式固定为稳定版 `X.Y.Z`。
- 当前不支持 `alpha`、`beta`、`rc` 或 `dev` 后缀。

## 提交到版本级别的映射

版本推荐机制完全依赖 Conventional Commits，不再从文件路径反推改动语义。

- `feat`: 触发 `minor`
- `fix`: 触发 `patch`
- `type!:` 或提交正文包含 `BREAKING CHANGE:`: 触发 `major`
- `docs`、`style`、`refactor`、`test`、`chore`: 默认 `no release`

约束：

- 只要改动会影响玩家体验、兼容性、资源结果、运行行为或最终发布产物，就必须使用 `feat`、`fix` 或 breaking 标记表达出来。
- 如果真实改动会影响玩家，但提交写成 `chore` 或 `refactor`，版本检查不会替你纠正语义；提交者必须主动修正提交类型。

## 基线选择规则

CI 与本地手动检查使用不同的默认基线策略：

- `pull_request`：以目标分支为基线。
  - 读取 `origin/<base_ref>` 的 `mod_version`。
  - 分析范围为 `merge-base(HEAD, origin/<base_ref>)..HEAD`。
- `push` 到 `main`：以最近可达的 `v*` tag 为基线。
  - 读取 tag 中的版本号，例如 `v1.0.0 -> 1.0.0`。
  - 分析范围为 `<latest-tag>..HEAD`。
- 本地手动执行：
  - 默认优先使用 `origin/main`。
  - 若本地没有 `origin/main`，回退到最近可达的 `v*` tag。
  - 可通过 `-PversionGovernanceBaseRef=<ref>` 强制指定基线分支。

## 任务入口

- `./gradlew recommendModVersion`
  - 输出当前基线、分析范围、最低推荐版本与触发依据。
  - 会将 Markdown 报告写到 `build/reports/version-governance/recommendation.md`。
- `./gradlew verifyModVersionProgression`
  - 在当前 `mod_version` 低于最低推荐版本时失败。
  - 失败前同样会写出报告，便于 GitHub Actions 摘要或本地排查使用。

## GitHub Actions 约束

CI 默认顺序如下：

1. 拉取完整历史与 tags。
2. `pull_request` 额外抓取目标分支引用。
3. 运行 `buildSrc` 单元测试。
4. 运行 `recommendModVersion` 并把报告写入 Actions Summary。
5. 运行 `verifyModVersionProgression`。
6. 版本检查通过后再执行 `./gradlew build`。

这意味着：

- 玩家可见改动如果没有同步推进 `mod_version`，PR 或 `main` push 都会直接失败。
- 纯文档或纯工程维护提交可以不改版本，只要提交语义准确。

## 发布基线与 tag 规则

- 首个公开基线使用标注 tag：`v1.0.0`。
- 后续每次公开发布都必须保证：
  - Git tag 形如 `v<mod_version>`
  - tag 指向实际发布提交
  - GitHub Release、CurseForge、Modrinth 使用同一版本号

如果 `main` 上找不到任何 `v*` tag，版本检查会直接失败，并提示先建立发布基线。
