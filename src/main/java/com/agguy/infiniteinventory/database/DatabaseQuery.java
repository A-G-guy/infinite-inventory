package com.agguy.infiniteinventory.database;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record DatabaseQuery(
        DatabaseScopedTabRef focusedTab,
        List<DatabaseScopedTabRef> visibleTabs,
        Map<DatabaseScopedTabRef, DatabaseTabQueryState> tabStates
) {
    public static final int MAX_SEARCH_LENGTH = 64;
    public static final int DEFAULT_PAGE_SIZE = 54;
    public static final int MAX_PAGE_SIZE = 640;
    public static final int MAX_TAB_ID_LENGTH = 64;

    public DatabaseQuery {
        focusedTab = DatabaseQuerySupport.normalizeScopedTab(focusedTab, DatabaseScopedTabRef.defaultTab());
        visibleTabs = DatabaseQuerySupport.normalizeVisibleTabs(visibleTabs, focusedTab);
        if (!visibleTabs.contains(focusedTab)) {
            focusedTab = visibleTabs.getFirst();
        }
        tabStates = DatabaseQuerySupport.normalizeTabStates(tabStates, visibleTabs, focusedTab);
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
                DatabaseScopedTabRef.concreteTab(DatabaseScope.normalize(scope), focusedTabId),
                DatabaseQuerySupport.toScopedRefs(DatabaseScope.normalize(scope), visibleTabIds),
                DatabaseQuerySupport.buildLegacyTabStates(
                        DatabaseScope.normalize(scope),
                        focusedTabId,
                        visibleTabIds,
                        pageIndexes,
                        pageSizes,
                        sortOption,
                        searchText,
                        searchConfig
                )
        );
    }

    public static DatabaseQuery defaultQuery() {
        return defaultQuery(DatabaseScope.defaultScope());
    }

    public static DatabaseQuery defaultQuery(DatabaseScope scope) {
        DatabaseScopedTabRef defaultTab = DatabaseScopedTabRef.allTab(scope);
        return new DatabaseQuery(
                defaultTab,
                List.of(defaultTab),
                Map.of(defaultTab, DatabaseTabQueryState.defaultState())
        );
    }

    public static DatabaseQuery normalizeForScope(DatabaseScope scope, DatabaseQuery query) {
        DatabaseScope normalizedScope = DatabaseScope.normalize(scope);
        if (query == null) {
            return defaultQuery(normalizedScope);
        }
        return query.queryForScope(normalizedScope);
    }

    public DatabaseScope scope() {
        return this.focusedTab.scope();
    }

    public String focusedTabId() {
        return this.focusedTab.tabId();
    }

    public List<String> visibleTabIds() {
        return this.visibleTabs.stream().map(DatabaseScopedTabRef::tabId).toList();
    }

    public DatabaseTabQueryState tabStateFor(DatabaseScopedTabRef scopedTab) {
        DatabaseScopedTabRef normalizedScopedTab = DatabaseQuerySupport.normalizeScopedTab(scopedTab, this.focusedTab);
        return this.tabStates.getOrDefault(normalizedScopedTab, DatabaseTabQueryState.defaultState());
    }

    public DatabaseTabQueryState tabStateFor(String tabId) {
        return this.tabStateFor(DatabaseScopedTabRef.concreteTab(this.focusedTab.scope(), tabId));
    }

    public DatabaseSortOption sortOptionFor(DatabaseScopedTabRef scopedTab) {
        return this.tabStateFor(scopedTab).sortOption();
    }

    public DatabaseSortOption sortOptionFor(String tabId) {
        return this.sortOptionFor(DatabaseScopedTabRef.concreteTab(this.focusedTab.scope(), tabId));
    }

    public String searchTextFor(DatabaseScopedTabRef scopedTab) {
        return this.tabStateFor(scopedTab).searchText();
    }

    public String searchTextFor(String tabId) {
        return this.searchTextFor(DatabaseScopedTabRef.concreteTab(this.focusedTab.scope(), tabId));
    }

    public DatabaseSearchConfig searchConfigFor(DatabaseScopedTabRef scopedTab) {
        return this.tabStateFor(scopedTab).searchConfig();
    }

    public DatabaseSearchConfig searchConfigFor(String tabId) {
        return this.searchConfigFor(DatabaseScopedTabRef.concreteTab(this.focusedTab.scope(), tabId));
    }

    public int pageIndexFor(DatabaseScopedTabRef scopedTab) {
        return this.tabStateFor(scopedTab).pageIndex();
    }

    public int pageIndexFor(String tabId) {
        return this.pageIndexFor(DatabaseScopedTabRef.concreteTab(this.focusedTab.scope(), tabId));
    }

    public int pageSizeFor(DatabaseScopedTabRef scopedTab) {
        return this.tabStateFor(scopedTab).pageSize();
    }

    public int pageSizeFor(String tabId) {
        return this.pageSizeFor(DatabaseScopedTabRef.concreteTab(this.focusedTab.scope(), tabId));
    }

    public DatabaseSortOption sortOption() {
        return this.sortOptionFor(this.focusedTab);
    }

    public String searchText() {
        return this.searchTextFor(this.focusedTab);
    }

    public DatabaseSearchConfig searchConfig() {
        return this.searchConfigFor(this.focusedTab);
    }

    public int pageIndex() {
        return this.pageIndexFor(this.focusedTab);
    }

    public int pageSize() {
        return this.pageSizeFor(this.focusedTab);
    }

    public boolean isMultiTabView() {
        return this.visibleTabs.size() > 1;
    }

    public DatabaseQuery withFocusedTab(DatabaseScopedTabRef nextFocusedTab) {
        DatabaseScopedTabRef normalizedFocusedTab = DatabaseQuerySupport.normalizeScopedTab(nextFocusedTab, DatabaseScopedTabRef.defaultTab());
        java.util.ArrayList<DatabaseScopedTabRef> nextVisibleTabs = new java.util.ArrayList<>(this.visibleTabs);
        if (!nextVisibleTabs.contains(normalizedFocusedTab)) {
            nextVisibleTabs.clear();
            nextVisibleTabs.add(normalizedFocusedTab);
        }
        return new DatabaseQuery(normalizedFocusedTab, nextVisibleTabs, this.tabStates);
    }

    public DatabaseQuery withFocusedTabId(String nextFocusedTabId) {
        return this.withFocusedTab(DatabaseScopedTabRef.concreteTab(this.focusedTab.scope(), nextFocusedTabId));
    }

    public DatabaseQuery withSingleVisibleTab(DatabaseScopedTabRef scopedTab) {
        DatabaseScopedTabRef normalizedScopedTab = DatabaseQuerySupport.normalizeScopedTab(scopedTab, DatabaseScopedTabRef.defaultTab());
        return new DatabaseQuery(normalizedScopedTab, List.of(normalizedScopedTab), this.tabStates);
    }

    public DatabaseQuery withSingleVisibleTab(String tabId) {
        return this.withSingleVisibleTab(DatabaseScopedTabRef.concreteTab(this.focusedTab.scope(), tabId));
    }

    public DatabaseQuery withVisibleTabs(List<DatabaseScopedTabRef> nextVisibleTabs) {
        List<DatabaseScopedTabRef> normalizedVisibleTabs = DatabaseQuerySupport.normalizeVisibleTabs(nextVisibleTabs, this.focusedTab);
        DatabaseScopedTabRef normalizedFocusedTab = normalizedVisibleTabs.contains(this.focusedTab)
                ? this.focusedTab
                : normalizedVisibleTabs.getFirst();
        return new DatabaseQuery(normalizedFocusedTab, normalizedVisibleTabs, this.tabStates);
    }

    public DatabaseQuery withVisibleTabIds(List<String> nextVisibleTabIds) {
        return this.withVisibleTabs(DatabaseQuerySupport.toScopedRefs(this.focusedTab.scope(), nextVisibleTabIds));
    }

    public DatabaseQuery withTabState(DatabaseScopedTabRef scopedTab, DatabaseTabQueryState nextState) {
        LinkedHashMap<DatabaseScopedTabRef, DatabaseTabQueryState> nextTabStates = new LinkedHashMap<>(this.tabStates);
        nextTabStates.put(
                DatabaseQuerySupport.normalizeScopedTab(scopedTab, this.focusedTab),
                nextState == null ? DatabaseTabQueryState.defaultState() : nextState
        );
        return new DatabaseQuery(this.focusedTab, this.visibleTabs, nextTabStates);
    }

    public DatabaseQuery withSortOption(DatabaseScopedTabRef scopedTab, DatabaseSortOption nextSortOption) {
        DatabaseScopedTabRef normalizedScopedTab = DatabaseQuerySupport.normalizeScopedTab(scopedTab, this.focusedTab);
        return this.withTabState(normalizedScopedTab, this.tabStateFor(normalizedScopedTab).withSortOption(nextSortOption));
    }

    public DatabaseQuery withSortOption(String tabId, DatabaseSortOption nextSortOption) {
        return this.withSortOption(DatabaseScopedTabRef.concreteTab(this.focusedTab.scope(), tabId), nextSortOption);
    }

    public DatabaseQuery withSortOption(DatabaseSortOption nextSortOption) {
        return this.withSortOption(this.focusedTab, nextSortOption);
    }

    public DatabaseQuery withSearchText(DatabaseScopedTabRef scopedTab, String nextSearchText) {
        DatabaseScopedTabRef normalizedScopedTab = DatabaseQuerySupport.normalizeScopedTab(scopedTab, this.focusedTab);
        return this.withTabState(normalizedScopedTab, this.tabStateFor(normalizedScopedTab).withSearchText(nextSearchText));
    }

    public DatabaseQuery withSearchText(String tabId, String nextSearchText) {
        return this.withSearchText(DatabaseScopedTabRef.concreteTab(this.focusedTab.scope(), tabId), nextSearchText);
    }

    public DatabaseQuery withSearchText(String nextSearchText) {
        return this.withSearchText(this.focusedTab, nextSearchText);
    }

    public DatabaseQuery withSearchConfig(DatabaseScopedTabRef scopedTab, DatabaseSearchConfig nextSearchConfig) {
        DatabaseScopedTabRef normalizedScopedTab = DatabaseQuerySupport.normalizeScopedTab(scopedTab, this.focusedTab);
        return this.withTabState(normalizedScopedTab, this.tabStateFor(normalizedScopedTab).withSearchConfig(nextSearchConfig));
    }

    public DatabaseQuery withSearchConfig(String tabId, DatabaseSearchConfig nextSearchConfig) {
        return this.withSearchConfig(DatabaseScopedTabRef.concreteTab(this.focusedTab.scope(), tabId), nextSearchConfig);
    }

    public DatabaseQuery withSearchConfig(DatabaseSearchConfig nextSearchConfig) {
        return this.withSearchConfig(this.focusedTab, nextSearchConfig);
    }

    public DatabaseQuery withPageIndex(DatabaseScopedTabRef scopedTab, int nextPageIndex) {
        DatabaseScopedTabRef normalizedScopedTab = DatabaseQuerySupport.normalizeScopedTab(scopedTab, this.focusedTab);
        return this.withTabState(normalizedScopedTab, this.tabStateFor(normalizedScopedTab).withPageIndex(nextPageIndex));
    }

    public DatabaseQuery withPageIndex(String tabId, int nextPageIndex) {
        return this.withPageIndex(DatabaseScopedTabRef.concreteTab(this.focusedTab.scope(), tabId), nextPageIndex);
    }

    public DatabaseQuery withPageIndex(int nextPageIndex) {
        return this.withPageIndex(this.focusedTab, nextPageIndex);
    }

    public DatabaseQuery withPageSize(DatabaseScopedTabRef scopedTab, int nextPageSize) {
        DatabaseScopedTabRef normalizedScopedTab = DatabaseQuerySupport.normalizeScopedTab(scopedTab, this.focusedTab);
        return this.withTabState(normalizedScopedTab, this.tabStateFor(normalizedScopedTab).withPageSize(nextPageSize));
    }

    public DatabaseQuery withPageSize(String tabId, int nextPageSize) {
        return this.withPageSize(DatabaseScopedTabRef.concreteTab(this.focusedTab.scope(), tabId), nextPageSize);
    }

    public DatabaseQuery withPageSize(int nextPageSize) {
        return this.withPageSize(this.focusedTab, nextPageSize);
    }

    public DatabaseQuery withPanelLayout(
            Map<DatabaseScopedTabRef, Integer> nextPageIndexes,
            Map<DatabaseScopedTabRef, Integer> nextPageSizes
    ) {
        LinkedHashMap<DatabaseScopedTabRef, DatabaseTabQueryState> nextTabStates = new LinkedHashMap<>(this.tabStates);
        LinkedHashSet<DatabaseScopedTabRef> targetTabs = new LinkedHashSet<>();
        if (nextPageIndexes != null) {
            targetTabs.addAll(nextPageIndexes.keySet());
        }
        if (nextPageSizes != null) {
            targetTabs.addAll(nextPageSizes.keySet());
        }
        for (DatabaseScopedTabRef targetTab : targetTabs) {
            DatabaseScopedTabRef normalizedScopedTab = DatabaseQuerySupport.normalizeScopedTab(targetTab, this.focusedTab);
            DatabaseTabQueryState currentState = this.tabStateFor(normalizedScopedTab);
            int nextPageIndex = nextPageIndexes != null && nextPageIndexes.containsKey(targetTab)
                    ? nextPageIndexes.get(targetTab)
                    : currentState.pageIndex();
            int nextPageSize = nextPageSizes != null && nextPageSizes.containsKey(targetTab)
                    ? nextPageSizes.get(targetTab)
                    : currentState.pageSize();
            nextTabStates.put(
                    normalizedScopedTab,
                    currentState.withPageIndex(nextPageIndex).withPageSize(nextPageSize)
            );
        }
        return new DatabaseQuery(this.focusedTab, this.visibleTabs, nextTabStates);
    }

    public DatabaseQuery retargetScope(DatabaseScope nextScope) {
        return DatabaseQuerySupport.retargetScope(this, nextScope);
    }

    public DatabaseQuery queryForScope(DatabaseScope scope) {
        return DatabaseQuerySupport.queryForScope(this, scope);
    }

    public static DatabaseQuery read(FriendlyByteBuf buffer) {
        return DatabaseQuerySupport.read(buffer);
    }

    public static void write(FriendlyByteBuf buffer, DatabaseQuery query) {
        DatabaseQuerySupport.write(buffer, query);
    }

    public CompoundTag toTag() {
        return DatabaseQuerySupport.toTag(this);
    }

    public static DatabaseQuery fromTag(CompoundTag tag, DatabaseScope fallbackScope) {
        return DatabaseQuerySupport.fromTag(tag, fallbackScope);
    }
}
