package com.agguy.infiniteinventory.database;

import java.util.List;
import org.jetbrains.annotations.Nullable;

public record DatabasePage(DatabaseQuery query, int totalEntries, int totalPages, long totalItems, List<DatabasePageEntry> entries) {
    public DatabasePage {
        query = query == null ? DatabaseQuery.defaultQuery() : query;
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

    public DatabaseViewState toViewState(int containerId, long sessionId, DatabaseQuery personalQuery, DatabaseQuery publicQuery) {
        return this.toViewState(containerId, sessionId, personalQuery, publicQuery, DatabaseEnhancementConfig.defaultConfig());
    }

    public DatabaseViewState toViewState(
            int containerId,
            long sessionId,
            DatabaseQuery personalQuery,
            DatabaseQuery publicQuery,
            DatabaseEnhancementConfig enhancementConfig
    ) {
        return new DatabaseViewState(
                containerId,
                sessionId,
                this.query,
                personalQuery,
                publicQuery,
                enhancementConfig,
                this.totalEntries,
                this.totalPages,
                this.totalItems,
                this.entries.stream().map(DatabasePageEntry::view).toList()
        );
    }
}
