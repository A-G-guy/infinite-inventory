package com.agguy.infiniteinventory.service.tests;

import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseSearchConfig;
import com.agguy.infiniteinventory.database.DatabaseSearchField;
import com.agguy.infiniteinventory.database.DatabaseSearchWeight;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseQueryTest {
    @Test
    void queryShouldTrimAndClampInputs() {
        DatabaseQuery query = this.query(DatabaseScope.PUBLIC, DatabaseTabs.ALL_TAB_ID, DatabaseSortOption.NAME_ASC, "  diamonds  ", DatabaseSearchConfig.defaultConfig(), -3, Integer.MAX_VALUE);

        assertEquals("diamonds", query.searchText());
        assertEquals(0, query.pageIndex());
        assertEquals(DatabaseQuery.MAX_PAGE_SIZE, query.pageSize());
        assertEquals(DatabaseScope.PUBLIC, query.scope());
        assertEquals(DatabaseTabs.ALL_TAB_ID, query.focusedTabId());
    }

    @Test
    void searchUpdatesShouldResetPageIndexButKeepPageSize() {
        DatabaseQuery query = this.query(DatabaseScope.PERSONAL, DatabaseTabs.DEFAULT_TAB_ID, DatabaseSortOption.COUNT_DESC, "ore", DatabaseSearchConfig.defaultConfig(), 4, 72);

        assertEquals(0, query.withSearchText("stone").pageIndex());
        assertEquals(72, query.withSearchText("stone").pageSize());
        assertEquals(4, query.withSortOption(DatabaseSortOption.NAME_DESC).pageIndex());
        assertEquals(72, query.withSortOption(DatabaseSortOption.NAME_DESC).pageSize());
        assertEquals(DatabaseQuery.MAX_PAGE_SIZE, query.withPageSize(Integer.MAX_VALUE).pageSize());
    }

    @Test
    void defaultQueryShouldUseDefaultPageSize() {
        assertEquals(DatabaseQuery.DEFAULT_PAGE_SIZE, DatabaseQuery.defaultQuery().pageSize());
        assertEquals(DatabaseScope.PERSONAL, DatabaseQuery.defaultQuery().scope());
        assertEquals(List.of(DatabaseTabs.ALL_TAB_ID), DatabaseQuery.defaultQuery().visibleTabIds());
    }

    @Test
    void normalizeForScopeShouldProjectToTargetScopeState() {
        DatabaseQuery query = this.query(DatabaseScope.PERSONAL, DatabaseTabs.DEFAULT_TAB_ID, DatabaseSortOption.COUNT_DESC, "ore", DatabaseSearchConfig.defaultConfig(), 4, 72);
        DatabaseQuery normalized = DatabaseQuery.normalizeForScope(DatabaseScope.PUBLIC, query);

        assertEquals(DatabaseScope.PUBLIC, normalized.scope());
        assertEquals(DatabaseTabs.ALL_TAB_ID, normalized.focusedTabId());
        assertEquals(List.of(DatabaseTabs.ALL_TAB_ID), normalized.visibleTabIds());
        assertEquals(DatabaseSortOption.RECENTLY_CHANGED, normalized.sortOption());
        assertEquals("", normalized.searchText());
        assertEquals(0, normalized.pageIndex());
        assertEquals(DatabaseQuery.DEFAULT_PAGE_SIZE, normalized.pageSize());
    }

    @Test
    void searchConfigUpdatesShouldResetPageIndexButKeepPageSize() {
        DatabaseQuery query = this.query(DatabaseScope.PERSONAL, DatabaseTabs.ALL_TAB_ID, DatabaseSortOption.RECENTLY_CHANGED, "diamond", DatabaseSearchConfig.defaultConfig(), 3, 90);
        DatabaseSearchConfig newConfig = query.searchConfig().withWeight(DatabaseSearchField.ITEM_ID, DatabaseSearchWeight.HIGH);

        DatabaseQuery updatedQuery = query.withSearchConfig(newConfig);

        assertEquals(0, updatedQuery.pageIndex());
        assertEquals(90, updatedQuery.pageSize());
        assertEquals(newConfig, updatedQuery.searchConfig());
    }

    @Test
    void tagAndBufferRoundTripShouldKeepSearchConfig() {
        DatabaseSearchConfig searchConfig = DatabaseSearchConfig.defaultConfig()
                .withWeight(DatabaseSearchField.DISPLAY_NAME, DatabaseSearchWeight.LOW)
                .withWeight(DatabaseSearchField.MOD_NAMESPACE, DatabaseSearchWeight.HIGH)
                .withWeight(DatabaseSearchField.COUNT_BOOST, DatabaseSearchWeight.MEDIUM);
        DatabaseQuery query = this.query(DatabaseScope.PUBLIC, DatabaseTabs.ALL_TAB_ID, DatabaseSortOption.NAME_DESC, "diamond sword", searchConfig, 2, 72);

        assertEquals(query, DatabaseQuery.fromTag(query.toTag(), DatabaseScope.PUBLIC));

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        DatabaseQuery.write(buffer, query);

        assertEquals(query, DatabaseQuery.read(buffer));
    }

    @Test
    void hiddenTopTabsShouldDefaultToEmpty() {
        DatabaseQuery query = this.query(DatabaseScope.PERSONAL, DatabaseTabs.ALL_TAB_ID, DatabaseSortOption.RECENTLY_CHANGED, "", DatabaseSearchConfig.defaultConfig(), 0, DatabaseQuery.DEFAULT_PAGE_SIZE);

        assertEquals(List.of(), query.hiddenTopTabs());
        assertFalse(query.isTopTabHidden(DatabaseScopedTabRef.allTab(DatabaseScope.PERSONAL)));
    }

    @Test
    void withHiddenTopTabToggledShouldAddAndRemove() {
        DatabaseQuery query = this.query(DatabaseScope.PERSONAL, DatabaseTabs.ALL_TAB_ID, DatabaseSortOption.RECENTLY_CHANGED, "", DatabaseSearchConfig.defaultConfig(), 0, DatabaseQuery.DEFAULT_PAGE_SIZE);
        DatabaseScopedTabRef favorites = DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, DatabaseTabs.FAVORITES_TAB_ID);

        DatabaseQuery hidden = query.withHiddenTopTabToggled(favorites);

        assertTrue(hidden.isTopTabHidden(favorites));
        assertEquals(List.of(favorites), hidden.hiddenTopTabs());

        DatabaseQuery shown = hidden.withHiddenTopTabToggled(favorites);

        assertFalse(shown.isTopTabHidden(favorites));
        assertEquals(List.of(), shown.hiddenTopTabs());
    }

    @Test
    void tagAndBufferRoundTripShouldKeepHiddenTopTabs() {
        DatabaseQuery query = this.query(DatabaseScope.PUBLIC, DatabaseTabs.ALL_TAB_ID, DatabaseSortOption.NAME_DESC, "diamond sword", DatabaseSearchConfig.defaultConfig(), 2, 72)
                .withHiddenTopTabToggled(DatabaseScopedTabRef.concreteTab(DatabaseScope.PUBLIC, DatabaseTabs.FAVORITES_TAB_ID))
                .withHiddenTopTabToggled(DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, DatabaseTabs.DEFAULT_TAB_ID));

        assertEquals(query, DatabaseQuery.fromTag(query.toTag(), DatabaseScope.PUBLIC));

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        DatabaseQuery.write(buffer, query);

        assertEquals(query, DatabaseQuery.read(buffer));
    }

    @Test
    void retargetScopeShouldMoveHiddenTopTabs() {
        DatabaseQuery query = this.query(DatabaseScope.PERSONAL, DatabaseTabs.ALL_TAB_ID, DatabaseSortOption.RECENTLY_CHANGED, "", DatabaseSearchConfig.defaultConfig(), 0, DatabaseQuery.DEFAULT_PAGE_SIZE)
                .withHiddenTopTabToggled(DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, DatabaseTabs.FAVORITES_TAB_ID));

        DatabaseQuery retargeted = query.retargetScope(DatabaseScope.PUBLIC);

        assertTrue(retargeted.isTopTabHidden(DatabaseScopedTabRef.concreteTab(DatabaseScope.PUBLIC, DatabaseTabs.FAVORITES_TAB_ID)));
        assertFalse(retargeted.isTopTabHidden(DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, DatabaseTabs.FAVORITES_TAB_ID)));
    }

    @Test
    void queryForScopeShouldFilterHiddenTopTabs() {
        DatabaseQuery query = this.query(DatabaseScope.PERSONAL, DatabaseTabs.ALL_TAB_ID, DatabaseSortOption.RECENTLY_CHANGED, "", DatabaseSearchConfig.defaultConfig(), 0, DatabaseQuery.DEFAULT_PAGE_SIZE)
                .withHiddenTopTabToggled(DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, DatabaseTabs.FAVORITES_TAB_ID))
                .withHiddenTopTabToggled(DatabaseScopedTabRef.concreteTab(DatabaseScope.PUBLIC, DatabaseTabs.DEFAULT_TAB_ID));

        DatabaseQuery personalOnly = query.queryForScope(DatabaseScope.PERSONAL);

        assertTrue(personalOnly.isTopTabHidden(DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, DatabaseTabs.FAVORITES_TAB_ID)));
        assertFalse(personalOnly.isTopTabHidden(DatabaseScopedTabRef.concreteTab(DatabaseScope.PUBLIC, DatabaseTabs.DEFAULT_TAB_ID)));
    }

    private DatabaseQuery query(
            DatabaseScope scope,
            String focusedTabId,
            DatabaseSortOption sortOption,
            String searchText,
            DatabaseSearchConfig searchConfig,
            int pageIndex,
            int pageSize
    ) {
        return new DatabaseQuery(
                scope,
                focusedTabId,
                List.of(focusedTabId),
                Map.of(focusedTabId, pageIndex),
                Map.of(focusedTabId, pageSize),
                sortOption,
                searchText,
                searchConfig
        );
    }
}
