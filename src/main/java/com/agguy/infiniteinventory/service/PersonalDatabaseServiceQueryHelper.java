package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabDirectory;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.database.DatabaseScope;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/**
 * 负责 {@link PersonalDatabaseService#sanitizeQuery} 的查询条件清洗与规范化逻辑。
 *
 * <p>设计意图：将查询净化这一独立职责从服务门面中剥离，降低原类复杂度。
 * 处理逻辑包括：聚焦标签页回退、可见标签页截断、确保聚焦页在可见集合中、
 * 重建所有标签页的查询状态映射。</p>
 *
 * <p>本类为包级可见，仅由 PersonalDatabaseService 委托调用。</p>
 */
final class PersonalDatabaseServiceQueryHelper {

    private PersonalDatabaseServiceQueryHelper() {
    }

    /**
     * 清洗并规范化玩家提交的查询条件，防止非法标签页或越界可见标签页数量。
     *
     * @param service 服务实例，用于解析标签页目录与作用域
     * @param player  提交查询的玩家
     * @param query   原始查询条件；可为 null
     * @return 经校验后的安全查询条件
     */
    static DatabaseQuery sanitizeQuery(PersonalDatabaseService service, net.minecraft.server.level.ServerPlayer player, DatabaseQuery query) {
        DatabaseQuery normalizedQuery = query == null ? DatabaseQuery.defaultQuery() : query;
        DatabaseScopedTabRef focusedTab = resolveScopedTabForView(service, player, normalizedQuery.focusedTab());
        LinkedHashSet<DatabaseScopedTabRef> visibleTabs = new LinkedHashSet<>();
        for (DatabaseScopedTabRef visibleTab : normalizedQuery.visibleTabs()) {
            DatabaseScopedTabRef resolvedVisibleTab = resolveScopedTabForView(service, player, visibleTab);
            if (resolvedVisibleTab == null) {
                continue;
            }
            visibleTabs.add(resolvedVisibleTab);
            if (visibleTabs.size() >= DatabaseTabs.MAX_VISIBLE_TAB_COUNT) {
                break;
            }
        }
        if (focusedTab == null) {
            focusedTab = DatabaseScopedTabRef.defaultTab();
        }
        if (visibleTabs.isEmpty()) {
            visibleTabs.add(focusedTab);
        }
        if (!visibleTabs.contains(focusedTab)) {
            focusedTab = visibleTabs.getFirst();
        }
        LinkedHashMap<DatabaseScopedTabRef, com.agguy.infiniteinventory.database.DatabaseTabQueryState> tabStates =
                new LinkedHashMap<>();
        for (DatabaseTab tab : service.tabsForScope(player, DatabaseScope.PERSONAL)) {
            DatabaseScopedTabRef scopedTab = DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, tab.id());
            tabStates.put(scopedTab, normalizedQuery.tabStateFor(scopedTab));
        }
        for (DatabaseTab tab : service.tabsForScope(player, DatabaseScope.PUBLIC)) {
            DatabaseScopedTabRef scopedTab = DatabaseScopedTabRef.concreteTab(DatabaseScope.PUBLIC, tab.id());
            tabStates.put(scopedTab, normalizedQuery.tabStateFor(scopedTab));
        }
        return new DatabaseQuery(focusedTab, java.util.List.copyOf(visibleTabs), tabStates, normalizedQuery.hiddenTopTabs());
    }

    private static DatabaseScopedTabRef resolveScopedTabForView(PersonalDatabaseService service, net.minecraft.server.level.ServerPlayer player, DatabaseScopedTabRef scopedTab) {
        DatabaseScopedTabRef normalizedScopedTab = scopedTab == null ? DatabaseScopedTabRef.defaultTab() : scopedTab;
        DatabaseTabDirectory tabDirectory = service.resolveTabsForView(player, normalizedScopedTab.scope());
        String resolvedVisibleTabId = tabDirectory.resolveVisibleTabId(normalizedScopedTab.tabId());
        if (resolvedVisibleTabId == null) {
            return DatabaseScopedTabRef.allTab(normalizedScopedTab.scope());
        }
        return DatabaseScopedTabRef.concreteTab(normalizedScopedTab.scope(), resolvedVisibleTabId);
    }
}
