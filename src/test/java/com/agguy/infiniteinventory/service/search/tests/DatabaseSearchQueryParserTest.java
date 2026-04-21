package com.agguy.infiniteinventory.service.search.tests;

import com.agguy.infiniteinventory.service.search.DatabaseParsedSearchQuery;
import com.agguy.infiniteinventory.service.search.DatabaseSearchClause;
import com.agguy.infiniteinventory.service.search.DatabaseSearchFilter;
import com.agguy.infiniteinventory.service.search.DatabaseSearchFilterType;
import com.agguy.infiniteinventory.service.search.DatabaseSearchQueryParser;
import com.agguy.infiniteinventory.service.search.DatabaseSearchQueryParserContext;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSearchQueryParserTest {
    @Test
    void parserShouldSplitOrClausesAndKeepNegation() {
        DatabaseParsedSearchQuery parsedQuery = DatabaseSearchQueryParser.INSTANCE.parse("diamond sword|gold -axe");

        assertEquals(2, parsedQuery.clauses().size());
        assertEquals("diamond sword", parsedQuery.clauses().getFirst().positivePlainQuery());
        assertEquals("gold", parsedQuery.clauses().get(1).positivePlainQuery());
        assertEquals(java.util.List.of("axe"), parsedQuery.clauses().get(1).negativePlainTerms());
    }

    @Test
    void parserShouldAbsorbKnownMultiWordFilters() {
        DatabaseParsedSearchQuery parsedQuery = DatabaseSearchQueryParser.INSTANCE.parse(
                "%building blocks @applied energistics 2 cable",
                new DatabaseSearchQueryParserContext(
                        Set.of("applied energistics 2"),
                        Set.of("building blocks")
                )
        );

        DatabaseSearchClause clause = parsedQuery.clauses().getFirst();
        assertEquals(
                java.util.List.of(
                        new DatabaseSearchFilter(DatabaseSearchFilterType.CREATIVE_TAB, "building blocks"),
                        new DatabaseSearchFilter(DatabaseSearchFilterType.MOD, "applied energistics 2")
                ),
                clause.positiveFilters()
        );
        assertEquals(java.util.List.of("cable"), clause.positivePlainTerms());
        assertEquals("%\"building blocks\" @\"applied energistics 2\" cable", parsedQuery.normalizedExpression());
    }

    @Test
    void parserShouldKeepSpecialFiltersSeparatedFromPlainTerms() {
        DatabaseParsedSearchQuery parsedQuery = DatabaseSearchQueryParser.INSTANCE.parse("#minecraft:logs -&minecraft:oak_log @minecraft");

        DatabaseSearchClause clause = parsedQuery.clauses().getFirst();
        assertEquals(
                java.util.List.of(
                        new DatabaseSearchFilter(DatabaseSearchFilterType.TAG, "minecraft:logs"),
                        new DatabaseSearchFilter(DatabaseSearchFilterType.MOD, "minecraft")
                ),
                clause.positiveFilters()
        );
        assertEquals(java.util.List.of(new DatabaseSearchFilter(DatabaseSearchFilterType.ITEM_ID, "minecraft:oak_log")), clause.negativeFilters());
        assertTrue(clause.positivePlainTerms().isEmpty());
    }
}
