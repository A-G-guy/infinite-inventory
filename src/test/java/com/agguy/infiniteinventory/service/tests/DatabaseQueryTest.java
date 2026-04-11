package com.agguy.infiniteinventory.service.tests;

import com.agguy.infiniteinventory.database.DatabaseCategory;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseSearchConfig;
import com.agguy.infiniteinventory.database.DatabaseSearchField;
import com.agguy.infiniteinventory.database.DatabaseSearchWeight;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import net.minecraft.network.FriendlyByteBuf;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseQueryTest {
    @Test
    void queryShouldTrimAndClampInputs() {
        DatabaseQuery query = new DatabaseQuery(DatabaseScope.PUBLIC, DatabaseCategory.ALL, DatabaseSortOption.NAME_ASC, "  diamonds  ", -3, 0);

        assertEquals("diamonds", query.searchText());
        assertEquals(0, query.pageIndex());
        assertEquals(1, query.pageSize());
        assertEquals(DatabaseScope.PUBLIC, query.scope());
    }

    @Test
    void categoryAndSearchUpdatesShouldResetPageIndexButKeepPageSize() {
        DatabaseQuery query = new DatabaseQuery(DatabaseScope.PERSONAL, DatabaseCategory.MATERIALS, DatabaseSortOption.COUNT_DESC, "ore", 4, 72);

        assertEquals(0, query.withCategory(DatabaseCategory.BLOCKS).pageIndex());
        assertEquals(72, query.withCategory(DatabaseCategory.BLOCKS).pageSize());
        assertEquals(0, query.withSearchText("stone").pageIndex());
        assertEquals(72, query.withSearchText("stone").pageSize());
        assertEquals(4, query.withSortOption(DatabaseSortOption.NAME_DESC).pageIndex());
        assertEquals(72, query.withSortOption(DatabaseSortOption.NAME_DESC).pageSize());
        assertEquals(90, query.withPageSize(90).pageSize());
    }

    @Test
    void defaultQueryShouldUseDefaultPageSize() {
        assertEquals(DatabaseQuery.DEFAULT_PAGE_SIZE, DatabaseQuery.defaultQuery().pageSize());
        assertEquals(DatabaseScope.PERSONAL, DatabaseQuery.defaultQuery().scope());
    }

    @Test
    void normalizeForScopeShouldRetargetStoredQueryWithoutDroppingFilters() {
        DatabaseQuery query = new DatabaseQuery(DatabaseScope.PERSONAL, DatabaseCategory.MATERIALS, DatabaseSortOption.COUNT_DESC, "ore", 4, 72);
        DatabaseQuery normalized = DatabaseQuery.normalizeForScope(DatabaseScope.PUBLIC, query);

        assertEquals(DatabaseScope.PUBLIC, normalized.scope());
        assertEquals(DatabaseCategory.MATERIALS, normalized.category());
        assertEquals(DatabaseSortOption.COUNT_DESC, normalized.sortOption());
        assertEquals("ore", normalized.searchText());
        assertEquals(4, normalized.pageIndex());
        assertEquals(72, normalized.pageSize());
    }

    @Test
    void searchConfigUpdatesShouldResetPageIndexButKeepPageSize() {
        DatabaseQuery query = new DatabaseQuery(DatabaseScope.PERSONAL, DatabaseCategory.ALL, DatabaseSortOption.RECENTLY_CHANGED, "diamond", 3, 90);
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
        DatabaseQuery query = new DatabaseQuery(
                DatabaseScope.PUBLIC,
                DatabaseCategory.BLOCKS,
                DatabaseSortOption.NAME_DESC,
                "diamond sword",
                searchConfig,
                2,
                72
        );

        assertEquals(query, DatabaseQuery.fromTag(query.toTag(), DatabaseScope.PUBLIC));

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        DatabaseQuery.write(buffer, query);

        assertEquals(query, DatabaseQuery.read(buffer));
    }
}
