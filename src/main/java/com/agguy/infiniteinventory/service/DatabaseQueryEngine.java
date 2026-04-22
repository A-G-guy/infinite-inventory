package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabasePage;
import com.agguy.infiniteinventory.database.DatabasePageEntry;
import com.agguy.infiniteinventory.database.DatabasePagination;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSearchConfig;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabDirectory;
import com.agguy.infiniteinventory.database.DatabaseTabQueryState;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackEntry;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.database.VisibleDatabaseEntry;
import com.agguy.infiniteinventory.localization.ViewerLanguage;
import com.agguy.infiniteinventory.service.search.DatabaseItemSearchMetadata;
import com.agguy.infiniteinventory.service.search.DatabaseItemSearchMetadataResolver;
import com.agguy.infiniteinventory.service.search.DatabaseItemSearchResolver;
import com.agguy.infiniteinventory.service.search.DatabaseParsedSearchQuery;
import com.agguy.infiniteinventory.service.search.DatabaseSearchEnvironment;
import com.agguy.infiniteinventory.service.search.DatabaseSearchEnvironmentSignature;
import com.agguy.infiniteinventory.service.search.DatabaseSearchEvaluator;
import com.agguy.infiniteinventory.service.search.DatabaseSearchExpressionEvaluator;
import com.agguy.infiniteinventory.service.search.DatabaseSearchIndex;
import com.agguy.infiniteinventory.service.search.DatabaseSearchQueryParser;
import com.agguy.infiniteinventory.service.search.DatabaseSearchQueryParserContext;
import com.agguy.infiniteinventory.service.search.DatabaseSearchRanking;
import com.agguy.infiniteinventory.service.search.SearchTextNormalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;

public final class DatabaseQueryEngine {
    public static final DatabaseQueryEngine INSTANCE = new DatabaseQueryEngine();

    private static final int MAX_SEARCH_CACHE_SIZE = 32;

    private final DatabaseEntrySorter entrySorter = DatabaseEntrySorter.INSTANCE;
    private final DatabaseSearchEvaluator searchEvaluator = new DatabaseSearchEvaluator();
    private final DatabaseSearchExpressionEvaluator searchExpressionEvaluator = DatabaseSearchExpressionEvaluator.INSTANCE;
    private final Map<StoredItemDatabase, LocalizedRuntimeIndexes> runtimeIndexes = new WeakHashMap<>();

    private DatabaseQueryEngine() {
    }

    public DatabasePage buildPage(
            StoredItemDatabase database,
            DatabaseTabDirectory tabDirectory,
            DatabaseQuery query,
            DatabaseScopedTabRef scopedTab
    ) {
        return this.buildPage(
                database,
                tabDirectory,
                query,
                scopedTab,
                ViewerLanguage.defaultLanguage(),
                DatabaseSearchEnvironment.defaultEnvironment()
        );
    }

    public DatabasePage buildPage(
            StoredItemDatabase database,
            DatabaseTabDirectory tabDirectory,
            DatabaseQuery query,
            DatabaseScopedTabRef scopedTab,
            ViewerLanguage viewerLanguage
    ) {
        return this.buildPage(
                database,
                tabDirectory,
                query,
                scopedTab,
                viewerLanguage,
                DatabaseSearchEnvironment.defaultEnvironment()
        );
    }

    public DatabasePage buildPage(
            StoredItemDatabase database,
            DatabaseTabDirectory tabDirectory,
            DatabaseQuery query,
            DatabaseScopedTabRef scopedTab,
            ViewerLanguage viewerLanguage,
            DatabaseSearchEnvironment searchEnvironment
    ) {
        StoredItemDatabase resolvedDatabase = database == null ? new StoredItemDatabase() : database;
        DatabaseTabDirectory resolvedTabDirectory = tabDirectory == null ? new DatabaseTabDirectory() : tabDirectory;
        DatabaseQuery normalizedQuery = resolvedTabDirectory.sanitizeQuery(query == null ? DatabaseQuery.defaultQuery() : query);
        DatabaseScopedTabRef normalizedScopedTab = normalizeScopedTab(scopedTab, resolvedTabDirectory, normalizedQuery.focusedTab().scope());
        String normalizedTabId = normalizedScopedTab.tabId();
        CachedQueryResult queryResult = this.resolveQueryResult(
                this.runtimeIndexFor(resolvedDatabase, viewerLanguage),
                normalizedQuery.tabStateFor(normalizedScopedTab),
                normalizedTabId,
                searchEnvironment
        );
        return this.toPage(normalizedQuery, queryResult, normalizedScopedTab, resolvedTabDirectory.resolve(normalizedTabId));
    }

    private synchronized DatabaseRuntimeIndex runtimeIndexFor(StoredItemDatabase database, ViewerLanguage viewerLanguage) {
        ViewerLanguage normalizedLanguage = viewerLanguage == null ? ViewerLanguage.defaultLanguage() : viewerLanguage;
        LocalizedRuntimeIndexes cachedIndexes = this.runtimeIndexes.get(database);
        if (cachedIndexes != null && cachedIndexes.revision() == database.revision()) {
            DatabaseRuntimeIndex cachedIndex = cachedIndexes.indexFor(normalizedLanguage);
            if (cachedIndex != null) {
                return cachedIndex;
            }
        }

        LocalizedRuntimeIndexes activeIndexes = cachedIndexes;
        if (activeIndexes == null || activeIndexes.revision() != database.revision()) {
            activeIndexes = new LocalizedRuntimeIndexes(database.revision());
            this.runtimeIndexes.put(database, activeIndexes);
        }

        DatabaseRuntimeIndex rebuiltIndex = DatabaseRuntimeIndex.build(database, normalizedLanguage);
        activeIndexes.put(normalizedLanguage, rebuiltIndex);
        return rebuiltIndex;
    }

    private CachedQueryResult resolveQueryResult(
            DatabaseRuntimeIndex runtimeIndex,
            DatabaseTabQueryState tabQueryState,
            String tabId,
            DatabaseSearchEnvironment searchEnvironment
    ) {
        if (tabQueryState.searchText().isBlank()) {
            return this.resolveNoSearchResult(runtimeIndex, tabQueryState, tabId);
        }
        DatabaseParsedSearchQuery parsedQuery = DatabaseSearchQueryParser.INSTANCE.parse(
                tabQueryState.searchText(),
                DatabaseSearchQueryParserContext.fromQuery(tabQueryState.searchText(), searchEnvironment)
        );
        if (!parsedQuery.active()) {
            return this.resolveNoSearchResult(runtimeIndex, tabQueryState, tabId);
        }
        return this.resolveSearchResult(runtimeIndex, tabQueryState, tabId, parsedQuery, searchEnvironment);
    }

    private CachedQueryResult resolveNoSearchResult(DatabaseRuntimeIndex runtimeIndex, DatabaseTabQueryState tabQueryState, String tabId) {
        String normalizedTabId = normalizeTabId(tabId, null);
        return runtimeIndex.noSearchResult(normalizedTabId, tabQueryState.sortOption(), () -> {
            List<ResolvedQueryRecord> sortedRecords = new ArrayList<>(runtimeIndex.recordsFor(normalizedTabId).size());
            for (DatabaseRuntimeEntryRecord entryRecord : runtimeIndex.recordsFor(normalizedTabId)) {
                sortedRecords.add(new ResolvedQueryRecord(entryRecord, entryRecord.baseSortSnapshot()));
            }
            sortedRecords.sort(Comparator.comparing(ResolvedQueryRecord::sortSnapshot, this.entrySorter.comparatorFor(tabQueryState)));
            return new CachedQueryResult(sortedRecords, runtimeIndex.totalItemsFor(normalizedTabId));
        });
    }

    private CachedQueryResult resolveSearchResult(
            DatabaseRuntimeIndex runtimeIndex,
            DatabaseTabQueryState tabQueryState,
            String tabId,
            DatabaseParsedSearchQuery parsedQuery,
            DatabaseSearchEnvironment searchEnvironment
    ) {
        String normalizedTabId = normalizeTabId(tabId, null);
        QueryFingerprint fingerprint = QueryFingerprint.of(tabQueryState, normalizedTabId, parsedQuery, searchEnvironment);
        CachedQueryResult cachedResult = runtimeIndex.searchResult(fingerprint);
        if (cachedResult != null) {
            return cachedResult;
        }

        List<ResolvedQueryRecord> matchedRecords = new ArrayList<>();
        long totalItems = 0L;
        for (DatabaseRuntimeEntryRecord entryRecord : runtimeIndex.recordsFor(normalizedTabId)) {
            DatabaseSearchRanking ranking = this.searchExpressionEvaluator.evaluate(
                    tabQueryState,
                    parsedQuery,
                    entryRecord.searchIndex(),
                    entryRecord.searchMetadata(),
                    entryRecord.key(),
                    entryRecord.entry().amount(),
                    searchEnvironment
            );
            if (!ranking.matched()) {
                continue;
            }
            matchedRecords.add(new ResolvedQueryRecord(entryRecord, entryRecord.baseSortSnapshot().withSearchRanking(ranking)));
            totalItems = safeAdd(totalItems, entryRecord.entry().amount());
        }
        matchedRecords.sort(Comparator.comparing(ResolvedQueryRecord::sortSnapshot, this.entrySorter.comparatorFor(tabQueryState)));

        CachedQueryResult builtResult = new CachedQueryResult(matchedRecords, totalItems);
        runtimeIndex.cacheSearchResult(fingerprint, builtResult);
        return builtResult;
    }

    private DatabasePage toPage(
            DatabaseQuery query,
            CachedQueryResult queryResult,
            DatabaseScopedTabRef scopedTab,
            DatabaseTab tab
    ) {
        int safePageSize = Math.max(1, query.pageSizeFor(scopedTab));
        int totalEntries = queryResult.records().size();
        int totalPages = DatabasePagination.resolveTotalPages(totalEntries, safePageSize);
        int pageIndex = Math.min(query.pageIndexFor(scopedTab), totalPages - 1);
        int fromIndex = resolvePageFromIndex(pageIndex, safePageSize, totalEntries);
        int toIndex = resolvePageToIndex(fromIndex, safePageSize, totalEntries);

        List<DatabasePageEntry> pageEntries = new ArrayList<>(safePageSize);
        for (int index = fromIndex; index < toIndex; index++) {
            ResolvedQueryRecord record = queryResult.records().get(index);
            pageEntries.add(record.entryRecord().toPageEntry(scopedTab.scope()));
        }
        return new DatabasePage(scopedTab, tab, pageIndex, safePageSize, totalEntries, totalPages, queryResult.totalItems(), pageEntries);
    }

    private static int resolvePageFromIndex(int pageIndex, int pageSize, int totalEntries) {
        long startIndex = Math.min((long) Math.max(0, pageIndex) * Math.max(1L, pageSize), Math.max(0L, totalEntries));
        return (int) startIndex;
    }

    private static int resolvePageToIndex(int fromIndex, int pageSize, int totalEntries) {
        long endIndex = Math.min((long) Math.max(0L, totalEntries), (long) Math.max(0L, fromIndex) + Math.max(1L, pageSize));
        return (int) endIndex;
    }

    private static String normalizeTabId(String tabId, DatabaseTabDirectory tabDirectory) {
        if (tabDirectory == null) {
            return DatabaseTabs.isAllTabId(tabId) ? DatabaseTabs.ALL_TAB_ID : DatabaseTabs.normalizeConcreteTarget(tabId);
        }
        String resolvedVisibleTabId = tabDirectory.resolveVisibleTabId(tabId);
        if (resolvedVisibleTabId != null) {
            return resolvedVisibleTabId;
        }
        return tabDirectory.defaultConcreteTab().id();
    }

    private static DatabaseScopedTabRef normalizeScopedTab(
            DatabaseScopedTabRef scopedTab,
            DatabaseTabDirectory tabDirectory,
            DatabaseScope fallbackScope
    ) {
        if (scopedTab == null) {
            return DatabaseScopedTabRef.allTab(fallbackScope);
        }
        return DatabaseScopedTabRef.concreteTab(scopedTab.scope(), normalizeTabId(scopedTab.tabId(), tabDirectory));
    }

    private static long safeAdd(long left, long right) {
        if (right <= 0L) {
            return left;
        }
        if (Long.MAX_VALUE - left < right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private record QueryFingerprint(
            String tabId,
            DatabaseSortOption sortOption,
            String normalizedSearchExpression,
            DatabaseSearchConfig searchConfig,
            DatabaseSearchEnvironmentSignature searchEnvironmentSignature
    ) {
        private static QueryFingerprint of(
                DatabaseTabQueryState tabQueryState,
                String tabId,
                DatabaseParsedSearchQuery parsedQuery,
                DatabaseSearchEnvironment searchEnvironment
        ) {
            return new QueryFingerprint(
                    normalizeTabId(tabId, null),
                    tabQueryState.sortOption(),
                    parsedQuery == null ? "" : parsedQuery.normalizedExpression(),
                    tabQueryState.searchConfig(),
                    (searchEnvironment == null ? DatabaseSearchEnvironment.defaultEnvironment() : searchEnvironment).signature()
            );
        }
    }

    private record CachedQueryResult(List<ResolvedQueryRecord> records, long totalItems) {
        private CachedQueryResult(List<ResolvedQueryRecord> records, long totalItems) {
            this.records = List.copyOf(records);
            this.totalItems = Math.max(0L, totalItems);
        }
    }

    private record ResolvedQueryRecord(DatabaseRuntimeEntryRecord entryRecord, DatabaseSortSnapshot sortSnapshot) {
    }

    private record DatabaseRuntimeEntryRecord(
            StoredStackKey key,
            StoredStackEntry entry,
            ItemStack displayStack,
            String note,
            boolean starred,
            DatabaseSearchIndex searchIndex,
            DatabaseItemSearchMetadata searchMetadata,
            DatabaseSortSnapshot baseSortSnapshot
    ) {
        private static DatabaseRuntimeEntryRecord of(StoredStackKey key, StoredStackEntry entry, String note, boolean starred, ViewerLanguage viewerLanguage) {
            ItemStack displayStack = key.displayStack();
            DatabaseSearchIndex searchIndex = DatabaseItemSearchResolver.INSTANCE.resolve(key, viewerLanguage);
            DatabaseItemSearchMetadata searchMetadata = DatabaseItemSearchMetadataResolver.INSTANCE.resolve(key);
            return new DatabaseRuntimeEntryRecord(
                    key,
                    entry,
                    displayStack,
                    note,
                    starred,
                    searchIndex,
                    searchMetadata,
                    new DatabaseSortSnapshot(
                            searchIndex.displayNameNormalized(),
                            key.registryName(),
                            key.registryNamespace(),
                            key.registryPath(),
                            entry.amount(),
                            entry.firstAdded(),
                            entry.lastModified(),
                            key.hashCode(),
                            DatabaseSearchRanking.unfiltered()
                    )
            );
        }

        private DatabasePageEntry toPageEntry(DatabaseScope scope) {
            return new DatabasePageEntry(
                    this.key,
                    new VisibleDatabaseEntry(
                            scope,
                            this.displayStack.copyWithCount(1),
                            this.entry.amount(),
                            this.entry.tabId(),
                            this.key.registryName(),
                            this.note,
                            this.starred
                    )
            );
        }
    }

    private static final class DatabaseRuntimeIndex {
        private final long revision;
        private final Map<String, List<DatabaseRuntimeEntryRecord>> tabBuckets;
        private final Map<String, Long> tabTotals;
        private final Map<String, java.util.EnumMap<DatabaseSortOption, CachedQueryResult>> noSearchSortedCache = new LinkedHashMap<>();
        private final LinkedHashMap<QueryFingerprint, CachedQueryResult> searchCache =
                new LinkedHashMap<>(16, 0.75F, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<QueryFingerprint, CachedQueryResult> eldest) {
                        return this.size() > MAX_SEARCH_CACHE_SIZE;
                    }
                };

        private DatabaseRuntimeIndex(
                long revision,
                Map<String, List<DatabaseRuntimeEntryRecord>> tabBuckets,
                Map<String, Long> tabTotals
        ) {
            this.revision = Math.max(0L, revision);
            this.tabBuckets = tabBuckets;
            this.tabTotals = tabTotals;
        }

        private List<DatabaseRuntimeEntryRecord> recordsFor(String tabId) {
            return this.tabBuckets.getOrDefault(normalizeTabId(tabId, null), List.of());
        }

        private long totalItemsFor(String tabId) {
            return this.tabTotals.getOrDefault(normalizeTabId(tabId, null), 0L);
        }

        private CachedQueryResult noSearchResult(
                String tabId,
                DatabaseSortOption sortOption,
                Supplier<CachedQueryResult> builder
        ) {
            java.util.EnumMap<DatabaseSortOption, CachedQueryResult> tabCache =
                    this.noSearchSortedCache.computeIfAbsent(normalizeTabId(tabId, null), ignored -> new java.util.EnumMap<>(DatabaseSortOption.class));
            return tabCache.computeIfAbsent(sortOption, ignored -> builder.get());
        }

        private CachedQueryResult searchResult(QueryFingerprint fingerprint) {
            return this.searchCache.get(fingerprint);
        }

        private void cacheSearchResult(QueryFingerprint fingerprint, CachedQueryResult result) {
            this.searchCache.put(fingerprint, result);
        }

        private static DatabaseRuntimeIndex build(StoredItemDatabase database, ViewerLanguage viewerLanguage) {
            Map<String, List<DatabaseRuntimeEntryRecord>> tabBuckets = new LinkedHashMap<>();
            Map<String, Long> tabTotals = new LinkedHashMap<>();
            tabBuckets.put(DatabaseTabs.ALL_TAB_ID, new ArrayList<>());
            tabTotals.put(DatabaseTabs.ALL_TAB_ID, 0L);
            tabBuckets.put(DatabaseTabs.FAVORITES_TAB_ID, new ArrayList<>());
            tabTotals.put(DatabaseTabs.FAVORITES_TAB_ID, 0L);

            for (Map.Entry<StoredStackKey, StoredStackEntry> mapEntry : database.entries().entrySet()) {
                String note = database.noteFor(mapEntry.getKey());
                boolean starred = database.isStarred(mapEntry.getKey());
                DatabaseRuntimeEntryRecord record = DatabaseRuntimeEntryRecord.of(mapEntry.getKey(), mapEntry.getValue(), note, starred, viewerLanguage);
                String tabId = DatabaseTabs.normalizeConcreteTarget(record.entry().tabId());
                tabBuckets.computeIfAbsent(tabId, ignored -> new ArrayList<>()).add(record);
                tabBuckets.get(DatabaseTabs.ALL_TAB_ID).add(record);
                tabTotals.put(tabId, safeAdd(tabTotals.getOrDefault(tabId, 0L), record.entry().amount()));
                tabTotals.put(DatabaseTabs.ALL_TAB_ID, safeAdd(tabTotals.get(DatabaseTabs.ALL_TAB_ID), record.entry().amount()));
                if (record.starred) {
                    tabBuckets.get(DatabaseTabs.FAVORITES_TAB_ID).add(record);
                    tabTotals.put(DatabaseTabs.FAVORITES_TAB_ID, safeAdd(tabTotals.get(DatabaseTabs.FAVORITES_TAB_ID), record.entry().amount()));
                }
            }

            Map<String, List<DatabaseRuntimeEntryRecord>> immutableBuckets = new LinkedHashMap<>();
            for (Map.Entry<String, List<DatabaseRuntimeEntryRecord>> entry : tabBuckets.entrySet()) {
                immutableBuckets.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            return new DatabaseRuntimeIndex(database.revision(), Map.copyOf(immutableBuckets), Map.copyOf(tabTotals));
        }
    }

    private static final class LocalizedRuntimeIndexes {
        private final long revision;
        private final java.util.EnumMap<ViewerLanguage, DatabaseRuntimeIndex> localizedIndexes = new java.util.EnumMap<>(ViewerLanguage.class);

        private LocalizedRuntimeIndexes(long revision) {
            this.revision = Math.max(0L, revision);
        }

        private long revision() {
            return this.revision;
        }

        private DatabaseRuntimeIndex indexFor(ViewerLanguage viewerLanguage) {
            return this.localizedIndexes.get(viewerLanguage == null ? ViewerLanguage.defaultLanguage() : viewerLanguage);
        }

        private void put(ViewerLanguage viewerLanguage, DatabaseRuntimeIndex runtimeIndex) {
            this.localizedIndexes.put(viewerLanguage == null ? ViewerLanguage.defaultLanguage() : viewerLanguage, runtimeIndex);
        }
    }
}
