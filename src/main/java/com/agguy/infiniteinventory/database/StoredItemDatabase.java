package com.agguy.infiniteinventory.database;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * 单个数据库实例的数据容器，负责管理已解析物品条目、未解析条目、备注、收藏状态及操作日志。
 *
 * <p>设计意图：将“数据”与“服务”严格分离。本类只持有原始数据并提供最小化的原子操作，
 * 不涉及任何玩家交互、网络同步或业务编排。所有公共写操作都会递增内部版本号（{@link #revision}），
 * 供上层服务快速判断数据是否发生变化，从而决定是否需要重新查询或同步客户端。</p>
 *
 * <p>本类同时承担 NBT 序列化与多版本格式兼容职责，通过 {@code schema_version} 字段实现向前兼容的存档升级。</p>
 */
public class StoredItemDatabase implements INBTSerializable<CompoundTag> {
    public static final int CURRENT_SCHEMA_VERSION = 6;

    private static final String SCHEMA_VERSION_KEY = "schema_version";
    private static final String ENTRIES_KEY = "entries";
    private static final String UNRESOLVED_ENTRIES_KEY = "unresolved_entries";
    private static final String STACK_KEY = "stack";
    private static final String COUNT_KEY = "count";
    static final String TAB_ID_KEY = "tab_id";
    static final String FIRST_ADDED_KEY = "first_added";
    private static final String LAST_MODIFIED_KEY = "last_modified";
    private static final String NEXT_SEQUENCE_KEY = "next_sequence";
    private static final String LOG_ENTRIES_KEY = "log_entries";
    private static final String NOTES_KEY = "notes";
    private static final String NOTE_KEY = "note_key";
    private static final String NOTE_TEXT_KEY = "note_text";
    private static final String STARRED_ENTRIES_KEY = "starred_entries";
    private static final int MAX_LOG_ENTRIES = 500;
    private static final int MAX_NOTE_LENGTH = 256;

    private final Map<StoredStackKey, StoredStackEntry> entries = new LinkedHashMap<>();
    private final Map<StoredStackKey, String> notes = new LinkedHashMap<>();
    private final Set<StoredStackKey> starredEntries = new LinkedHashSet<>();
    private final java.util.List<UnresolvedStoredEntry> unresolvedEntries = new java.util.ArrayList<>();
    private final java.util.List<DatabaseLogEntry> logEntries = new java.util.ArrayList<>();
    private long nextSequence = 1L;
    private long revision;
    private boolean needsResave;

    /**
     * 获取所有已解析物品条目的不可变视图。
     *
     * <p>返回不可变映射以防止外部直接修改内部状态，所有变更应通过 {@link #store}、{@link #extract} 等原子方法完成。</p>
     *
     * @return 物品键到条目的映射视图
     */
    public Map<StoredStackKey, StoredStackEntry> entries() { return Collections.unmodifiableMap(this.entries); }

    /**
     * 获取所有备注的不可变视图。
     *
     * @return 物品键到备注文本的映射视图
     */
    public Map<StoredStackKey, String> notes() { return Collections.unmodifiableMap(this.notes); }

    /**
     * 获取所有被收藏物品键的不可变视图。
     *
     * @return 被收藏物品键的集合视图
     */
    public Set<StoredStackKey> starredEntries() { return Collections.unmodifiableSet(this.starredEntries); }

    /**
     * 获取所有未解析条目的不可变副本。
     *
     * <p>未解析条目通常由模组卸载或版本升级导致物品注册失效而产生，
     * 保留原始 NBT 以便未来模组重新安装后恢复。</p>
     *
     * @return 未解析条目列表的不可变副本
     */
    public List<UnresolvedStoredEntry> unresolvedEntries() { return List.copyOf(this.unresolvedEntries); }

    /**
     * 获取操作日志条目的不可变副本。
     *
     * <p>日志上限由 {@code MAX_LOG_ENTRIES} 控制，超出时自动移除最旧的条目。</p>
     *
     * @return 日志条目列表的不可变副本
     */
    public List<DatabaseLogEntry> logEntries() {
        return List.copyOf(this.logEntries);
    }

    /**
     * 获取未解析条目的数量。
     *
     * @return 未解析条目数
     */
    public int unresolvedEntryCount() {
        return this.unresolvedEntries.size();
    }

    /**
     * 判断是否存在未解析条目。
     *
     * @return 若存在至少一条未解析条目则返回 {@code true}
     */
    public boolean hasUnresolvedEntries() {
        return !this.unresolvedEntries.isEmpty();
    }

    /**
     * 获取指定物品键在当前仓库中的存储数量。
     *
     * @param key 物品键
     * @return 存储数量，若不存在则返回 {@code 0}
     */
    public long getAmount(StoredStackKey key) { StoredStackEntry entry = this.entries.get(key); return entry == null ? 0L : entry.amount(); }

    /**
     * 获取已解析条目的种类数（不同物品键的数量）。
     *
     * @return 条目种类数
     */
    public int entryCount() { return this.entries.size(); }

    /**
     * 获取当前数据版本号。
     *
     * <p>设计意图：每次写操作都会递增版本号，上层服务可通过比对版本号快速判断缓存是否失效，
     * 避免无意义的重复查询。</p>
     *
     * @return 当前版本号
     */
    public long revision() { return this.revision; }

    /**
     * 判断数据是否需要重新保存。
     *
     * <p>当从旧版 schema 反序列化时，本标志会被置为 {@code true}，提示上层在下次存档时以当前 schema 重写。</p>
     *
     * @return 若需要重新保存则返回 {@code true}
     */
    public boolean needsResave() { return this.needsResave; }

    /**
     * 清空仓库中的所有数据（包括已解析、未解析、备注、收藏、日志）。
     *
     * <p>业务约束：仅在仓库确实含有内容时才标记为脏状态，避免空清空导致无意义的版本递增。</p>
     */
    public void clear() { if (this.hasStoredContent()) { this.resetContent(); this.markRuntimeStateDirty(); } }

    /**
     * 获取指定物品的备注文本。
     *
     * @param key 物品键
     * @return 备注内容，若无备注或 key 为 {@code null} 则返回空字符串
     */
    public String noteFor(StoredStackKey key) { return key == null ? "" : this.notes.getOrDefault(key, ""); }

    /**
     * 判断指定物品是否被收藏。
     *
     * @param key 物品键
     * @return 若已收藏则返回 {@code true}；key 为 {@code null} 时返回 {@code false}
     */
    public boolean isStarred(StoredStackKey key) { return key != null && this.starredEntries.contains(key); }

    /**
     * 切换指定物品的收藏状态。
     *
     * <p>若当前已收藏则取消收藏，否则加入收藏。操作成功后触发脏标记。</p>
     *
     * @param key 物品键
     * @return 若收藏状态发生变化则返回 {@code true}
     */
    public boolean toggleStar(StoredStackKey key) {
        if (key == null) return false;
        boolean changed = this.starredEntries.contains(key) ? this.starredEntries.remove(key) : this.starredEntries.add(key);
        if (changed) this.markRuntimeStateDirty();
        return changed;
    }

    /**
     * 强制设置指定物品的收藏状态。
     *
     * <p>与 {@link #toggleStar} 的区别在于直接指定目标状态，避免不必要的取反逻辑。</p>
     *
     * @param key     物品键
     * @param starred 目标收藏状态
     * @return 若收藏状态发生变化则返回 {@code true}
     */
    public boolean setStarred(StoredStackKey key, boolean starred) {
        if (key == null) return false;
        boolean currentlyStarred = this.starredEntries.contains(key);
        if (currentlyStarred == starred) return false;
        if (starred) {
            this.starredEntries.add(key);
        } else {
            this.starredEntries.remove(key);
        }
        this.markRuntimeStateDirty();
        return true;
    }

    /**
     * 为指定物品设置备注。
     *
     * <p>业务约束：备注长度超过 {@code MAX_NOTE_LENGTH}（256）时会被截断；
     * 空字符串或仅空白字符会清除已有备注。只有实际内容发生变化时才触发脏标记。</p>
     *
     * @param key  物品键
     * @param note 备注内容，{@code null} 会被视为空字符串
     */
    public void setNote(StoredStackKey key, String note) {
        if (key == null) return;
        String normalized = note == null ? "" : note.trim();
        if (normalized.length() > MAX_NOTE_LENGTH) normalized = normalized.substring(0, MAX_NOTE_LENGTH);
        if (normalized.isEmpty()) { if (this.notes.remove(key) != null) this.markRuntimeStateDirty(); return; }
        String existing = this.notes.get(key);
        if (existing == null || !existing.equals(normalized)) { this.notes.put(key, normalized); this.markRuntimeStateDirty(); }
    }

    /**
     * 将另一个数据库的数据合并到本数据库。
     *
     * <p>设计意图：用于跨作用域转移标签页或数据迁移场景。合并策略如下：</p>
     * <ul>
     *   <li>已解析条目：按 {@code lastModified} 时间戳决定保留哪个标签页归属，数量做安全加法；</li>
     *   <li>备注与收藏：以“存在即覆盖”方式合并；</li>
     *   <li>未解析条目：直接追加，保留原始 NBT；</li>
     *   <li>序列号：取双方最大值并加一，防止时间戳冲突。</li>
     * </ul>
     *
     * @param other 要合并的源数据库，若为 {@code null} 则直接返回
     */
    public void mergeFrom(StoredItemDatabase other) {
        if (other == null) return;
        boolean changed = false;
        long highestMergedSequence = 0L;
        for (Map.Entry<StoredStackKey, StoredStackEntry> mapEntry : other.entries().entrySet()) {
            this.mergeResolvedEntryInternal(mapEntry.getKey(), mapEntry.getValue());
            highestMergedSequence = Math.max(highestMergedSequence, mapEntry.getValue().lastModified());
            changed = true;
        }
        for (Map.Entry<StoredStackKey, String> noteEntry : other.notes().entrySet()) {
            String existing = this.notes.get(noteEntry.getKey());
            if (existing == null || !existing.equals(noteEntry.getValue())) {
                this.notes.put(noteEntry.getKey(), noteEntry.getValue()); changed = true;
            }
        }
        for (StoredStackKey starredKey : other.starredEntries()) {
            if (!this.starredEntries.contains(starredKey)) {
                this.starredEntries.add(starredKey); changed = true;
            }
        }
        for (UnresolvedStoredEntry unresolvedEntry : other.unresolvedEntries()) {
            if (!unresolvedEntry.isEmpty()) {
                this.unresolvedEntries.add(new UnresolvedStoredEntry(
                        unresolvedEntry.stackTag(), unresolvedEntry.amount(), unresolvedEntry.tabId(),
                        unresolvedEntry.lastModified(), unresolvedEntry.firstAdded()
                ));
                highestMergedSequence = Math.max(highestMergedSequence, unresolvedEntry.lastModified());
                changed = true;
            }
        }
        if (!changed) return;
        long nextAfterMerged = highestMergedSequence == Long.MAX_VALUE ? Long.MAX_VALUE : highestMergedSequence + 1L;
        this.nextSequence = Math.max(this.nextSequence, Math.max(other.nextSequence, nextAfterMerged));
        if (this.nextSequence <= 0L) this.nextSequence = 1L;
        this.markRuntimeStateDirty();
    }

    /**
     * 将物品存入默认标签页。
     *
     * <p>等价于以 {@link DatabaseTabs#DEFAULT_TAB_ID} 为标签页调用 {@link #store(ItemStack, String)}。</p>
     *
     * @param stack 待存入的物品堆，若为 {@link ItemStack#EMPTY} 则忽略
     */
    public void store(ItemStack stack) {
        this.store(stack, DatabaseTabs.DEFAULT_TAB_ID);
    }

    /**
     * 将物品存入指定标签页。
     *
     * <p>业务约束：空物品堆会被静默忽略。若该物品键已存在，则累加数量并更新最后修改时间戳；
     * 否则新建条目。时间戳由内部单调递增序列号生成，保证合并时的因果一致性。</p>
     *
     * @param stack 待存入的物品堆
     * @param tabId 目标标签页 ID
     */
    public void store(ItemStack stack, String tabId) {
        if (stack.isEmpty()) return;
        long sequence = this.nextSequence();
        String normalizedTabId = DatabaseTabs.normalizeConcreteTarget(tabId);
        StoredStackKey key = StoredStackKey.of(stack);
        StoredStackEntry entry = this.entries.get(key);
        if (entry == null) {
            entry = new StoredStackEntry(normalizedTabId, 0L, sequence, sequence);
            this.entries.put(key, entry);
        } else {
            entry.moveToTab(normalizedTabId, sequence);
        }
        entry.add(stack.getCount(), sequence);
        this.markRuntimeStateDirty();
    }

    /**
     * 将整个标签页下的所有物品转移到另一个标签页。
     *
     * <p>业务约束：源标签页与目标标签页相同时直接返回 {@code false}，避免无意义操作。
     * 同时处理已解析条目与未解析条目，确保数据完整性。</p>
     *
     * @param sourceTabId 源标签页 ID
     * @param targetTabId 目标标签页 ID
     * @return 若至少有一条目发生迁移则返回 {@code true}
     */
    public boolean transferTab(String sourceTabId, String targetTabId) {
        String normalizedSourceTabId = DatabaseTabs.normalizeConcreteTarget(sourceTabId), normalizedTargetTabId = DatabaseTabs.normalizeConcreteTarget(targetTabId);
        if (normalizedSourceTabId.equals(normalizedTargetTabId)) return false;
        long sequence = this.nextSequence();
        boolean changed = false;
        for (StoredStackEntry entry : this.entries.values()) {
            changed = entry.tabId().equals(normalizedSourceTabId) && entry.moveToTab(normalizedTargetTabId, sequence) || changed;
        }
        if (!this.unresolvedEntries.isEmpty()) {
            java.util.ArrayList<UnresolvedStoredEntry> updatedEntries = new java.util.ArrayList<>(this.unresolvedEntries.size());
            for (UnresolvedStoredEntry unresolvedEntry : this.unresolvedEntries) {
                if (unresolvedEntry.tabId().equals(normalizedSourceTabId)) {
                    updatedEntries.add(unresolvedEntry.withTabId(normalizedTargetTabId, sequence));
                    changed = true;
                } else {
                    updatedEntries.add(unresolvedEntry);
                }
            }
            this.unresolvedEntries.clear();
            this.unresolvedEntries.addAll(updatedEntries);
        }
        if (changed) {
            this.markRuntimeStateDirty();
        }
        return changed;
    }

    /**
     * 将指定物品从源标签页移动到目标标签页。
     *
     * <p>业务约束：仅当该物品当前确实存在于源标签页时才会执行移动，防止误操作或并发覆盖。</p>
     *
     * @param key         物品键
     * @param sourceTabId 源标签页 ID
     * @param targetTabId 目标标签页 ID
     * @return 若移动成功则返回 {@code true}
     */
    public boolean moveEntryToTab(StoredStackKey key, String sourceTabId, String targetTabId) {
        if (key == null) return false;
        StoredStackEntry entry = this.entries.get(key);
        if (entry == null || !entry.tabId().equals(DatabaseTabs.normalizeConcreteTarget(sourceTabId))) {
            return false;
        }
        boolean changed = entry.moveToTab(targetTabId, this.nextSequence());
        if (changed) {
            this.markRuntimeStateDirty();
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
     * @param tabDirectory 当前有效的标签页目录
     * @return 若发生了任何回退操作则返回 {@code true}
     */
    public boolean ensureTabAssignments(DatabaseTabDirectory tabDirectory) {
        if (tabDirectory == null) return false;
        String defaultTabId = tabDirectory.defaultConcreteTab().id();
        boolean changed = false;
        for (StoredStackEntry entry : this.entries.values()) {
            if (!tabDirectory.containsConcreteTab(entry.tabId())) {
                entry.moveToTab(defaultTabId, entry.lastModified());
                changed = true;
            }
        }
        if (!this.unresolvedEntries.isEmpty()) {
            java.util.ArrayList<UnresolvedStoredEntry> updatedUnresolvedEntries = new java.util.ArrayList<>(this.unresolvedEntries.size());
            for (UnresolvedStoredEntry unresolvedEntry : this.unresolvedEntries) {
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
                this.unresolvedEntries.clear();
                this.unresolvedEntries.addAll(updatedUnresolvedEntries);
            }
        }
        if (changed) {
            this.markRuntimeStateDirty();
        }
        return changed;
    }

    /**
     * 从仓库中提取指定数量的物品。
     *
     * <p>业务约束：请求数量小于等于 0 时直接返回空堆；
     * 实际提取数量受物品最大堆叠上限限制，避免生成超过客户端合理预期的超大堆叠。
     * 若提取后该物品库存归零，则自动移除条目以节省内存。</p>
     *
     * @param key             物品键
     * @param requestedAmount 请求提取的数量
     * @return 实际提取到的物品堆，若无法提取则返回 {@link ItemStack#EMPTY}
     */
    public ItemStack extract(StoredStackKey key, int requestedAmount) {
        if (requestedAmount <= 0) return ItemStack.EMPTY;
        StoredStackEntry entry = this.entries.get(key);
        if (entry == null) return ItemStack.EMPTY;
        int maxExtractableAmount = Math.max(1, key.maxStackSize());
        int extractedAmount = (int) Math.min(entry.amount(), Math.min((long) requestedAmount, (long) maxExtractableAmount));
        if (extractedAmount <= 0) {
            return ItemStack.EMPTY;
        }
        long sequence = this.nextSequence();
        entry.remove(extractedAmount, sequence);
        ItemStack extractedStack = key.toStack(extractedAmount);
        if (entry.isEmpty()) {
            this.entries.remove(key);
        }
        this.markRuntimeStateDirty();
        return extractedStack;
    }

    /**
     * 将当前数据库序列化为 NBT 复合标签。
     *
     * <p>设计意图：序列化时写入当前 schema 版本号，以便未来反序列化时识别格式并升级。
     * 已解析条目以物品展示堆栈的 NBT 作为键存储，确保即使模组环境变化也能保留尽可能完整的元数据。
     * 备注与收藏以物品堆栈 NBT 作为关联键，保证跨会话一致性。</p>
     *
     * <p>实际逻辑已委托至 {@link StoredItemDatabaseSerializer}。</p>
     *
     * @param provider 用于物品堆栈序列化的注册表查找提供者
     * @return 包含完整数据库状态的 NBT 标签
     */
    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return StoredItemDatabaseSerializer.serialize(this, provider);
    }

    /**
     * 从 NBT 复合标签反序列化数据库状态。
     *
     * <p>设计意图：支持多版本 schema 兼容。若标签中包含 {@code schema_version} 则按当前格式读取；
     * 否则按旧版格式读取，并标记 {@code needsResave} 以便下次存档时自动升级到最新格式。
     * 反序列化过程中会尝试将未解析条目恢复为已解析状态（若当前模组环境已具备对应物品注册）。</p>
     *
     * <p>实际逻辑已委托至 {@link StoredItemDatabaseSerializer}。</p>
     *
     * @param provider 用于物品堆栈反序列化的注册表查找提供者
     * @param tag      包含数据库状态的 NBT 标签，可能为 {@code null} 或空
     */
    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        StoredItemDatabaseSerializer.deserialize(this, provider, tag);
    }

    long nextSequence() {
        if (this.nextSequence == Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return this.nextSequence++;
    }

    private boolean hasStoredContent() {
        return !this.entries.isEmpty() || !this.unresolvedEntries.isEmpty() || this.nextSequence != 1L;
    }

    void resetContent() {
        this.entries.clear();
        this.notes.clear();
        this.starredEntries.clear();
        this.unresolvedEntries.clear();
        this.logEntries.clear();
        this.nextSequence = 1L;
        this.needsResave = false;
    }

    /**
     * 追加一条操作日志。
     *
     * <p>业务约束：空日志或无效日志会被忽略。日志总量超过 {@code MAX_LOG_ENTRIES}（500）时，
     * 自动移除最旧的条目，防止存档无限膨胀。</p>
     *
     * @param entry 要追加的日志条目
     */
    public void appendLogEntry(DatabaseLogEntry entry) {
        if (entry == null || entry.isEmpty()) return;
        this.logEntries.add(entry);
        if (this.logEntries.size() > MAX_LOG_ENTRIES) {
            this.logEntries.removeFirst();
        }
        this.markRuntimeStateDirty();
    }

    void markRuntimeStateDirty() {
        if (this.revision < Long.MAX_VALUE) {
            this.revision++;
        }
    }

    // 包级可见的内部状态访问方法，仅供 StoredItemDatabaseSerializer 使用

    java.util.List<UnresolvedStoredEntry> unresolvedEntriesInternal() {
        return this.unresolvedEntries;
    }

    java.util.List<DatabaseLogEntry> logEntriesInternal() {
        return this.logEntries;
    }

    Map<StoredStackKey, String> notesInternal() {
        return this.notes;
    }

    Set<StoredStackKey> starredEntriesInternal() {
        return this.starredEntries;
    }

    void setNeedsResave(boolean needsResave) {
        this.needsResave = needsResave;
    }

    void setNextSequence(long nextSequence) {
        this.nextSequence = nextSequence;
    }

    void mergeResolvedEntryInternal(StoredStackKey key, StoredStackEntry incomingEntry) {
        if (key == null || incomingEntry == null || incomingEntry.isEmpty()) {
            return;
        }
        StoredStackEntry existingEntry = this.entries.get(key);
        if (existingEntry == null) {
            this.entries.put(key, new StoredStackEntry(
                    incomingEntry.tabId(),
                    incomingEntry.amount(),
                    incomingEntry.lastModified(),
                    incomingEntry.firstAdded()
            ));
            return;
        }
        String mergedTabId = incomingEntry.lastModified() >= existingEntry.lastModified()
                ? incomingEntry.tabId()
                : existingEntry.tabId();
        this.entries.put(key, new StoredStackEntry(
                mergedTabId,
                StoredItemDatabaseHelper.safeAdd(existingEntry.amount(), incomingEntry.amount()),
                Math.max(existingEntry.lastModified(), incomingEntry.lastModified()),
                StoredItemDatabaseHelper.mergeFirstAdded(existingEntry.firstAdded(), incomingEntry.firstAdded())
        ));
    }
}
