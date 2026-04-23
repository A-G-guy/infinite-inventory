package com.agguy.infiniteinventory.menu;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSelectionEntry;
import com.agguy.infiniteinventory.network.DatabaseSelectionAction;
import com.agguy.infiniteinventory.service.PersonalDatabaseService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * 多选批处理操作相关逻辑的委托辅助类。
 *
 * <p>将 {@link PersonalDatabaseMenu} 中所有 {@code handleSelectionAction} 重载抽取至此，
 * 统一处理按作用域分组、跨域转移与精准同步。
 */
final class PersonalDatabaseMenuSelectionHelper {

    private final PersonalDatabaseMenu menu;

    PersonalDatabaseMenuSelectionHelper(PersonalDatabaseMenu menu) {
        this.menu = menu;
    }

    /**
     * 处理多选批处理操作（使用当前作用域作为目标）。
     *
     * @param action           选择动作
     * @param selectionEntries 选中的数据库条目
     * @param targetScope      目标作用域，可为 null（按默认规则解析）
     * @param targetTabId      目标标签页标识，可为 null
     */
    void handleSelectionAction(
            DatabaseSelectionAction action,
            List<DatabaseSelectionEntry> selectionEntries,
            @Nullable DatabaseScope targetScope,
            @Nullable String targetTabId
    ) {
        this.handleSelectionAction(action, selectionEntries, targetScope, targetTabId, 0L);
    }

    /**
     * 处理多选批处理操作（目标作用域由调用方显式指定或默认）。
     *
     * @param action           选择动作
     * @param selectionEntries 选中的数据库条目
     * @param targetTabId      目标标签页标识，可为 null
     */
    void handleSelectionAction(
            DatabaseSelectionAction action,
            List<DatabaseSelectionEntry> selectionEntries,
            @Nullable String targetTabId
    ) {
        this.handleSelectionAction(action, selectionEntries, null, targetTabId, 0L);
    }

    /**
     * 处理多选批处理操作的完整重载，支持跨作用域转移与按数量提取。
     *
     * <p>业务约束：
     * <ul>
     *   <li>空选择或无效动作直接返回，避免无意义的服务端计算
     *   <li>按作用域分组后批量调用服务层，减少数据库往返
     *   <li>跨作用域转移时同时追踪 personalChanged 与 publicChanged，以精准同步受影响的查看者
     * </ul>
     *
     * @param action           选择动作
     * @param selectionEntries 选中的数据库条目
     * @param targetScope      目标作用域，可为 null
     * @param targetTabId      目标标签页标识，可为 null
     * @param requestedAmount  请求提取数量，0 表示按动作默认值处理
     */
    void handleSelectionAction(
            DatabaseSelectionAction action,
            List<DatabaseSelectionEntry> selectionEntries,
            @Nullable DatabaseScope targetScope,
            @Nullable String targetTabId,
            long requestedAmount
    ) {
        if (!(this.menu.owner instanceof ServerPlayer serverPlayer)
                || action == null
                || selectionEntries == null
                || selectionEntries.isEmpty()) {
            return;
        }
        boolean changed = false;
        boolean personalChanged = false;
        boolean publicChanged = false;
        Map<DatabaseScope, List<DatabaseSelectionEntry>> entriesByScope = new LinkedHashMap<>();
        for (DatabaseSelectionEntry selectionEntry : selectionEntries) {
            if (selectionEntry == null || selectionEntry.isEmpty()) {
                continue;
            }
            entriesByScope.computeIfAbsent(selectionEntry.scope(), ignored -> new ArrayList<>()).add(selectionEntry);
        }
        if (action.requiresTargetTab()) {
            DatabaseScope normalizedTargetScope = DatabaseScope.normalize(targetScope);
            for (Map.Entry<DatabaseScope, List<DatabaseSelectionEntry>> entry : entriesByScope.entrySet()) {
                boolean scopeChanged = PersonalDatabaseService.INSTANCE.transferSelection(
                        serverPlayer,
                        entry.getKey(),
                        normalizedTargetScope,
                        entry.getValue(),
                        targetTabId
                );
                changed = changed || scopeChanged;
                if (scopeChanged) {
                    personalChanged = personalChanged || entry.getKey() == DatabaseScope.PERSONAL || normalizedTargetScope == DatabaseScope.PERSONAL;
                    publicChanged = publicChanged || entry.getKey() == DatabaseScope.PUBLIC || normalizedTargetScope == DatabaseScope.PUBLIC;
                }
            }
        } else {
            for (Map.Entry<DatabaseScope, List<DatabaseSelectionEntry>> entry : entriesByScope.entrySet()) {
                boolean scopeChanged = PersonalDatabaseService.INSTANCE.extractSelectionToInventory(
                        serverPlayer,
                        entry.getKey(),
                        entry.getValue(),
                        action,
                        requestedAmount
                ) > 0L;
                changed = changed || scopeChanged;
                if (scopeChanged) {
                    personalChanged = personalChanged || entry.getKey() == DatabaseScope.PERSONAL;
                    publicChanged = publicChanged || entry.getKey() == DatabaseScope.PUBLIC;
                }
            }
        }
        if (changed) {
            this.menu.broadcastChanges();
            this.menu.syncAfterScopedMutations(serverPlayer, personalChanged, publicChanged);
        } else if (entriesByScope.containsKey(DatabaseScope.PUBLIC)) {
            PersonalDatabaseService.INSTANCE.syncPublicViewers(serverPlayer.server);
        }
    }
}
