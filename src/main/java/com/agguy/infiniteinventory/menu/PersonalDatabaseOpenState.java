package com.agguy.infiniteinventory.menu;

import com.agguy.infiniteinventory.database.DatabaseAutoStoreTarget;
import com.agguy.infiniteinventory.database.DatabaseEnhancementConfig;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import net.minecraft.network.FriendlyByteBuf;

public record PersonalDatabaseOpenState(
        long sessionId,
        DatabaseQuery query,
        DatabaseEnhancementConfig enhancementConfig,
        DatabaseAutoStoreTarget autoStoreTarget
) {
    public PersonalDatabaseOpenState {
        sessionId = Math.max(0L, sessionId);
        query = query == null ? DatabaseQuery.defaultQuery() : query;
        enhancementConfig = enhancementConfig == null ? DatabaseEnhancementConfig.defaultConfig() : enhancementConfig;
        autoStoreTarget = autoStoreTarget == null ? DatabaseAutoStoreTarget.defaultTarget() : autoStoreTarget;
    }

    public static PersonalDatabaseOpenState defaultState() {
        return new PersonalDatabaseOpenState(
                0L,
                DatabaseQuery.defaultQuery(),
                DatabaseEnhancementConfig.defaultConfig(),
                DatabaseAutoStoreTarget.defaultTarget()
        );
    }

    public DatabaseScope activeScope() {
        return this.query.scope();
    }

    public DatabaseQuery queryForScope(DatabaseScope scope) {
        return DatabaseQuery.normalizeForScope(scope, this.query);
    }

    public static PersonalDatabaseOpenState read(FriendlyByteBuf buffer) {
        if (buffer == null) {
            return defaultState();
        }
        return new PersonalDatabaseOpenState(
                buffer.readVarLong(),
                DatabaseQuery.read(buffer),
                DatabaseEnhancementConfig.read(buffer),
                DatabaseAutoStoreTarget.read(buffer)
        );
    }

    public static void write(FriendlyByteBuf buffer, PersonalDatabaseOpenState state) {
        PersonalDatabaseOpenState normalizedState = state == null ? defaultState() : state;
        buffer.writeVarLong(normalizedState.sessionId());
        DatabaseQuery.write(buffer, normalizedState.query());
        DatabaseEnhancementConfig.write(buffer, normalizedState.enhancementConfig());
        DatabaseAutoStoreTarget.write(buffer, normalizedState.autoStoreTarget());
    }
}
