package com.agguy.infiniteinventory.database;

import net.minecraft.nbt.CompoundTag;

final class StoredItemDatabaseHelper {
    private StoredItemDatabaseHelper() {
    }

    static String readTabId(CompoundTag tag, int storedSchemaVersion, int currentSchemaVersion) {
        if (storedSchemaVersion >= currentSchemaVersion && tag.contains(StoredItemDatabase.TAB_ID_KEY)) {
            return DatabaseTabs.normalizeConcreteTarget(tag.getString(StoredItemDatabase.TAB_ID_KEY));
        }
        if (tag.contains(StoredItemDatabase.TAB_ID_KEY)) {
            return DatabaseTabs.normalizeConcreteTarget(tag.getString(StoredItemDatabase.TAB_ID_KEY));
        }
        return DatabaseTabs.DEFAULT_TAB_ID;
    }

    static long readFirstAdded(CompoundTag tag, long lastModified) {
        if (!tag.contains(StoredItemDatabase.FIRST_ADDED_KEY)) {
            return Math.max(0L, lastModified);
        }
        return Math.max(0L, tag.getLong(StoredItemDatabase.FIRST_ADDED_KEY));
    }

    static long mergeFirstAdded(long existingFirstAdded, long incomingFirstAdded) {
        long normalizedExisting = Math.max(0L, existingFirstAdded);
        long normalizedIncoming = Math.max(0L, incomingFirstAdded);
        if (normalizedExisting == 0L) {
            return normalizedIncoming;
        }
        if (normalizedIncoming == 0L) {
            return normalizedExisting;
        }
        return Math.min(normalizedExisting, normalizedIncoming);
    }

    static long safeAdd(long left, long right) {
        if (right <= 0L) {
            return left;
        }
        if (Long.MAX_VALUE - left < right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }
}
