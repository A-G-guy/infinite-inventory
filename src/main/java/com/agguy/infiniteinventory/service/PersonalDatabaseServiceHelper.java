package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackEntry;
import com.agguy.infiniteinventory.database.StoredStackKey;

public final class PersonalDatabaseServiceHelper {
    private PersonalDatabaseServiceHelper() {
    }

    static String entryTabId(StoredItemDatabase database, StoredStackKey key) {
        StoredStackEntry entry = database.entries().get(key);
        return entry == null ? DatabaseTabs.DEFAULT_TAB_ID : entry.tabId();
    }

    static long safeAddMovedItems(long currentTotal, long movedItems) {
        if (movedItems <= 0L) {
            return currentTotal;
        }
        if (Long.MAX_VALUE - currentTotal < movedItems) {
            return Long.MAX_VALUE;
        }
        return currentTotal + movedItems;
    }

    static boolean isPrimaryStorageSlot(int slotIndex) {
        return PersonalDatabaseServiceStorageHelper.isPrimaryStorageSlot(slotIndex);
    }
}
