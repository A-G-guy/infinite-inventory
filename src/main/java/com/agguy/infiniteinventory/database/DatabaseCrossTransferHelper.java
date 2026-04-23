package com.agguy.infiniteinventory.database;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 通过序列化标签跨数据库搬运条目，确保显式目标分类优先且保留原始元数据。
 */
public final class DatabaseCrossTransferHelper {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String ENTRIES_KEY = "entries";
    private static final String UNRESOLVED_ENTRIES_KEY = "unresolved_entries";
    private static final String STACK_KEY = "stack";
    private static final String COUNT_KEY = "count";
    private static final String TAB_ID_KEY = "tab_id";
    private static final String FIRST_ADDED_KEY = "first_added";
    private static final String LAST_MODIFIED_KEY = "last_modified";
    private static final String NEXT_SEQUENCE_KEY = "next_sequence";

    private DatabaseCrossTransferHelper() {
    }

    public static boolean transferSelection(
            HolderLookup.Provider provider,
            StoredItemDatabase sourceDatabase,
            StoredItemDatabase targetDatabase,
            List<DatabaseSelectionEntry> selectionEntries,
            String targetTabId
    ) {
        if (sourceDatabase == null || targetDatabase == null || selectionEntries == null || selectionEntries.isEmpty()) {
            return false;
        }
        HolderLookup.Provider resolvedProvider = DatabaseHolderLookup.resolve(provider);
        LinkedHashMap<StoredStackKey, String> requestedEntries = normalizeSelectionEntries(selectionEntries);
        if (requestedEntries.isEmpty()) {
            return false;
        }

        CompoundTag sourceTag = sourceDatabase.serializeNBT(resolvedProvider);
        CompoundTag targetTag = targetDatabase.serializeNBT(resolvedProvider);
        LinkedHashMap<StoredStackKey, CompoundTag> targetEntriesByKey = indexResolvedEntries(resolvedProvider, targetTag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND));
        ListTag updatedSourceEntries = new ListTag();
        long highestMovedSequence = 0L;
        boolean changed = false;

        for (Tag element : sourceTag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND)) {
            if (!(element instanceof CompoundTag entryTag)) {
                continue;
            }
            StoredStackKey key = readResolvedKey(resolvedProvider, entryTag);
            String expectedSourceTabId = key == null ? null : requestedEntries.get(key);
            if (expectedSourceTabId != null && readTabId(entryTag).equals(expectedSourceTabId)) {
                mergeResolvedEntry(targetEntriesByKey, key, entryTag, targetTabId);
                highestMovedSequence = Math.max(highestMovedSequence, readLastModified(entryTag));
                changed = true;
                continue;
            }
            updatedSourceEntries.add(entryTag.copy());
        }

        if (!changed) {
            return false;
        }
        sourceTag.put(ENTRIES_KEY, updatedSourceEntries);
        targetTag.put(ENTRIES_KEY, toListTag(targetEntriesByKey));
        bumpNextSequence(targetTag, highestMovedSequence);
        sourceDatabase.deserializeNBT(resolvedProvider, sourceTag);
        targetDatabase.deserializeNBT(resolvedProvider, targetTag);
        return true;
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
        HolderLookup.Provider resolvedProvider = DatabaseHolderLookup.resolve(provider);
        String normalizedSourceTabId = DatabaseTabs.normalizeConcreteTarget(sourceTabId);
        String normalizedTargetTabId = DatabaseTabs.normalizeConcreteTarget(targetTabId);

        CompoundTag sourceTag = sourceDatabase.serializeNBT(resolvedProvider);
        CompoundTag targetTag = targetDatabase.serializeNBT(resolvedProvider);
        LinkedHashMap<StoredStackKey, CompoundTag> targetEntriesByKey = indexResolvedEntries(resolvedProvider, targetTag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND));
        ListTag updatedSourceEntries = new ListTag();
        ListTag updatedSourceUnresolvedEntries = new ListTag();
        ListTag updatedTargetUnresolvedEntries = copyList(targetTag.getList(UNRESOLVED_ENTRIES_KEY, Tag.TAG_COMPOUND));
        long highestMovedSequence = 0L;
        boolean changed = false;

        for (Tag element : sourceTag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND)) {
            if (!(element instanceof CompoundTag entryTag)) {
                continue;
            }
            if (!readTabId(entryTag).equals(normalizedSourceTabId)) {
                updatedSourceEntries.add(entryTag.copy());
                continue;
            }
            StoredStackKey key = readResolvedKey(resolvedProvider, entryTag);
            if (key == null) {
                updatedSourceEntries.add(entryTag.copy());
                continue;
            }
            mergeResolvedEntry(targetEntriesByKey, key, entryTag, normalizedTargetTabId);
            highestMovedSequence = Math.max(highestMovedSequence, readLastModified(entryTag));
            changed = true;
        }

        for (Tag element : sourceTag.getList(UNRESOLVED_ENTRIES_KEY, Tag.TAG_COMPOUND)) {
            if (!(element instanceof CompoundTag entryTag)) {
                continue;
            }
            if (!readTabId(entryTag).equals(normalizedSourceTabId)) {
                updatedSourceUnresolvedEntries.add(entryTag.copy());
                continue;
            }
            CompoundTag movedEntry = entryTag.copy();
            movedEntry.putString(TAB_ID_KEY, normalizedTargetTabId);
            updatedTargetUnresolvedEntries.add(movedEntry);
            highestMovedSequence = Math.max(highestMovedSequence, readLastModified(entryTag));
            changed = true;
        }

        if (!changed) {
            return false;
        }
        sourceTag.put(ENTRIES_KEY, updatedSourceEntries);
        sourceTag.put(UNRESOLVED_ENTRIES_KEY, updatedSourceUnresolvedEntries);
        targetTag.put(ENTRIES_KEY, toListTag(targetEntriesByKey));
        targetTag.put(UNRESOLVED_ENTRIES_KEY, updatedTargetUnresolvedEntries);
        bumpNextSequence(targetTag, highestMovedSequence);
        sourceDatabase.deserializeNBT(resolvedProvider, sourceTag);
        targetDatabase.deserializeNBT(resolvedProvider, targetTag);
        return true;
    }

    private static LinkedHashMap<StoredStackKey, String> normalizeSelectionEntries(List<DatabaseSelectionEntry> selectionEntries) {
        LinkedHashMap<StoredStackKey, String> normalizedEntries = new LinkedHashMap<>();
        for (DatabaseSelectionEntry selectionEntry : new LinkedHashSet<>(selectionEntries)) {
            if (selectionEntry == null || selectionEntry.isEmpty()) {
                continue;
            }
            try {
                normalizedEntries.put(StoredStackKey.of(selectionEntry.displayStack()), selectionEntry.sourceTabId());
            } catch (IllegalArgumentException exception) {
                LOGGER.debug("跳过异常选择条目：客户端展示物品无法解析为 StoredStackKey", exception);
            }
        }
        return normalizedEntries;
    }

    private static LinkedHashMap<StoredStackKey, CompoundTag> indexResolvedEntries(HolderLookup.Provider provider, ListTag entries) {
        LinkedHashMap<StoredStackKey, CompoundTag> indexedEntries = new LinkedHashMap<>();
        for (Tag element : entries) {
            if (!(element instanceof CompoundTag entryTag)) {
                continue;
            }
            StoredStackKey key = readResolvedKey(provider, entryTag);
            if (key == null) {
                continue;
            }
            indexedEntries.putIfAbsent(key, entryTag.copy());
        }
        return indexedEntries;
    }

    private static StoredStackKey readResolvedKey(HolderLookup.Provider provider, CompoundTag entryTag) {
        if (provider == null || entryTag == null || !entryTag.contains(STACK_KEY)) {
            return null;
        }
        ItemStack stack = ItemStack.parseOptional(provider, entryTag.getCompound(STACK_KEY).copy());
        if (stack.isEmpty()) {
            return null;
        }
        return StoredStackKey.of(stack);
    }

    private static void mergeResolvedEntry(
            Map<StoredStackKey, CompoundTag> targetEntriesByKey,
            StoredStackKey key,
            CompoundTag incomingEntryTag,
            String targetTabId
    ) {
        if (key == null || incomingEntryTag == null) {
            return;
        }
        String normalizedTargetTabId = DatabaseTabs.normalizeConcreteTarget(targetTabId);
        CompoundTag existingEntryTag = targetEntriesByKey.get(key);
        if (existingEntryTag == null) {
            CompoundTag copiedEntryTag = incomingEntryTag.copy();
            copiedEntryTag.putString(TAB_ID_KEY, normalizedTargetTabId);
            targetEntriesByKey.put(key, copiedEntryTag);
            return;
        }
        existingEntryTag.putLong(COUNT_KEY, safeAdd(readAmount(existingEntryTag), readAmount(incomingEntryTag)));
        existingEntryTag.putString(TAB_ID_KEY, normalizedTargetTabId);
        existingEntryTag.putLong(LAST_MODIFIED_KEY, Math.max(readLastModified(existingEntryTag), readLastModified(incomingEntryTag)));
        existingEntryTag.putLong(FIRST_ADDED_KEY, mergeFirstAdded(readFirstAdded(existingEntryTag), readFirstAdded(incomingEntryTag)));
    }

    private static ListTag toListTag(Map<StoredStackKey, CompoundTag> entriesByKey) {
        ListTag entries = new ListTag();
        for (CompoundTag entryTag : entriesByKey.values()) {
            entries.add(entryTag.copy());
        }
        return entries;
    }

    private static ListTag copyList(ListTag sourceEntries) {
        ListTag copiedEntries = new ListTag();
        for (Tag element : sourceEntries) {
            copiedEntries.add(element.copy());
        }
        return copiedEntries;
    }

    private static void bumpNextSequence(CompoundTag tag, long highestMovedSequence) {
        long nextAfterMovedEntries = highestMovedSequence == Long.MAX_VALUE ? Long.MAX_VALUE : highestMovedSequence + 1L;
        tag.putLong(NEXT_SEQUENCE_KEY, Math.max(tag.getLong(NEXT_SEQUENCE_KEY), nextAfterMovedEntries));
    }

    private static String readTabId(CompoundTag tag) {
        return DatabaseTabs.normalizeConcreteTarget(tag.getString(TAB_ID_KEY));
    }

    private static long readAmount(CompoundTag tag) {
        return Math.max(0L, tag.getLong(COUNT_KEY));
    }

    private static long readLastModified(CompoundTag tag) {
        return Math.max(0L, tag.getLong(LAST_MODIFIED_KEY));
    }

    private static long readFirstAdded(CompoundTag tag) {
        if (!tag.contains(FIRST_ADDED_KEY)) {
            return readLastModified(tag);
        }
        return Math.max(0L, tag.getLong(FIRST_ADDED_KEY));
    }

    private static long mergeFirstAdded(long left, long right) {
        long normalizedLeft = Math.max(0L, left);
        long normalizedRight = Math.max(0L, right);
        if (normalizedLeft == 0L) {
            return normalizedRight;
        }
        if (normalizedRight == 0L) {
            return normalizedLeft;
        }
        return Math.min(normalizedLeft, normalizedRight);
    }

    private static long safeAdd(long left, long right) {
        if (right <= 0L) {
            return left;
        }
        if (Long.MAX_VALUE - left < right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }
}
