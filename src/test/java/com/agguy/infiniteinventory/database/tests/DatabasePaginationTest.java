package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabasePagination;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabasePaginationTest {
    @Test
    void shouldKeepAtLeastOnePageForEmptyDatabase() {
        assertEquals(1, DatabasePagination.resolveTotalPages(0, 54));
    }

    @Test
    void shouldExposeAnExtraBlankPageWhenCurrentPagesAreFull() {
        assertEquals(2, DatabasePagination.resolveTotalPages(54, 54));
        assertEquals(3, DatabasePagination.resolveTotalPages(108, 54));
    }

    @Test
    void shouldNotAddBlankPagesWhenLastPageIsPartiallyFilled() {
        assertEquals(1, DatabasePagination.resolveTotalPages(1, 54));
        assertEquals(2, DatabasePagination.resolveTotalPages(55, 54));
    }
}
