package com.agguy.infiniteinventory.database;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import com.agguy.infiniteinventory.network.NetworkConstants;
import net.minecraft.network.FriendlyByteBuf;

final class DatabaseQuerySupport {
    private static final String SCOPE_KEY = "scope";
    private static final String FOCUSED_TAB_ID_KEY = "focused_tab_id";
    private static final String VISIBLE_TABS_KEY = "visible_tabs";
    private static final String TAB_STATES_KEY = "tab_states";
    private static final String VISIBLE_TAB_ID_KEY = "tab_id";
    private static final String FOCUSED_TAB_KEY = "focused";
    private static final String TAB_STATE_ENTRY_REF_KEY = "tab";
    private static final String TAB_STATE_ENTRY_STATE_KEY = "state";
    private static final String HIDDEN_TOP_TABS_KEY = "hidden_top_tabs";

    private DatabaseQuerySupport() {
    }

    static DatabaseScopedTabRef normalizeScopedTab(DatabaseScopedTabRef scopedTab, DatabaseScopedTabRef fallback) {
        return scopedTab == null ? fallback : scopedTab;
    }

    static List<DatabaseScopedTabRef> normalizeVisibleTabs(
            List<DatabaseScopedTabRef> visibleTabs,
            DatabaseScopedTabRef fallbackFocusedTab
    ) {
        LinkedHashSet<DatabaseScopedTabRef> normalizedVisibleTabs = new LinkedHashSet<>();
        if (visibleTabs != null) {
            for (DatabaseScopedTabRef visibleTab : visibleTabs) {
                normalizedVisibleTabs.add(normalizeScopedTab(visibleTab, fallbackFocusedTab));
                if (normalizedVisibleTabs.size() >= DatabaseTabs.MAX_VISIBLE_TAB_COUNT) {
                    break;
                }
            }
        }
        if (normalizedVisibleTabs.isEmpty()) {
            normalizedVisibleTabs.add(normalizeScopedTab(fallbackFocusedTab, DatabaseScopedTabRef.defaultTab()));
        }
        return List.copyOf(normalizedVisibleTabs);
    }

    static Map<DatabaseScopedTabRef, DatabaseTabQueryState> normalizeTabStates(
            Map<DatabaseScopedTabRef, DatabaseTabQueryState> tabStates,
            List<DatabaseScopedTabRef> visibleTabs,
            DatabaseScopedTabRef focusedTab
    ) {
        LinkedHashMap<DatabaseScopedTabRef, DatabaseTabQueryState> normalizedTabStates = new LinkedHashMap<>();
        if (tabStates != null) {
            for (Map.Entry<DatabaseScopedTabRef, DatabaseTabQueryState> entry : tabStates.entrySet()) {
                normalizedTabStates.put(
                        normalizeScopedTab(entry.getKey(), focusedTab),
                        entry.getValue() == null ? DatabaseTabQueryState.defaultState() : entry.getValue()
                );
            }
        }
        normalizedTabStates.putIfAbsent(focusedTab, DatabaseTabQueryState.defaultState());
        for (DatabaseScopedTabRef visibleTab : visibleTabs) {
            normalizedTabStates.putIfAbsent(visibleTab, DatabaseTabQueryState.defaultState());
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(normalizedTabStates));
    }

    static List<DatabaseScopedTabRef> toScopedRefs(DatabaseScope scope, List<String> visibleTabIds) {
        if (visibleTabIds == null || visibleTabIds.isEmpty()) {
            return List.of(DatabaseScopedTabRef.allTab(scope));
        }
        java.util.ArrayList<DatabaseScopedTabRef> scopedTabs = new java.util.ArrayList<>(visibleTabIds.size());
        for (String visibleTabId : visibleTabIds) {
            scopedTabs.add(DatabaseScopedTabRef.concreteTab(scope, visibleTabId));
        }
        return List.copyOf(scopedTabs);
    }

    static Map<DatabaseScopedTabRef, DatabaseTabQueryState> buildLegacyTabStates(
            DatabaseScope scope,
            String focusedTabId,
            List<String> visibleTabIds,
            Map<String, Integer> pageIndexes,
            Map<String, Integer> pageSizes,
            DatabaseSortOption sortOption,
            String searchText,
            DatabaseSearchConfig searchConfig
    ) {
        DatabaseScopedTabRef normalizedFocusedTab = DatabaseScopedTabRef.concreteTab(scope, focusedTabId);
        List<DatabaseScopedTabRef> normalizedVisibleTabs = normalizeVisibleTabs(toScopedRefs(scope, visibleTabIds), normalizedFocusedTab);
        LinkedHashMap<DatabaseScopedTabRef, DatabaseTabQueryState> legacyTabStates = new LinkedHashMap<>();
        LinkedHashSet<DatabaseScopedTabRef> scopedTabs = new LinkedHashSet<>();
        scopedTabs.add(normalizedFocusedTab);
        scopedTabs.addAll(normalizedVisibleTabs);
        if (pageIndexes != null) {
            for (String tabId : pageIndexes.keySet()) {
                scopedTabs.add(DatabaseScopedTabRef.concreteTab(scope, tabId));
            }
        }
        if (pageSizes != null) {
            for (String tabId : pageSizes.keySet()) {
                scopedTabs.add(DatabaseScopedTabRef.concreteTab(scope, tabId));
            }
        }
        for (DatabaseScopedTabRef scopedTab : scopedTabs) {
            String tabId = scopedTab.tabId();
            int pageIndex = pageIndexes == null ? 0 : Math.max(0, pageIndexes.getOrDefault(tabId, 0));
            int pageSize = pageSizes == null
                    ? DatabaseQuery.DEFAULT_PAGE_SIZE
                    : normalizePageSize(pageSizes.getOrDefault(tabId, DatabaseQuery.DEFAULT_PAGE_SIZE));
            legacyTabStates.put(scopedTab, new DatabaseTabQueryState(sortOption, searchText, searchConfig, pageIndex, pageSize));
        }
        if (legacyTabStates.isEmpty()) {
            legacyTabStates.put(normalizedFocusedTab, DatabaseTabQueryState.defaultState());
        }
        return legacyTabStates;
    }

    static DatabaseQuery retargetScope(DatabaseQuery query, DatabaseScope nextScope) {
        DatabaseScope normalizedScope = DatabaseScope.normalize(nextScope);
        LinkedHashMap<DatabaseScopedTabRef, DatabaseTabQueryState> nextTabStates = new LinkedHashMap<>();
        for (Map.Entry<DatabaseScopedTabRef, DatabaseTabQueryState> entry : query.tabStates().entrySet()) {
            nextTabStates.put(entry.getKey().withScope(normalizedScope), entry.getValue());
        }
        // hiddenTopTabs 是 per-scope 的隐藏配置：原 scope 的隐藏列表不应被迁移，
        // 否则切回原 scope 时已隐藏的页签会全部冒出来（典型 bug 现象：「多加几个页签」）。
        return new DatabaseQuery(
                query.focusedTab().withScope(normalizedScope),
                query.visibleTabs().stream().map(ref -> ref.withScope(normalizedScope)).toList(),
                nextTabStates,
                query.hiddenTopTabs()
        );
    }

    static DatabaseQuery queryForScope(DatabaseQuery query, DatabaseScope scope) {
        DatabaseScope normalizedScope = DatabaseScope.normalize(scope);
        LinkedHashMap<DatabaseScopedTabRef, DatabaseTabQueryState> scopedTabStates = new LinkedHashMap<>();
        for (Map.Entry<DatabaseScopedTabRef, DatabaseTabQueryState> entry : query.tabStates().entrySet()) {
            if (entry.getKey().scope() == normalizedScope) {
                scopedTabStates.put(entry.getKey(), entry.getValue());
            }
        }

        java.util.ArrayList<DatabaseScopedTabRef> scopedVisibleTabs = new java.util.ArrayList<>();
        for (DatabaseScopedTabRef visibleTab : query.visibleTabs()) {
            if (visibleTab.scope() == normalizedScope) {
                scopedVisibleTabs.add(visibleTab);
            }
        }

        java.util.ArrayList<DatabaseScopedTabRef> scopedHiddenTopTabs = new java.util.ArrayList<>();
        for (DatabaseScopedTabRef hiddenTab : query.hiddenTopTabs()) {
            if (hiddenTab.scope() == normalizedScope) {
                scopedHiddenTopTabs.add(hiddenTab);
            }
        }

        DatabaseScopedTabRef scopedFocusedTab = query.focusedTab().scope() == normalizedScope
                ? query.focusedTab()
                : !scopedVisibleTabs.isEmpty()
                        ? scopedVisibleTabs.getFirst()
                        : !scopedTabStates.isEmpty()
                                ? scopedTabStates.keySet().iterator().next()
                                : DatabaseScopedTabRef.allTab(normalizedScope);
        if (scopedVisibleTabs.isEmpty()) {
            scopedVisibleTabs.add(scopedFocusedTab);
        }
        if (!scopedTabStates.containsKey(scopedFocusedTab)) {
            scopedTabStates.put(scopedFocusedTab, DatabaseTabQueryState.defaultState());
        }
        for (DatabaseScopedTabRef scopedVisibleTab : scopedVisibleTabs) {
            scopedTabStates.putIfAbsent(scopedVisibleTab, DatabaseTabQueryState.defaultState());
        }
        return new DatabaseQuery(scopedFocusedTab, List.copyOf(scopedVisibleTabs), scopedTabStates, List.copyOf(scopedHiddenTopTabs));
    }

    static DatabaseQuery read(FriendlyByteBuf buffer) {
        DatabaseScopedTabRef focusedTab = DatabaseScopedTabRef.read(buffer);
        int visibleTabCount = buffer.readVarInt();
        NetworkConstants.checkListSize(visibleTabCount, NetworkConstants.MAX_QUERY_VISIBLE_TAB_COUNT, "visibleTabs");
        java.util.ArrayList<DatabaseScopedTabRef> visibleTabs = new java.util.ArrayList<>(visibleTabCount);
        for (int index = 0; index < visibleTabCount; index++) {
            visibleTabs.add(DatabaseScopedTabRef.read(buffer));
        }
        int tabStateCount = buffer.readVarInt();
        NetworkConstants.checkListSize(tabStateCount, NetworkConstants.MAX_QUERY_TAB_STATE_COUNT, "tabStates");
        LinkedHashMap<DatabaseScopedTabRef, DatabaseTabQueryState> tabStates = new LinkedHashMap<>(tabStateCount);
        for (int index = 0; index < tabStateCount; index++) {
            tabStates.put(DatabaseScopedTabRef.read(buffer), DatabaseTabQueryState.read(buffer));
        }
        int hiddenTopTabCount = buffer.readVarInt();
        NetworkConstants.checkListSize(hiddenTopTabCount, NetworkConstants.MAX_QUERY_HIDDEN_TOP_TAB_COUNT, "hiddenTopTabs");
        java.util.ArrayList<DatabaseScopedTabRef> hiddenTopTabs = new java.util.ArrayList<>(hiddenTopTabCount);
        for (int index = 0; index < hiddenTopTabCount; index++) {
            hiddenTopTabs.add(DatabaseScopedTabRef.read(buffer));
        }
        return new DatabaseQuery(focusedTab, visibleTabs, tabStates, hiddenTopTabs);
    }

    static void write(FriendlyByteBuf buffer, DatabaseQuery query) {
        query.focusedTab().write(buffer);
        buffer.writeVarInt(query.visibleTabs().size());
        for (DatabaseScopedTabRef visibleTab : query.visibleTabs()) {
            visibleTab.write(buffer);
        }
        buffer.writeVarInt(query.tabStates().size());
        for (Map.Entry<DatabaseScopedTabRef, DatabaseTabQueryState> entry : query.tabStates().entrySet()) {
            entry.getKey().write(buffer);
            entry.getValue().write(buffer);
        }
        buffer.writeVarInt(query.hiddenTopTabs().size());
        for (DatabaseScopedTabRef hiddenTab : query.hiddenTopTabs()) {
            hiddenTab.write(buffer);
        }
    }

    static CompoundTag toTag(DatabaseQuery query) {
        CompoundTag tag = new CompoundTag();
        tag.put(FOCUSED_TAB_KEY, query.focusedTab().toTag());
        ListTag visibleTabsTag = new ListTag();
        for (DatabaseScopedTabRef visibleTab : query.visibleTabs()) {
            visibleTabsTag.add(visibleTab.toTag());
        }
        tag.put(VISIBLE_TABS_KEY, visibleTabsTag);
        ListTag tabStatesTag = new ListTag();
        for (Map.Entry<DatabaseScopedTabRef, DatabaseTabQueryState> entry : query.tabStates().entrySet()) {
            CompoundTag tabStateEntry = new CompoundTag();
            tabStateEntry.put(TAB_STATE_ENTRY_REF_KEY, entry.getKey().toTag());
            tabStateEntry.put(TAB_STATE_ENTRY_STATE_KEY, entry.getValue().toTag());
            tabStatesTag.add(tabStateEntry);
        }
        tag.put(TAB_STATES_KEY, tabStatesTag);
        ListTag hiddenTopTabsTag = new ListTag();
        for (DatabaseScopedTabRef hiddenTab : query.hiddenTopTabs()) {
            hiddenTopTabsTag.add(hiddenTab.toTag());
        }
        tag.put(HIDDEN_TOP_TABS_KEY, hiddenTopTabsTag);
        return tag;
    }

    static DatabaseQuery fromTag(CompoundTag tag, DatabaseScope fallbackScope) {
        DatabaseScope normalizedFallbackScope = DatabaseScope.normalize(fallbackScope);
        if (tag == null || tag.isEmpty()) {
            return DatabaseQuery.defaultQuery(normalizedFallbackScope);
        }
        if (tag.contains(FOCUSED_TAB_KEY, Tag.TAG_COMPOUND)) {
            DatabaseScopedTabRef focusedTab = DatabaseScopedTabRef.fromTag(tag.getCompound(FOCUSED_TAB_KEY), normalizedFallbackScope);
            List<DatabaseScopedTabRef> visibleTabs = fromVisibleTabsTag(tag.getList(VISIBLE_TABS_KEY, Tag.TAG_COMPOUND), normalizedFallbackScope);
            List<DatabaseScopedTabRef> hiddenTopTabs = fromHiddenTopTabsTag(tag.getList(HIDDEN_TOP_TABS_KEY, Tag.TAG_COMPOUND), normalizedFallbackScope);
            return new DatabaseQuery(
                    focusedTab,
                    visibleTabs,
                    fromTabStatesListTag(tag.getList(TAB_STATES_KEY, Tag.TAG_COMPOUND), normalizedFallbackScope, focusedTab),
                    hiddenTopTabs
            );
        }

        DatabaseScope legacyScope = DatabaseScope.read(tag.getString(SCOPE_KEY), normalizedFallbackScope);
        if (tag.contains(TAB_STATES_KEY, Tag.TAG_COMPOUND)) {
            return new DatabaseQuery(
                    DatabaseScopedTabRef.concreteTab(legacyScope, tag.getString(FOCUSED_TAB_ID_KEY)),
                    fromVisibleTabsTag(tag.getList(VISIBLE_TABS_KEY, Tag.TAG_COMPOUND), legacyScope),
                    fromLegacyTabStatesTag(tag.getCompound(TAB_STATES_KEY), legacyScope, tag.getString(FOCUSED_TAB_ID_KEY)),
                    List.of()
            );
        }
        if (tag.contains(FOCUSED_TAB_ID_KEY) || tag.contains(VISIBLE_TABS_KEY)) {
            String focusedTabId = tag.getString(FOCUSED_TAB_ID_KEY);
            List<String> visibleTabIds = fromLegacyVisibleTabIdsTag(tag.getList(VISIBLE_TABS_KEY, Tag.TAG_COMPOUND));
            return new DatabaseQuery(
                    DatabaseScopedTabRef.concreteTab(legacyScope, focusedTabId),
                    toScopedRefs(legacyScope, visibleTabIds),
                    buildLegacyTabStates(
                            legacyScope,
                            focusedTabId,
                            visibleTabIds,
                            fromPageValueTag(tag.getCompound("page_indexes"), 0),
                            fromPageValueTag(tag.getCompound("page_sizes"), DatabaseQuery.DEFAULT_PAGE_SIZE),
                            readEnum(tag.getString("sort_option"), DatabaseSortOption.class, DatabaseSortOption.RECENTLY_CHANGED),
                            tag.getString("search_text"),
                            DatabaseSearchConfig.fromTag(tag.getCompound("search_config"))
                    ),
                    List.of()
            );
        }

        DatabaseCategory legacyCategory = readEnum(tag.getString("category"), DatabaseCategory.class, DatabaseCategory.ALL);
        String focusedTabId = legacyCategory == DatabaseCategory.ALL ? DatabaseTabs.ALL_TAB_ID : DatabaseTabs.DEFAULT_TAB_ID;
        DatabaseScopedTabRef focusedTab = DatabaseScopedTabRef.concreteTab(legacyScope, focusedTabId);
        return new DatabaseQuery(
                focusedTab,
                List.of(focusedTab),
                Map.of(
                        focusedTab,
                        new DatabaseTabQueryState(
                                readEnum(tag.getString("sort_option"), DatabaseSortOption.class, DatabaseSortOption.RECENTLY_CHANGED),
                                tag.getString("search_text"),
                                DatabaseSearchConfig.fromTag(tag.getCompound("search_config")),
                                Math.max(0, tag.getInt("page_index")),
                                normalizePageSize(tag.getInt("page_size"))
                        )
                ),
                List.of()
        );
    }

    private static Map<DatabaseScopedTabRef, DatabaseTabQueryState> fromTabStatesListTag(
            ListTag tag,
            DatabaseScope fallbackScope,
            DatabaseScopedTabRef fallbackFocusedTab
    ) {
        LinkedHashMap<DatabaseScopedTabRef, DatabaseTabQueryState> tabStates = new LinkedHashMap<>();
        for (Tag element : tag) {
            if (!(element instanceof CompoundTag tabStateEntry)) {
                continue;
            }
            DatabaseScopedTabRef scopedTab = DatabaseScopedTabRef.fromTag(
                    tabStateEntry.getCompound(TAB_STATE_ENTRY_REF_KEY),
                    fallbackScope
            );
            if (tabStateEntry.contains(TAB_STATE_ENTRY_STATE_KEY, Tag.TAG_COMPOUND)) {
                tabStates.put(scopedTab, DatabaseTabQueryState.fromTag(tabStateEntry.getCompound(TAB_STATE_ENTRY_STATE_KEY)));
                continue;
            }
            tabStates.put(scopedTab, DatabaseTabQueryState.defaultState());
        }
        if (tabStates.isEmpty()) {
            tabStates.put(fallbackFocusedTab, DatabaseTabQueryState.defaultState());
        }
        return tabStates;
    }

    private static Map<DatabaseScopedTabRef, DatabaseTabQueryState> fromLegacyTabStatesTag(
            CompoundTag tag,
            DatabaseScope scope,
            String fallbackFocusedTabId
    ) {
        LinkedHashMap<DatabaseScopedTabRef, DatabaseTabQueryState> tabStates = new LinkedHashMap<>();
        if (tag == null) {
            return tabStates;
        }
        for (String key : tag.getAllKeys()) {
            tabStates.put(
                    DatabaseScopedTabRef.concreteTab(scope, normalizeTabId(key, fallbackFocusedTabId)),
                    DatabaseTabQueryState.fromTag(tag.getCompound(key))
            );
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
            values.put(DatabaseTabs.ALL_TAB_ID, fallbackValue == DatabaseQuery.DEFAULT_PAGE_SIZE ? DatabaseQuery.DEFAULT_PAGE_SIZE : 0);
        }
        return values;
    }

    private static List<DatabaseScopedTabRef> fromVisibleTabsTag(ListTag tag, DatabaseScope fallbackScope) {
        java.util.ArrayList<DatabaseScopedTabRef> visibleTabs = new java.util.ArrayList<>();
        for (Tag element : tag) {
            if (element instanceof CompoundTag visibleTabTag) {
                if (visibleTabTag.contains(SCOPE_KEY, Tag.TAG_STRING)) {
                    visibleTabs.add(DatabaseScopedTabRef.fromTag(visibleTabTag, fallbackScope));
                    continue;
                }
                visibleTabs.add(DatabaseScopedTabRef.concreteTab(fallbackScope, visibleTabTag.getString(VISIBLE_TAB_ID_KEY)));
            }
        }
        return visibleTabs;
    }

    private static List<DatabaseScopedTabRef> fromHiddenTopTabsTag(ListTag tag, DatabaseScope fallbackScope) {
        java.util.ArrayList<DatabaseScopedTabRef> hiddenTopTabs = new java.util.ArrayList<>();
        if (tag == null) {
            return hiddenTopTabs;
        }
        for (Tag element : tag) {
            if (element instanceof CompoundTag hiddenTabTag) {
                if (hiddenTabTag.contains(SCOPE_KEY, Tag.TAG_STRING)) {
                    hiddenTopTabs.add(DatabaseScopedTabRef.fromTag(hiddenTabTag, fallbackScope));
                }
            }
        }
        return hiddenTopTabs;
    }

    private static List<String> fromLegacyVisibleTabIdsTag(ListTag tag) {
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
        if (trimmed.length() <= DatabaseQuery.MAX_TAB_ID_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, DatabaseQuery.MAX_TAB_ID_LENGTH);
    }

    private static int normalizePageSize(int pageSize) {
        return Math.min(DatabaseQuery.MAX_PAGE_SIZE, Math.max(1, pageSize));
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
