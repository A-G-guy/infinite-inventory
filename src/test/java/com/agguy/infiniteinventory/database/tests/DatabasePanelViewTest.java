package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabasePanelView;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DatabasePanelViewTest {

    @Test
    void shouldApplyCompactConstructorDefaultsForNullScopedTab() {
        DatabasePanelView panel = new DatabasePanelView(null, DatabaseTabs.allTab(), 0, 10, 0, 1, 0, List.of());

        assertNotNull(panel.scopedTab());
    }

    @Test
    void shouldApplyCompactConstructorDefaultsForNullTab() {
        DatabasePanelView panel = new DatabasePanelView(DatabaseScopedTabRef.defaultTab(), null, 0, 10, 0, 1, 0,
                List.of());

        assertNotNull(panel.tab());
    }

    @Test
    void shouldClampNegativePageIndex() {
        DatabasePanelView panel = new DatabasePanelView(DatabaseScopedTabRef.defaultTab(), DatabaseTabs.allTab(),
                -5, 10, 0, 1, 0, List.of());

        assertEquals(0, panel.pageIndex());
    }

    @Test
    void shouldClampZeroPageSize() {
        DatabasePanelView panel = new DatabasePanelView(DatabaseScopedTabRef.defaultTab(), DatabaseTabs.allTab(),
                0, 0, 0, 1, 0, List.of());

        assertEquals(1, panel.pageSize());
    }

    @Test
    void shouldClampNegativeTotalEntries() {
        DatabasePanelView panel = new DatabasePanelView(DatabaseScopedTabRef.defaultTab(), DatabaseTabs.allTab(),
                0, 10, -1, 1, 0, List.of());

        assertEquals(0, panel.totalEntries());
    }

    @Test
    void shouldClampNegativeTotalPages() {
        DatabasePanelView panel = new DatabasePanelView(DatabaseScopedTabRef.defaultTab(), DatabaseTabs.allTab(),
                0, 10, 0, -1, 0, List.of());

        assertEquals(1, panel.totalPages());
    }

    @Test
    void shouldClampNegativeTotalItems() {
        DatabasePanelView panel = new DatabasePanelView(DatabaseScopedTabRef.defaultTab(), DatabaseTabs.allTab(),
                0, 10, 0, 1, -100L, List.of());

        assertEquals(0L, panel.totalItems());
    }

    @Test
    void shouldPreserveValidValues() {
        DatabaseTab tab = DatabaseTabs.allTab();
        DatabaseScopedTabRef scopedTab = DatabaseScopedTabRef.defaultTab();
        DatabasePanelView panel = new DatabasePanelView(scopedTab, tab, 3, 25, 100, 4, 5000L, List.of());

        assertEquals(3, panel.pageIndex());
        assertEquals(25, panel.pageSize());
        assertEquals(100, panel.totalEntries());
        assertEquals(4, panel.totalPages());
        assertEquals(5000L, panel.totalItems());
    }
}
