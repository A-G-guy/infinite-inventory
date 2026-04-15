package com.agguy.infiniteinventory.database;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
        Map<String, DatabaseTabQueryState> tabStates
) {
    private static final String SCOPE_KEY = "scope";
    private static final String FOCUSED_TAB_ID_KEY = "focused_tab_id";
    private static final String VISIBLE_TABS_KEY = "visible_tabs";
    private static final String TAB_STATES_KEY = "tab_states";
    private static final String VISIBLE_TAB_ID_KEY = "tab_id";

    public static final int MAX_SEARCH_LENGTH = 64;
    public static final int DEFAULT_PAGE_SIZE = 54;
    public static final int MAX_PAGE_SIZE = 640;
    public static final int MAX_TAB_ID_LENGTH = 64;

    public DatabaseQuery {
        scope = DatabaseScope.normalize(scope);
        focusedTabId = normalizeTabId(focusedTabId, DatabaseTabs.ALL_TAB_ID);
        visibleTabIds = normalizeVisibleTabIds(visibleTabIds, focusedTabId);
        if (!visibleTabIds.contains(focusedTabId)) {
            focusedTabId = visibleTabIds.getFirst();
        }
        tabStates = normalizeTabStates(tabStates, visibleTabIds, focusedTabId);
    }

    public DatabaseQuery(
            DatabaseScope scope,
            String focusedTabId,
            List<String> visibleTabIds,
            Map<String, Integer> pageIndexes,
            Map<String, Integer> pageSizes,
            DatabaseSortOption sortOption,
            String searchText,
            DatabaseSearchConfig searchConfig
    ) {
        this(
                scope,
                focusedTabId,
                visibleTabIds,
                buildLegacyTabStates(focusedTabId, visibleTabIds, pageIndexes, pageSizes, sortOption, searchText, searchConfig)
        );
    }

    public static DatabaseQuery defaultQuery() {
        return defaultQuery(DatabaseScope.defaultScope());
    }

    public static DatabaseQuery defaultQuery(DatabaseScope scope) {
        return new DatabaseQuery(
                scope,
                DatabaseTabs.ALL_TAB_ID,
                List.of(DatabaseTabs.ALL_TAB_ID),
                Map.of(DatabaseTabs.ALL_TAB_ID, DatabaseTabQueryState.defaultState())
        );
    }

    public static DatabaseQuery normalizeForScope(DatabaseScope scope, DatabaseQuery query) {
        DatabaseScope normalizedScope = DatabaseScope.normalize(scope);
        if (query == null) {
            return defaultQuery(normalizedScope);
        }
        return query.scope() == normalizedScope ? query : query.withScope(normalizedScope);
    }

    public DatabaseTabQueryState tabStateFor(String tabId) {
        String normalizedTabId = normalizeTabId(tabId, this.focusedTabId);
        return this.tabStates.getOrDefault(normalizedTabId, DatabaseTabQueryState.defaultState());
    }

    public DatabaseSortOption sortOptionFor(String tabId) {
        return this.tabStateFor(tabId).sortOption();
    }

    public String searchTextFor(String tabId) {
        return this.tabStateFor(tabId).searchText();
    }

    public DatabaseSearchConfig searchConfigFor(String tabId) {
        return this.tabStateFor(tabId).searchConfig();
    }

    public int pageIndexFor(String tabId) {
        return this.tabStateFor(tabId).pageIndex();
    }

    public int pageSizeFor(String tabId) {
        return this.tabStateFor(tabId).pageSize();
    }

    public DatabaseSortOption sortOption() {
        return this.sortOptionFor(this.focusedTabId);
    }

    public String searchText() {
        return this.searchTextFor(this.focusedTabId);
    }

    public DatabaseSearchConfig searchConfig() {
        return this.searchConfigFor(this.focusedTabId);
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
        return new DatabaseQuery(this.scope, normalizedFocusedTabId, nextVisibleTabs, this.tabStates);
    }

    public DatabaseQuery withSingleVisibleTab(String tabId) {
        String normalizedTabId = normalizeTabId(tabId, DatabaseTabs.ALL_TAB_ID);
        return new DatabaseQuery(this.scope, normalizedTabId, List.of(normalizedTabId), this.tabStates);
    }

    public DatabaseQuery withVisibleTabIds(List<String> newVisibleTabIds) {
        List<String> normalizedVisibleTabIds = normalizeVisibleTabIds(newVisibleTabIds, this.focusedTabId);
        String normalizedFocusedTabId = normalizedVisibleTabIds.contains(this.focusedTabId)
                ? this.focusedTabId
                : normalizedVisibleTabIds.getFirst();
        return new DatabaseQuery(this.scope, normalizedFocusedTabId, normalizedVisibleTabIds, this.tabStates);
    }

    public DatabaseQuery withTabState(String tabId, DatabaseTabQueryState newState) {
        LinkedHashMap<String, DatabaseTabQueryState> nextTabStates = new LinkedHashMap<>(this.tabStates);
        nextTabStates.put(normalizeTabId(tabId, this.focusedTabId), newState == null ? DatabaseTabQueryState.defaultState() : newState);
        return new DatabaseQuery(this.scope, this.focusedTabId, this.visibleTabIds, nextTabStates);
    }

    public DatabaseQuery withSortOption(String tabId, DatabaseSortOption newSortOption) {
        String normalizedTabId = normalizeTabId(tabId, this.focusedTabId);
        return this.withTabState(normalizedTabId, this.tabStateFor(normalizedTabId).withSortOption(newSortOption));
    }

    public DatabaseQuery withSortOption(DatabaseSortOption newSortOption) {
        return this.withSortOption(this.focusedTabId, newSortOption);
    }

    public DatabaseQuery withSearchText(String tabId, String newSearchText) {
        String normalizedTabId = normalizeTabId(tabId, this.focusedTabId);
        return this.withTabState(normalizedTabId, this.tabStateFor(normalizedTabId).withSearchText(newSearchText));
    }

    public DatabaseQuery withSearchText(String newSearchText) {
        return this.withSearchText(this.focusedTabId, newSearchText);
    }

    public DatabaseQuery withSearchConfig(String tabId, DatabaseSearchConfig newSearchConfig) {
        String normalizedTabId = normalizeTabId(tabId, this.focusedTabId);
        return this.withTabState(normalizedTabId, this.tabStateFor(normalizedTabId).withSearchConfig(newSearchConfig));
    }

    public DatabaseQuery withSearchConfig(DatabaseSearchConfig newSearchConfig) {
        return this.withSearchConfig(this.focusedTabId, newSearchConfig);
    }

    public DatabaseQuery withPageIndex(String tabId, int newPageIndex) {
        String normalizedTabId = normalizeTabId(tabId, this.focusedTabId);
        return this.withTabState(normalizedTabId, this.tabStateFor(normalizedTabId).withPageIndex(newPageIndex));
    }

    public DatabaseQuery withPageIndex(int newPageIndex) {
        return this.withPageIndex(this.focusedTabId, newPageIndex);
    }

    public DatabaseQuery withPageSize(String tabId, int newPageSize) {
        String normalizedTabId = normalizeTabId(tabId, this.focusedTabId);
        return this.withTabState(normalizedTabId, this.tabStateFor(normalizedTabId).withPageSize(newPageSize));
    }

    public DatabaseQuery withPageSize(int newPageSize) {
        return this.withPageSize(this.focusedTabId, newPageSize);
    }

    public DatabaseQuery withPanelLayout(Map<String, Integer> nextPageIndexes, Map<String, Integer> nextPageSizes) {
        LinkedHashMap<String, DatabaseTabQueryState> nextTabStates = new LinkedHashMap<>(this.tabStates);
        LinkedHashSet<String> targetTabIds = new LinkedHashSet<>();
        if (nextPageIndexes != null) {
            targetTabIds.addAll(nextPageIndexes.keySet());
        }
        if (nextPageSizes != null) {
            targetTabIds.addAll(nextPageSizes.keySet());
        }
        for (String tabId : targetTabIds) {
            String normalizedTabId = normalizeTabId(tabId, this.focusedTabId);
            DatabaseTabQueryState currentState = this.tabStateFor(normalizedTabId);
            int nextPageIndex = nextPageIndexes != null && nextPageIndexes.containsKey(tabId)
                    ? nextPageIndexes.get(tabId)
                    : currentState.pageIndex();
            int nextPageSize = nextPageSizes != null && nextPageSizes.containsKey(tabId)
                    ? nextPageSizes.get(tabId)
                    : currentState.pageSize();
            nextTabStates.put(
                    normalizedTabId,
                    currentState.withPageIndex(nextPageIndex).withPageSize(nextPageSize)
            );
        }
        return new DatabaseQuery(this.scope, this.focusedTabId, this.visibleTabIds, nextTabStates);
    }

    public DatabaseQuery withScope(DatabaseScope newScope) {
        return new DatabaseQuery(newScope, this.focusedTabId, this.visibleTabIds, this.tabStates);
    }

    public static DatabaseQuery read(FriendlyByteBuf buffer) {
        DatabaseScope scope = buffer.readEnum(DatabaseScope.class);
        String focusedTabId = buffer.readUtf(MAX_TAB_ID_LENGTH);
        int visibleTabCount = buffer.readVarInt();
        java.util.ArrayList<String> visibleTabIds = new java.util.ArrayList<>(visibleTabCount);
        for (int index = 0; index < visibleTabCount; index++) {
            visibleTabIds.add(buffer.readUtf(MAX_TAB_ID_LENGTH));
        }
        int tabStateCount = buffer.readVarInt();
        LinkedHashMap<String, DatabaseTabQueryState> tabStates = new LinkedHashMap<>(tabStateCount);
        for (int index = 0; index < tabStateCount; index++) {
            tabStates.put(
                    normalizeTabId(buffer.readUtf(MAX_TAB_ID_LENGTH), focusedTabId),
                    DatabaseTabQueryState.read(buffer)
            );
        }
        return new DatabaseQuery(scope, focusedTabId, visibleTabIds, tabStates);
    }

    public static void write(FriendlyByteBuf buffer, DatabaseQuery query) {
        buffer.writeEnum(query.scope());
        buffer.writeUtf(query.focusedTabId(), MAX_TAB_ID_LENGTH);
        buffer.writeVarInt(query.visibleTabIds().size());
        for (String visibleTabId : query.visibleTabIds()) {
            buffer.writeUtf(visibleTabId, MAX_TAB_ID_LENGTH);
        }
        buffer.writeVarInt(query.tabStates().size());
        for (Map.Entry<String, DatabaseTabQueryState> entry : query.tabStates().entrySet()) {
            buffer.writeUtf(entry.getKey(), MAX_TAB_ID_LENGTH);
            entry.getValue().write(buffer);
        }
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
        CompoundTag tabStatesTag = new CompoundTag();
        for (Map.Entry<String, DatabaseTabQueryState> entry : this.tabStates.entrySet()) {
            tabStatesTag.put(entry.getKey(), entry.getValue().toTag());
        }
        tag.put(TAB_STATES_KEY, tabStatesTag);
        return tag;
    }

    public static DatabaseQuery fromTag(CompoundTag tag, DatabaseScope fallbackScope) {
        if (tag == null || tag.isEmpty()) {
            return defaultQuery(fallbackScope);
        }
        DatabaseScope scope = readEnum(tag.getString(SCOPE_KEY), DatabaseScope.class, DatabaseScope.normalize(fallbackScope));
        if (tag.contains(TAB_STATES_KEY, Tag.TAG_COMPOUND)) {
            return new DatabaseQuery(
                    scope,
                    tag.getString(FOCUSED_TAB_ID_KEY),
                    fromVisibleTabsTag(tag.getList(VISIBLE_TABS_KEY, Tag.TAG_COMPOUND)),
                    fromTabStatesTag(tag.getCompound(TAB_STATES_KEY), tag.getString(FOCUSED_TAB_ID_KEY))
            );
        }
        if (tag.contains(FOCUSED_TAB_ID_KEY) || tag.contains(VISIBLE_TABS_KEY)) {
            return new DatabaseQuery(
                    scope,
                    tag.getString(FOCUSED_TAB_ID_KEY),
                    fromVisibleTabsTag(tag.getList(VISIBLE_TABS_KEY, Tag.TAG_COMPOUND)),
                    buildLegacyTabStates(
                            tag.getString(FOCUSED_TAB_ID_KEY),
                            fromVisibleTabsTag(tag.getList(VISIBLE_TABS_KEY, Tag.TAG_COMPOUND)),
                            fromPageValueTag(tag.getCompound("page_indexes"), 0),
                            fromPageValueTag(tag.getCompound("page_sizes"), DEFAULT_PAGE_SIZE),
                            readEnum(tag.getString("sort_option"), DatabaseSortOption.class, DatabaseSortOption.RECENTLY_CHANGED),
                            tag.getString("search_text"),
                            DatabaseSearchConfig.fromTag(tag.getCompound("search_config"))
                    )
            );
        }

        DatabaseCategory legacyCategory = readEnum(tag.getString("category"), DatabaseCategory.class, DatabaseCategory.ALL);
        String focusedTabId = legacyCategory == DatabaseCategory.ALL ? DatabaseTabs.ALL_TAB_ID : DatabaseTabs.DEFAULT_TAB_ID;
        return new DatabaseQuery(
                scope,
                focusedTabId,
                List.of(focusedTabId),
                Map.of(
                        focusedTabId,
                        new DatabaseTabQueryState(
                                readEnum(tag.getString("sort_option"), DatabaseSortOption.class, DatabaseSortOption.RECENTLY_CHANGED),
                                tag.getString("search_text"),
                                DatabaseSearchConfig.fromTag(tag.getCompound("search_config")),
                                Math.max(0, tag.getInt("page_index")),
                                normalizePageSize(tag.getInt("page_size"))
                        )
                )
        );
    }

    private static Map<String, DatabaseTabQueryState> buildLegacyTabStates(
            String focusedTabId,
            List<String> visibleTabIds,
            Map<String, Integer> pageIndexes,
            Map<String, Integer> pageSizes,
            DatabaseSortOption sortOption,
            String searchText,
            DatabaseSearchConfig searchConfig
    ) {
        String normalizedFocusedTabId = normalizeTabId(focusedTabId, DatabaseTabs.ALL_TAB_ID);
        List<String> normalizedVisibleTabIds = normalizeVisibleTabIds(visibleTabIds, normalizedFocusedTabId);
        LinkedHashMap<String, DatabaseTabQueryState> legacyTabStates = new LinkedHashMap<>();
        LinkedHashSet<String> tabIds = new LinkedHashSet<>();
        tabIds.add(normalizedFocusedTabId);
        tabIds.addAll(normalizedVisibleTabIds);
        if (pageIndexes != null) {
            tabIds.addAll(pageIndexes.keySet());
        }
        if (pageSizes != null) {
            tabIds.addAll(pageSizes.keySet());
        }
        for (String tabId : tabIds) {
            String normalizedTabId = normalizeTabId(tabId, normalizedFocusedTabId);
            int pageIndex = pageIndexes == null ? 0 : Math.max(0, pageIndexes.getOrDefault(tabId, pageIndexes.getOrDefault(normalizedTabId, 0)));
            int pageSize = pageSizes == null
                    ? DEFAULT_PAGE_SIZE
                    : normalizePageSize(pageSizes.getOrDefault(tabId, pageSizes.getOrDefault(normalizedTabId, DEFAULT_PAGE_SIZE)));
            legacyTabStates.put(
                    normalizedTabId,
                    new DatabaseTabQueryState(sortOption, searchText, searchConfig, pageIndex, pageSize)
            );
        }
        if (legacyTabStates.isEmpty()) {
            legacyTabStates.put(normalizedFocusedTabId, DatabaseTabQueryState.defaultState());
        }
        return legacyTabStates;
    }

    private static Map<String, DatabaseTabQueryState> normalizeTabStates(
            Map<String, DatabaseTabQueryState> tabStates,
            List<String> visibleTabIds,
            String focusedTabId
    ) {
        LinkedHashMap<String, DatabaseTabQueryState> normalizedTabStates = new LinkedHashMap<>();
        if (tabStates != null) {
            for (Map.Entry<String, DatabaseTabQueryState> entry : tabStates.entrySet()) {
                normalizedTabStates.put(
                        normalizeTabId(entry.getKey(), focusedTabId),
                        entry.getValue() == null ? DatabaseTabQueryState.defaultState() : entry.getValue()
                );
            }
        }
        normalizedTabStates.putIfAbsent(focusedTabId, DatabaseTabQueryState.defaultState());
        for (String visibleTabId : visibleTabIds) {
            normalizedTabStates.putIfAbsent(visibleTabId, DatabaseTabQueryState.defaultState());
        }
        return Map.copyOf(normalizedTabStates);
    }

    private static Map<String, DatabaseTabQueryState> fromTabStatesTag(CompoundTag tag, String focusedTabId) {
        LinkedHashMap<String, DatabaseTabQueryState> tabStates = new LinkedHashMap<>();
        if (tag == null) {
            return tabStates;
        }
        for (String key : tag.getAllKeys()) {
            tabStates.put(normalizeTabId(key, focusedTabId), DatabaseTabQueryState.fromTag(tag.getCompound(key)));
        }
        return tabStates;
    }

    private static Map<String, Integer> fromPageValueTag(CompoundTag tag, int fallbackValue) {
        LinkedHashMap<String, Integer> values = new LinkedHashMap<>();
        if (tag != null) {
            for (String key : tag.getAllKeys()) {
                values.put(normalizeTabId(key, DatabaseTabs.ALL_TAB_ID), Math.max(0, tag.getInt(key)));
            }
        }
        if (values.isEmpty() && fallbackValue >= 0) {
            values.put(DatabaseTabs.ALL_TAB_ID, fallbackValue == DEFAULT_PAGE_SIZE ? DEFAULT_PAGE_SIZE : 0);
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
        LinkedHashSet<String> normalizedVisibleTabs = new LinkedHashSet<>();
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

    private static int normalizePageSize(int pageSize) {
        return Math.min(MAX_PAGE_SIZE, Math.max(1, pageSize));
    }

    private static <T extends Enum<T>> T readEnum(String serializedName, Class<T> enumType, T fallbackValue) {
        if (serializedName == null || serializedName.isBlank()) {
            return fallbackValue;
        }
        try {
            return Enum.valueOf(enumType, serializedName);
        } catch (IllegalArgumentException exception) {
            return fallbackValue;
        }
    }
}
