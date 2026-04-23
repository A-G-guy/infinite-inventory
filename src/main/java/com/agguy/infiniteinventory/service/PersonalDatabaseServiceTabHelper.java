package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabaseAutoStoreTarget;
import com.agguy.infiniteinventory.database.DatabaseLogAction;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabDirectory;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.database.DatabaseViewPreferencesAttachment;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackEntry;
import com.agguy.infiniteinventory.database.StoredStackKey;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * 负责 {@link PersonalDatabaseService} 中的标签页生命周期管理。
 * <p>
 * 设计意图：将标签页的创建、重命名、移动、删除、转移等独立职责从服务门面中剥离，
 * 降低原类复杂度，保持单一职责原则。
 * </p>
 * <p>本类为包级可见，仅由 PersonalDatabaseService 委托调用。</p>
 */
final class PersonalDatabaseServiceTabHelper {

    private PersonalDatabaseServiceTabHelper() {
    }

    /**
     * 在指定作用域下新建自定义标签页。
     */
    static boolean createTab(PersonalDatabaseService service, ServerPlayer player, DatabaseScope scope, String name, String iconItemId) {
        DatabaseTabDirectory tabDirectory = service.resolveTabsForMutation(player, scope);
        tabDirectory.addCustomTab(name, iconItemId);
        service.markScopeDirty(player, scope);
        return true;
    }

    /**
     * 重命名指定标签页。
     */
    static boolean renameTab(PersonalDatabaseService service, ServerPlayer player, DatabaseScope scope, String tabId, String name) {
        boolean changed = service.resolveTabsForMutation(player, scope).renameTab(tabId, name);
        if (changed) {
            service.markScopeDirty(player, scope);
        }
        return changed;
    }

    /**
     * 更新指定标签页的图标。
     */
    static boolean updateTabIcon(PersonalDatabaseService service, ServerPlayer player, DatabaseScope scope, String tabId, String iconItemId) {
        boolean changed = service.resolveTabsForMutation(player, scope).updateTabIcon(tabId, iconItemId);
        if (changed) {
            service.markScopeDirty(player, scope);
        }
        return changed;
    }

    /**
     * 在标签页排序中移动指定标签页的位置。
     */
    static boolean moveTab(PersonalDatabaseService service, ServerPlayer player, DatabaseScope scope, String tabId, int direction) {
        boolean changed = service.resolveTabsForMutation(player, scope).moveTab(tabId, direction);
        if (changed) {
            service.markScopeDirty(player, scope);
        }
        return changed;
    }

    /**
     * 删除指定标签页，并将其中的物品转移到另一个目标标签页。
     * <p>
     * 业务约束：
     * <ul>
     *   <li>系统标签页不可删除</li>
     *   <li>若目标标签页无效，将自动解析为默认标签页</li>
     *   <li>删除后会自动修正玩家的自动存储目标</li>
     * </ul>
     */
    static boolean deleteTab(PersonalDatabaseService service, ServerPlayer player, DatabaseScope scope, String tabId, String targetTabId) {
        DatabaseTabDirectory tabDirectory = service.resolveTabsForMutation(player, scope);
        String resolvedTargetTabId = tabDirectory.sanitizeConcreteTarget(targetTabId);
        if (!tabDirectory.find(tabId).map(DatabaseTab::canDelete).orElse(false)) {
            return false;
        }
        StoredItemDatabase database = service.resolveDatabaseForMutation(player, scope);
        String normalizedTabId = DatabaseTabs.normalizeConcreteTarget(tabId);
        Map<StoredStackKey, Long> entriesToDelete = new LinkedHashMap<>();
        for (Map.Entry<StoredStackKey, StoredStackEntry> entry : database.entries().entrySet()) {
            if (entry.getValue().tabId().equals(normalizedTabId)) {
                entriesToDelete.put(entry.getKey(), entry.getValue().amount());
            }
        }
        boolean databaseChanged = database.transferTab(tabId, resolvedTargetTabId);
        boolean directoryChanged = tabDirectory.deleteTab(tabId);
        DatabaseScope normalizedScope = DatabaseScope.normalize(scope);
        DatabaseViewPreferencesAttachment preferences = service.getViewPreferences(player);
        DatabaseAutoStoreTarget autoStoreTarget = preferences.autoStoreTarget();
        if (autoStoreTarget.scope() == normalizedScope && autoStoreTarget.tabId().equals(tabId)) {
            preferences.setAutoStoreTarget(new DatabaseAutoStoreTarget(normalizedScope, resolvedTargetTabId));
        }
        if (databaseChanged || directoryChanged) {
            service.markScopeDirty(player, scope);
            for (Map.Entry<StoredStackKey, Long> entry : entriesToDelete.entrySet()) {
                PersonalDatabaseServiceLogHelper.recordLog(service, player, scope, DatabaseLogAction.DELETE,
                        entry.getKey().displayStack(), entry.getValue(), normalizedTabId, resolvedTargetTabId, null);
            }
        }
        return databaseChanged || directoryChanged;
    }

    /**
     * 切换指定顶部标签页的可见性（显示/隐藏）。
     */
    static boolean toggleTopTabVisibility(PersonalDatabaseService service, ServerPlayer player, DatabaseScope scope, String tabId) {
        DatabaseViewPreferencesAttachment preferences = service.getViewPreferences(player);
        com.agguy.infiniteinventory.database.DatabaseQuery currentQuery = preferences.query();
        com.agguy.infiniteinventory.database.DatabaseScopedTabRef scopedTab =
                com.agguy.infiniteinventory.database.DatabaseScopedTabRef.concreteTab(DatabaseScope.normalize(scope), tabId);
        com.agguy.infiniteinventory.database.DatabaseQuery updatedQuery = currentQuery.withHiddenTopTabToggled(scopedTab);
        preferences.setQuery(updatedQuery);
        return !updatedQuery.hiddenTopTabs().equals(currentQuery.hiddenTopTabs());
    }

    /**
     * 将请求的目标标签页标识解析为实际存在的具体标签页标识。
     */
    static String resolveConcreteTargetTabId(PersonalDatabaseService service, ServerPlayer player, DatabaseScope scope, String requestedTabId) {
        return service.resolveTabsForMutation(player, scope).sanitizeConcreteTarget(requestedTabId);
    }
}
