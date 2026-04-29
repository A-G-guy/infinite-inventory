package com.agguy.infiniteinventory.database;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 通过直接内存操作跨数据库搬运条目，避免 NBT 序列化往返与数据丢失风险。
 *
 * <p>设计意图：将跨库转移从“序列化-操作-反序列化”的非原子流程改为纯内存操作。
 * 转移过程中若发生异常，源库数据不会被部分修改；成功时在同一事务中完成源库扣减与目标库增加。</p>
 */
public final class DatabaseCrossTransferHelper {
    private static final Logger LOGGER = LogManager.getLogger();

    private DatabaseCrossTransferHelper() {
    }

    public static boolean transferSelection(
            @SuppressWarnings("unused") HolderLookup.Provider provider,
            StoredItemDatabase sourceDatabase,
            StoredItemDatabase targetDatabase,
            List<DatabaseSelectionEntry> selectionEntries,
            String targetTabId
    ) {
        if (sourceDatabase == null || targetDatabase == null || selectionEntries == null || selectionEntries.isEmpty()) {
            return false;
        }
        LinkedHashMap<StoredStackKey, String> requestedEntries = normalizeSelectionEntries(selectionEntries);
        if (requestedEntries.isEmpty()) {
            return false;
        }

        String normalizedTargetTabId = DatabaseTabs.normalizeConcreteTarget(targetTabId);
        List<TransferCandidate> candidates = new ArrayList<>();
        long highestMovedSequence = 0L;

        for (Map.Entry<StoredStackKey, StoredStackEntry> entry : new LinkedHashMap<>(sourceDatabase.entriesInternal()).entrySet()) {
            String expectedSourceTabId = requestedEntries.get(entry.getKey());
            if (expectedSourceTabId == null) {
                continue;
            }
            StoredStackEntry sourceEntry = entry.getValue();
            if (!sourceEntry.tabId().equals(expectedSourceTabId)) {
                continue;
            }
            candidates.add(new TransferCandidate(entry.getKey(), sourceEntry));
            highestMovedSequence = Math.max(highestMovedSequence, sourceEntry.lastModified());
        }

        if (candidates.isEmpty()) {
            return false;
        }

        long nextAfterMoved = highestMovedSequence == Long.MAX_VALUE ? Long.MAX_VALUE : highestMovedSequence + 1L;
        long newTargetNextSequence = Math.max(targetDatabase.nextSequenceInternal(), nextAfterMoved);
        targetDatabase.setNextSequence(newTargetNextSequence);

        for (TransferCandidate candidate : candidates) {
            StoredStackKey key = candidate.key;
            StoredStackEntry sourceEntry = candidate.entry;

            // 从源库移除
            sourceDatabase.entriesInternal().remove(key);
            sourceDatabase.trackAmountDelta(key, sourceEntry.tabId(), 0L, true);

            // 合并到目标库
            StoredStackEntry targetEntry = targetDatabase.entriesInternal().get(key);
            if (targetEntry == null) {
                targetDatabase.entriesInternal().put(key, new StoredStackEntry(
                        normalizedTargetTabId,
                        sourceEntry.amount(),
                        sourceEntry.lastModified(),
                        sourceEntry.firstAdded()
                ));
            } else {
                targetDatabase.entriesInternal().put(key, new StoredStackEntry(
                        normalizedTargetTabId,
                        StoredItemDatabaseHelper.safeAdd(targetEntry.amount(), sourceEntry.amount()),
                        Math.max(targetEntry.lastModified(), sourceEntry.lastModified()),
                        StoredItemDatabaseHelper.mergeFirstAdded(targetEntry.firstAdded(), sourceEntry.firstAdded())
                ));
            }
            StoredStackEntry newTargetEntry = targetDatabase.entriesInternal().get(key);
            targetDatabase.trackAmountDelta(key, newTargetEntry.tabId(), newTargetEntry.amount(), false);
        }

        sourceDatabase.markRuntimeStateDirty();
        targetDatabase.markRuntimeStateDirty();
        return true;
    }

    public static boolean transferTab(
            @SuppressWarnings("unused") HolderLookup.Provider provider,
            StoredItemDatabase sourceDatabase,
            StoredItemDatabase targetDatabase,
            String sourceTabId,
            String targetTabId
    ) {
        if (sourceDatabase == null || targetDatabase == null) {
            return false;
        }
        String normalizedSourceTabId = DatabaseTabs.normalizeConcreteTarget(sourceTabId);
        String normalizedTargetTabId = DatabaseTabs.normalizeConcreteTarget(targetTabId);

        long highestMovedSequence = 0L;
        boolean changed = false;

        Iterator<Map.Entry<StoredStackKey, StoredStackEntry>> resolvedIterator =
                sourceDatabase.entriesInternal().entrySet().iterator();
        while (resolvedIterator.hasNext()) {
            Map.Entry<StoredStackKey, StoredStackEntry> entry = resolvedIterator.next();
            StoredStackEntry sourceEntry = entry.getValue();
            if (!sourceEntry.tabId().equals(normalizedSourceTabId)) {
                continue;
            }
            StoredStackKey key = entry.getKey();

            resolvedIterator.remove();
            sourceDatabase.trackAmountDelta(key, sourceEntry.tabId(), 0L, true);

            StoredStackEntry targetEntry = targetDatabase.entriesInternal().get(key);
            if (targetEntry == null) {
                targetDatabase.entriesInternal().put(key, new StoredStackEntry(
                        normalizedTargetTabId,
                        sourceEntry.amount(),
                        sourceEntry.lastModified(),
                        sourceEntry.firstAdded()
                ));
            } else {
                targetDatabase.entriesInternal().put(key, new StoredStackEntry(
                        normalizedTargetTabId,
                        StoredItemDatabaseHelper.safeAdd(targetEntry.amount(), sourceEntry.amount()),
                        Math.max(targetEntry.lastModified(), sourceEntry.lastModified()),
                        StoredItemDatabaseHelper.mergeFirstAdded(targetEntry.firstAdded(), sourceEntry.firstAdded())
                ));
            }
            StoredStackEntry newTargetEntry = targetDatabase.entriesInternal().get(key);
            targetDatabase.trackAmountDelta(key, newTargetEntry.tabId(), newTargetEntry.amount(), false);
            highestMovedSequence = Math.max(highestMovedSequence, sourceEntry.lastModified());
            changed = true;
        }

        Iterator<UnresolvedStoredEntry> unresolvedIterator = sourceDatabase.unresolvedEntriesInternal().iterator();
        while (unresolvedIterator.hasNext()) {
            UnresolvedStoredEntry entry = unresolvedIterator.next();
            if (!entry.tabId().equals(normalizedSourceTabId)) {
                continue;
            }
            unresolvedIterator.remove();
            targetDatabase.unresolvedEntriesInternal().add(
                    entry.withTabId(normalizedTargetTabId, entry.lastModified())
            );
            highestMovedSequence = Math.max(highestMovedSequence, entry.lastModified());
            changed = true;
        }

        if (!changed) {
            return false;
        }

        long nextAfterMoved = highestMovedSequence == Long.MAX_VALUE ? Long.MAX_VALUE : highestMovedSequence + 1L;
        long newTargetNextSequence = Math.max(targetDatabase.nextSequenceInternal(), nextAfterMoved);
        targetDatabase.setNextSequence(newTargetNextSequence);

        sourceDatabase.markRuntimeStateDirty();
        targetDatabase.markRuntimeStateDirty();
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

    private record TransferCandidate(StoredStackKey key, StoredStackEntry entry) {
    }
}
