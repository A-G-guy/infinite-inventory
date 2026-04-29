package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSelectionEntry;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackEntry;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.network.DatabaseSelectionAction;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class PersonalDatabaseExtractionHelper {
    private static final Logger LOGGER = LogManager.getLogger();

    private PersonalDatabaseExtractionHelper() {
    }

    static long extractToInventory(
            PersonalDatabaseService service,
            ServerPlayer player,
            DatabaseScope scope,
            StoredItemDatabase database,
            StoredStackKey key,
            long requestedAmount
    ) {
        if (requestedAmount <= 0L) {
            return 0L;
        }
        Inventory inventory = player.getInventory();
        String originalTabId = PersonalDatabaseServiceHelper.entryTabId(database, key);
        long movedItems = 0L;
        long remainingAmount = requestedAmount;
        database.beginBatchUpdate();
        try {
            while (remainingAmount > 0L && hasSpaceFor(inventory, key)) {
                int extractedCount = (int) Math.min((long) key.maxStackSize(), Math.min(remainingAmount, (long) Integer.MAX_VALUE));
                ItemStack extracted = database.extract(key, extractedCount);
                if (extracted.isEmpty()) {
                    break;
                }
                int originalCount = extracted.getCount();
                inventory.add(extracted);
                int movedNow = originalCount - extracted.getCount();
                if (movedNow <= 0) {
                    database.store(extracted, originalTabId);
                    break;
                }
                movedItems = safeAddMovedItems(movedItems, movedNow);
                remainingAmount -= movedNow;
                if (!extracted.isEmpty()) {
                    database.store(extracted, originalTabId);
                    break;
                }
            }
        } finally {
            database.endBatchUpdate();
        }
        if (movedItems > 0L) {
            service.markScopeDirty(player, scope);
            inventory.setChanged();
        }
        return movedItems;
    }

    static long extractToWorld(
            PersonalDatabaseService service,
            ServerPlayer player,
            DatabaseScope scope,
            StoredItemDatabase database,
            StoredStackKey key,
            long requestedAmount
    ) {
        if (requestedAmount <= 0L) {
            return 0L;
        }
        String originalTabId = PersonalDatabaseServiceHelper.entryTabId(database, key);
        ItemStack extracted = database.extract(
                key,
                (int) Math.min(Integer.MAX_VALUE, Math.min((long) key.maxStackSize(), requestedAmount))
        );
        if (extracted.isEmpty()) {
            return 0L;
        }
        if (player.drop(extracted, true) == null) {
            database.store(extracted, originalTabId);
            return 0L;
        }
        service.markScopeDirty(player, scope);
        return extracted.getCount();
    }

    static long extractSelectionToInventory(
            PersonalDatabaseService service,
            ServerPlayer player,
            DatabaseScope scope,
            StoredItemDatabase database,
            List<DatabaseSelectionEntry> selectionEntries,
            DatabaseSelectionAction action,
            long requestedAmount
    ) {
        if (action == null || !action.extractsToInventory()) {
            return 0L;
        }
        long movedItems = 0L;
        for (DatabaseSelectionEntry selectionEntry : normalizeSelectionEntries(selectionEntries)) {
            StoredStackKey key = selectionKey(selectionEntry);
            if (key == null) {
                continue;
            }
            StoredStackEntry storedEntry = database.entries().get(key);
            if (storedEntry == null || !storedEntry.tabId().equals(selectionEntry.sourceTabId())) {
                continue;
            }
            long resolvedRequestedAmount = action.resolveRequestedAmount(
                    storedEntry.amount(),
                    key.maxStackSize(),
                    requestedAmount
            );
            movedItems = safeAddMovedItems(
                    movedItems,
                    extractToInventory(service, player, scope, database, key, resolvedRequestedAmount)
            );
        }
        return movedItems;
    }

    static boolean transferSelection(
            StoredItemDatabase database,
            List<DatabaseSelectionEntry> selectionEntries,
            String targetTabId
    ) {
        boolean changed = false;
        for (DatabaseSelectionEntry selectionEntry : normalizeSelectionEntries(selectionEntries)) {
            StoredStackKey key = selectionKey(selectionEntry);
            if (key == null) {
                continue;
            }
            changed = database.moveEntryToTab(key, selectionEntry.sourceTabId(), targetTabId) || changed;
        }
        return changed;
    }

    static List<DatabaseSelectionEntry> normalizeSelectionEntries(List<DatabaseSelectionEntry> selectionEntries) {
        if (selectionEntries == null || selectionEntries.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<DatabaseSelectionEntry> normalizedEntries = new LinkedHashSet<>();
        for (DatabaseSelectionEntry selectionEntry : selectionEntries) {
            if (selectionEntry == null || selectionEntry.isEmpty()) {
                continue;
            }
            normalizedEntries.add(selectionEntry);
        }
        return List.copyOf(normalizedEntries);
    }

    static StoredStackKey selectionKey(DatabaseSelectionEntry selectionEntry) {
        if (selectionEntry == null || selectionEntry.isEmpty()) {
            return null;
        }
        try {
            return StoredStackKey.of(selectionEntry.displayStack());
        } catch (IllegalArgumentException exception) {
            LOGGER.debug("选择条目的展示物品无法解析为 StoredStackKey：{}", selectionEntry.displayStack(), exception);
            return null;
        }
    }

    private static boolean hasSpaceFor(Inventory inventory, StoredStackKey key) {
        ItemStack probe = key.toStack(1);
        return inventory.getFreeSlot() != -1 || inventory.getSlotWithRemainingSpace(probe) != -1;
    }

    private static long safeAddMovedItems(long currentTotal, long movedItems) {
        if (movedItems <= 0L) {
            return currentTotal;
        }
        if (Long.MAX_VALUE - currentTotal < movedItems) {
            return Long.MAX_VALUE;
        }
        return currentTotal + movedItems;
    }
}
