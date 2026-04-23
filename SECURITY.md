# 安全政策

## 支持版本

以下版本当前接受安全更新：

| 版本 | 支持状态 |
|------|----------|
| 1.3.x | 活跃支持 |
| 1.2.x | 仅关键安全修复 |
| < 1.2.0 | 不再支持 |

## 报告安全漏洞

如果你发现了安全漏洞，请**不要**通过公开的 GitHub Issue 披露。

### 私密报告流程

1. **发送邮件至**：`agguy@example.com`（请将 `example.com` 替换为实际联系方式，或在 GitHub 上私信维护者获取安全报告邮箱）
2. **邮件主题**：`[SECURITY] Infinite Inventory - <简短描述>`
3. **邮件内容请包含**：
   - 漏洞类型（如：SQL 注入、XSS、权限绕过、数据泄露等）
   - 受影响版本
   - 复现步骤或概念验证（PoC）
   - 可能的影响范围
   - 建议的修复方案（如有）

### 响应时间线

| 阶段 | 时间目标 |
|------|----------|
| 确认收到报告 | 48 小时内 |
| 初步评估与分类 | 7 天内 |
| 修复版本发布 | 根据严重程度，通常在 30 天内 |
| 公开披露 | 修复发布后，或经报告者同意后 |

### 披露政策

- 在修复版本发布前，漏洞细节不会公开披露。
- 修复发布后，我们会在 [CHANGELOG.md](./CHANGELOG.md) 中记录安全修复，并在 GitHub Security Advisories 中发布详细说明。
- 报告者如愿意，将在安全公告中获得致谢。

## 安全最佳实践

### 对于服务器管理员

- 定期备份数据库存档（模组提供 `/infiniteinventory database backup now` 命令）。
- 仅向信任的玩家授予操作数据库的权限。
- 关注模组更新，及时升级到最新支持版本。

### 对于开发者

- 所有网络包处理必须验证发送方权限与数据合法性。
- 命令处理器必须检查 `CommandSourceStack.hasPermission`。
- 避免在日志中输出敏感数据（如玩家坐标、完整 NBT 数据）。
- 序列化/反序列化时严格校验 NBT 类型与版本标记。

## 联系方式

- 安全漏洞报告：请通过上述私密邮件流程
- 一般问题与讨论：[GitHub Issues](https://github.com/agguy/minecraft-infinite-inventory-mod-neoforge/issues)
- 项目主页：<https://github.com/agguy/minecraft-infinite-inventory-mod-neoforge>
