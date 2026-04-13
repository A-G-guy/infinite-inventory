package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabasePage;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseEnhancementConfig;
import com.agguy.infiniteinventory.database.DatabaseEnhancementOption;
import com.agguy.infiniteinventory.database.DatabaseViewState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DatabasePageTest {
    @Test
    void emptyPageShouldNormalizeCountsAndRejectOutOfRangeAccess() {
        DatabasePage page = new DatabasePage(DatabaseQuery.defaultQuery(), -2, 0, -5L, List.of());

        assertEquals(0, page.totalEntries());
        assertEquals(1, page.totalPages());
        assertEquals(0L, page.totalItems());
        assertNull(page.entryAt(0));
        assertNull(page.entryAt(-1));
    }

    @Test
    void toViewStateShouldPropagateSessionId() {
        DatabaseQuery pageQuery = DatabaseQuery.defaultQuery(DatabaseScope.PUBLIC);
        DatabaseQuery personalQuery = DatabaseQuery.defaultQuery(DatabaseScope.PERSONAL);
        DatabaseQuery publicQuery = pageQuery.withPageIndex(1);
        DatabaseEnhancementConfig enhancementConfig = DatabaseEnhancementConfig.defaultConfig()
                .withOption(DatabaseEnhancementOption.AUTO_STORE_PICKED_UP_ITEMS, true);
        DatabasePage page = new DatabasePage(pageQuery, 1, 1, 12L, List.of());

        DatabaseViewState viewState = page.toViewState(7, 88L, personalQuery, publicQuery, enhancementConfig);

        assertEquals(7, viewState.containerId());
        assertEquals(88L, viewState.sessionId());
        assertEquals(pageQuery, viewState.query());
        assertEquals(personalQuery, viewState.personalQuery());
        assertEquals(pageQuery, viewState.publicQuery());
        assertEquals(enhancementConfig, viewState.enhancementConfig());
        assertEquals(0, viewState.entries().size());
    }
}
