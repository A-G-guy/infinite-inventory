package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabaseLogEntry;
import com.agguy.infiniteinventory.database.DatabasePage;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseAutoStoreTarget;
import com.agguy.infiniteinventory.database.DatabaseEnhancementConfig;
import com.agguy.infiniteinventory.database.DatabaseStorageSavedData;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabDirectory;
import com.agguy.infiniteinventory.database.DatabaseSelectionEntry;
import com.agguy.infiniteinventory.database.DatabaseViewPreferencesAttachment;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.PlayerDatabaseAttachment;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.localization.ViewerLanguage;
import com.agguy.infiniteinventory.menu.PersonalDatabaseMenu;
import com.agguy.infiniteinventory.network.DatabaseSelectionAction;
import com.agguy.infiniteinventory.registry.ModAttachments;
import com.agguy.infiniteinventory.registry.ModItems;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
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
 *   <li>物品流转：存入（deposit/store，委托至 {@link PersonalDatabaseServiceDepositHelper}）、取出（extract，委托至 {@link PersonalDatabaseServiceExtractHelper}）</li>
 *   <li>查询与视图：分页构建（{@link #buildPage}）、查询条件清洗（{@link #sanitizeQuery}）</li>
 *   <li>标签页生命周期：创建、重命名、移动、删除、转移（委托至 {@link PersonalDatabaseServiceTabHelper}）</li>
 *   <li>增强功能：自动拾取存储、星标/备注（委托至 {@link PersonalDatabaseServiceStarNoteHelper}）、操作日志</li>
 *   <li>同步与兼容：JEI 数量同步、多玩家视图刷新（委托至 {@link PersonalDatabaseServiceSyncHelper}）、旧数据迁移</li>
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
     * 打开前会自动触发旧版数据迁移检查；打开后同步视图并提示未解析日志条目。
     *
     * @param player 要打开界面的服务端玩家，不可为 null
     */
    public void open(ServerPlayer player) {
        PersonalDatabaseServiceMigrationHelper.ensureLegacyPersonalMigration(this, player);
        player.openMenu(new PersonalDatabaseMenuProvider(player, this.getViewPreferences(player)));
        if (player.containerMenu instanceof PersonalDatabaseMenu menu) {
            menu.syncViewToClient();
            PersonalDatabaseServiceSyncHelper.notifyViewerAboutUnresolvedEntries(player, menu.activeScope());
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
     * 获取玩家的视图偏好设置。
     *
     * @param player 目标玩家
     * @return 玩家的视图偏好设置
     */
    public DatabaseViewPreferencesAttachment getViewPreferences(Player player) {
        return player.getData(ModAttachments.DATABASE_VIEW_PREFERENCES.get());
    }

    /**
     * 获取玩家当前已启用的增强功能配置。
     *
     * @param player 目标玩家
     * @return 增强功能配置快照
     */
    public DatabaseEnhancementConfig getEnhancementConfig(Player player) {
        return this.getViewPreferences(player).enhancementConfig();
    }

    /**
     * 判断给定物品堆是否可以存入无限仓库。
     * <p>业务约束：空物品与数据库访问物品禁止存入。</p>
     *
     * @param stack 待检查的物品堆
     * @return 若允许存入则返回 true，否则返回 false
     */
    public boolean canStore(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() != ModItems.DATABASE_ACCESS_ITEM.get();
    }

    public boolean depositSlot(ServerPlayer player, DatabaseScope scope, String targetTabId, Slot slot) {
        return PersonalDatabaseServiceDepositHelper.depositSlot(this, player, scope, targetTabId, slot);
    }

    public long depositMainInventory(ServerPlayer player, DatabaseScope scope, String targetTabId) {
        return PersonalDatabaseServiceDepositHelper.depositMainInventory(this, player, scope, targetTabId);
    }

    public long depositExistingByTab(ServerPlayer player, DatabaseScope scope) {
        return PersonalDatabaseServiceDepositHelper.depositExistingByTab(this, player, scope);
    }

    public boolean storeStack(ServerPlayer player, DatabaseScope scope, String targetTabId, ItemStack stack) {
        return PersonalDatabaseServiceDepositHelper.storeStack(this, player, scope, targetTabId, stack);
    }

    public boolean tryAutoStorePickedUpItem(ServerPlayer player, ItemEntity itemEntity) {
        return PersonalDatabaseServiceDepositHelper.tryAutoStorePickedUpItem(this, player, itemEntity);
    }

    public PersonalDatabaseServiceDepositHelper.DepositConflict checkDepositConflict(
            ServerPlayer player, DatabaseScope scope, String targetTabId, ItemStack stack
    ) {
        return PersonalDatabaseServiceDepositHelper.checkDepositConflict(this, player, scope, targetTabId, stack);
    }

    public void resolveDepositConflictMoveEntry(
            ServerPlayer player, DatabaseScope scope, StoredStackKey key, String existingTabId, String targetTabId
    ) {
        StoredItemDatabase database = this.resolveDatabaseForMutation(player, scope);
        database.moveEntryToTab(key, existingTabId, targetTabId);
    }

    public ItemStack extractToCarried(ServerPlayer player, DatabaseScope scope, StoredStackKey key, int requestedAmount) {
        return PersonalDatabaseServiceExtractHelper.extractToCarried(this, player, scope, key, requestedAmount);
    }

    public long extractAllToInventory(ServerPlayer player, DatabaseScope scope, StoredStackKey key) {
        return PersonalDatabaseServiceExtractHelper.extractAllToInventory(this, player, scope, key);
    }

    public long extractToInventory(ServerPlayer player, DatabaseScope scope, StoredStackKey key, long requestedAmount) {
        return PersonalDatabaseServiceExtractHelper.extractToInventory(this, player, scope, key, requestedAmount);
    }

    public long extractToWorld(ServerPlayer player, DatabaseScope scope, StoredStackKey key, long requestedAmount) {
        return PersonalDatabaseServiceExtractHelper.extractToWorld(this, player, scope, key, requestedAmount);
    }

    public long extractSelectionToInventory(ServerPlayer player, DatabaseScope scope, List<DatabaseSelectionEntry> selectionEntries, DatabaseSelectionAction action) {
        return PersonalDatabaseServiceExtractHelper.extractSelectionToInventory(this, player, scope, selectionEntries, action);
    }

    public long extractSelectionToInventory(
            ServerPlayer player, DatabaseScope scope, List<DatabaseSelectionEntry> selectionEntries, DatabaseSelectionAction action, long requestedAmount
    ) {
        return PersonalDatabaseServiceExtractHelper.extractSelectionToInventory(this, player, scope, selectionEntries, action, requestedAmount);
    }

    /**
     * 根据查询条件构建分页结果，用于界面渲染。
     *
     * @param player         请求分页的玩家
     * @param query          查询条件；可为 null
     * @param scopedTab      当前聚焦的标签页引用
     * @param viewerLanguage 玩家当前界面语言
     * @return 分页数据
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
     * 清洗并规范化玩家提交的查询条件。实际逻辑委托至 {@link PersonalDatabaseServiceQueryHelper}。
     *
     * @param player 提交查询的玩家
     * @param query  原始查询条件；可为 null
     * @return 经校验后的安全查询条件
     */
    public DatabaseQuery sanitizeQuery(ServerPlayer player, DatabaseQuery query) {
        return PersonalDatabaseServiceQueryHelper.sanitizeQuery(this, player, query);
    }

    /**
     * 切换指定顶部标签页的可见性（显示/隐藏）。
     *
     * @param player 执行操作的玩家
     * @param scope  标签页所属作用域
     * @param tabId  目标标签页标识
     * @return 若可见性状态发生实际变化返回 true
     */
    public boolean toggleTopTabVisibility(ServerPlayer player, DatabaseScope scope, String tabId) {
        return PersonalDatabaseServiceTabHelper.toggleTopTabVisibility(this, player, scope, tabId);
    }

    /**
     * 获取指定作用域下所有已排序的标签页列表。
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
     *
     * @param player         当前玩家
     * @param scope          目标作用域
     * @param requestedTabId 请求的标签页标识；可为空
     * @return 经校验后的有效具体标签页标识
     */
    public String resolveConcreteTargetTabId(ServerPlayer player, DatabaseScope scope, String requestedTabId) {
        return PersonalDatabaseServiceTabHelper.resolveConcreteTargetTabId(this, player, scope, requestedTabId);
    }

    /**
     * 解析并持久化玩家当前的自动存储目标。
     *
     * @param player 当前玩家
     * @return 经校验后的自动存储目标
     */
    public DatabaseAutoStoreTarget resolveAutoStoreTarget(ServerPlayer player) {
        DatabaseViewPreferencesAttachment preferences = this.getViewPreferences(player);
        DatabaseAutoStoreTarget preferredTarget = preferences.autoStoreTarget();
        String targetTabId = this.resolveTabsForMutation(player, preferredTarget.scope()).sanitizeConcreteTarget(preferredTarget.tabId());
        DatabaseAutoStoreTarget resolvedTarget = new DatabaseAutoStoreTarget(preferredTarget.scope(), targetTabId);
        preferences.setAutoStoreTarget(resolvedTarget);
        return resolvedTarget;
    }

    public boolean createTab(ServerPlayer player, DatabaseScope scope, String name, String iconItemId) {
        return PersonalDatabaseServiceTabHelper.createTab(this, player, scope, name, iconItemId);
    }

    public boolean renameTab(ServerPlayer player, DatabaseScope scope, String tabId, String name) {
        return PersonalDatabaseServiceTabHelper.renameTab(this, player, scope, tabId, name);
    }

    public boolean updateTabIcon(ServerPlayer player, DatabaseScope scope, String tabId, String iconItemId) {
        return PersonalDatabaseServiceTabHelper.updateTabIcon(this, player, scope, tabId, iconItemId);
    }

    public boolean moveTab(ServerPlayer player, DatabaseScope scope, String tabId, int direction) {
        return PersonalDatabaseServiceTabHelper.moveTab(this, player, scope, tabId, direction);
    }

    /**
     * 删除指定标签页，并将其中的物品转移到另一个目标标签页。
     *
     * @param player      执行操作的玩家
     * @param scope       标签页所属作用域
     * @param tabId       待删除的标签页标识
     * @param targetTabId 接收物品的目标标签页标识
     * @return 若数据库或目录发生实际变化返回 true
     */
    public boolean deleteTab(ServerPlayer player, DatabaseScope scope, String tabId, String targetTabId) {
        return PersonalDatabaseServiceTabHelper.deleteTab(this, player, scope, tabId, targetTabId);
    }

    public boolean transferTab(ServerPlayer player, DatabaseScope sourceScope, DatabaseScope targetScope, String sourceTabId, String targetTabId) {
        return PersonalDatabaseTransferHelper.transferTab(this, player, sourceScope, targetScope, sourceTabId, targetTabId);
    }

    public boolean transferSelection(ServerPlayer player, DatabaseScope sourceScope, DatabaseScope targetScope, List<DatabaseSelectionEntry> selectionEntries, String targetTabId) {
        return PersonalDatabaseTransferHelper.transferSelection(this, player, sourceScope, targetScope, selectionEntries, targetTabId);
    }

    public void setNote(ServerPlayer player, DatabaseScope scope, StoredStackKey key, String note) {
        PersonalDatabaseServiceStarNoteHelper.setNote(this, player, scope, key, note);
    }

    public String noteFor(ServerPlayer player, DatabaseScope scope, StoredStackKey key) {
        return PersonalDatabaseServiceStarNoteHelper.noteFor(this, player, scope, key);
    }

    public boolean toggleStar(ServerPlayer player, DatabaseScope scope, StoredStackKey key) {
        return PersonalDatabaseServiceStarNoteHelper.toggleStar(this, player, scope, key);
    }

    public boolean setStarred(ServerPlayer player, DatabaseScope scope, StoredStackKey key, boolean starred) {
        return PersonalDatabaseServiceStarNoteHelper.setStarred(this, player, scope, key, starred);
    }

    public boolean isStarred(ServerPlayer player, DatabaseScope scope, StoredStackKey key) {
        return PersonalDatabaseServiceStarNoteHelper.isStarred(this, player, scope, key);
    }

    /**
     * 获取指定作用域下的操作日志条目列表。
     *
     * @param player 请求玩家
     * @param scope  目标作用域
     * @return 按时间倒序排列的日志条目列表
     */
    public List<DatabaseLogEntry> getLogEntries(ServerPlayer player, DatabaseScope scope) {
        return this.resolveDatabaseForMutation(player, scope).logEntries();
    }

    /**
     * 获取指定作用域下的统计快照，自动使用缓存。
     *
     * @param player 请求玩家
     * @param scope  目标作用域
     * @return 统计快照
     */
    public com.agguy.infiniteinventory.database.statistics.DatabaseStatisticsSnapshot getStatisticsSnapshot(ServerPlayer player, DatabaseScope scope) {
        StoredItemDatabase database = this.resolveDatabaseForView(player, scope);
        return StatisticsCache.INSTANCE.getOrCompute(
                player.getUUID(),
                DatabaseScope.normalize(scope),
                database.revision(),
                () -> PersonalDatabaseServiceStatisticsHelper.buildStatisticsSnapshot(this, player, scope)
        );
    }

    public long databaseRevisionFor(ServerPlayer player, DatabaseScope scope) {
        return this.resolveDatabaseForView(player, scope).revision();
    }

    public void syncAmountsToPlayer(ServerPlayer player) {
        PersonalDatabaseServiceSyncHelper.syncFullAmountsToPlayer(this, player);
    }

    public void syncPublicViewers(MinecraftServer server) {
        PersonalDatabaseServiceSyncHelper.syncPublicViewers(server);
    }

    public void syncAllViewers(MinecraftServer server) {
        PersonalDatabaseServiceSyncHelper.syncAllViewers(server);
    }

    public void syncAllViewersAndNotifyCurrentScope(MinecraftServer server) {
        PersonalDatabaseServiceSyncHelper.syncAllViewersAndNotifyCurrentScope(server);
    }

    public void notifyViewerAboutUnresolvedEntries(ServerPlayer player, DatabaseScope scope) {
        PersonalDatabaseServiceSyncHelper.notifyViewerAboutUnresolvedEntries(player, scope);
    }

    public void registerPublicViewer(ServerPlayer player) {
        PersonalDatabaseServiceViewerHelper.registerPublicViewer(player);
    }

    public void unregisterPublicViewer(ServerPlayer player) {
        PersonalDatabaseServiceViewerHelper.unregisterPublicViewer(player);
    }

    public void onPlayerLogout(ServerPlayer player) {
        PersonalDatabaseServiceViewerHelper.onPlayerLogout(player);
    }

    StoredItemDatabase resolveDatabaseForView(ServerPlayer player, DatabaseScope scope) {
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.get(player.server);
        if (DatabaseScope.normalize(scope) == DatabaseScope.PUBLIC) {
            return storage.publicDatabase();
        }
        PersonalDatabaseServiceMigrationHelper.ensureLegacyPersonalMigration(this, player);
        return storage.personalDatabaseView(player.getUUID());
    }

    public DatabaseTabDirectory resolveTabsForView(ServerPlayer player, DatabaseScope scope) {
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
        storage.requestForceSave();
        boolean deltaSent = PersonalDatabaseServiceSyncHelper.syncAmountDeltasToPlayer(this, player, scope);
        if (!deltaSent) {
            PersonalDatabaseServiceSyncHelper.syncFullAmountsToPlayer(this, player);
        }
        StatisticsCache.INSTANCE.invalidate(player.getUUID(), DatabaseScope.normalize(scope));
    }
}
