package com.agguy.infiniteinventory.service.search.tests;

import com.agguy.infiniteinventory.service.search.DatabaseSearchClause;
import com.agguy.infiniteinventory.service.search.DatabaseSearchFilter;
import com.agguy.infiniteinventory.service.search.DatabaseSearchFilterType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSearchClauseTest {

    @Test
    void shouldHandleNullArguments() {
        DatabaseSearchClause clause = new DatabaseSearchClause(null, null, null, null);

        assertNotNull(clause.positiveFilters());
        assertNotNull(clause.negativeFilters());
        assertNotNull(clause.positivePlainTerms());
        assertNotNull(clause.negativePlainTerms());
        assertTrue(clause.positiveFilters().isEmpty());
        assertTrue(clause.positivePlainTerms().isEmpty());
    }

    @Test
    void shouldBeInactiveWhenEmpty() {
        DatabaseSearchClause clause = new DatabaseSearchClause(
                List.of(), List.of(), List.of(), List.of());

        assertFalse(clause.active());
    }

    @Test
    void shouldBeActiveWithPositiveFilter() {
        DatabaseSearchFilter filter = new DatabaseSearchFilter(
                DatabaseSearchFilterType.MOD, "minecraft");
        DatabaseSearchClause clause = new DatabaseSearchClause(
                List.of(filter), List.of(), List.of(), List.of());

        assertTrue(clause.active());
    }

    @Test
    void shouldBeActiveWithNegativeFilter() {
        DatabaseSearchFilter filter = new DatabaseSearchFilter(
                DatabaseSearchFilterType.TAG, "weapon");
        DatabaseSearchClause clause = new DatabaseSearchClause(
                List.of(), List.of(filter), List.of(), List.of());

        assertTrue(clause.active());
    }

    @Test
    void shouldBeActiveWithPositivePlainTerm() {
        DatabaseSearchClause clause = new DatabaseSearchClause(
                List.of(), List.of(), List.of("sword"), List.of());

        assertTrue(clause.active());
    }

    @Test
    void shouldBeActiveWithNegativePlainTerm() {
        DatabaseSearchClause clause = new DatabaseSearchClause(
                List.of(), List.of(), List.of(), List.of("axe"));

        assertTrue(clause.active());
    }

    @Test
    void positivePlainQueryShouldJoinTerms() {
        DatabaseSearchClause clause = new DatabaseSearchClause(
                List.of(), List.of(), List.of("diamond", "sword"), List.of());

        assertEquals("diamond sword", clause.positivePlainQuery());
    }

    @Test
    void canonicalExpressionShouldIncludeAllParts() {
        DatabaseSearchFilter positiveFilter = new DatabaseSearchFilter(
                DatabaseSearchFilterType.MOD, "minecraft");
        DatabaseSearchFilter negativeFilter = new DatabaseSearchFilter(
                DatabaseSearchFilterType.TAG, "weapon");
        DatabaseSearchClause clause = new DatabaseSearchClause(
                List.of(positiveFilter),
                List.of(negativeFilter),
                List.of("sword"),
                List.of("axe"));

        String expr = clause.canonicalExpression();

        assertTrue(expr.contains("@minecraft"));
        assertTrue(expr.contains("-#weapon"));
        assertTrue(expr.contains("sword"));
        assertTrue(expr.contains("-axe"));
    }

    @Test
    void canonicalExpressionShouldSkipEmptyNormalizedTerms() {
        DatabaseSearchClause clause = new DatabaseSearchClause(
                List.of(), List.of(),
                List.of("   "),
                List.of(""));

        assertEquals("", clause.canonicalExpression());
    }
}
