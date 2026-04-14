package com.agguy.infiniteinventory.database;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

public record DatabaseQuery(
        DatabaseScope scope,
        String focusedTabId,
        List<String> visibleTabIds,
        Map<String, Integer> pageIndexes,
        Map<String, Integer> pageSizes,
        DatabaseSortOption sortOption,
        String searchText,
        DatabaseSearchConfig searchConfig
) {
    private static final String SCOPE_KEY = "scope";
    private static final String FOCUSED_TAB_ID_KEY = "focused_tab_id";
    private static final String VISIBLE_TABS_KEY = "visible_tabs";
    private static final String PAGE_INDEXES_KEY = "page_indexes";
    private static final String PAGE_SIZES_KEY = "page_sizes";
    private static final String SORT_OPTION_KEY = "sort_option";
    private static final String SEARCH_TEXT_KEY = "search_text";
    private static final String SEARCH_CONFIG_KEY = "search_config";
    private static final String VISIBLE_TAB_ID_KEY = "tab_id";
    private static final String PAGE_VALUE_KEY = "value";

    public static final int MAX_SEARCH_LENGTH = 64;
    public static final int DEFAULT_PAGE_SIZE = 54;
    public static final int MAX_PAGE_SIZE = 640;
    public static final int MAX_TAB_ID_LENGTH = 64;

    public DatabaseQuery {
        scope = DatabaseScope.normalize(scope);
        focusedTabId = normalizeTabId(focusedTabId, DatabaseTabs.ALL_TAB_ID);
        visibleTabIds = normalizeVisibleTabIds(visibleTabIds, focusedTabId);
        pageIndexes = normalizePageValues(pageIndexes, 0);
        pageSizes = normalizePageValues(pageSizes, DEFAULT_PAGE_SIZE);
        sortOption = sortOption == null ? DatabaseSortOption.RECENTLY_CHANGED : sortOption;
        searchText = normalize(searchText);
        searchConfig = searchConfig == null ? DatabaseSearchConfig.defaultConfig() : searchConfig;
    }

    public static DatabaseQuery defaultQuery() {
        return defaultQuery(DatabaseScope.defaultScope());
    }

    public static DatabaseQuery defaultQuery(DatabaseScope scope) {
        return new DatabaseQuery(
                scope,
                DatabaseTabs.ALL_TAB_ID,
                List.of(DatabaseTabs.ALL_TAB_ID),
                Map.of(DatabaseTabs.ALL_TAB_ID, 0),
                Map.of(DatabaseTabs.ALL_TAB_ID, DEFAULT_PAGE_SIZE),
                DatabaseSortOption.RECENTLY_CHANGED,
                "",
                DatabaseSearchConfig.defaultConfig()
        );
    }

    public static DatabaseQuery normalizeForScope(DatabaseScope scope, DatabaseQuery query) {
        DatabaseScope normalizedScope = DatabaseScope.normalize(scope);
        if (query == null) {
            return defaultQuery(normalizedScope);
        }
        return query.scope() == normalizedScope ? query : query.withScope(normalizedScope);
    }

    public int pageIndexFor(String tabId) {
        return this.pageIndexes.getOrDefault(normalizeTabId(tabId, this.focusedTabId), 0);
    }

    public int pageSizeFor(String tabId) {
        return this.pageSizes.getOrDefault(normalizeTabId(tabId, this.focusedTabId), DEFAULT_PAGE_SIZE);
    }

    public int pageIndex() {
        return this.pageIndexFor(this.focusedTabId);
    }

    public int pageSize() {
        return this.pageSizeFor(this.focusedTabId);
    }

    public boolean isMultiTabView() {
        return this.visibleTabIds.size() > 1;
    }

    public DatabaseQuery withFocusedTabId(String newFocusedTabId) {
        String normalizedFocusedTabId = normalizeTabId(newFocusedTabId, DatabaseTabs.ALL_TAB_ID);
        java.util.ArrayList<String> nextVisibleTabs = new java.util.ArrayList<>(this.visibleTabIds);
        if (!nextVisibleTabs.contains(normalizedFocusedTabId)) {
            nextVisibleTabs.clear();
            nextVisibleTabs.add(normalizedFocusedTabId);
        }
        return new DatabaseQuery(
                this.scope,
                normalizedFocusedTabId,
                nextVisibleTabs,
                this.pageIndexes,
                this.pageSizes,
                this.sortOption,
                this.searchText,
                this.searchConfig
        );
    }

    public DatabaseQuery withSingleVisibleTab(String tabId) {
        String normalizedTabId = normalizeTabId(tabId, DatabaseTabs.ALL_TAB_ID);
        return new DatabaseQuery(
                this.scope,
                normalizedTabId,
                List.of(normalizedTabId),
                this.pageIndexes,
                this.pageSizes,
                this.sortOption,
                this.searchText,
                this.searchConfig
        );
    }

    public DatabaseQuery withVisibleTabIds(List<String> newVisibleTabIds) {
        List<String> normalizedVisibleTabIds = normalizeVisibleTabIds(newVisibleTabIds, this.focusedTabId);
        String normalizedFocusedTabId = normalizedVisibleTabIds.contains(this.focusedTabId)
                ? this.focusedTabId
                : normalizedVisibleTabIds.getFirst();
        return new DatabaseQuery(
                this.scope,
                normalizedFocusedTabId,
                normalizedVisibleTabIds,
                this.pageIndexes,
                this.pageSizes,
                this.sortOption,
                this.searchText,
                this.searchConfig
        );
    }

    public DatabaseQuery withSortOption(DatabaseSortOption newSortOption) {
        return new DatabaseQuery(
                this.scope,
                this.focusedTabId,
                this.visibleTabIds,
                this.pageIndexes,
                this.pageSizes,
                newSortOption,
                this.searchText,
                this.searchConfig
        );
    }

    public DatabaseQuery withSearchText(String newSearchText) {
        return new DatabaseQuery(
                this.scope,
                this.focusedTabId,
                this.visibleTabIds,
                resetPageIndexes(this.pageIndexes),
                this.pageSizes,
                this.sortOption,
                newSearchText,
                this.searchConfig
        );
    }

    public DatabaseQuery withSearchConfig(DatabaseSearchConfig newSearchConfig) {
        return new DatabaseQuery(
                this.scope,
                this.focusedTabId,
                this.visibleTabIds,
                resetPageIndexes(this.pageIndexes),
                this.pageSizes,
                this.sortOption,
                this.searchText,
                newSearchConfig,
                true
        );
    }

    private DatabaseQuery(
            DatabaseScope scope,
            String focusedTabId,
            List<String> visibleTabIds,
            Map<String, Integer> pageIndexes,
            Map<String, Integer> pageSizes,
            DatabaseSortOption sortOption,
            String searchText,
            DatabaseSearchConfig searchConfig,
            boolean ignored
    ) {
        this(scope, focusedTabId, visibleTabIds, pageIndexes, pageSizes, sortOption, searchText, searchConfig);
    }

    public DatabaseQuery withPageIndex(String tabId, int newPageIndex) {
        LinkedHashMap<String, Integer> nextPageIndexes = new LinkedHashMap<>(this.pageIndexes);
        nextPageIndexes.put(normalizeTabId(tabId, this.focusedTabId), Math.max(0, newPageIndex));
        return new DatabaseQuery(
                this.scope,
                this.focusedTabId,
                this.visibleTabIds,
                nextPageIndexes,
                this.pageSizes,
                this.sortOption,
                this.searchText,
                this.searchConfig
        );
    }

    public DatabaseQuery withPageIndex(int newPageIndex) {
        return this.withPageIndex(this.focusedTabId, newPageIndex);
    }

    public DatabaseQuery withPageSize(String tabId, int newPageSize) {
        LinkedHashMap<String, Integer> nextPageSizes = new LinkedHashMap<>(this.pageSizes);
        nextPageSizes.put(normalizeTabId(tabId, this.focusedTabId), normalizePageSize(newPageSize));
        return new DatabaseQuery(
                this.scope,
                this.focusedTabId,
                this.visibleTabIds,
                this.pageIndexes,
                nextPageSizes,
                this.sortOption,
                this.searchText,
                this.searchConfig
        );
    }

    public DatabaseQuery withPageSize(int newPageSize) {
        return this.withPageSize(this.focusedTabId, newPageSize);
    }

    public DatabaseQuery withPanelLayout(Map<String, Integer> nextPageIndexes, Map<String, Integer> nextPageSizes) {
        return new DatabaseQuery(
                this.scope,
                this.focusedTabId,
                this.visibleTabIds,
                nextPageIndexes,
                nextPageSizes,
                this.sortOption,
                this.searchText,
                this.searchConfig
        );
    }

    public DatabaseQuery withScope(DatabaseScope newScope) {
        return new DatabaseQuery(
                newScope,
                this.focusedTabId,
                this.visibleTabIds,
                this.pageIndexes,
                this.pageSizes,
                this.sortOption,
                this.searchText,
                this.searchConfig
        );
    }

    public static DatabaseQuery read(FriendlyByteBuf buffer) {
        DatabaseScope scope = buffer.readEnum(DatabaseScope.class);
        String focusedTabId = buffer.readUtf(MAX_TAB_ID_LENGTH);
        int visibleTabCount = buffer.readVarInt();
        java.util.ArrayList<String> visibleTabIds = new java.util.ArrayList<>(visibleTabCount);
        for (int index = 0; index < visibleTabCount; index++) {
            visibleTabIds.add(buffer.readUtf(MAX_TAB_ID_LENGTH));
        }
        Map<String, Integer> pageIndexes = readPageValueMap(buffer);
        Map<String, Integer> pageSizes = readPageValueMap(buffer);
        DatabaseSortOption sortOption = buffer.readEnum(DatabaseSortOption.class);
        String searchText = buffer.readUtf(MAX_SEARCH_LENGTH);
        DatabaseSearchConfig searchConfig = DatabaseSearchConfig.read(buffer);
        return new DatabaseQuery(scope, focusedTabId, visibleTabIds, pageIndexes, pageSizes, sortOption, searchText, searchConfig);
    }

    public static void write(FriendlyByteBuf buffer, DatabaseQuery query) {
        buffer.writeEnum(query.scope());
        buffer.writeUtf(query.focusedTabId(), MAX_TAB_ID_LENGTH);
        buffer.writeVarInt(query.visibleTabIds().size());
        for (String visibleTabId : query.visibleTabIds()) {
            buffer.writeUtf(visibleTabId, MAX_TAB_ID_LENGTH);
        }
        writePageValueMap(buffer, query.pageIndexes());
        writePageValueMap(buffer, query.pageSizes());
        buffer.writeEnum(query.sortOption());
        buffer.writeUtf(query.searchText(), MAX_SEARCH_LENGTH);
        DatabaseSearchConfig.write(buffer, query.searchConfig());
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString(SCOPE_KEY, this.scope.name());
        tag.putString(FOCUSED_TAB_ID_KEY, this.focusedTabId);
        ListTag visibleTabs = new ListTag();
        for (String visibleTabId : this.visibleTabIds) {
            CompoundTag visibleTabTag = new CompoundTag();
            visibleTabTag.putString(VISIBLE_TAB_ID_KEY, visibleTabId);
            visibleTabs.add(visibleTabTag);
        }
        tag.put(VISIBLE_TABS_KEY, visibleTabs);
        tag.put(PAGE_INDEXES_KEY, toPageValueTag(this.pageIndexes));
        tag.put(PAGE_SIZES_KEY, toPageValueTag(this.pageSizes));
        tag.putString(SORT_OPTION_KEY, this.sortOption.name());
        tag.putString(SEARCH_TEXT_KEY, this.searchText);
        tag.put(SEARCH_CONFIG_KEY, this.searchConfig.toTag());
        return tag;
    }

    public static DatabaseQuery fromTag(CompoundTag tag, DatabaseScope fallbackScope) {
        if (tag == null || tag.isEmpty()) {
            return defaultQuery(fallbackScope);
        }
        if (tag.contains(FOCUSED_TAB_ID_KEY) || tag.contains(VISIBLE_TABS_KEY)) {
            return new DatabaseQuery(
                    readEnum(tag.getString(SCOPE_KEY), DatabaseScope.class, DatabaseScope.normalize(fallbackScope)),
                    tag.getString(FOCUSED_TAB_ID_KEY),
                    fromVisibleTabsTag(tag.getList(VISIBLE_TABS_KEY, Tag.TAG_COMPOUND)),
                    fromPageValueTag(tag.getCompound(PAGE_INDEXES_KEY), 0),
                    fromPageValueTag(tag.getCompound(PAGE_SIZES_KEY), DEFAULT_PAGE_SIZE),
                    readEnum(tag.getString(SORT_OPTION_KEY), DatabaseSortOption.class, DatabaseSortOption.RECENTLY_CHANGED),
                    tag.getString(SEARCH_TEXT_KEY),
                    DatabaseSearchConfig.fromTag(tag.getCompound(SEARCH_CONFIG_KEY))
            );
        }

        DatabaseScope scope = readEnum(tag.getString(SCOPE_KEY), DatabaseScope.class, DatabaseScope.normalize(fallbackScope));
        DatabaseCategory legacyCategory = readEnum(tag.getString("category"), DatabaseCategory.class, DatabaseCategory.ALL);
        String focusedTabId = legacyCategory == DatabaseCategory.ALL ? DatabaseTabs.ALL_TAB_ID : DatabaseTabs.DEFAULT_TAB_ID;
        return new DatabaseQuery(
                scope,
                focusedTabId,
                List.of(focusedTabId),
                Map.of(focusedTabId, Math.max(0, tag.getInt("page_index"))),
                Map.of(focusedTabId, normalizePageSize(tag.getInt("page_size"))),
                readEnum(tag.getString(SORT_OPTION_KEY), DatabaseSortOption.class, DatabaseSortOption.RECENTLY_CHANGED),
                tag.getString(SEARCH_TEXT_KEY),
                DatabaseSearchConfig.fromTag(tag.getCompound(SEARCH_CONFIG_KEY))
        );
    }

    private static Map<String, Integer> readPageValueMap(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        LinkedHashMap<String, Integer> values = new LinkedHashMap<>();
        for (int index = 0; index < size; index++) {
            values.put(buffer.readUtf(MAX_TAB_ID_LENGTH), buffer.readVarInt());
        }
        return values;
    }

    private static void writePageValueMap(FriendlyByteBuf buffer, Map<String, Integer> values) {
        buffer.writeVarInt(values.size());
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            buffer.writeUtf(entry.getKey(), MAX_TAB_ID_LENGTH);
            buffer.writeVarInt(entry.getValue());
        }
    }

    private static CompoundTag toPageValueTag(Map<String, Integer> values) {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            tag.putInt(entry.getKey(), entry.getValue());
        }
        return tag;
    }

    private static Map<String, Integer> fromPageValueTag(CompoundTag tag, int fallbackValue) {
        LinkedHashMap<String, Integer> values = new LinkedHashMap<>();
        if (tag == null) {
            return values;
        }
        for (String key : tag.getAllKeys()) {
            values.put(normalizeTabId(key, DatabaseTabs.ALL_TAB_ID), Math.max(0, tag.getInt(key)));
        }
        if (values.isEmpty() && fallbackValue >= 0) {
            values.put(DatabaseTabs.ALL_TAB_ID, fallbackValue);
        }
        return values;
    }

    private static List<String> fromVisibleTabsTag(ListTag tag) {
        java.util.ArrayList<String> visibleTabIds = new java.util.ArrayList<>();
        for (Tag element : tag) {
            if (element instanceof CompoundTag visibleTabTag) {
                visibleTabIds.add(normalizeTabId(visibleTabTag.getString(VISIBLE_TAB_ID_KEY), DatabaseTabs.ALL_TAB_ID));
            }
        }
        return visibleTabIds;
    }

    private static Map<String, Integer> resetPageIndexes(Map<String, Integer> currentPageIndexes) {
        LinkedHashMap<String, Integer> resetPageIndexes = new LinkedHashMap<>();
        for (String tabId : currentPageIndexes.keySet()) {
            resetPageIndexes.put(tabId, 0);
        }
        return resetPageIndexes;
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= MAX_SEARCH_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_SEARCH_LENGTH);
    }

    private static String normalizeTabId(String tabId, String fallback) {
        if (tabId == null || tabId.isBlank()) {
            return fallback;
        }
        String trimmed = tabId.trim();
        if (trimmed.length() <= MAX_TAB_ID_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_TAB_ID_LENGTH);
    }

    private static List<String> normalizeVisibleTabIds(List<String> visibleTabIds, String fallbackFocusedTabId) {
        java.util.LinkedHashSet<String> normalizedVisibleTabs = new java.util.LinkedHashSet<>();
        if (visibleTabIds != null) {
            for (String visibleTabId : visibleTabIds) {
                normalizedVisibleTabs.add(normalizeTabId(visibleTabId, fallbackFocusedTabId));
                if (normalizedVisibleTabs.size() >= DatabaseTabs.MAX_VISIBLE_TAB_COUNT) {
                    break;
                }
            }
        }
        if (normalizedVisibleTabs.isEmpty()) {
            normalizedVisibleTabs.add(normalizeTabId(fallbackFocusedTabId, DatabaseTabs.ALL_TAB_ID));
        }
        return List.copyOf(normalizedVisibleTabs);
    }

    private static Map<String, Integer> normalizePageValues(Map<String, Integer> pageValues, int fallbackValue) {
        LinkedHashMap<String, Integer> normalizedPageValues = new LinkedHashMap<>();
        if (pageValues != null) {
            for (Map.Entry<String, Integer> entry : pageValues.entrySet()) {
                String normalizedTabId = normalizeTabId(entry.getKey(), DatabaseTabs.ALL_TAB_ID);
                int value = fallbackValue == DEFAULT_PAGE_SIZE
                        ? normalizePageSize(entry.getValue())
                        : Math.max(0, entry.getValue());
                normalizedPageValues.put(normalizedTabId, value);
            }
        }
        if (normalizedPageValues.isEmpty()) {
            normalizedPageValues.put(DatabaseTabs.ALL_TAB_ID, fallbackValue == DEFAULT_PAGE_SIZE ? DEFAULT_PAGE_SIZE : 0);
        }
        return normalizedPageValues;
    }

    private static int normalizePageSize(int pageSize) {
        return Math.min(MAX_PAGE_SIZE, Math.max(1, pageSize));
    }

    private static <T extends Enum<T>> T readEnum(String serializedName, Class<T> enumType, T fallbackValue) {
        try {
            return Enum.valueOf(enumType, serializedName);
        } catch (IllegalArgumentException exception) {
            return fallbackValue;
        }
    }
}
