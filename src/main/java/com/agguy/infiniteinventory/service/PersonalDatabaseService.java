package com.agguy.infiniteinventory.service;
import com.agguy.infiniteinventory.database.DatabaseLogAction;
import com.agguy.infiniteinventory.database.DatabaseLogEntry;
import com.agguy.infiniteinventory.database.DatabasePage;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseAutoStoreTarget;
import com.agguy.infiniteinventory.database.DatabaseEnhancementConfig;
import com.agguy.infiniteinventory.database.DatabaseEnhancementOption;
import com.agguy.infiniteinventory.database.DatabaseBackupManager;
import com.agguy.infiniteinventory.database.DatabaseStorageSavedData;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabDirectory;
import com.agguy.infiniteinventory.database.DatabaseSelectionEntry;
import com.agguy.infiniteinventory.database.DatabaseViewPreferencesAttachment;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.database.LegacyMigrationState;
import com.agguy.infiniteinventory.database.PlayerDatabaseAttachment;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackEntry;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.localization.ViewerLanguage;
import com.agguy.infiniteinventory.menu.PersonalDatabaseMenu;
import com.agguy.infiniteinventory.menu.PersonalDatabaseOpenState;
import com.agguy.infiniteinventory.network.DatabaseSelectionAction;
import com.agguy.infiniteinventory.registry.ModAttachments;
import com.agguy.infiniteinventory.registry.ModItems;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.stats.Stats;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 * 无限仓库的核心服务门面，作为玩家与底层数据存储之间的唯一交互入口。
 * <p>
 * 设计上采用单例模式（{@link #INSTANCE}），所有对无限仓库的存入/取出、分页查询、标签页管理、
 * 收藏/备注、自动存储、日志记录及多玩家数据同步等操作，均应通过此类完成，以保证数据一致性与事务边界。
 * <p>
 * 职责划分：
 * <ul>
 *   <li>物品流转：存入（deposit/store）、取出（extract）到背包或世界</li>
 *   <li>查询与视图：分页构建（{@link #buildPage}）、查询条件清洗（{@link #sanitizeQuery}）</li>
 *   <li>标签页生命周期：创建、重命名、移动、删除、转移</li>
 *   <li>增强功能：自动拾取存储、星标/备注、操作日志</li>
 *   <li>同步与兼容：JEI 数量同步、多玩家视图刷新、旧数据迁移</li>
 * </ul>
 */
public final class PersonalDatabaseService {
    public static final PersonalDatabaseService INSTANCE = new PersonalDatabaseService();
    static final int HOTBAR_SLOT_COUNT = 9;
    private static final Logger LOGGER = LogManager.getLogger();
    private final DatabaseQueryEngine queryEngine = DatabaseQueryEngine.INSTANCE;
    private PersonalDatabaseService() {
    }

    /**
     * 为指定玩家打开无限仓库界面。
     * <p>
     * 打开前会自动触发旧版数据迁移检查，确保玩家数据已升级至当前存储格式；
     * 打开后向客户端同步当前视图，并提示是否存在未解析的日志条目。
     *
     * @param player 要打开界面的服务端玩家，不可为 null
     */
    public void open(ServerPlayer player) {
        PersonalDatabaseServiceMigrationHelper.ensureLegacyPersonalMigration(this, player);
        player.openMenu(new PersonalDatabaseMenuProvider(player, this.getViewPreferences(player)));
        if (player.containerMenu instanceof PersonalDatabaseMenu menu) {
            menu.syncViewToClient();
            PersonalDatabaseServiceViewerHelper.notifyViewerAboutUnresolvedEntries(player, menu.activeScope());
        }
    }

    /**
     * 获取玩家附加数据中的旧版个人仓库实例（用于兼容与迁移）。
     *
     * @param player 目标玩家
     * @return 旧版个人仓库附加数据
     */
    public PlayerDatabaseAttachment getLegacyPersonalDatabase(Player player) {
        return player.getData(ModAttachments.PERSONAL_DATABASE.get());
    }

    /**
     * 获取玩家的视图偏好设置（查询条件、可见标签页、自动存储目标等）。
     * <p>
     * 偏好数据存储在玩家附加数据中，随玩家持久化，用于恢复上次打开界面时的状态。
     *
     * @param player 目标玩家
     * @return 玩家的视图偏好设置
     */
    public DatabaseViewPreferencesAttachment getViewPreferences(Player player) {
        return player.getData(ModAttachments.DATABASE_VIEW_PREFERENCES.get());
    }

    /**
     * 获取玩家当前已启用的增强功能配置（如自动拾取存储等）。
     *
     * @param player 目标玩家
     * @return 增强功能配置快照
     */
    public DatabaseEnhancementConfig getEnhancementConfig(Player player) {
        return this.getViewPreferences(player).enhancementConfig();
    }

    /**
     * 判断给定物品堆是否可以存入无限仓库。
     * <p>
     * 业务约束：空物品与数据库访问物品（避免循环存放）禁止存入。
     *
     * @param stack 待检查的物品堆
     * @return 若允许存入则返回 true，否则返回 false
     */
    public boolean canStore(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() != ModItems.DATABASE_ACCESS_ITEM.get();
    }

    /**
     * 将界面中指定槽位的物品存入无限仓库。
     * <p>
     * 存入前会校验物品是否允许存储，存入后清空原槽位并记录操作日志。
     *
     * @param player       执行操作的玩家
     * @param scope        目标作用域（个人/公共）
     * @param targetTabId  目标标签页标识；若传入空或无效值，将自动解析为默认标签页
     * @param slot         源槽位，不可为 null
     * @return 若成功存入返回 true；若物品不可存储则返回 false
     */
    public boolean depositSlot(ServerPlayer player, DatabaseScope scope, String targetTabId, Slot slot) {
        ItemStack stack = slot.getItem();
        if (!this.canStore(stack)) {
            return false;
        }
        String resolvedTargetTabId = this.resolveConcreteTargetTabId(player, scope, targetTabId);
        this.resolveDatabaseForMutation(player, scope).store(stack.copy(), resolvedTargetTabId);
        this.markScopeDirty(player, scope);
        slot.setByPlayer(ItemStack.EMPTY, stack.copy());
        slot.setChanged();
        PersonalDatabaseServiceLogHelper.recordLog(this, player, scope, DatabaseLogAction.DEPOSIT, stack, stack.getCount(), "", resolvedTargetTabId, null);
        return true;
    }

    /**
     * 将玩家主物品栏（不含装备与副手）中所有可存储物品批量存入无限仓库。
     * <p>
     * 仅遍历主存储槽位（跳过装备槽），自动过滤不可存储物品；
     * 操作完成后若发生实际移动，会标记数据脏并触发背包变更事件。
     *
     * @param player       执行操作的玩家
     * @param scope        目标作用域
     * @param targetTabId  目标标签页标识；无效值将解析为默认标签页
     * @return 实际移动的物品总数量（按各堆叠计数累加）
     */
    public long depositMainInventory(ServerPlayer player, DatabaseScope scope, String targetTabId) {
        Inventory inventory = player.getInventory();
        StoredItemDatabase database = this.resolveDatabaseForMutation(player, scope);
        String resolvedTargetTabId = this.resolveConcreteTargetTabId(player, scope, targetTabId);
        long movedItems = 0L;
        boolean movedAny = false;
        for (int slotIndex = 0; slotIndex < inventory.items.size(); slotIndex++) {
            if (!PersonalDatabaseServiceHelper.isPrimaryStorageSlot(slotIndex)) {
                continue;
            }
            ItemStack stack = inventory.items.get(slotIndex);
            if (!this.canStore(stack)) {
                continue;
            }
            movedItems = PersonalDatabaseServiceStorageHelper.safeAddMovedItems(movedItems, stack);
            movedAny = true;
            database.store(stack.copy(), resolvedTargetTabId);
            PersonalDatabaseServiceLogHelper.recordLog(this, player, scope, DatabaseLogAction.DEPOSIT, stack, stack.getCount(), "", resolvedTargetTabId, null);
            inventory.items.set(slotIndex, ItemStack.EMPTY);
        }
        if (movedAny) {
            this.markScopeDirty(player, scope);
            inventory.setChanged();
        }
        return movedItems;
    }

    /**
     * 将指定物品堆直接存入无限仓库。
     * <p>
     * 与 {@link #depositSlot} 不同，此方法不操作槽位，而是直接接收一个物品堆副本；
     * 适用于命令、自动化管道等非界面触发的存储场景。
     *
     * @param player       执行操作的玩家
     * @param scope        目标作用域
     * @param targetTabId  目标标签页标识；无效值将解析为默认标签页
     * @param stack        待存入的物品堆，不可为 null
     * @return 若成功存入返回 true；若物品不可存储则返回 false
     */
    public boolean storeStack(ServerPlayer player, DatabaseScope scope, String targetTabId, ItemStack stack) {
        if (!this.canStore(stack)) {
            return false;
        }
        String resolvedTargetTabId = this.resolveConcreteTargetTabId(player, scope, targetTabId);
        this.resolveDatabaseForMutation(player, scope).store(stack, resolvedTargetTabId);
        this.markScopeDirty(player, scope);
        PersonalDatabaseServiceLogHelper.recordLog(this, player, scope, DatabaseLogAction.DEPOSIT, stack, stack.getCount(), "", resolvedTargetTabId, null);
        return true;
    }

    /**
     * 尝试将玩家拾取到的掉落物自动存入无限仓库。
     * <p>
     * 触发条件（需同时满足）：
     * <ul>
     *   <li>玩家启用了“自动存储拾取物品”增强功能</li>
     *   <li>掉落物无拾取延迟，且目标为当前玩家或无人锁定</li>
     *   <li>物品允许存入仓库</li>
     * </ul>
     * 存入后会模拟正常拾取流程（统计、事件、实体移除），并同步相关视图。
     *
     * @param player     拾取玩家
     * @param itemEntity 被拾取的掉落物实体
     * @return 若自动存储成功返回 true；任一条件不满足则返回 false
     */
    public boolean tryAutoStorePickedUpItem(ServerPlayer player, ItemEntity itemEntity) {
        if (player == null || itemEntity == null) return false;
        if (!this.getEnhancementConfig(player).isEnabled(DatabaseEnhancementOption.AUTO_STORE_PICKED_UP_ITEMS)) {
            return false;
        }
        if (itemEntity.hasPickUpDelay()) {
            return false;
        }
        if (itemEntity.getTarget() != null && !itemEntity.getTarget().equals(player.getUUID())) {
            return false;
        }
        ItemStack stack = itemEntity.getItem();
        if (!this.canStore(stack)) {
            return false;
        }
        int pickedUpAmount = stack.getCount();
        if (pickedUpAmount <= 0) {
            return false;
        }
        DatabaseAutoStoreTarget autoStoreTarget = this.resolveAutoStoreTarget(player);
        this.resolveDatabaseForMutation(player, autoStoreTarget.scope()).store(stack.copy(), autoStoreTarget.tabId());
        this.markScopeDirty(player, autoStoreTarget.scope());
        player.take(itemEntity, pickedUpAmount);
        player.awardStat(Stats.ITEM_PICKED_UP.get(stack.getItem()), pickedUpAmount);
        player.onItemPickup(itemEntity);
        itemEntity.discard();
        PersonalDatabaseServiceLogHelper.recordLog(this, player, autoStoreTarget.scope(), DatabaseLogAction.DEPOSIT, stack, pickedUpAmount, "", autoStoreTarget.tabId(), null);
        if (autoStoreTarget.scope() == DatabaseScope.PUBLIC) {
            this.syncPublicViewers(player.server);
        } else if (player.containerMenu instanceof PersonalDatabaseMenu menu && menu.activeScope() == DatabaseScope.PERSONAL) {
            menu.syncViewToClient();
        }
        return true;
    }

    /**
     * 从无限仓库中提取指定物品到玩家的光标（carried）槽位。
     * <p>
     * 常用于界面中“拿起”物品的操作；提取数量受物品最大堆叠上限限制。
     *
     * @param player          执行操作的玩家
     * @param scope           源作用域
     * @param key             要提取的物品唯一键
     * @param requestedAmount 请求提取的数量
     * @return 实际提取到的物品堆；若仓库中无足够数量或键不存在则返回空堆
     */
    public ItemStack extractToCarried(ServerPlayer player, DatabaseScope scope, StoredStackKey key, int requestedAmount) {
        String sourceTabId = PersonalDatabaseServiceHelper.entryTabId(this.resolveDatabaseForMutation(player, scope), key);
        ItemStack extracted = this.resolveDatabaseForMutation(player, scope).extract(key, requestedAmount);
        if (!extracted.isEmpty()) {
            this.markScopeDirty(player, scope);
            PersonalDatabaseServiceLogHelper.recordLog(this, player, scope, DatabaseLogAction.EXTRACT, extracted, extracted.getCount(), sourceTabId, "", null);
        }
        return extracted;
    }

    /**
     * 将指定物品从仓库全部提取到玩家背包（尽可能多）。
     * <p>
     * 等价于以 {@link Long#MAX_VALUE} 为请求量调用 {@link #extractToInventory}。
     *
     * @param player 执行操作的玩家
     * @param scope  源作用域
     * @param key    要提取的物品唯一键
     * @return 实际成功移入背包的物品总数量
     */
    public long extractAllToInventory(ServerPlayer player, DatabaseScope scope, StoredStackKey key) {
        return this.extractToInventory(player, scope, key, Long.MAX_VALUE);
    }

    /**
     * 从无限仓库提取指定数量的物品到玩家背包。
     * <p>
     * 提取逻辑由 {@link PersonalDatabaseExtractionHelper} 处理，会优先填充已有堆叠再寻找空槽；
     * 实际移动数量可能小于请求量，受背包剩余空间限制。
     *
     * @param player          执行操作的玩家
     * @param scope           源作用域
     * @param key             要提取的物品唯一键
     * @param requestedAmount 请求提取的数量
     * @return 实际成功移入背包的物品总数量
     */
    public long extractToInventory(ServerPlayer player, DatabaseScope scope, StoredStackKey key, long requestedAmount) {
        String sourceTabId = PersonalDatabaseServiceHelper.entryTabId(this.resolveDatabaseForMutation(player, scope), key);
        long moved = PersonalDatabaseExtractionHelper.extractToInventory(this, player, scope, this.resolveDatabaseForMutation(player, scope), key, requestedAmount);
        if (moved > 0L) {
            PersonalDatabaseServiceLogHelper.recordLog(this, player, scope, DatabaseLogAction.EXTRACT, key.displayStack(), moved, sourceTabId, "", null);
        }
        return moved;
    }

    /**
     * 从无限仓库提取物品并直接生成到世界中（如丢出）。
     * <p>
     * 适用于“取出到世界”的场景；若玩家附近空间不足，多余物品可能散落到邻近位置。
     *
     * @param player          执行操作的玩家
     * @param scope           源作用域
     * @param key             要提取的物品唯一键
     * @param requestedAmount 请求提取的数量
     * @return 实际成功生成到世界的物品总数量
     */
    public long extractToWorld(ServerPlayer player, DatabaseScope scope, StoredStackKey key, long requestedAmount) {
        String sourceTabId = PersonalDatabaseServiceHelper.entryTabId(this.resolveDatabaseForMutation(player, scope), key);
        long moved = PersonalDatabaseExtractionHelper.extractToWorld(this, player, scope, this.resolveDatabaseForMutation(player, scope), key, requestedAmount);
        if (moved > 0L) {
            PersonalDatabaseServiceLogHelper.recordLog(this, player, scope, DatabaseLogAction.EXTRACT, key.displayStack(), moved, sourceTabId, "", null);
        }
        return moved;
    }

    /**
     * 将多选条目批量提取到玩家背包（默认不限制单条目数量）。
     * <p>
     * 等价于以 {@code 0L} 为请求量调用重载方法，表示按动作自身规则决定数量。
     *
     * @param player           执行操作的玩家
     * @param scope            源作用域
     * @param selectionEntries 玩家选中的条目列表
     * @param action           提取动作类型，决定数量计算方式
     * @return 实际成功移入背包的物品总数量
     */
    public long extractSelectionToInventory(ServerPlayer player, DatabaseScope scope, List<DatabaseSelectionEntry> selectionEntries, DatabaseSelectionAction action) {
        return this.extractSelectionToInventory(player, scope, selectionEntries, action, 0L);
    }

    /**
     * 将多选条目批量提取到玩家背包，支持自定义单条目请求上限。
     * <p>
     * 仅当 {@code action} 非空且支持提取到背包时才会执行；
     * 每个条目会校验其当前所在标签页与选中时记录的标签页是否一致，防止并发操作导致数据错位。
     *
     * @param player           执行操作的玩家
     * @param scope            源作用域
     * @param selectionEntries 玩家选中的条目列表
     * @param action           提取动作类型
     * @param requestedAmount  单条目请求数量；{@code 0L} 表示由动作自行解析
     * @return 实际成功移入背包的物品总数量
     */
    public long extractSelectionToInventory(
            ServerPlayer player, DatabaseScope scope, List<DatabaseSelectionEntry> selectionEntries, DatabaseSelectionAction action, long requestedAmount
    ) {
        if (action == null || !action.extractsToInventory()) {
            return 0L;
        }
        long totalMoved = 0L;
        StoredItemDatabase database = this.resolveDatabaseForMutation(player, scope);
        for (DatabaseSelectionEntry selectionEntry : PersonalDatabaseExtractionHelper.normalizeSelectionEntries(selectionEntries)) {
            StoredStackKey key = PersonalDatabaseExtractionHelper.selectionKey(selectionEntry);
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
            long moved = PersonalDatabaseExtractionHelper.extractToInventory(this, player, scope, database, key, resolvedRequestedAmount);
            if (moved > 0L) {
                totalMoved = PersonalDatabaseServiceHelper.safeAddMovedItems(totalMoved, moved);
                PersonalDatabaseServiceLogHelper.recordLog(this, player, scope, DatabaseLogAction.EXTRACT, key.displayStack(), moved, selectionEntry.sourceTabId(), "", null);
            }
        }
        return totalMoved;
    }

    /**
     * 根据查询条件构建分页结果，用于界面渲染。
     * <p>
     * 查询引擎会综合当前数据库、标签页目录、搜索环境（如 JEI 书签过滤）生成一页数据；
     * 若传入 {@code null} 查询，将使用默认查询条件。
     *
     * @param player          请求分页的玩家
     * @param query           查询条件；可为 null
     * @param scopedTab       当前聚焦的标签页引用
     * @param viewerLanguage  玩家当前界面语言，用于本地化排序与显示
     * @return 包含当前页条目、总页数、标签页状态等的分页数据
     */
    public DatabasePage buildPage(ServerPlayer player, DatabaseQuery query, DatabaseScopedTabRef scopedTab, ViewerLanguage viewerLanguage) {
        DatabaseQuery normalizedQuery = query == null ? DatabaseQuery.defaultQuery() : query;
        return this.queryEngine.buildPage(
                this.resolveDatabaseForView(player, scopedTab.scope()),
                this.resolveTabsForView(player, scopedTab.scope()),
                normalizedQuery,
                scopedTab,
                viewerLanguage,
                PersonalDatabaseSearchEnvironmentResolver.resolve(player)
        );
    }

    /**
     * 清洗并规范化玩家提交的查询条件，防止非法标签页或越界可见标签页数量。
     * <p>
     * 处理逻辑包括：
     * <ul>
     *   <li>将无效聚焦标签页回退到默认标签页</li>
     *   <li>截断可见标签页数量至上限 {@link DatabaseTabs#MAX_VISIBLE_TAB_COUNT}</li>
     *   <li>确保聚焦标签页始终位于可见集合中</li>
     *   <li>重建所有标签页的查询状态映射</li>
     * </ul>
     *
     * @param player 提交查询的玩家
     * @param query  原始查询条件；可为 null
     * @return 经校验后的安全查询条件
     */
    public DatabaseQuery sanitizeQuery(ServerPlayer player, DatabaseQuery query) {
        DatabaseQuery normalizedQuery = query == null ? DatabaseQuery.defaultQuery() : query;
        DatabaseScopedTabRef focusedTab = this.resolveScopedTabForView(player, normalizedQuery.focusedTab());
        java.util.LinkedHashSet<DatabaseScopedTabRef> visibleTabs = new java.util.LinkedHashSet<>();
        for (DatabaseScopedTabRef visibleTab : normalizedQuery.visibleTabs()) {
            DatabaseScopedTabRef resolvedVisibleTab = this.resolveScopedTabForView(player, visibleTab);
            if (resolvedVisibleTab == null) {
                continue;
            }
            visibleTabs.add(resolvedVisibleTab);
            if (visibleTabs.size() >= DatabaseTabs.MAX_VISIBLE_TAB_COUNT) {
                break;
            }
        }
        if (focusedTab == null) {
            focusedTab = DatabaseScopedTabRef.defaultTab();
        }
        if (visibleTabs.isEmpty()) {
            visibleTabs.add(focusedTab);
        }
        if (!visibleTabs.contains(focusedTab)) {
            focusedTab = visibleTabs.getFirst();
        }
        java.util.LinkedHashMap<DatabaseScopedTabRef, com.agguy.infiniteinventory.database.DatabaseTabQueryState> tabStates =
                new java.util.LinkedHashMap<>();
        for (DatabaseTab tab : this.tabsForScope(player, DatabaseScope.PERSONAL)) {
            DatabaseScopedTabRef scopedTab = DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, tab.id());
            tabStates.put(scopedTab, normalizedQuery.tabStateFor(scopedTab));
        }
        for (DatabaseTab tab : this.tabsForScope(player, DatabaseScope.PUBLIC)) {
            DatabaseScopedTabRef scopedTab = DatabaseScopedTabRef.concreteTab(DatabaseScope.PUBLIC, tab.id());
            tabStates.put(scopedTab, normalizedQuery.tabStateFor(scopedTab));
        }
        return new DatabaseQuery(focusedTab, java.util.List.copyOf(visibleTabs), tabStates, normalizedQuery.hiddenTopTabs());
    }

    /**
     * 切换指定顶部标签页的可见性（显示/隐藏）。
     * <p>
     * 隐藏的标签页不会出现在顶部栏，但仍可通过“全部”或其他方式访问其中物品；
     * 变更直接写入玩家视图偏好并持久化。
     *
     * @param player 执行操作的玩家
     * @param scope  标签页所属作用域
     * @param tabId  目标标签页标识
     * @return 若可见性状态发生实际变化返回 true；无变化返回 false
     */
    public boolean toggleTopTabVisibility(ServerPlayer player, DatabaseScope scope, String tabId) {
        DatabaseViewPreferencesAttachment preferences = this.getViewPreferences(player);
        DatabaseQuery currentQuery = preferences.query();
        DatabaseScopedTabRef scopedTab = DatabaseScopedTabRef.concreteTab(DatabaseScope.normalize(scope), tabId);
        DatabaseQuery updatedQuery = currentQuery.withHiddenTopTabToggled(scopedTab);
        preferences.setQuery(updatedQuery);
        return !updatedQuery.hiddenTopTabs().equals(currentQuery.hiddenTopTabs());
    }

    /**
     * 获取指定作用域下所有已排序的标签页列表（含系统标签页与自定义标签页）。
     *
     * @param player 请求玩家
     * @param scope  目标作用域
     * @return 按当前排序规则排列的标签页列表
     */
    public List<DatabaseTab> tabsForScope(ServerPlayer player, DatabaseScope scope) {
        return this.resolveTabsForView(player, scope).orderedTabs();
    }

    /**
     * 将请求的目标标签页标识解析为实际存在的具体标签页标识。
     * <p>
     * 若传入空值或指向已删除标签页，将回退到默认标签页，确保存储操作总有合法落点。
     *
     * @param player         当前玩家
     * @param scope          目标作用域
     * @param requestedTabId 请求的标签页标识；可为空
     * @return 经校验后的有效具体标签页标识
     */
    public String resolveConcreteTargetTabId(ServerPlayer player, DatabaseScope scope, String requestedTabId) {
        return this.resolveTabsForMutation(player, scope).sanitizeConcreteTarget(requestedTabId);
    }

    /**
     * 解析并持久化玩家当前的自动存储目标。
     * <p>
     * 若玩家之前设置的目标标签页已失效（如被删除），会自动回退到默认标签页，
     * 并将修正后的目标写回偏好设置，避免后续自动存储失败。
     *
     * @param player 当前玩家
     * @return 经校验后的自动存储目标（含作用域与标签页标识）
     */
    public DatabaseAutoStoreTarget resolveAutoStoreTarget(ServerPlayer player) {
        DatabaseViewPreferencesAttachment preferences = this.getViewPreferences(player);
        DatabaseAutoStoreTarget preferredTarget = preferences.autoStoreTarget();
        String targetTabId = this.resolveTabsForMutation(player, preferredTarget.scope()).sanitizeConcreteTarget(preferredTarget.tabId());
        DatabaseAutoStoreTarget resolvedTarget = new DatabaseAutoStoreTarget(preferredTarget.scope(), targetTabId);
        preferences.setAutoStoreTarget(resolvedTarget);
        return resolvedTarget;
    }

    /**
     * 在指定作用域下新建自定义标签页。
     *
     * @param player      执行操作的玩家
     * @param scope       目标作用域
     * @param name        标签页显示名称
     * @param iconItemId  标签页图标对应的物品注册名
     * @return 固定返回 true（创建操作目前不会失败）
     */
    public boolean createTab(ServerPlayer player, DatabaseScope scope, String name, String iconItemId) {
        DatabaseTabDirectory tabDirectory = this.resolveTabsForMutation(player, scope);
        tabDirectory.addCustomTab(name, iconItemId);
        this.markScopeDirty(player, scope);
        return true;
    }

    /**
     * 重命名指定标签页。
     *
     * @param player 执行操作的玩家
     * @param scope  标签页所属作用域
     * @param tabId  目标标签页标识
     * @param name   新名称
     * @return 若名称发生实际变化返回 true；否则返回 false
     */
    public boolean renameTab(ServerPlayer player, DatabaseScope scope, String tabId, String name) {
        boolean changed = this.resolveTabsForMutation(player, scope).renameTab(tabId, name);
        if (changed) {
            this.markScopeDirty(player, scope);
        }
        return changed;
    }

    /**
     * 更新指定标签页的图标。
     *
     * @param player      执行操作的玩家
     * @param scope       标签页所属作用域
     * @param tabId       目标标签页标识
     * @param iconItemId  新图标对应的物品注册名
     * @return 若图标发生实际变化返回 true；否则返回 false
     */
    public boolean updateTabIcon(ServerPlayer player, DatabaseScope scope, String tabId, String iconItemId) {
        boolean changed = this.resolveTabsForMutation(player, scope).updateTabIcon(tabId, iconItemId);
        if (changed) {
            this.markScopeDirty(player, scope);
        }
        return changed;
    }

    /**
     * 在标签页排序中移动指定标签页的位置。
     *
     * @param player    执行操作的玩家
     * @param scope     标签页所属作用域
     * @param tabId     目标标签页标识
     * @param direction 移动方向与步长；正数向右，负数向左
     * @return 若位置发生实际变化返回 true；否则返回 false
     */
    public boolean moveTab(ServerPlayer player, DatabaseScope scope, String tabId, int direction) {
        boolean changed = this.resolveTabsForMutation(player, scope).moveTab(tabId, direction);
        if (changed) {
            this.markScopeDirty(player, scope);
        }
        return changed;
    }

    /**
     * 删除指定标签页，并将其中的物品转移到另一个目标标签页。
     * <p>
     * 业务约束：
     * <ul>
     *   <li>系统标签页不可删除（由 {@link DatabaseTab#canDelete()} 控制）</li>
     *   <li>若目标标签页无效，将自动解析为默认标签页</li>
     *   <li>删除后会自动修正玩家的自动存储目标（若指向被删标签页）</li>
     * </ul>
     * 所有被转移的物品会逐条记录 DELETE 日志。
     *
     * @param player       执行操作的玩家
     * @param scope        标签页所属作用域
     * @param tabId        待删除的标签页标识
     * @param targetTabId  接收物品的目标标签页标识
     * @return 若数据库或目录发生实际变化返回 true；不可删除或无任何变化返回 false
     */
    public boolean deleteTab(ServerPlayer player, DatabaseScope scope, String tabId, String targetTabId) {
        DatabaseTabDirectory tabDirectory = this.resolveTabsForMutation(player, scope);
        String resolvedTargetTabId = tabDirectory.sanitizeConcreteTarget(targetTabId);
        if (!tabDirectory.find(tabId).map(DatabaseTab::canDelete).orElse(false)) {
            return false;
        }
        StoredItemDatabase database = this.resolveDatabaseForMutation(player, scope);
        String normalizedTabId = DatabaseTabs.normalizeConcreteTarget(tabId);
        java.util.Map<StoredStackKey, Long> entriesToDelete = new java.util.LinkedHashMap<>();
        for (java.util.Map.Entry<StoredStackKey, StoredStackEntry> entry : database.entries().entrySet()) {
            if (entry.getValue().tabId().equals(normalizedTabId)) {
                entriesToDelete.put(entry.getKey(), entry.getValue().amount());
            }
        }
        boolean databaseChanged = database.transferTab(tabId, resolvedTargetTabId);
        boolean directoryChanged = tabDirectory.deleteTab(tabId);
        DatabaseScope normalizedScope = DatabaseScope.normalize(scope);
        DatabaseViewPreferencesAttachment preferences = this.getViewPreferences(player);
        DatabaseAutoStoreTarget autoStoreTarget = preferences.autoStoreTarget();
        if (autoStoreTarget.scope() == normalizedScope && autoStoreTarget.tabId().equals(tabId)) {
            preferences.setAutoStoreTarget(new DatabaseAutoStoreTarget(normalizedScope, resolvedTargetTabId));
        }
        if (databaseChanged || directoryChanged) {
            this.markScopeDirty(player, scope);
            for (java.util.Map.Entry<StoredStackKey, Long> entry : entriesToDelete.entrySet()) {
                PersonalDatabaseServiceLogHelper.recordLog(this, player, scope, DatabaseLogAction.DELETE, entry.getKey().displayStack(), entry.getValue(), normalizedTabId, resolvedTargetTabId, null);
            }
        }
        return databaseChanged || directoryChanged;
    }

    /**
     * 将整个标签页（含其中所有物品）从一个作用域转移到另一个作用域。
     * <p>
     * 例如将个人标签页转移到公共仓库，或反之；具体转移逻辑由 {@link PersonalDatabaseTransferHelper} 实现。
     *
     * @param player      执行操作的玩家
     * @param sourceScope 源作用域
     * @param targetScope 目标作用域
     * @param sourceTabId 源标签页标识
     * @param targetTabId 目标标签页标识（若目标已存在则合并物品）
     * @return 若转移成功返回 true；否则返回 false
     */
    public boolean transferTab(
            ServerPlayer player,
            DatabaseScope sourceScope,
            DatabaseScope targetScope,
            String sourceTabId,
            String targetTabId
    ) {
        return PersonalDatabaseTransferHelper.transferTab(this, player, sourceScope, targetScope, sourceTabId, targetTabId);
    }

    /**
     * 将玩家选中的多个条目批量转移到另一个作用域的指定标签页。
     * <p>
     * 适用于跨作用域批量搬运（如个人到公共）；仅转移选中条目，非整个标签页。
     *
     * @param player           执行操作的玩家
     * @param sourceScope      源作用域
     * @param targetScope      目标作用域
     * @param selectionEntries 玩家选中的条目列表
     * @param targetTabId      目标标签页标识
     * @return 若转移成功返回 true；否则返回 false
     */
    public boolean transferSelection(
            ServerPlayer player,
            DatabaseScope sourceScope,
            DatabaseScope targetScope,
            List<DatabaseSelectionEntry> selectionEntries,
            String targetTabId
    ) {
        return PersonalDatabaseTransferHelper.transferSelection(this, player, sourceScope, targetScope, selectionEntries, targetTabId);
    }

    /**
     * 为指定物品设置备注文本。
     * <p>
     * 备注按物品键维度存储，同一物品在不同标签页共享同一条备注；
     * 空备注等效于清除。
     *
     * @param player 执行操作的玩家
     * @param scope  物品所在作用域
     * @param key    物品唯一键；若为 null 则直接返回，不做任何操作
     * @param note   备注内容；可为空字符串
     */
    public void setNote(ServerPlayer player, DatabaseScope scope, StoredStackKey key, String note) {
        if (key == null) return;
        this.resolveDatabaseForMutation(player, scope).setNote(key, note);
        this.markScopeDirty(player, scope);
    }

    /**
     * 查询指定物品的当前备注文本。
     *
     * @param player 请求玩家
     * @param scope  物品所在作用域
     * @param key    物品唯一键；若为 null 返回空字符串
     * @return 当前备注内容；无备注时返回空字符串
     */
    public String noteFor(ServerPlayer player, DatabaseScope scope, StoredStackKey key) {
        return key == null ? "" : this.resolveDatabaseForView(player, scope).noteFor(key);
    }

    /**
     * 切换指定物品的星标状态。
     * <p>
     * 星标用于玩家快速标记重要物品，便于后续筛选与检索。
     *
     * @param player 执行操作的玩家
     * @param scope  物品所在作用域
     * @param key    物品唯一键；若为 null 返回 false
     * @return 若星标状态发生实际变化返回 true；否则返回 false
     */
    public boolean toggleStar(ServerPlayer player, DatabaseScope scope, StoredStackKey key) {
        if (key == null) return false;
        boolean changed = this.resolveDatabaseForMutation(player, scope).toggleStar(key);
        if (changed) this.markScopeDirty(player, scope);
        return changed;
    }

    /**
     * 显式设置指定物品的星标状态（而非切换）。
     *
     * @param player  执行操作的玩家
     * @param scope   物品所在作用域
     * @param key     物品唯一键；若为 null 返回 false
     * @param starred 目标星标状态：true 为星标，false 为取消星标
     * @return 若星标状态发生实际变化返回 true；否则返回 false
     */
    public boolean setStarred(ServerPlayer player, DatabaseScope scope, StoredStackKey key, boolean starred) {
        if (key == null) return false;
        boolean changed = this.resolveDatabaseForMutation(player, scope).setStarred(key, starred);
        if (changed) this.markScopeDirty(player, scope);
        return changed;
    }

    /**
     * 判断指定物品是否已被星标。
     *
     * @param player 请求玩家
     * @param scope  物品所在作用域
     * @param key    物品唯一键；若为 null 返回 false
     * @return 已星标返回 true；否则返回 false
     */
    public boolean isStarred(ServerPlayer player, DatabaseScope scope, StoredStackKey key) {
        return key != null && this.resolveDatabaseForView(player, scope).isStarred(key);
    }

    /**
     * 获取指定作用域下的操作日志条目列表。
     * <p>
     * 日志记录存入/取出/删除等关键操作，用于界面展示与审计追溯。
     *
     * @param player 请求玩家
     * @param scope  目标作用域
     * @return 按时间倒序排列的日志条目列表
     */
    public List<DatabaseLogEntry> getLogEntries(ServerPlayer player, DatabaseScope scope) {
        return this.resolveDatabaseForMutation(player, scope).logEntries();
    }

    /**
     * 将玩家在个人与公共仓库中的物品数量同步到 JEI，使其配方界面显示可用数量。
     * <p>
     * 每次仓库发生变更后由 {@link #markScopeDirty} 自动调用，确保 JEI 数据实时。
     *
     * @param player 目标玩家
     */
    public void syncJeiAmountsToPlayer(ServerPlayer player) {
        java.util.Map<ItemStack, Long> personalAmounts = new java.util.LinkedHashMap<>();
        java.util.Map<ItemStack, Long> publicAmounts = new java.util.LinkedHashMap<>();
        for (java.util.Map.Entry<StoredStackKey, StoredStackEntry> entry : this.resolveDatabaseForView(player, DatabaseScope.PERSONAL).entries().entrySet()) {
            personalAmounts.put(entry.getKey().displayStack(), entry.getValue().amount());
        }
        for (java.util.Map.Entry<StoredStackKey, StoredStackEntry> entry : this.resolveDatabaseForView(player, DatabaseScope.PUBLIC).entries().entrySet()) {
            publicAmounts.put(entry.getKey().displayStack(), entry.getValue().amount());
        }
        com.agguy.infiniteinventory.compat.jei.JeiCompat.syncAmounts(player, personalAmounts, publicAmounts);
    }

    /**
     * 同步所有正在查看公共仓库的玩家视图。
     * <p>
     * 公共仓库数据变更后调用，确保多玩家并发操作时的界面一致性。
     *
     * @param server 当前 Minecraft 服务端实例
     */
    public void syncPublicViewers(MinecraftServer server) { PersonalDatabaseServiceViewerHelper.syncPublicViewers(server); }

    /**
     * 同步所有正在查看无限仓库的玩家视图（含个人与公共）。
     *
     * @param server 当前 Minecraft 服务端实例
     */
    public void syncAllViewers(MinecraftServer server) { PersonalDatabaseServiceViewerHelper.syncAllViewers(server); }

    /**
     * 同步所有玩家视图，并额外通知当前正在查看仓库的玩家其所在作用域的变更。
     * <p>
     * 适用于大范围数据刷新场景（如标签页删除、跨作用域转移），确保玩家及时感知上下文变化。
     *
     * @param server 当前 Minecraft 服务端实例
     */
    public void syncAllViewersAndNotifyCurrentScope(MinecraftServer server) { PersonalDatabaseServiceViewerHelper.syncAllViewersAndNotifyCurrentScope(server); }

    /**
     * 通知指定玩家当前作用域下是否存在未解析的日志条目（如其他玩家造成的变更）。
     * <p>
     * 用于在玩家打开界面时提示“仓库已被他人修改”，引导其查看日志。
     *
     * @param player 目标玩家
     * @param scope  当前作用域
     */
    public void notifyViewerAboutUnresolvedEntries(ServerPlayer player, DatabaseScope scope) { PersonalDatabaseServiceViewerHelper.notifyViewerAboutUnresolvedEntries(player, scope); }
    private static long safeAddMovedItems(long currentTotal, ItemStack stack) { return PersonalDatabaseServiceStorageHelper.safeAddMovedItems(currentTotal, stack); }


    private StoredItemDatabase resolveDatabaseForView(ServerPlayer player, DatabaseScope scope) {
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.get(player.server);
        if (DatabaseScope.normalize(scope) == DatabaseScope.PUBLIC) {
            return storage.publicDatabase();
        }
        PersonalDatabaseServiceMigrationHelper.ensureLegacyPersonalMigration(this, player);
        return storage.personalDatabaseView(player.getUUID());
    }

    private DatabaseTabDirectory resolveTabsForView(ServerPlayer player, DatabaseScope scope) {
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.get(player.server);
        if (DatabaseScope.normalize(scope) == DatabaseScope.PUBLIC) {
            storage.publicDatabase().ensureTabAssignments(storage.publicTabs());
            return storage.publicTabs();
        }
        PersonalDatabaseServiceMigrationHelper.ensureLegacyPersonalMigration(this, player);
        StoredItemDatabase database = storage.personalDatabaseView(player.getUUID());
        DatabaseTabDirectory tabDirectory = storage.personalTabsView(player.getUUID());
        database.ensureTabAssignments(tabDirectory);
        return tabDirectory;
    }

    private DatabaseScopedTabRef resolveScopedTabForView(ServerPlayer player, DatabaseScopedTabRef scopedTab) {
        DatabaseScopedTabRef normalizedScopedTab = scopedTab == null ? DatabaseScopedTabRef.defaultTab() : scopedTab;
        DatabaseTabDirectory tabDirectory = this.resolveTabsForView(player, normalizedScopedTab.scope());
        String resolvedVisibleTabId = tabDirectory.resolveVisibleTabId(normalizedScopedTab.tabId());
        if (resolvedVisibleTabId == null) {
            return DatabaseScopedTabRef.allTab(normalizedScopedTab.scope());
        }
        return DatabaseScopedTabRef.concreteTab(normalizedScopedTab.scope(), resolvedVisibleTabId);
    }

    StoredItemDatabase resolveDatabaseForMutation(ServerPlayer player, DatabaseScope scope) {
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.get(player.server);
        if (DatabaseScope.normalize(scope) == DatabaseScope.PUBLIC) {
            storage.publicDatabase().ensureTabAssignments(storage.publicTabs());
            return storage.publicDatabase();
        }
        PersonalDatabaseServiceMigrationHelper.ensureLegacyPersonalMigration(this, player);
        StoredItemDatabase database = storage.personalDatabase(player.getUUID());
        database.ensureTabAssignments(storage.personalTabs(player.getUUID()));
        return database;
    }

    DatabaseTabDirectory resolveTabsForMutation(ServerPlayer player, DatabaseScope scope) {
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.get(player.server);
        if (DatabaseScope.normalize(scope) == DatabaseScope.PUBLIC) {
            storage.publicDatabase().ensureTabAssignments(storage.publicTabs());
            return storage.publicTabs();
        }
        PersonalDatabaseServiceMigrationHelper.ensureLegacyPersonalMigration(this, player);
        StoredItemDatabase database = storage.personalDatabase(player.getUUID());
        DatabaseTabDirectory tabDirectory = storage.personalTabs(player.getUUID());
        database.ensureTabAssignments(tabDirectory);
        return tabDirectory;
    }

    void markScopeDirty(ServerPlayer player, DatabaseScope scope) {
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.get(player.server);
        if (DatabaseScope.normalize(scope) == DatabaseScope.PERSONAL) {
            storage.prunePersonalDatabase(player.getUUID());
        }
        storage.setDirty();
        this.syncJeiAmountsToPlayer(player);
    }

}
