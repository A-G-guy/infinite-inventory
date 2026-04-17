package com.agguy.infiniteinventory.database;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;

public record DatabasePanelView(
        DatabaseScopedTabRef scopedTab,
        DatabaseTab tab,
        int pageIndex,
        int pageSize,
        int totalEntries,
        int totalPages,
        long totalItems,
        List<VisibleDatabaseEntry> entries
) {
    public DatabasePanelView {
        scopedTab = scopedTab == null ? DatabaseScopedTabRef.defaultTab() : scopedTab;
        tab = tab == null ? DatabaseTabs.allTab() : tab;
        pageIndex = Math.max(0, pageIndex);
        pageSize = Math.max(1, pageSize);
        totalEntries = Math.max(0, totalEntries);
        totalPages = Math.max(1, totalPages);
        totalItems = Math.max(0L, totalItems);
        entries = List.copyOf(entries);
    }

    public static DatabasePanelView read(RegistryFriendlyByteBuf buffer) {
        DatabaseScopedTabRef scopedTab = DatabaseScopedTabRef.read(buffer);
        DatabaseTab tab = DatabaseTab.read(buffer);
        int pageIndex = buffer.readVarInt();
        int pageSize = buffer.readVarInt();
        int totalEntries = buffer.readVarInt();
        int totalPages = buffer.readVarInt();
        long totalItems = buffer.readVarLong();
        int entryCount = buffer.readVarInt();
        java.util.ArrayList<VisibleDatabaseEntry> entries = new java.util.ArrayList<>(entryCount);
        for (int index = 0; index < entryCount; index++) {
            entries.add(VisibleDatabaseEntry.read(buffer));
        }
        return new DatabasePanelView(scopedTab, tab, pageIndex, pageSize, totalEntries, totalPages, totalItems, entries);
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        this.scopedTab.write(buffer);
        this.tab.write(buffer);
        buffer.writeVarInt(this.pageIndex);
        buffer.writeVarInt(this.pageSize);
        buffer.writeVarInt(this.totalEntries);
        buffer.writeVarInt(this.totalPages);
        buffer.writeVarLong(this.totalItems);
        buffer.writeVarInt(this.entries.size());
        for (VisibleDatabaseEntry entry : this.entries) {
            entry.write(buffer);
        }
    }
}
