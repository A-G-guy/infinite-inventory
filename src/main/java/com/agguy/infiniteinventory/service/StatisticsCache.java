package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.statistics.DatabaseStatisticsSnapshot;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端统计缓存，按 (playerUUID, scope) 维度缓存统计快照。
 * 使用数据库 revision 作为缓存失效标记。
 */
public final class StatisticsCache {
    public static final StatisticsCache INSTANCE = new StatisticsCache();

    private final Map<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();

    private StatisticsCache() {
    }

    public DatabaseStatisticsSnapshot getOrCompute(
            UUID playerId,
            DatabaseScope scope,
            long currentDatabaseRevision,
            java.util.function.Supplier<DatabaseStatisticsSnapshot> compute
    ) {
        CacheKey key = new CacheKey(playerId, scope);
        CacheEntry entry = cache.get(key);
        if (entry != null && entry.databaseRevision == currentDatabaseRevision) {
            return entry.snapshot;
        }
        DatabaseStatisticsSnapshot snapshot = compute.get();
        cache.put(key, new CacheEntry(currentDatabaseRevision, snapshot));
        return snapshot;
    }

    public void invalidate(UUID playerId, DatabaseScope scope) {
        cache.remove(new CacheKey(playerId, scope));
    }

    public void invalidateAll(UUID playerId) {
        cache.keySet().removeIf(k -> k.playerId.equals(playerId));
    }

    public void clear() {
        cache.clear();
    }

    private record CacheKey(UUID playerId, DatabaseScope scope) {
    }

    private record CacheEntry(long databaseRevision, DatabaseStatisticsSnapshot snapshot) {
    }
}
