package com.agguy.infiniteinventory.service.search.tests;

import com.agguy.infiniteinventory.service.search.DatabaseSearchRanking;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DatabaseSearchRanking 单元测试。
 */
class DatabaseSearchRankingTest {

    @Test
    void unfilteredShouldReturnMatchedInactiveInstance() {
        DatabaseSearchRanking ranking = DatabaseSearchRanking.unfiltered();
        assertTrue(ranking.matched());
        assertFalse(ranking.active());
        assertEquals(0, ranking.exactMatches());
        assertEquals(0, ranking.prefixMatches());
        assertEquals(0, ranking.containsMatches());
        assertEquals(0, ranking.fuzzyMatches());
        assertEquals(0.0D, ranking.textScore());
        assertEquals(0.0D, ranking.countBoostScore());
    }

    @Test
    void noMatchShouldReturnUnmatchedActiveInstance() {
        DatabaseSearchRanking ranking = DatabaseSearchRanking.noMatch();
        assertFalse(ranking.matched());
        assertTrue(ranking.active());
        assertEquals(0, ranking.exactMatches());
        assertEquals(0, ranking.prefixMatches());
        assertEquals(0, ranking.containsMatches());
        assertEquals(0, ranking.fuzzyMatches());
        assertEquals(0.0D, ranking.textScore());
        assertEquals(0.0D, ranking.countBoostScore());
    }

    @Test
    void unfilteredShouldReturnSameInstanceOnMultipleCalls() {
        DatabaseSearchRanking first = DatabaseSearchRanking.unfiltered();
        DatabaseSearchRanking second = DatabaseSearchRanking.unfiltered();
        assertSame(first, second);
    }

    @Test
    void noMatchShouldReturnSameInstanceOnMultipleCalls() {
        DatabaseSearchRanking first = DatabaseSearchRanking.noMatch();
        DatabaseSearchRanking second = DatabaseSearchRanking.noMatch();
        assertSame(first, second);
    }

    @Test
    void customRankingShouldPreserveAllFields() {
        DatabaseSearchRanking ranking = new DatabaseSearchRanking(
                true,
                true,
                3,
                2,
                1,
                0,
                5.5D,
                1.2D
        );
        assertTrue(ranking.matched());
        assertTrue(ranking.active());
        assertEquals(3, ranking.exactMatches());
        assertEquals(2, ranking.prefixMatches());
        assertEquals(1, ranking.containsMatches());
        assertEquals(0, ranking.fuzzyMatches());
        assertEquals(5.5D, ranking.textScore());
        assertEquals(1.2D, ranking.countBoostScore());
    }

    @Test
    void unfilteredAndNoMatchShouldBeDifferentInstances() {
        DatabaseSearchRanking unfiltered = DatabaseSearchRanking.unfiltered();
        DatabaseSearchRanking noMatch = DatabaseSearchRanking.noMatch();
        assertNotSame(unfiltered, noMatch);
    }

    @Test
    void zeroScoreRankingShouldStillBeValid() {
        DatabaseSearchRanking ranking = new DatabaseSearchRanking(true, true, 0, 0, 0, 0, 0.0D, 0.0D);
        assertTrue(ranking.matched());
        assertTrue(ranking.active());
    }
}
