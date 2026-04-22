package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseAutoStoreTarget;
import com.agguy.infiniteinventory.database.DatabaseEnhancementConfig;
import com.agguy.infiniteinventory.database.DatabaseEnhancementOption;
import com.agguy.infiniteinventory.database.DatabaseAutoStoreTarget;
import com.agguy.infiniteinventory.database.DatabaseEnhancementConfig;
import com.agguy.infiniteinventory.database.DatabaseEnhancementOption;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseSearchConfig;
import com.agguy.infiniteinventory.database.DatabaseSearchField;
import com.agguy.infiniteinventory.database.DatabaseSearchWeight;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.database.DatabaseViewPreferencesAttachment;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseViewPreferencesAttachmentTest {
    @Test
    void shouldPersistQueriesPerScopeAndLastScope() {
        DatabaseViewPreferencesAttachment preferences = new DatabaseViewPreferencesAttachment();
        DatabaseSearchConfig personalConfig = DatabaseSearchConfig.defaultConfig()
                .withWeight(DatabaseSearchField.PINYIN, DatabaseSearchWeight.LOW);
        DatabaseSearchConfig publicConfig = DatabaseSearchConfig.defaultConfig()
                .withWeight(DatabaseSearchField.ITEM_ID, DatabaseSearchWeight.HIGH)
                .withWeight(DatabaseSearchField.COUNT_BOOST, DatabaseSearchWeight.HIGH);
        DatabaseQuery personalQuery = new DatabaseQuery(
                DatabaseScope.PERSONAL,
                DatabaseTabs.DEFAULT_TAB_ID,
                List.of(DatabaseTabs.DEFAULT_TAB_ID),
                Map.of(DatabaseTabs.DEFAULT_TAB_ID, 2),
                Map.of(DatabaseTabs.DEFAULT_TAB_ID, 81),
                DatabaseSortOption.NAME_ASC,
                "iron",
                personalConfig
        );
        DatabaseQuery publicQuery = new DatabaseQuery(
                DatabaseScope.PUBLIC,
                DatabaseTabs.ALL_TAB_ID,
                List.of(DatabaseTabs.ALL_TAB_ID),
                Map.of(DatabaseTabs.ALL_TAB_ID, 1),
                Map.of(DatabaseTabs.ALL_TAB_ID, 96),
                DatabaseSortOption.COUNT_DESC,
                "stone",
                publicConfig
        );
        DatabaseEnhancementConfig enhancementConfig = DatabaseEnhancementConfig.defaultConfig()
                .withOption(DatabaseEnhancementOption.AUTO_STORE_PICKED_UP_ITEMS, true);
        preferences.setQuery(DatabaseScope.PERSONAL, personalQuery);
        preferences.setQuery(DatabaseScope.PUBLIC, publicQuery);
        preferences.setLastScope(DatabaseScope.PUBLIC);
        preferences.setEnhancementConfig(enhancementConfig);
        preferences.setAutoStoreTarget(new DatabaseAutoStoreTarget(DatabaseScope.PUBLIC, "public_blocks"));

        DatabaseViewPreferencesAttachment restored = new DatabaseViewPreferencesAttachment();
        restored.deserializeNBT(null, preferences.serializeNBT(null));

        assertEquals(DatabaseScope.PUBLIC, restored.lastScope());
        assertEquals(personalQuery, restored.queryFor(DatabaseScope.PERSONAL));
        assertEquals(publicQuery, restored.queryFor(DatabaseScope.PUBLIC));
        assertEquals(enhancementConfig, restored.enhancementConfig());
        assertEquals(new DatabaseAutoStoreTarget(DatabaseScope.PUBLIC, "public_blocks"), restored.autoStoreTarget());
    }

    @Test
    void setQueryShouldMergeHiddenTopTabsPerScope() {
        DatabaseViewPreferencesAttachment preferences = new DatabaseViewPreferencesAttachment();
        DatabaseQuery personalQuery = new DatabaseQuery(
                DatabaseScope.PERSONAL,
                DatabaseTabs.ALL_TAB_ID,
                List.of(DatabaseTabs.ALL_TAB_ID),
                Map.of(DatabaseTabs.ALL_TAB_ID, 0),
                Map.of(DatabaseTabs.ALL_TAB_ID, DatabaseQuery.DEFAULT_PAGE_SIZE),
                DatabaseSortOption.RECENTLY_CHANGED,
                "",
                DatabaseSearchConfig.defaultConfig()
        ).withHiddenTopTabToggled(DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, DatabaseTabs.FAVORITES_TAB_ID));
        DatabaseQuery publicQuery = new DatabaseQuery(
                DatabaseScope.PUBLIC,
                DatabaseTabs.ALL_TAB_ID,
                List.of(DatabaseTabs.ALL_TAB_ID),
                Map.of(DatabaseTabs.ALL_TAB_ID, 0),
                Map.of(DatabaseTabs.ALL_TAB_ID, DatabaseQuery.DEFAULT_PAGE_SIZE),
                DatabaseSortOption.RECENTLY_CHANGED,
                "",
                DatabaseSearchConfig.defaultConfig()
        ).withHiddenTopTabToggled(DatabaseScopedTabRef.concreteTab(DatabaseScope.PUBLIC, DatabaseTabs.DEFAULT_TAB_ID));

        preferences.setQuery(DatabaseScope.PERSONAL, personalQuery);
        preferences.setQuery(DatabaseScope.PUBLIC, publicQuery);

        DatabaseQuery mergedQuery = preferences.query();
        assertTrue(mergedQuery.isTopTabHidden(DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, DatabaseTabs.FAVORITES_TAB_ID)));
        assertTrue(mergedQuery.isTopTabHidden(DatabaseScopedTabRef.concreteTab(DatabaseScope.PUBLIC, DatabaseTabs.DEFAULT_TAB_ID)));
    }

    @Test
    void serializeShouldPreserveHiddenTopTabs() {
        DatabaseViewPreferencesAttachment preferences = new DatabaseViewPreferencesAttachment();
        DatabaseQuery query = new DatabaseQuery(
                DatabaseScope.PERSONAL,
                DatabaseTabs.ALL_TAB_ID,
                List.of(DatabaseTabs.ALL_TAB_ID),
                Map.of(DatabaseTabs.ALL_TAB_ID, 0),
                Map.of(DatabaseTabs.ALL_TAB_ID, DatabaseQuery.DEFAULT_PAGE_SIZE),
                DatabaseSortOption.RECENTLY_CHANGED,
                "",
                DatabaseSearchConfig.defaultConfig()
        ).withHiddenTopTabToggled(DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, DatabaseTabs.FAVORITES_TAB_ID));
        preferences.setQuery(query);

        DatabaseViewPreferencesAttachment restored = new DatabaseViewPreferencesAttachment();
        restored.deserializeNBT(null, preferences.serializeNBT(null));

        assertTrue(restored.query().isTopTabHidden(DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, DatabaseTabs.FAVORITES_TAB_ID)));
    }
}
