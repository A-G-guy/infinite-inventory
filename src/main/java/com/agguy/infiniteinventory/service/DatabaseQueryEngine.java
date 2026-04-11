package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabaseCategory;
import com.agguy.infiniteinventory.database.DatabasePage;
import com.agguy.infiniteinventory.database.DatabasePageEntry;
import com.agguy.infiniteinventory.database.DatabasePagination;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseSearchConfig;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackEntry;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.database.VisibleDatabaseEntry;
import com.agguy.infiniteinventory.service.search.DatabaseItemSearchResolver;
import com.agguy.infiniteinventory.service.search.DatabaseSearchEvaluator;
import com.agguy.infiniteinventory.service.search.DatabaseSearchIndex;
import com.agguy.infiniteinventory.service.search.DatabaseSearchRanking;
import com.agguy.infiniteinventory.service.search.SearchTextNormalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
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
    private final Map<StoredItemDatabase, DatabaseRuntimeIndex> runtimeIndexes = new WeakHashMap<>();

    private DatabaseQueryEngine() {
    }

    public DatabasePage buildPage(StoredItemDatabase database, DatabaseQuery query) {
        StoredItemDatabase resolvedDatabase = database == null ? new StoredItemDatabase() : database;
        DatabaseQuery normalizedQuery = query == null ? DatabaseQuery.defaultQuery() : query;
        CachedQueryResult queryResult = this.resolveQueryResult(this.runtimeIndexFor(resolvedDatabase), normalizedQuery);
        return this.toPage(normalizedQuery, queryResult);
    }

    private synchronized DatabaseRuntimeIndex runtimeIndexFor(StoredItemDatabase database) {
        DatabaseRuntimeIndex cachedIndex = this.runtimeIndexes.get(database);
        if (cachedIndex != null && cachedIndex.revision() == database.revision()) {
            return cachedIndex;
        }
        DatabaseRuntimeIndex rebuiltIndex = DatabaseRuntimeIndex.build(database);
        this.runtimeIndexes.put(database, rebuiltIndex);
        return rebuiltIndex;
    }

    private CachedQueryResult resolveQueryResult(DatabaseRuntimeIndex runtimeIndex, DatabaseQuery query) {
        if (SearchTextNormalizer.splitTerms(query.searchText()).isEmpty()) {
            return this.resolveNoSearchResult(runtimeIndex, query);
        }
        return this.resolveSearchResult(runtimeIndex, query);
    }

    private CachedQueryResult resolveNoSearchResult(DatabaseRuntimeIndex runtimeIndex, DatabaseQuery query) {
        DatabaseCategory normalizedCategory = normalizeCategory(query.category());
        return runtimeIndex.noSearchResult(normalizedCategory, query.sortOption(), () -> {
            List<ResolvedQueryRecord> sortedRecords = new ArrayList<>(runtimeIndex.recordsFor(normalizedCategory).size());
            for (DatabaseRuntimeEntryRecord entryRecord : runtimeIndex.recordsFor(normalizedCategory)) {
                sortedRecords.add(new ResolvedQueryRecord(entryRecord, entryRecord.baseSortSnapshot()));
            }
            sortedRecords.sort(Comparator.comparing(ResolvedQueryRecord::sortSnapshot, this.entrySorter.comparatorFor(query)));
            return new CachedQueryResult(sortedRecords, runtimeIndex.totalItemsFor(normalizedCategory));
        });
    }

    private CachedQueryResult resolveSearchResult(DatabaseRuntimeIndex runtimeIndex, DatabaseQuery query) {
        QueryFingerprint fingerprint = QueryFingerprint.of(query);
        CachedQueryResult cachedResult = runtimeIndex.searchResult(fingerprint);
        if (cachedResult != null) {
            return cachedResult;
        }

        DatabaseCategory normalizedCategory = normalizeCategory(query.category());
        List<ResolvedQueryRecord> matchedRecords = new ArrayList<>();
        long totalItems = 0L;
        for (DatabaseRuntimeEntryRecord entryRecord : runtimeIndex.recordsFor(normalizedCategory)) {
            DatabaseSearchRanking ranking = this.searchEvaluator.evaluate(query, entryRecord.searchIndex(), entryRecord.entry().amount());
            if (!ranking.matched()) {
                continue;
            }
            matchedRecords.add(new ResolvedQueryRecord(entryRecord, entryRecord.baseSortSnapshot().withSearchRanking(ranking)));
            totalItems = safeAdd(totalItems, entryRecord.entry().amount());
        }
        matchedRecords.sort(Comparator.comparing(ResolvedQueryRecord::sortSnapshot, this.entrySorter.comparatorFor(query)));

        CachedQueryResult builtResult = new CachedQueryResult(matchedRecords, totalItems);
        runtimeIndex.cacheSearchResult(fingerprint, builtResult);
        return builtResult;
    }

    private DatabasePage toPage(DatabaseQuery query, CachedQueryResult queryResult) {
        int safePageSize = Math.max(1, query.pageSize());
        int totalEntries = queryResult.records().size();
        int totalPages = DatabasePagination.resolveTotalPages(totalEntries, safePageSize);
        int pageIndex = Math.min(query.pageIndex(), totalPages - 1);
        DatabaseQuery resolvedQuery = query.withPageSize(safePageSize).withPageIndex(pageIndex);
        int fromIndex = Math.min(pageIndex * safePageSize, totalEntries);
        int toIndex = Math.min(fromIndex + safePageSize, totalEntries);

        List<DatabasePageEntry> pageEntries = new ArrayList<>(safePageSize);
        for (int index = fromIndex; index < toIndex; index++) {
            ResolvedQueryRecord record = queryResult.records().get(index);
            pageEntries.add(record.entryRecord().toPageEntry());
        }
        return new DatabasePage(resolvedQuery, totalEntries, totalPages, queryResult.totalItems(), pageEntries);
    }

    private static DatabaseCategory normalizeCategory(DatabaseCategory category) {
        return category == null ? DatabaseCategory.ALL : category;
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
            DatabaseCategory category,
            DatabaseSortOption sortOption,
            String normalizedSearchText,
            DatabaseSearchConfig searchConfig
    ) {
        private static QueryFingerprint of(DatabaseQuery query) {
            return new QueryFingerprint(
                    normalizeCategory(query.category()),
                    query.sortOption(),
                    SearchTextNormalizer.normalizeQueryText(query.searchText()),
                    query.searchConfig()
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
            DatabaseSearchIndex searchIndex,
            DatabaseSortSnapshot baseSortSnapshot
    ) {
        private static DatabaseRuntimeEntryRecord of(StoredStackKey key, StoredStackEntry entry) {
            ItemStack displayStack = key.displayStack();
            DatabaseSearchIndex searchIndex = DatabaseItemSearchResolver.INSTANCE.resolve(key);
            return new DatabaseRuntimeEntryRecord(
                    key,
                    entry,
                    displayStack,
                    searchIndex,
                    new DatabaseSortSnapshot(
                            searchIndex.displayNameNormalized(),
                            key.registryName(),
                            key.registryNamespace(),
                            key.registryPath(),
                            entry.amount(),
                            entry.lastModified(),
                            key.hashCode(),
                            DatabaseSearchRanking.unfiltered()
                    )
            );
        }

        private DatabaseCategory category() {
            return this.entry.category();
        }

        private DatabasePageEntry toPageEntry() {
            return new DatabasePageEntry(
                    this.key,
                    new VisibleDatabaseEntry(this.displayStack.copyWithCount(1), this.entry.amount(), this.entry.category(), this.key.registryName())
            );
        }
    }

    private static final class DatabaseRuntimeIndex {
        private final long revision;
        private final EnumMap<DatabaseCategory, List<DatabaseRuntimeEntryRecord>> categoryBuckets;
        private final EnumMap<DatabaseCategory, Long> categoryTotals;
        private final EnumMap<DatabaseCategory, EnumMap<DatabaseSortOption, CachedQueryResult>> noSearchSortedCache = new EnumMap<>(DatabaseCategory.class);
        private final LinkedHashMap<QueryFingerprint, CachedQueryResult> searchCache =
                new LinkedHashMap<>(16, 0.75F, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<QueryFingerprint, CachedQueryResult> eldest) {
                        return this.size() > MAX_SEARCH_CACHE_SIZE;
                    }
                };

        private DatabaseRuntimeIndex(
                long revision,
                EnumMap<DatabaseCategory, List<DatabaseRuntimeEntryRecord>> categoryBuckets,
                EnumMap<DatabaseCategory, Long> categoryTotals
        ) {
            this.revision = Math.max(0L, revision);
            this.categoryBuckets = categoryBuckets;
            this.categoryTotals = categoryTotals;
        }

        private long revision() {
            return this.revision;
        }

        private List<DatabaseRuntimeEntryRecord> recordsFor(DatabaseCategory category) {
            return this.categoryBuckets.getOrDefault(normalizeCategory(category), List.of());
        }

        private long totalItemsFor(DatabaseCategory category) {
            return this.categoryTotals.getOrDefault(normalizeCategory(category), 0L);
        }

        private CachedQueryResult noSearchResult(
                DatabaseCategory category,
                DatabaseSortOption sortOption,
                Supplier<CachedQueryResult> builder
        ) {
            EnumMap<DatabaseSortOption, CachedQueryResult> categoryCache =
                    this.noSearchSortedCache.computeIfAbsent(normalizeCategory(category), ignored -> new EnumMap<>(DatabaseSortOption.class));
            return categoryCache.computeIfAbsent(sortOption, ignored -> builder.get());
        }

        private CachedQueryResult searchResult(QueryFingerprint fingerprint) {
            return this.searchCache.get(fingerprint);
        }

        private void cacheSearchResult(QueryFingerprint fingerprint, CachedQueryResult result) {
            this.searchCache.put(fingerprint, result);
        }

        private static DatabaseRuntimeIndex build(StoredItemDatabase database) {
            EnumMap<DatabaseCategory, List<DatabaseRuntimeEntryRecord>> categoryBuckets = new EnumMap<>(DatabaseCategory.class);
            EnumMap<DatabaseCategory, Long> categoryTotals = new EnumMap<>(DatabaseCategory.class);
            for (DatabaseCategory category : DatabaseCategory.values()) {
                categoryBuckets.put(category, new ArrayList<>());
                categoryTotals.put(category, 0L);
            }

            for (Map.Entry<StoredStackKey, StoredStackEntry> mapEntry : database.entries().entrySet()) {
                DatabaseRuntimeEntryRecord record = DatabaseRuntimeEntryRecord.of(mapEntry.getKey(), mapEntry.getValue());
                DatabaseCategory category = normalizeCategory(record.category());
                categoryBuckets.get(DatabaseCategory.ALL).add(record);
                categoryBuckets.get(category).add(record);
                categoryTotals.put(DatabaseCategory.ALL, safeAdd(categoryTotals.get(DatabaseCategory.ALL), record.entry().amount()));
                categoryTotals.put(category, safeAdd(categoryTotals.get(category), record.entry().amount()));
            }

            EnumMap<DatabaseCategory, List<DatabaseRuntimeEntryRecord>> immutableBuckets = new EnumMap<>(DatabaseCategory.class);
            for (Map.Entry<DatabaseCategory, List<DatabaseRuntimeEntryRecord>> entry : categoryBuckets.entrySet()) {
                immutableBuckets.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            return new DatabaseRuntimeIndex(database.revision(), immutableBuckets, categoryTotals);
        }
    }
}
