package com.agguy.infiniteinventory.database;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 * 单个数据库实例的数据容器，负责管理已解析物品条目、未解析条目、备注、收藏状态及操作日志。
 *
 * <p>设计意图：将"数据"与"服务"严格分离。本类只持有原始数据并提供最小化的原子操作，
 * 不涉及任何玩家交互、网络同步或业务编排。所有公共写操作都会递增内部版本号（{@link #revision}），
 * 供上层服务快速判断数据是否发生变化，从而决定是否需要重新查询或同步客户端。</p>
 *
 * <p>本类同时承担 NBT 序列化与多版本格式兼容职责，通过 {@code schema_version} 字段实现向前兼容的存档升级。</p>
 */
public class StoredItemDatabase implements INBTSerializable<CompoundTag> {
    public static final int CURRENT_SCHEMA_VERSION = 6;
    private static final Logger LOGGER = LogManager.getLogger();
    static final String TAB_ID_KEY = "tab_id";
    static final String FIRST_ADDED_KEY = "first_added";
    private final Map<StoredStackKey, StoredStackEntry> entries = new LinkedHashMap<>();
    private final Map<StoredStackKey, String> notes = new LinkedHashMap<>();
    private final Set<StoredStackKey> starredEntries = new LinkedHashSet<>();
    private final java.util.List<UnresolvedStoredEntry> unresolvedEntries = new java.util.ArrayList<>();
    private final java.util.List<DatabaseLogEntry> logEntries = new java.util.ArrayList<>();
    private final java.util.List<AmountDelta> pendingAmountDeltas = new java.util.ArrayList<>();
    private final AtomicLong nextSequence = new AtomicLong(1L);
    private final AtomicLong revision = new AtomicLong();
    private boolean needsResave;
    private boolean nextSequenceOverflowWarned;
    private long lastValidatedTabDirectoryRevision = -1L;
    private final ThreadLocal<Integer> batchUpdateDepth = ThreadLocal.withInitial(() -> 0);
    private final ThreadLocal<Boolean> batchDirty = ThreadLocal.withInitial(() -> false);
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
     * <p>日志上限为 500 条，超出时自动移除最旧的条目。</p>
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
    public long revision() { return this.revision.get(); }
    /**
     * 判断数据是否需要重新保存。
     *
     * <p>当从旧版 schema 反序列化时，本标志会被置为 {@code true}，提示上层在下次存档时以当前 schema 重写。</p>
     *
     * @return 若需要重新保存则返回 {@code true}
     */
    public synchronized boolean needsResave() { return this.needsResave; }
    /**
     * 清空仓库中的所有数据（包括已解析、未解析、备注、收藏、日志）。
     *
     * <p>业务约束：仅在仓库确实含有内容时才标记为脏状态，避免空清空导致无意义的版本递增。</p>
     */
    public synchronized void clear() {
        if (this.hasStoredContent()) {
            for (Map.Entry<StoredStackKey, StoredStackEntry> entry : this.entries.entrySet()) {
                this.pendingAmountDeltas.add(new AmountDelta(entry.getKey(), entry.getValue().tabId(), 0L, true));
            }
            this.resetContent();
            this.markRuntimeStateDirty();
        }
    }
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
    public synchronized boolean toggleStar(StoredStackKey key) {
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
    public synchronized boolean setStarred(StoredStackKey key, boolean starred) {
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
     * <p>业务约束：备注长度超过 256 时会被截断；
     * 空字符串或仅空白字符会清除已有备注。只有实际内容发生变化时才触发脏标记。</p>
     *
     * @param key  物品键
     * @param note 备注内容，{@code null} 会被视为空字符串
     */
    public synchronized void setNote(StoredStackKey key, String note) {
        if (key == null) return;
        String normalized = note == null ? "" : note.trim();
        if (normalized.length() > 256) normalized = normalized.substring(0, 256);
        if (normalized.isEmpty()) { if (this.notes.remove(key) != null) this.markRuntimeStateDirty(); return; }
        String existing = this.notes.get(key);
        if (existing == null || !existing.equals(normalized)) { this.notes.put(key, normalized); this.markRuntimeStateDirty(); }
    }
    /**
     * 将另一个数据库的数据合并到本数据库。
     *
     * <p>实际逻辑已委托至 {@link StoredItemDatabaseStoreHelper}。</p>
     *
     * @param other 要合并的源数据库，若为 {@code null} 则直接返回
     */
    public synchronized void mergeFrom(StoredItemDatabase other) {
        StoredItemDatabaseStoreHelper.mergeFrom(this, other);
    }
    /**
     * 将物品存入默认标签页。
     *
     * <p>等价于以 {@link DatabaseTabs#DEFAULT_TAB_ID} 为标签页调用 {@link #store(ItemStack, String)}。</p>
     *
     * @param stack 待存入的物品堆，若为 {@link ItemStack#EMPTY} 则忽略
     */
    public synchronized void store(ItemStack stack) {
        StoredItemDatabaseStoreHelper.store(this, stack);
    }
    /**
     * 将物品存入指定标签页。
     *
     * <p>实际逻辑已委托至 {@link StoredItemDatabaseStoreHelper}。</p>
     *
     * @param stack 待存入的物品堆
     * @param tabId 目标标签页 ID
     */
    public synchronized void store(ItemStack stack, String tabId) {
        StoredItemDatabaseStoreHelper.store(this, stack, tabId);
    }
    /**
     * 将整个标签页下的所有物品转移到另一个标签页。
     *
     * <p>实际逻辑已委托至 {@link StoredItemDatabaseTabHelper}。</p>
     *
     * @param sourceTabId 源标签页 ID
     * @param targetTabId 目标标签页 ID
     * @return 若至少有一条目发生迁移则返回 {@code true}
     */
    public synchronized boolean transferTab(String sourceTabId, String targetTabId) {
        return StoredItemDatabaseTabHelper.transferTab(this, sourceTabId, targetTabId);
    }
    /**
     * 将指定物品从源标签页移动到目标标签页。
     *
     * <p>实际逻辑已委托至 {@link StoredItemDatabaseTabHelper}。</p>
     *
     * @param key         物品键
     * @param sourceTabId 源标签页 ID
     * @param targetTabId 目标标签页 ID
     * @return 若移动成功则返回 {@code true}
     */
    public synchronized boolean moveEntryToTab(StoredStackKey key, String sourceTabId, String targetTabId) {
        return StoredItemDatabaseTabHelper.moveEntryToTab(this, key, sourceTabId, targetTabId);
    }
    /**
     * 确保所有条目都分配到了有效的标签页。
     *
     * <p>实际逻辑已委托至 {@link StoredItemDatabaseTabHelper}。</p>
     *
     * @param tabDirectory 当前有效的标签页目录
     * @return 若发生了任何回退操作则返回 {@code true}
     */
    public synchronized boolean ensureTabAssignments(DatabaseTabDirectory tabDirectory) {
        if (tabDirectory == null) {
            return false;
        }
        if (this.lastValidatedTabDirectoryRevision == tabDirectory.revision()) {
            return false;
        }
        boolean changed = StoredItemDatabaseTabHelper.ensureTabAssignments(this, tabDirectory);
        this.lastValidatedTabDirectoryRevision = tabDirectory.revision();
        return changed;
    }
    /**
     * 从仓库中提取指定数量的物品。
     *
     * <p>实际逻辑已委托至 {@link StoredItemDatabaseStoreHelper}。</p>
     *
     * @param key             物品键
     * @param requestedAmount 请求提取的数量
     * @return 实际提取到的物品堆，若无法提取则返回 {@link ItemStack#EMPTY}
     */
    public synchronized ItemStack extract(StoredStackKey key, int requestedAmount) {
        return StoredItemDatabaseStoreHelper.extract(this, key, requestedAmount);
    }
    /**
     * 将当前数据库序列化为 NBT 复合标签。
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
        long current = this.nextSequence.get();
        if (current == Long.MAX_VALUE) {
            if (!this.nextSequenceOverflowWarned) {
                LOGGER.warn("StoredItemDatabase sequence number has reached Long.MAX_VALUE and will remain fixed. Timestamps may collide.");
                this.nextSequenceOverflowWarned = true;
            }
            return Long.MAX_VALUE;
        }
        return this.nextSequence.getAndIncrement();
    }
    private boolean hasStoredContent() {
        return !this.entries.isEmpty() || !this.unresolvedEntries.isEmpty() || this.nextSequence.get() != 1L;
    }
    synchronized void resetContent() {
        this.entries.clear();
        this.notes.clear();
        this.starredEntries.clear();
        this.unresolvedEntries.clear();
        this.logEntries.clear();
        this.nextSequence.set(1L);
        this.needsResave = false;
        this.batchUpdateDepth.set(0);
        this.batchDirty.set(false);
    }
    /**
     * 追加一条操作日志。
     *
     * <p>业务约束：空日志或无效日志会被忽略。日志总量超过 500 时，
     * 自动移除最旧的条目，防止存档无限膨胀。</p>
     *
     * @param entry 要追加的日志条目
     */
    public synchronized void appendLogEntry(DatabaseLogEntry entry) {
        if (entry == null || entry.isEmpty()) return;
        this.logEntries.add(entry);
        if (this.logEntries.size() > 500) {
            this.logEntries.removeFirst();
        }
        this.markRuntimeStateDirty();
    }
    /**
     * 进入批量更新模式。
     *
     * <p>设计意图：在批量操作（如一次性存入多个物品）期间，延迟 revision 递增和索引重建，
     * 直到 {@link #endBatchUpdate()} 被调用。支持嵌套调用，只有最外层结束时才真正触发 dirty。</p>
     *
     * <p>业务约束：必须与 {@link #endBatchUpdate()} 成对使用，建议使用 try-finally 确保配对。</p>
     */
    public synchronized void beginBatchUpdate() {
        this.batchUpdateDepth.set(this.batchUpdateDepth.get() + 1);
    }
    /**
     * 退出批量更新模式。
     *
     * <p>若当前为最外层 batch 且期间发生过数据变更，则触发一次 revision 递增。
     * 嵌套调用时只有 depth 归零时才真正执行。</p>
     */
    public synchronized void endBatchUpdate() {
        int depth = this.batchUpdateDepth.get();
        if (depth <= 0) {
            this.batchUpdateDepth.set(0);
            return;
        }
        depth--;
        this.batchUpdateDepth.set(depth);
        if (depth == 0 && this.batchDirty.get()) {
            this.batchDirty.set(false);
            this.revision.incrementAndGet();
        }
    }
    synchronized void markRuntimeStateDirty() {
        if (this.batchUpdateDepth.get() > 0) {
            this.batchDirty.set(true);
            return;
        }
        this.revision.incrementAndGet();
    }
    // 包级可见的内部状态访问方法，仅供同包辅助类使用
    Map<StoredStackKey, StoredStackEntry> entriesInternal() {
        return this.entries;
    }
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
    long nextSequenceInternal() {
        return this.nextSequence.get();
    }
    synchronized void setNeedsResave(boolean needsResave) {
        this.needsResave = needsResave;
    }
    synchronized void setNextSequence(long nextSequence) {
        this.nextSequence.set(nextSequence);
    }
    synchronized void mergeResolvedEntryInternal(StoredStackKey key, StoredStackEntry incomingEntry) {
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
    public record AmountDelta(StoredStackKey key, String tabId, long amount, boolean removed) {}
    synchronized void trackAmountDelta(StoredStackKey key, String tabId, long amount, boolean removed) {
        if (key == null) return;
        this.pendingAmountDeltas.add(new AmountDelta(key, tabId, amount, removed));
    }
    public synchronized List<AmountDelta> drainPendingAmountDeltas() {
        if (this.pendingAmountDeltas.isEmpty()) return List.of();
        Map<StoredStackKey, AmountDelta> merged = new LinkedHashMap<>();
        for (AmountDelta delta : this.pendingAmountDeltas) {
            merged.put(delta.key(), delta);
        }
        this.pendingAmountDeltas.clear();
        return List.copyOf(merged.values());
    }
}
