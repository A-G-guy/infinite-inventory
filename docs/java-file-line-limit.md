# Java 文件行数约束

## 规则

- 检查范围：`src/main/java/**/*.java` 与 `src/test/java/**/*.java`
- 上限：单文件不得超过 `500` 行
- 生效方式：`./gradlew check` 会自动执行 `checkJavaFileLineLimit`
- 提交前校验：仓库内置 `.githooks/pre-commit`，用于在提交前执行 `./gradlew check`

## 使用方式

首次启用仓库 hook：

```bash
git config core.hooksPath .githooks
```

## 修改要求

- 允许抽取辅助类、工厂类、协调类，但禁止借拆分改变现有功能、业务逻辑、协议语义和存档语义
- 新增或重构后的 Java 文件同样必须满足 `<= 500` 行
- 如需继续重构，优先按职责边界拆分，不允许用复制粘贴制造重复逻辑来规避行数限制
