package com.agguy.infiniteinventory.service.search.tests;

import com.agguy.infiniteinventory.service.search.DatabaseParsedSearchQuery;
import com.agguy.infiniteinventory.service.search.DatabaseSearchClause;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseParsedSearchQueryTest {

    @Test
    void emptyShouldReturnInactiveQuery() {
        DatabaseParsedSearchQuery query = DatabaseParsedSearchQuery.empty();

        assertNotNull(query.clauses());
        assertTrue(query.clauses().isEmpty());
        assertEquals("", query.normalizedExpression());
        assertFalse(query.active());
    }

    @Test
    void shouldActivateWhenClausesPresent() {
        DatabaseSearchClause clause = new DatabaseSearchClause(
                List.of(), List.of(), List.of("hello"), List.of());
        DatabaseParsedSearchQuery query = new DatabaseParsedSearchQuery(
                List.of(clause), "hello");

        assertTrue(query.active());
    }

    @Test
    void shouldHandleNullClauses() {
        DatabaseParsedSearchQuery query = new DatabaseParsedSearchQuery(null, null);

        assertNotNull(query.clauses());
        assertTrue(query.clauses().isEmpty());
        assertEquals("", query.normalizedExpression());
    }

    @Test
    void shouldPreserveNormalizedExpression() {
        DatabaseParsedSearchQuery query = new DatabaseParsedSearchQuery(
                List.of(), "  trimmed expression  ");

        assertEquals("  trimmed expression  ", query.normalizedExpression());
    }

    @Test
    void emptyInstanceShouldBeSingleton() {
        assertEquals(DatabaseParsedSearchQuery.empty(), DatabaseParsedSearchQuery.empty());
    }
}
