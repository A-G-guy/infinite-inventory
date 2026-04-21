package com.agguy.infiniteinventory.service.search;

import java.util.List;

public record DatabaseParsedSearchQuery(
        List<DatabaseSearchClause> clauses,
        String normalizedExpression
) {
    private static final DatabaseParsedSearchQuery EMPTY = new DatabaseParsedSearchQuery(List.of(), "");

    public DatabaseParsedSearchQuery {
        clauses = List.copyOf(clauses == null ? List.of() : clauses);
        normalizedExpression = normalizedExpression == null ? "" : normalizedExpression;
    }

    public static DatabaseParsedSearchQuery empty() {
        return EMPTY;
    }

    public boolean active() {
        return !this.clauses.isEmpty();
    }
}
