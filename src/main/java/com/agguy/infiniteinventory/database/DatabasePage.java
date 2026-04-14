package com.agguy.infiniteinventory.database;

import java.util.List;
import org.jetbrains.annotations.Nullable;

public record DatabasePage(
        DatabaseTab tab,
        int pageIndex,
        int pageSize,
        int totalEntries,
        int totalPages,
        long totalItems,
        List<DatabasePageEntry> entries
) {
    public DatabasePage {
        tab = tab == null ? DatabaseTabs.allTab() : tab;
        pageIndex = Math.max(0, pageIndex);
        pageSize = Math.max(1, pageSize);
        totalEntries = Math.max(0, totalEntries);
        totalPages = Math.max(1, totalPages);
        totalItems = Math.max(0L, totalItems);
        entries = List.copyOf(entries);
    }

    @Nullable
    public DatabasePageEntry entryAt(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= this.entries.size()) {
            return null;
        }
        return this.entries.get(slotIndex);
    }

    public DatabasePanelView toPanelView() {
        return new DatabasePanelView(
                this.tab,
                this.pageIndex,
                this.pageSize,
                this.totalEntries,
                this.totalPages,
                this.totalItems,
                this.entries.stream().map(DatabasePageEntry::view).toList()
        );
    }
}
