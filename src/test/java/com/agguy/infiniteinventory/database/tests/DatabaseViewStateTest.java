package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseAutoStoreTarget;
import com.agguy.infiniteinventory.database.DatabaseEnhancementConfig;
import com.agguy.infiniteinventory.database.DatabaseEnhancementOption;
import com.agguy.infiniteinventory.database.JeiCraftingTabSourceConfig;
import com.agguy.infiniteinventory.database.DatabasePanelView;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.database.DatabaseViewState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseViewStateTest {
    @Test
    void activeQueryShouldOverrideMatchingStoredScopeQuery() {
        long sessionId = 42L;
        DatabaseQuery personalQuery = new DatabaseQuery(
                DatabaseScope.PERSONAL,
                DatabaseTabs.DEFAULT_TAB_ID,
                List.of(DatabaseTabs.DEFAULT_TAB_ID),
                Map.of(DatabaseTabs.DEFAULT_TAB_ID, 2),
                Map.of(DatabaseTabs.DEFAULT_TAB_ID, 81),
                DatabaseSortOption.NAME_ASC,
                "stone",
                com.agguy.infiniteinventory.database.DatabaseSearchConfig.defaultConfig()
        );
        DatabaseQuery publicQuery = new DatabaseQuery(
                DatabaseScope.PUBLIC,
                DatabaseTabs.ALL_TAB_ID,
                List.of(DatabaseTabs.ALL_TAB_ID),
                Map.of(DatabaseTabs.ALL_TAB_ID, 1),
                Map.of(DatabaseTabs.ALL_TAB_ID, 96),
                DatabaseSortOption.COUNT_DESC,
                "iron",
                com.agguy.infiniteinventory.database.DatabaseSearchConfig.defaultConfig()
        );
        DatabaseQuery activePublicQuery = publicQuery.withSearchText("gold");
        DatabaseEnhancementConfig enhancementConfig = DatabaseEnhancementConfig.defaultConfig()
                .withOption(DatabaseEnhancementOption.AUTO_STORE_PICKED_UP_ITEMS, true);
        List<DatabaseTab> publicTabs = List.of(DatabaseTabs.allTab(), DatabaseTabs.defaultConcreteTab());
        List<DatabasePanelView> panels = List.of(
                new DatabasePanelView(DatabaseScopedTabRef.allTab(DatabaseScope.PUBLIC), DatabaseTabs.allTab(), 1, 96, 5, 2, 64L, List.of()),
                new DatabasePanelView(
                        DatabaseScopedTabRef.concreteTab(DatabaseScope.PUBLIC, DatabaseTabs.DEFAULT_TAB_ID),
                        DatabaseTabs.defaultConcreteTab(),
                        0,
                        54,
                        0,
                        1,
                        0L,
                        List.of()
                )
        );

        DatabaseViewState viewState = new DatabaseViewState(
                3,
                sessionId,
                activePublicQuery,
                enhancementConfig,
                new DatabaseAutoStoreTarget(DatabaseScope.PUBLIC, DatabaseTabs.DEFAULT_TAB_ID),
                JeiCraftingTabSourceConfig.allEnabled(),
                List.of(DatabaseTabs.allTab(), DatabaseTabs.defaultConcreteTab()),
                publicTabs,
                panels
        );

        assertEquals(activePublicQuery, viewState.query());
        assertEquals(sessionId, viewState.sessionId());
        assertEquals(DatabaseQuery.normalizeForScope(DatabaseScope.PERSONAL, activePublicQuery), viewState.queryForScope(DatabaseScope.PERSONAL));
        assertEquals(activePublicQuery, viewState.queryForScope(DatabaseScope.PUBLIC));
        assertEquals(enhancementConfig, viewState.enhancementConfig());
        assertEquals(new DatabaseAutoStoreTarget(DatabaseScope.PUBLIC, DatabaseTabs.DEFAULT_TAB_ID), viewState.autoStoreTarget());
        assertEquals(5, viewState.totalEntries());
    }

    @Test
    void emptyStateShouldRetainProvidedSessionId() {
        DatabaseViewState viewState = DatabaseViewState.empty(5, 99L, DatabaseQuery.defaultQuery(DatabaseScope.PUBLIC));

        assertEquals(5, viewState.containerId());
        assertEquals(99L, viewState.sessionId());
        assertEquals(DatabaseScope.PUBLIC, viewState.query().scope());
        assertEquals(DatabaseEnhancementConfig.defaultConfig(), viewState.enhancementConfig());
    }
}
