package com.agguy.infiniteinventory.menu;

import com.agguy.infiniteinventory.database.DatabaseEnhancementConfig;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import net.minecraft.network.FriendlyByteBuf;

public record PersonalDatabaseOpenState(
        long sessionId,
        DatabaseScope activeScope,
        DatabaseQuery personalQuery,
        DatabaseQuery publicQuery,
        DatabaseEnhancementConfig enhancementConfig,
        String autoStoreTargetTabId
) {
    public PersonalDatabaseOpenState {
        sessionId = Math.max(0L, sessionId);
        activeScope = DatabaseScope.normalize(activeScope);
        personalQuery = DatabaseQuery.normalizeForScope(DatabaseScope.PERSONAL, personalQuery);
        publicQuery = DatabaseQuery.normalizeForScope(DatabaseScope.PUBLIC, publicQuery);
        enhancementConfig = enhancementConfig == null ? DatabaseEnhancementConfig.defaultConfig() : enhancementConfig;
        autoStoreTargetTabId = DatabaseTabs.normalizeConcreteTarget(autoStoreTargetTabId);
    }

    public DatabaseQuery queryForScope(DatabaseScope scope) {
        return DatabaseScope.normalize(scope) == DatabaseScope.PUBLIC ? this.publicQuery : this.personalQuery;
    }

    public static PersonalDatabaseOpenState defaultState() {
        return new PersonalDatabaseOpenState(
                0L,
                DatabaseScope.defaultScope(),
                DatabaseQuery.defaultQuery(DatabaseScope.PERSONAL),
                DatabaseQuery.defaultQuery(DatabaseScope.PUBLIC),
                DatabaseEnhancementConfig.defaultConfig(),
                DatabaseTabs.DEFAULT_TAB_ID
        );
    }

    public static PersonalDatabaseOpenState read(FriendlyByteBuf buffer) {
        if (buffer == null) {
            return defaultState();
        }
        return new PersonalDatabaseOpenState(
                buffer.readVarLong(),
                buffer.readEnum(DatabaseScope.class),
                DatabaseQuery.read(buffer),
                DatabaseQuery.read(buffer),
                DatabaseEnhancementConfig.read(buffer),
                buffer.readUtf(DatabaseQuery.MAX_TAB_ID_LENGTH)
        );
    }

    public static void write(FriendlyByteBuf buffer, PersonalDatabaseOpenState state) {
        PersonalDatabaseOpenState normalizedState = state == null ? defaultState() : state;
        buffer.writeVarLong(normalizedState.sessionId());
        buffer.writeEnum(normalizedState.activeScope());
        DatabaseQuery.write(buffer, normalizedState.personalQuery());
        DatabaseQuery.write(buffer, normalizedState.publicQuery());
        DatabaseEnhancementConfig.write(buffer, normalizedState.enhancementConfig());
        buffer.writeUtf(normalizedState.autoStoreTargetTabId(), DatabaseQuery.MAX_TAB_ID_LENGTH);
    }
}
