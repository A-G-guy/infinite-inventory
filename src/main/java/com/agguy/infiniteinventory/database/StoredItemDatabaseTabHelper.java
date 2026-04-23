package com.agguy.infiniteinventory.database;

import java.util.ArrayList;

/**
 * 负责 {@link StoredItemDatabase} 的标签页迁移与校验逻辑。
 *
 * <p>设计意图：将标签页相关的数据迁移与一致性校验从数据容器中剥离，
 * 降低原类复杂度，保持单一职责。</p>
 *
 * <p>本类为包级可见，仅由 StoredItemDatabase 委托调用。</p>
 */
final class StoredItemDatabaseTabHelper {

    private StoredItemDatabaseTabHelper() {
    }

    /**
     * 将整个标签页下的所有物品转移到另一个标签页。
     *
     * <p>业务约束：源标签页与目标标签页相同时直接返回 {@code false}，避免无意义操作。
     * 同时处理已解析条目与未解析条目，确保数据完整性。</p>
     *
     * @param database    目标数据库
     * @param sourceTabId 源标签页 ID
     * @param targetTabId 目标标签页 ID
     * @return 若至少有一条目发生迁移则返回 {@code true}
     */
    static boolean transferTab(StoredItemDatabase database, String sourceTabId, String targetTabId) {
        String normalizedSourceTabId = DatabaseTabs.normalizeConcreteTarget(sourceTabId);
        String normalizedTargetTabId = DatabaseTabs.normalizeConcreteTarget(targetTabId);
        if (normalizedSourceTabId.equals(normalizedTargetTabId)) {
            return false;
        }
        long sequence = database.nextSequence();
        boolean changed = false;
        for (StoredStackEntry entry : database.entries().values()) {
            changed = entry.tabId().equals(normalizedSourceTabId) && entry.moveToTab(normalizedTargetTabId, sequence) || changed;
        }
        if (!database.unresolvedEntries().isEmpty()) {
            ArrayList<UnresolvedStoredEntry> updatedEntries = new ArrayList<>(database.unresolvedEntries().size());
            for (UnresolvedStoredEntry unresolvedEntry : database.unresolvedEntries()) {
                if (unresolvedEntry.tabId().equals(normalizedSourceTabId)) {
                    updatedEntries.add(unresolvedEntry.withTabId(normalizedTargetTabId, sequence));
                    changed = true;
                } else {
                    updatedEntries.add(unresolvedEntry);
                }
            }
            database.unresolvedEntriesInternal().clear();
            database.unresolvedEntriesInternal().addAll(updatedEntries);
        }
        if (changed) {
            database.markRuntimeStateDirty();
        }
        return changed;
    }

    /**
     * 将指定物品从源标签页移动到目标标签页。
     *
     * <p>业务约束：仅当该物品当前确实存在于源标签页时才会执行移动，防止误操作或并发覆盖。</p>
     *
     * @param database    目标数据库
     * @param key         物品键
     * @param sourceTabId 源标签页 ID
     * @param targetTabId 目标标签页 ID
     * @return 若移动成功则返回 {@code true}
     */
    static boolean moveEntryToTab(StoredItemDatabase database, StoredStackKey key, String sourceTabId, String targetTabId) {
        if (key == null) {
            return false;
        }
        StoredStackEntry entry = database.entries().get(key);
        if (entry == null || !entry.tabId().equals(DatabaseTabs.normalizeConcreteTarget(sourceTabId))) {
            return false;
        }
        boolean changed = entry.moveToTab(targetTabId, database.nextSequence());
        if (changed) {
            database.markRuntimeStateDirty();
        }
        return changed;
    }

    /**
     * 确保所有条目都分配到了有效的标签页。
     *
     * <p>设计意图：标签页可能被删除或重命名，导致数据库中残留无效 {@code tabId}。
     * 本方法在每次数据访问前由服务层调用，将无效标签页回退到默认页，保证查询与展示的正确性。
     * 同时会重建未解析条目的标签页归属。</p>
     *
     * @param database     目标数据库
     * @param tabDirectory 当前有效的标签页目录
     * @return 若发生了任何回退操作则返回 {@code true}
     */
    static boolean ensureTabAssignments(StoredItemDatabase database, DatabaseTabDirectory tabDirectory) {
        if (tabDirectory == null) {
            return false;
        }
        String defaultTabId = tabDirectory.defaultConcreteTab().id();
        boolean changed = false;
        for (StoredStackEntry entry : database.entries().values()) {
            if (!tabDirectory.containsConcreteTab(entry.tabId())) {
                entry.moveToTab(defaultTabId, entry.lastModified());
                changed = true;
            }
        }
        if (!database.unresolvedEntries().isEmpty()) {
            ArrayList<UnresolvedStoredEntry> updatedUnresolvedEntries = new ArrayList<>(database.unresolvedEntries().size());
            for (UnresolvedStoredEntry unresolvedEntry : database.unresolvedEntries()) {
                if (!tabDirectory.containsConcreteTab(unresolvedEntry.tabId())) {
                    updatedUnresolvedEntries.add(new UnresolvedStoredEntry(
                            unresolvedEntry.stackTag(),
                            unresolvedEntry.amount(),
                            defaultTabId,
                            unresolvedEntry.lastModified(),
                            unresolvedEntry.firstAdded()
                    ));
                    changed = true;
                } else {
                    updatedUnresolvedEntries.add(unresolvedEntry);
                }
            }
            if (changed) {
                database.unresolvedEntriesInternal().clear();
                database.unresolvedEntriesInternal().addAll(updatedUnresolvedEntries);
            }
        }
        if (changed) {
            database.markRuntimeStateDirty();
        }
        return changed;
    }
}
