package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabaseCrossTransferHelper;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSelectionEntry;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class PersonalDatabaseTransferHelper {
    private PersonalDatabaseTransferHelper() {
    }

    public static DatabaseScope resolveTargetScope(DatabaseScope sourceScope, @Nullable DatabaseScope requestedTargetScope) {
        return requestedTargetScope == null ? DatabaseScope.normalize(sourceScope) : DatabaseScope.normalize(requestedTargetScope);
    }

    public static boolean affectsPublicScope(DatabaseScope sourceScope, @Nullable DatabaseScope targetScope) {
        DatabaseScope normalizedSourceScope = DatabaseScope.normalize(sourceScope);
        DatabaseScope normalizedTargetScope = resolveTargetScope(sourceScope, targetScope);
        return normalizedSourceScope == DatabaseScope.PUBLIC || normalizedTargetScope == DatabaseScope.PUBLIC;
    }

    public static boolean transferTab(
            HolderLookup.Provider provider,
            StoredItemDatabase sourceDatabase,
            StoredItemDatabase targetDatabase,
            String sourceTabId,
            String targetTabId
    ) {
        if (sourceDatabase == null || targetDatabase == null) {
            return false;
        }
        if (sourceDatabase == targetDatabase) {
            return sourceDatabase.transferTab(sourceTabId, targetTabId);
        }
        return DatabaseCrossTransferHelper.transferTab(provider, sourceDatabase, targetDatabase, sourceTabId, targetTabId);
    }

    public static boolean transferTab(
            StoredItemDatabase sourceDatabase,
            StoredItemDatabase targetDatabase,
            String sourceTabId,
            String targetTabId
    ) {
        return transferTab(null, sourceDatabase, targetDatabase, sourceTabId, targetTabId);
    }

    public static boolean transferSelection(
            HolderLookup.Provider provider,
            StoredItemDatabase sourceDatabase,
            StoredItemDatabase targetDatabase,
            List<DatabaseSelectionEntry> selectionEntries,
            String targetTabId
    ) {
        if (sourceDatabase == null || targetDatabase == null) {
            return false;
        }
        if (sourceDatabase == targetDatabase) {
            return PersonalDatabaseExtractionHelper.transferSelection(sourceDatabase, selectionEntries, targetTabId);
        }
        return DatabaseCrossTransferHelper.transferSelection(provider, sourceDatabase, targetDatabase, selectionEntries, targetTabId);
    }

    public static boolean transferSelection(
            StoredItemDatabase sourceDatabase,
            StoredItemDatabase targetDatabase,
            List<DatabaseSelectionEntry> selectionEntries,
            String targetTabId
    ) {
        return transferSelection(null, sourceDatabase, targetDatabase, selectionEntries, targetTabId);
    }

    static boolean transferTab(
            PersonalDatabaseService service,
            ServerPlayer player,
            DatabaseScope sourceScope,
            @Nullable DatabaseScope targetScope,
            String sourceTabId,
            String targetTabId
    ) {
        DatabaseScope normalizedSourceScope = DatabaseScope.normalize(sourceScope);
        DatabaseScope normalizedTargetScope = resolveTargetScope(sourceScope, targetScope);
        boolean changed = transferTab(
                player.registryAccess(),
                service.resolveDatabaseForMutation(player, normalizedSourceScope),
                service.resolveDatabaseForMutation(player, normalizedTargetScope),
                sourceTabId,
                service.resolveConcreteTargetTabId(player, normalizedTargetScope, targetTabId)
        );
        if (changed) {
            service.markScopeDirty(player, normalizedSourceScope);
            if (normalizedTargetScope != normalizedSourceScope) {
                service.markScopeDirty(player, normalizedTargetScope);
            }
        }
        return changed;
    }

    static boolean transferSelection(
            PersonalDatabaseService service,
            ServerPlayer player,
            DatabaseScope sourceScope,
            @Nullable DatabaseScope targetScope,
            List<DatabaseSelectionEntry> selectionEntries,
            String targetTabId
    ) {
        DatabaseScope normalizedSourceScope = DatabaseScope.normalize(sourceScope);
        DatabaseScope normalizedTargetScope = resolveTargetScope(sourceScope, targetScope);
        boolean changed = transferSelection(
                player.registryAccess(),
                service.resolveDatabaseForMutation(player, normalizedSourceScope),
                service.resolveDatabaseForMutation(player, normalizedTargetScope),
                selectionEntries,
                service.resolveConcreteTargetTabId(player, normalizedTargetScope, targetTabId)
        );
        if (changed) {
            service.markScopeDirty(player, normalizedSourceScope);
            if (normalizedTargetScope != normalizedSourceScope) {
                service.markScopeDirty(player, normalizedTargetScope);
            }
        }
        return changed;
    }
}
