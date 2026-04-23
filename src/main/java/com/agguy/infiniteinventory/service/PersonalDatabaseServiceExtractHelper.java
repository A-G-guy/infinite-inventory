package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabaseLogAction;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSelectionEntry;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackEntry;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.network.DatabaseSelectionAction;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

final class PersonalDatabaseServiceExtractHelper {
    private PersonalDatabaseServiceExtractHelper() {
    }

    static ItemStack extractToCarried(PersonalDatabaseService service, ServerPlayer player, DatabaseScope scope, StoredStackKey key, int requestedAmount) {
        String sourceTabId = PersonalDatabaseServiceHelper.entryTabId(service.resolveDatabaseForMutation(player, scope), key);
        ItemStack extracted = service.resolveDatabaseForMutation(player, scope).extract(key, requestedAmount);
        if (!extracted.isEmpty()) {
            service.markScopeDirty(player, scope);
            PersonalDatabaseServiceLogHelper.recordLog(service, player, scope, DatabaseLogAction.EXTRACT, extracted, extracted.getCount(), sourceTabId, "", null);
        }
        return extracted;
    }

    static long extractAllToInventory(PersonalDatabaseService service, ServerPlayer player, DatabaseScope scope, StoredStackKey key) {
        return extractToInventory(service, player, scope, key, Long.MAX_VALUE);
    }

    static long extractToInventory(PersonalDatabaseService service, ServerPlayer player, DatabaseScope scope, StoredStackKey key, long requestedAmount) {
        String sourceTabId = PersonalDatabaseServiceHelper.entryTabId(service.resolveDatabaseForMutation(player, scope), key);
        long moved = PersonalDatabaseExtractionHelper.extractToInventory(service, player, scope, service.resolveDatabaseForMutation(player, scope), key, requestedAmount);
        if (moved > 0L) {
            PersonalDatabaseServiceLogHelper.recordLog(service, player, scope, DatabaseLogAction.EXTRACT, key.displayStack(), moved, sourceTabId, "", null);
        }
        return moved;
    }

    static long extractToWorld(PersonalDatabaseService service, ServerPlayer player, DatabaseScope scope, StoredStackKey key, long requestedAmount) {
        String sourceTabId = PersonalDatabaseServiceHelper.entryTabId(service.resolveDatabaseForMutation(player, scope), key);
        long moved = PersonalDatabaseExtractionHelper.extractToWorld(service, player, scope, service.resolveDatabaseForMutation(player, scope), key, requestedAmount);
        if (moved > 0L) {
            PersonalDatabaseServiceLogHelper.recordLog(service, player, scope, DatabaseLogAction.EXTRACT, key.displayStack(), moved, sourceTabId, "", null);
        }
        return moved;
    }

    static long extractSelectionToInventory(PersonalDatabaseService service, ServerPlayer player, DatabaseScope scope, List<DatabaseSelectionEntry> selectionEntries, DatabaseSelectionAction action) {
        return extractSelectionToInventory(service, player, scope, selectionEntries, action, 0L);
    }

    static long extractSelectionToInventory(
            PersonalDatabaseService service, ServerPlayer player, DatabaseScope scope,
            List<DatabaseSelectionEntry> selectionEntries, DatabaseSelectionAction action, long requestedAmount
    ) {
        if (action == null || !action.extractsToInventory()) {
            return 0L;
        }
        long totalMoved = 0L;
        StoredItemDatabase database = service.resolveDatabaseForMutation(player, scope);
        for (DatabaseSelectionEntry selectionEntry : PersonalDatabaseExtractionHelper.normalizeSelectionEntries(selectionEntries)) {
            StoredStackKey key = PersonalDatabaseExtractionHelper.selectionKey(selectionEntry);
            if (key == null) {
                continue;
            }
            StoredStackEntry storedEntry = database.entries().get(key);
            if (storedEntry == null || !storedEntry.tabId().equals(selectionEntry.sourceTabId())) {
                continue;
            }
            long resolvedRequestedAmount = action.resolveRequestedAmount(storedEntry.amount(), key.maxStackSize(), requestedAmount);
            long moved = PersonalDatabaseExtractionHelper.extractToInventory(service, player, scope, database, key, resolvedRequestedAmount);
            if (moved > 0L) {
                totalMoved = PersonalDatabaseServiceHelper.safeAddMovedItems(totalMoved, moved);
                PersonalDatabaseServiceLogHelper.recordLog(service, player, scope, DatabaseLogAction.EXTRACT, key.displayStack(), moved, selectionEntry.sourceTabId(), "", null);
            }
        }
        return totalMoved;
    }
}
