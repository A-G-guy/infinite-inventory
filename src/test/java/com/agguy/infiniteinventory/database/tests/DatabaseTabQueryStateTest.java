package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseSearchConfig;
import com.agguy.infiniteinventory.database.DatabaseSearchField;
import com.agguy.infiniteinventory.database.DatabaseSearchWeight;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import com.agguy.infiniteinventory.database.DatabaseTabQueryState;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DatabaseTabQueryStateTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void shouldApplyCompactConstructorDefaults() {
        DatabaseTabQueryState state = new DatabaseTabQueryState(null, null, null, -1, 0);

        assertEquals(DatabaseSortOption.RECENTLY_CHANGED, state.sortOption());
        assertEquals("", state.searchText());
        assertNotNull(state.searchConfig());
        assertEquals(0, state.pageIndex());
        assertEquals(1, state.pageSize());
    }

    @Test
    void shouldTruncateLongSearchText() {
        String longText = "a".repeat(DatabaseQuery.MAX_SEARCH_LENGTH + 10);
        DatabaseTabQueryState state = new DatabaseTabQueryState(
                DatabaseSortOption.RECENTLY_CHANGED, longText, DatabaseSearchConfig.defaultConfig(), 0, 20);

        assertEquals(DatabaseQuery.MAX_SEARCH_LENGTH, state.searchText().length());
    }

    @Test
    void shouldTrimSearchText() {
        DatabaseTabQueryState state = new DatabaseTabQueryState(
                DatabaseSortOption.RECENTLY_CHANGED, "  hello  ", DatabaseSearchConfig.defaultConfig(), 0, 20);

        assertEquals("hello", state.searchText());
    }

    @Test
    void shouldClampPageSizeToMax() {
        DatabaseTabQueryState state = new DatabaseTabQueryState(
                DatabaseSortOption.RECENTLY_CHANGED, "", DatabaseSearchConfig.defaultConfig(), 0,
                DatabaseQuery.MAX_PAGE_SIZE + 100);

        assertEquals(DatabaseQuery.MAX_PAGE_SIZE, state.pageSize());
    }

    @Test
    void shouldClampNegativePageIndex() {
        DatabaseTabQueryState state = new DatabaseTabQueryState(
                DatabaseSortOption.RECENTLY_CHANGED, "", DatabaseSearchConfig.defaultConfig(), -5, 20);

        assertEquals(0, state.pageIndex());
    }

    @Test
    void defaultStateShouldHaveSensibleDefaults() {
        DatabaseTabQueryState state = DatabaseTabQueryState.defaultState();

        assertEquals(DatabaseSortOption.RECENTLY_CHANGED, state.sortOption());
        assertEquals("", state.searchText());
        assertEquals(0, state.pageIndex());
        assertEquals(DatabaseQuery.DEFAULT_PAGE_SIZE, state.pageSize());
    }

    @Test
    void withSortOptionShouldPreserveOtherFields() {
        DatabaseTabQueryState state = DatabaseTabQueryState.defaultState();
        DatabaseTabQueryState updated = state.withSortOption(DatabaseSortOption.NAME_ASC);

        assertEquals(DatabaseSortOption.NAME_ASC, updated.sortOption());
        assertEquals(state.searchText(), updated.searchText());
        assertEquals(state.pageSize(), updated.pageSize());
    }

    @Test
    void withSearchTextShouldResetPageIndex() {
        DatabaseTabQueryState state = new DatabaseTabQueryState(
                DatabaseSortOption.RECENTLY_CHANGED, "old", DatabaseSearchConfig.defaultConfig(), 5, 20);
        DatabaseTabQueryState updated = state.withSearchText("new");

        assertEquals("new", updated.searchText());
        assertEquals(0, updated.pageIndex());
    }

    @Test
    void withSearchConfigShouldResetPageIndex() {
        DatabaseTabQueryState state = new DatabaseTabQueryState(
                DatabaseSortOption.RECENTLY_CHANGED, "text", DatabaseSearchConfig.defaultConfig(), 5, 20);
        DatabaseTabQueryState updated = state.withSearchConfig(DatabaseSearchConfig.defaultConfig());

        assertEquals(0, updated.pageIndex());
    }

    @Test
    void withPageIndexShouldClampNegativeValue() {
        DatabaseTabQueryState state = DatabaseTabQueryState.defaultState();
        DatabaseTabQueryState updated = state.withPageIndex(-10);

        assertEquals(0, updated.pageIndex());
    }

    @Test
    void withPageSizeShouldRespectMaxBound() {
        DatabaseTabQueryState state = DatabaseTabQueryState.defaultState();
        DatabaseTabQueryState updated = state.withPageSize(DatabaseQuery.MAX_PAGE_SIZE + 50);

        assertEquals(DatabaseQuery.MAX_PAGE_SIZE, updated.pageSize());
    }

    @Test
    void toTagShouldRoundTrip() {
        DatabaseTabQueryState state = new DatabaseTabQueryState(
                DatabaseSortOption.COUNT_DESC, "diamond", DatabaseSearchConfig.defaultConfig(), 3, 25);
        CompoundTag tag = state.toTag();

        DatabaseTabQueryState restored = DatabaseTabQueryState.fromTag(tag);

        assertEquals(state.sortOption(), restored.sortOption());
        assertEquals(state.searchText(), restored.searchText());
        assertEquals(state.pageIndex(), restored.pageIndex());
        assertEquals(state.pageSize(), restored.pageSize());
    }

    @Test
    void fromTagShouldHandleNullTag() {
        DatabaseTabQueryState state = DatabaseTabQueryState.fromTag(null);

        assertEquals(DatabaseSortOption.RECENTLY_CHANGED, state.sortOption());
        assertEquals(0, state.pageIndex());
    }

    @Test
    void fromTagShouldHandleEmptyTag() {
        DatabaseTabQueryState state = DatabaseTabQueryState.fromTag(new CompoundTag());

        assertEquals(DatabaseSortOption.RECENTLY_CHANGED, state.sortOption());
    }

    @Test
    void fromTagShouldHandleInvalidEnumValue() {
        CompoundTag tag = new CompoundTag();
        tag.putString("sort_option", "NONEXISTENT");
        tag.putString("search_text", "test");
        tag.putInt("page_index", 2);
        tag.putInt("page_size", 20);
        tag.put("search_config", DatabaseSearchConfig.defaultConfig().toTag());

        DatabaseTabQueryState state = DatabaseTabQueryState.fromTag(tag);

        assertEquals(DatabaseSortOption.RECENTLY_CHANGED, state.sortOption());
        assertEquals("test", state.searchText());
    }

    @Test
    void shouldResolveSortOptionExternally() {
        DatabaseSortOption option = DatabaseSortOption.valueOf("COUNT_DESC");

        assertEquals("screen.infiniteinventory.sort.count_desc", option.translationKey());
    }
}
