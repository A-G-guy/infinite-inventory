package com.agguy.infiniteinventory.service.tests;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.statistics.DatabaseStatisticsSnapshot;
import com.agguy.infiniteinventory.service.StatisticsCache;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StatisticsCache 单元测试。
 */
class StatisticsCacheTest {

    private final StatisticsCache cache = StatisticsCache.INSTANCE;
    private final UUID playerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cache.clear();
    }

    @Test
    void cacheMissShouldTriggerCompute() {
        DatabaseStatisticsSnapshot snapshot = createSnapshot(DatabaseScope.PERSONAL, 10, 100);

        DatabaseStatisticsSnapshot result = cache.getOrCompute(
                playerId, DatabaseScope.PERSONAL, 1L, () -> snapshot
        );

        assertSame(snapshot, result);
    }

    @Test
    void cacheHitWithSameRevisionShouldReturnCachedValue() {
        DatabaseStatisticsSnapshot first = createSnapshot(DatabaseScope.PERSONAL, 10, 100);
        cache.getOrCompute(playerId, DatabaseScope.PERSONAL, 1L, () -> first);

        DatabaseStatisticsSnapshot second = createSnapshot(DatabaseScope.PERSONAL, 20, 200);
        DatabaseStatisticsSnapshot result = cache.getOrCompute(
                playerId, DatabaseScope.PERSONAL, 1L, () -> second
        );

        assertSame(first, result);
    }

    @Test
    void cacheMissWithDifferentRevisionShouldRecompute() {
        DatabaseStatisticsSnapshot first = createSnapshot(DatabaseScope.PERSONAL, 10, 100);
        cache.getOrCompute(playerId, DatabaseScope.PERSONAL, 1L, () -> first);

        DatabaseStatisticsSnapshot second = createSnapshot(DatabaseScope.PERSONAL, 20, 200);
        DatabaseStatisticsSnapshot result = cache.getOrCompute(
                playerId, DatabaseScope.PERSONAL, 2L, () -> second
        );

        assertSame(second, result);
    }

    @Test
    void invalidateShouldRemoveCachedEntry() {
        DatabaseStatisticsSnapshot snapshot = createSnapshot(DatabaseScope.PERSONAL, 10, 100);
        cache.getOrCompute(playerId, DatabaseScope.PERSONAL, 1L, () -> snapshot);
        cache.invalidate(playerId, DatabaseScope.PERSONAL);

        DatabaseStatisticsSnapshot next = createSnapshot(DatabaseScope.PERSONAL, 20, 200);
        DatabaseStatisticsSnapshot result = cache.getOrCompute(
                playerId, DatabaseScope.PERSONAL, 1L, () -> next
        );

        assertSame(next, result);
    }

    @Test
    void invalidateAllShouldRemoveAllScopesForPlayer() {
        DatabaseStatisticsSnapshot personal = createSnapshot(DatabaseScope.PERSONAL, 10, 100);
        DatabaseStatisticsSnapshot publicSnapshot = createSnapshot(DatabaseScope.PUBLIC, 5, 50);
        cache.getOrCompute(playerId, DatabaseScope.PERSONAL, 1L, () -> personal);
        cache.getOrCompute(playerId, DatabaseScope.PUBLIC, 1L, () -> publicSnapshot);

        cache.invalidateAll(playerId);

        DatabaseStatisticsSnapshot nextPersonal = createSnapshot(DatabaseScope.PERSONAL, 20, 200);
        DatabaseStatisticsSnapshot result = cache.getOrCompute(
                playerId, DatabaseScope.PERSONAL, 1L, () -> nextPersonal
        );
        assertSame(nextPersonal, result);
    }

    @Test
    void differentPlayersShouldHaveIndependentCaches() {
        UUID otherPlayer = UUID.randomUUID();
        DatabaseStatisticsSnapshot snapshotA = createSnapshot(DatabaseScope.PERSONAL, 10, 100);
        DatabaseStatisticsSnapshot snapshotB = createSnapshot(DatabaseScope.PERSONAL, 20, 200);

        cache.getOrCompute(playerId, DatabaseScope.PERSONAL, 1L, () -> snapshotA);
        cache.getOrCompute(otherPlayer, DatabaseScope.PERSONAL, 1L, () -> snapshotB);

        DatabaseStatisticsSnapshot resultA = cache.getOrCompute(
                playerId, DatabaseScope.PERSONAL, 1L, () -> createSnapshot(DatabaseScope.PERSONAL, 99, 999)
        );
        DatabaseStatisticsSnapshot resultB = cache.getOrCompute(
                otherPlayer, DatabaseScope.PERSONAL, 1L, () -> createSnapshot(DatabaseScope.PERSONAL, 88, 888)
        );

        assertSame(snapshotA, resultA);
        assertSame(snapshotB, resultB);
    }

    @Test
    void clearShouldRemoveAllEntries() {
        cache.getOrCompute(playerId, DatabaseScope.PERSONAL, 1L,
                () -> createSnapshot(DatabaseScope.PERSONAL, 10, 100));
        cache.clear();

        DatabaseStatisticsSnapshot next = createSnapshot(DatabaseScope.PERSONAL, 20, 200);
        DatabaseStatisticsSnapshot result = cache.getOrCompute(
                playerId, DatabaseScope.PERSONAL, 1L, () -> next
        );
        assertSame(next, result);
    }

    private static DatabaseStatisticsSnapshot createSnapshot(DatabaseScope scope, long entries, long items) {
        return new DatabaseStatisticsSnapshot(
                scope, entries, items,
                List.of(), List.of(), List.of(), List.of(), List.of()
        );
    }
}
