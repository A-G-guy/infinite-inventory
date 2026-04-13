package com.agguy.infiniteinventory.database;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;

public record DatabaseViewState(
        int containerId,
        long sessionId,
        DatabaseQuery query,
        DatabaseQuery personalQuery,
        DatabaseQuery publicQuery,
        DatabaseEnhancementConfig enhancementConfig,
        int totalEntries,
        int totalPages,
        long totalItems,
        List<VisibleDatabaseEntry> entries
) {
    public DatabaseViewState {
        query = query == null ? DatabaseQuery.defaultQuery() : query;
        personalQuery = DatabaseQuery.normalizeForScope(DatabaseScope.PERSONAL, personalQuery);
        publicQuery = DatabaseQuery.normalizeForScope(DatabaseScope.PUBLIC, publicQuery);
        if (query.scope() == DatabaseScope.PUBLIC) {
            publicQuery = query;
        } else {
            personalQuery = query;
        }
        enhancementConfig = enhancementConfig == null ? DatabaseEnhancementConfig.defaultConfig() : enhancementConfig;
        sessionId = Math.max(0L, sessionId);
        totalEntries = Math.max(0, totalEntries);
        totalPages = Math.max(1, totalPages);
        totalItems = Math.max(0L, totalItems);
        entries = List.copyOf(entries);
    }

    public static DatabaseViewState empty(int containerId) {
        return empty(containerId, 0L, DatabaseQuery.defaultQuery());
    }

    public static DatabaseViewState empty(int containerId, DatabaseQuery query) {
        return empty(containerId, 0L, query);
    }

    public static DatabaseViewState empty(int containerId, long sessionId, DatabaseQuery query) {
        return new DatabaseViewState(
                containerId,
                sessionId,
                query,
                DatabaseQuery.defaultQuery(DatabaseScope.PERSONAL),
                DatabaseQuery.defaultQuery(DatabaseScope.PUBLIC),
                DatabaseEnhancementConfig.defaultConfig(),
                0,
                1,
                0L,
                List.of()
        );
    }

    public DatabaseQuery queryForScope(DatabaseScope scope) {
        return DatabaseScope.normalize(scope) == DatabaseScope.PUBLIC ? this.publicQuery : this.personalQuery;
    }

    public static DatabaseViewState read(RegistryFriendlyByteBuf buffer) {
        int containerId = buffer.readVarInt();
        long sessionId = buffer.readVarLong();
        DatabaseQuery query = DatabaseQuery.read(buffer);
        DatabaseQuery personalQuery = DatabaseQuery.read(buffer);
        DatabaseQuery publicQuery = DatabaseQuery.read(buffer);
        DatabaseEnhancementConfig enhancementConfig = DatabaseEnhancementConfig.read(buffer);
        int totalEntries = buffer.readVarInt();
        int totalPages = buffer.readVarInt();
        long totalItems = buffer.readVarLong();
        int entryCount = buffer.readVarInt();
        java.util.ArrayList<VisibleDatabaseEntry> entries = new java.util.ArrayList<>(entryCount);
        for (int index = 0; index < entryCount; index++) {
            entries.add(VisibleDatabaseEntry.read(buffer));
        }
        return new DatabaseViewState(containerId, sessionId, query, personalQuery, publicQuery, enhancementConfig, totalEntries, totalPages, totalItems, entries);
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(this.containerId);
        buffer.writeVarLong(this.sessionId);
        DatabaseQuery.write(buffer, this.query);
        DatabaseQuery.write(buffer, this.personalQuery);
        DatabaseQuery.write(buffer, this.publicQuery);
        DatabaseEnhancementConfig.write(buffer, this.enhancementConfig);
        buffer.writeVarInt(this.totalEntries);
        buffer.writeVarInt(this.totalPages);
        buffer.writeVarLong(this.totalItems);
        buffer.writeVarInt(this.entries.size());
        for (VisibleDatabaseEntry entry : this.entries) {
            entry.write(buffer);
        }
    }
}
