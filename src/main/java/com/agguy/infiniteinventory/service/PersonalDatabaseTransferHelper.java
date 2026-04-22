package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabaseCrossTransferHelper;
import com.agguy.infiniteinventory.database.DatabaseLogAction;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSelectionEntry;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackEntry;
import com.agguy.infiniteinventory.database.StoredStackKey;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        StoredItemDatabase sourceDatabase = service.resolveDatabaseForMutation(player, normalizedSourceScope);
        String normalizedSourceTabId = DatabaseTabs.normalizeConcreteTarget(sourceTabId);
        Map<StoredStackKey, Long> preTransferEntries = new LinkedHashMap<>();
        for (Map.Entry<StoredStackKey, StoredStackEntry> entry : sourceDatabase.entries().entrySet()) {
            if (entry.getValue().tabId().equals(normalizedSourceTabId)) {
                preTransferEntries.put(entry.getKey(), entry.getValue().amount());
            }
        }
        boolean changed = transferTab(
                player.registryAccess(),
                sourceDatabase,
                service.resolveDatabaseForMutation(player, normalizedTargetScope),
                sourceTabId,
                service.resolveConcreteTargetTabId(player, normalizedTargetScope, targetTabId)
        );
        if (!changed) {
            return false;
        }
        service.markScopeDirty(player, normalizedSourceScope);
        if (normalizedTargetScope != normalizedSourceScope) {
            service.markScopeDirty(player, normalizedTargetScope);
        }
        String resolvedTargetTabId = service.resolveConcreteTargetTabId(player, normalizedTargetScope, targetTabId);
        for (Map.Entry<StoredStackKey, Long> entry : preTransferEntries.entrySet()) {
            service.recordLog(player, normalizedSourceScope, DatabaseLogAction.TRANSFER,
                    entry.getKey().displayStack(), entry.getValue(),
                    normalizedSourceTabId, resolvedTargetTabId,
                    normalizedTargetScope != normalizedSourceScope ? normalizedTargetScope : null);
            if (normalizedTargetScope != normalizedSourceScope) {
                service.recordLog(player, normalizedTargetScope, DatabaseLogAction.TRANSFER,
                        entry.getKey().displayStack(), entry.getValue(),
                        normalizedSourceTabId, resolvedTargetTabId, normalizedSourceScope);
            }
        }
        return true;
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
        StoredItemDatabase sourceDatabase = service.resolveDatabaseForMutation(player, normalizedSourceScope);
        String resolvedTargetTabId = service.resolveConcreteTargetTabId(player, normalizedTargetScope, targetTabId);
        Map<StoredStackKey, TransferEntryInfo> preTransferEntries = collectSelectionEntries(sourceDatabase, selectionEntries);
        boolean changed = transferSelection(
                player.registryAccess(),
                sourceDatabase,
                service.resolveDatabaseForMutation(player, normalizedTargetScope),
                selectionEntries,
                resolvedTargetTabId
        );
        if (!changed) {
            return false;
        }
        service.markScopeDirty(player, normalizedSourceScope);
        if (normalizedTargetScope != normalizedSourceScope) {
            service.markScopeDirty(player, normalizedTargetScope);
        }
        for (Map.Entry<StoredStackKey, TransferEntryInfo> entry : preTransferEntries.entrySet()) {
            TransferEntryInfo info = entry.getValue();
            service.recordLog(player, normalizedSourceScope, DatabaseLogAction.TRANSFER,
                    entry.getKey().displayStack(), info.amount(),
                    info.sourceTabId(), resolvedTargetTabId,
                    normalizedTargetScope != normalizedSourceScope ? normalizedTargetScope : null);
            if (normalizedTargetScope != normalizedSourceScope) {
                service.recordLog(player, normalizedTargetScope, DatabaseLogAction.TRANSFER,
                        entry.getKey().displayStack(), info.amount(),
                        info.sourceTabId(), resolvedTargetTabId, normalizedSourceScope);
            }
        }
        return true;
    }

    private record TransferEntryInfo(long amount, String sourceTabId) {
    }

    private static Map<StoredStackKey, TransferEntryInfo> collectSelectionEntries(
            StoredItemDatabase database,
            List<DatabaseSelectionEntry> selectionEntries
    ) {
        Map<StoredStackKey, TransferEntryInfo> result = new LinkedHashMap<>();
        for (DatabaseSelectionEntry selectionEntry : PersonalDatabaseExtractionHelper.normalizeSelectionEntries(selectionEntries)) {
            StoredStackKey key = PersonalDatabaseExtractionHelper.selectionKey(selectionEntry);
            if (key == null) {
                continue;
            }
            StoredStackEntry storedEntry = database.entries().get(key);
            if (storedEntry == null || !storedEntry.tabId().equals(selectionEntry.sourceTabId())) {
                continue;
            }
            result.put(key, new TransferEntryInfo(storedEntry.amount(), selectionEntry.sourceTabId()));
        }
        return result;
    }
}
