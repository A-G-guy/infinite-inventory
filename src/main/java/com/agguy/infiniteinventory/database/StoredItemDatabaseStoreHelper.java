package com.agguy.infiniteinventory.database;

import java.util.Map;
import net.minecraft.world.item.ItemStack;

/**
 * 负责 {@link StoredItemDatabase} 的存取与合并逻辑。
 *
 * <p>设计意图：将物品存储、提取、跨库合并等写操作从数据容器中剥离，
 * 降低原类复杂度，保持单一职责。</p>
 *
 * <p>本类为包级可见，仅由 StoredItemDatabase 委托调用。</p>
 */
final class StoredItemDatabaseStoreHelper {

    private StoredItemDatabaseStoreHelper() {
    }

    /**
     * 将物品存入默认标签页。
     *
     * @param database 目标数据库
     * @param stack    待存入的物品堆
     */
    static void store(StoredItemDatabase database, ItemStack stack) {
        store(database, stack, DatabaseTabs.DEFAULT_TAB_ID);
    }

    /**
     * 将物品存入指定标签页。
     *
     * <p>业务约束：空物品堆会被静默忽略。若该物品键已存在，则累加数量并更新最后修改时间戳；
     * 否则新建条目。时间戳由内部单调递增序列号生成，保证合并时的因果一致性。</p>
     *
     * @param database 目标数据库
     * @param stack    待存入的物品堆
     * @param tabId    目标标签页 ID
     */
    static void store(StoredItemDatabase database, ItemStack stack, String tabId) {
        if (stack.isEmpty()) {
            return;
        }
        long sequence = database.nextSequence();
        String normalizedTabId = DatabaseTabs.normalizeConcreteTarget(tabId);
        StoredStackKey key = StoredStackKey.of(stack);
        StoredStackEntry entry = database.entries().get(key);
        if (entry == null) {
            entry = new StoredStackEntry(normalizedTabId, 0L, sequence, sequence);
            database.entriesInternal().put(key, entry);
        }
        entry.add(stack.getCount(), sequence);
        database.markRuntimeStateDirty();
    }

    /**
     * 从仓库中提取指定数量的物品。
     *
     * <p>业务约束：请求数量小于等于 0 时直接返回空堆；
     * 实际提取数量受物品最大堆叠上限限制，避免生成超过客户端合理预期的超大堆叠。
     * 若提取后该物品库存归零，则自动移除条目以节省内存。</p>
     *
     * @param database        目标数据库
     * @param key             物品键
     * @param requestedAmount 请求提取的数量
     * @return 实际提取到的物品堆，若无法提取则返回 {@link ItemStack#EMPTY}
     */
    static ItemStack extract(StoredItemDatabase database, StoredStackKey key, int requestedAmount) {
        if (requestedAmount <= 0) {
            return ItemStack.EMPTY;
        }
        StoredStackEntry entry = database.entries().get(key);
        if (entry == null) {
            return ItemStack.EMPTY;
        }
        int maxExtractableAmount = Math.max(1, key.maxStackSize());
        int extractedAmount = (int) Math.min(entry.amount(), Math.min((long) requestedAmount, (long) maxExtractableAmount));
        if (extractedAmount <= 0) {
            return ItemStack.EMPTY;
        }
        long sequence = database.nextSequence();
        entry.remove(extractedAmount, sequence);
        ItemStack extractedStack = key.toStack(extractedAmount);
        if (entry.isEmpty()) {
            database.entriesInternal().remove(key);
        }
        database.markRuntimeStateDirty();
        return extractedStack;
    }

    /**
     * 将另一个数据库的数据合并到本数据库。
     *
     * <p>设计意图：用于跨作用域转移标签页或数据迁移场景。合并策略如下：</p>
     * <ul>
     *   <li>已解析条目：按 {@code lastModified} 时间戳决定保留哪个标签页归属，数量做安全加法；</li>
     *   <li>备注与收藏：以"存在即覆盖"方式合并；</li>
     *   <li>未解析条目：直接追加，保留原始 NBT；</li>
     *   <li>序列号：取双方最大值并加一，防止时间戳冲突。</li>
     * </ul>
     *
     * @param database 目标数据库
     * @param other    要合并的源数据库，若为 {@code null} 则直接返回
     */
    static void mergeFrom(StoredItemDatabase database, StoredItemDatabase other) {
        if (other == null) {
            return;
        }
        boolean changed = false;
        long highestMergedSequence = 0L;
        for (Map.Entry<StoredStackKey, StoredStackEntry> mapEntry : other.entries().entrySet()) {
            database.mergeResolvedEntryInternal(mapEntry.getKey(), mapEntry.getValue());
            highestMergedSequence = Math.max(highestMergedSequence, mapEntry.getValue().lastModified());
            changed = true;
        }
        for (Map.Entry<StoredStackKey, String> noteEntry : other.notes().entrySet()) {
            String existing = database.notesInternal().get(noteEntry.getKey());
            if (existing == null || !existing.equals(noteEntry.getValue())) {
                database.notesInternal().put(noteEntry.getKey(), noteEntry.getValue());
                changed = true;
            }
        }
        for (StoredStackKey starredKey : other.starredEntries()) {
            if (!database.starredEntriesInternal().contains(starredKey)) {
                database.starredEntriesInternal().add(starredKey);
                changed = true;
            }
        }
        for (UnresolvedStoredEntry unresolvedEntry : other.unresolvedEntries()) {
            if (!unresolvedEntry.isEmpty()) {
                database.unresolvedEntriesInternal().add(new UnresolvedStoredEntry(
                        unresolvedEntry.stackTag(), unresolvedEntry.amount(), unresolvedEntry.tabId(),
                        unresolvedEntry.lastModified(), unresolvedEntry.firstAdded()
                ));
                highestMergedSequence = Math.max(highestMergedSequence, unresolvedEntry.lastModified());
                changed = true;
            }
        }
        if (!changed) {
            return;
        }
        long nextAfterMerged = highestMergedSequence == Long.MAX_VALUE ? Long.MAX_VALUE : highestMergedSequence + 1L;
        long newNextSequence = Math.max(database.nextSequenceInternal(), Math.max(other.nextSequenceInternal(), nextAfterMerged));
        if (newNextSequence <= 0L) {
            newNextSequence = 1L;
        }
        database.setNextSequence(newNextSequence);
        database.markRuntimeStateDirty();
    }
}
