package com.agguy.infiniteinventory.service;

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
public final class PersonalDatabaseService {
    public static final PersonalDatabaseService INSTANCE = new PersonalDatabaseService();
    static final int HOTBAR_SLOT_COUNT = 9;
    private static final Logger LOGGER = LogManager.getLogger();
    private final DatabaseQueryEngine queryEngine = DatabaseQueryEngine.INSTANCE;

    private PersonalDatabaseService() {
    }

    public void open(ServerPlayer player) {
        this.ensureLegacyPersonalMigration(player);
        player.openMenu(new PersonalDatabaseMenuProvider(player, this.getViewPreferences(player)));
        if (player.containerMenu instanceof PersonalDatabaseMenu menu) {
            menu.syncViewToClient();
            PersonalDatabaseServiceViewerHelper.notifyViewerAboutUnresolvedEntries(player, menu.activeScope());
        }
    }

    public PlayerDatabaseAttachment getLegacyPersonalDatabase(Player player) {
        return player.getData(ModAttachments.PERSONAL_DATABASE.get());
    }

    public DatabaseViewPreferencesAttachment getViewPreferences(Player player) {
        return player.getData(ModAttachments.DATABASE_VIEW_PREFERENCES.get());
    }

    public DatabaseEnhancementConfig getEnhancementConfig(Player player) {
        return this.getViewPreferences(player).enhancementConfig();
    }

    public boolean canStore(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() != ModItems.DATABASE_ACCESS_ITEM.get();
    }

    public boolean depositSlot(ServerPlayer player, DatabaseScope scope, String targetTabId, Slot slot) {
        ItemStack stack = slot.getItem();
        if (!this.canStore(stack)) {
            return false;
        }
        this.resolveDatabaseForMutation(player, scope).store(stack.copy(), this.resolveConcreteTargetTabId(player, scope, targetTabId));
        this.markScopeDirty(player, scope);
        slot.setByPlayer(ItemStack.EMPTY, stack.copy());
        slot.setChanged();
        return true;
    }

    public long depositMainInventory(ServerPlayer player, DatabaseScope scope, String targetTabId) {
        Inventory inventory = player.getInventory();
        StoredItemDatabase database = this.resolveDatabaseForMutation(player, scope);
        String resolvedTargetTabId = this.resolveConcreteTargetTabId(player, scope, targetTabId);
        long movedItems = 0L;
        boolean movedAny = false;
        for (int slotIndex = 0; slotIndex < inventory.items.size(); slotIndex++) {
            if (!PersonalDatabaseServiceStorageHelper.isPrimaryStorageSlot(slotIndex)) {
                continue;
            }
            ItemStack stack = inventory.items.get(slotIndex);
            if (!this.canStore(stack)) {
                continue;
            }
            movedItems = PersonalDatabaseServiceStorageHelper.safeAddMovedItems(movedItems, stack);
            movedAny = true;
            database.store(stack.copy(), resolvedTargetTabId);
            inventory.items.set(slotIndex, ItemStack.EMPTY);
        }
        if (movedAny) {
            this.markScopeDirty(player, scope);
            inventory.setChanged();
        }
        return movedItems;
    }

    public boolean storeStack(ServerPlayer player, DatabaseScope scope, String targetTabId, ItemStack stack) {
        if (!this.canStore(stack)) {
            return false;
        }
        this.resolveDatabaseForMutation(player, scope).store(stack, this.resolveConcreteTargetTabId(player, scope, targetTabId));
        this.markScopeDirty(player, scope);
        return true;
    }

    public boolean tryAutoStorePickedUpItem(ServerPlayer player, ItemEntity itemEntity) {
        if (player == null || itemEntity == null) {
            return false;
        }
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
        if (autoStoreTarget.scope() == DatabaseScope.PUBLIC) {
            this.syncPublicViewers(player.server);
        } else if (player.containerMenu instanceof PersonalDatabaseMenu menu && menu.activeScope() == DatabaseScope.PERSONAL) {
            menu.syncViewToClient();
        }
        return true;
    }

    public ItemStack extractToCarried(ServerPlayer player, DatabaseScope scope, StoredStackKey key, int requestedAmount) {
        ItemStack extracted = this.resolveDatabaseForMutation(player, scope).extract(key, requestedAmount);
        if (!extracted.isEmpty()) {
            this.markScopeDirty(player, scope);
        }
        return extracted;
    }

    public long extractAllToInventory(ServerPlayer player, DatabaseScope scope, StoredStackKey key) {
        return this.extractToInventory(player, scope, key, Long.MAX_VALUE);
    }

    public long extractToInventory(ServerPlayer player, DatabaseScope scope, StoredStackKey key, long requestedAmount) {
        return PersonalDatabaseExtractionHelper.extractToInventory(this, player, scope, this.resolveDatabaseForMutation(player, scope), key, requestedAmount);
    }

    public long extractToWorld(ServerPlayer player, DatabaseScope scope, StoredStackKey key, long requestedAmount) {
        return PersonalDatabaseExtractionHelper.extractToWorld(this, player, scope, this.resolveDatabaseForMutation(player, scope), key, requestedAmount);
    }

    public long extractSelectionToInventory(ServerPlayer player, DatabaseScope scope, List<DatabaseSelectionEntry> selectionEntries, DatabaseSelectionAction action) {
        return this.extractSelectionToInventory(player, scope, selectionEntries, action, 0L);
    }

    public long extractSelectionToInventory(
            ServerPlayer player, DatabaseScope scope, List<DatabaseSelectionEntry> selectionEntries, DatabaseSelectionAction action, long requestedAmount
    ) {
        return PersonalDatabaseExtractionHelper.extractSelectionToInventory(
                this,
                player,
                scope,
                this.resolveDatabaseForMutation(player, scope),
                selectionEntries,
                action,
                requestedAmount
        );
    }

    public DatabasePage buildPage(ServerPlayer player, DatabaseQuery query, DatabaseScopedTabRef scopedTab) {
        DatabaseQuery normalizedQuery = query == null ? DatabaseQuery.defaultQuery() : query;
        return this.queryEngine.buildPage(
                this.resolveDatabaseForView(player, scopedTab.scope()),
                this.resolveTabsForView(player, scopedTab.scope()),
                normalizedQuery,
                scopedTab
        );
    }

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
        return new DatabaseQuery(focusedTab, java.util.List.copyOf(visibleTabs), tabStates);
    }

    public List<DatabaseTab> tabsForScope(ServerPlayer player, DatabaseScope scope) {
        return this.resolveTabsForView(player, scope).orderedTabs();
    }

    public String resolveConcreteTargetTabId(ServerPlayer player, DatabaseScope scope, String requestedTabId) {
        return this.resolveTabsForMutation(player, scope).sanitizeConcreteTarget(requestedTabId);
    }

    public DatabaseAutoStoreTarget resolveAutoStoreTarget(ServerPlayer player) {
        DatabaseViewPreferencesAttachment preferences = this.getViewPreferences(player);
        DatabaseAutoStoreTarget preferredTarget = preferences.autoStoreTarget();
        String targetTabId = this.resolveTabsForMutation(player, preferredTarget.scope()).sanitizeConcreteTarget(preferredTarget.tabId());
        DatabaseAutoStoreTarget resolvedTarget = new DatabaseAutoStoreTarget(preferredTarget.scope(), targetTabId);
        preferences.setAutoStoreTarget(resolvedTarget);
        return resolvedTarget;
    }

    public boolean createTab(ServerPlayer player, DatabaseScope scope, String name, String iconItemId) {
        DatabaseTabDirectory tabDirectory = this.resolveTabsForMutation(player, scope);
        tabDirectory.addCustomTab(name, iconItemId);
        this.markScopeDirty(player, scope);
        return true;
    }

    public boolean renameTab(ServerPlayer player, DatabaseScope scope, String tabId, String name) {
        boolean changed = this.resolveTabsForMutation(player, scope).renameTab(tabId, name);
        if (changed) {
            this.markScopeDirty(player, scope);
        }
        return changed;
    }

    public boolean updateTabIcon(ServerPlayer player, DatabaseScope scope, String tabId, String iconItemId) {
        boolean changed = this.resolveTabsForMutation(player, scope).updateTabIcon(tabId, iconItemId);
        if (changed) {
            this.markScopeDirty(player, scope);
        }
        return changed;
    }

    public boolean moveTab(ServerPlayer player, DatabaseScope scope, String tabId, int direction) {
        boolean changed = this.resolveTabsForMutation(player, scope).moveTab(tabId, direction);
        if (changed) {
            this.markScopeDirty(player, scope);
        }
        return changed;
    }

    public boolean deleteTab(ServerPlayer player, DatabaseScope scope, String tabId, String targetTabId) {
        DatabaseTabDirectory tabDirectory = this.resolveTabsForMutation(player, scope);
        String resolvedTargetTabId = tabDirectory.sanitizeConcreteTarget(targetTabId);
        if (!tabDirectory.find(tabId).map(DatabaseTab::canDelete).orElse(false)) {
            return false;
        }
        boolean databaseChanged = this.resolveDatabaseForMutation(player, scope).transferTab(tabId, resolvedTargetTabId);
        boolean directoryChanged = tabDirectory.deleteTab(tabId);
        DatabaseScope normalizedScope = DatabaseScope.normalize(scope);
        DatabaseViewPreferencesAttachment preferences = this.getViewPreferences(player);
        DatabaseAutoStoreTarget autoStoreTarget = preferences.autoStoreTarget();
        if (autoStoreTarget.scope() == normalizedScope && autoStoreTarget.tabId().equals(tabId)) {
            preferences.setAutoStoreTarget(new DatabaseAutoStoreTarget(normalizedScope, resolvedTargetTabId));
        }
        if (databaseChanged || directoryChanged) {
            this.markScopeDirty(player, scope);
        }
        return databaseChanged || directoryChanged;
    }

    public boolean transferTab(
            ServerPlayer player,
            DatabaseScope sourceScope,
            DatabaseScope targetScope,
            String sourceTabId,
            String targetTabId
    ) {
        return PersonalDatabaseTransferHelper.transferTab(this, player, sourceScope, targetScope, sourceTabId, targetTabId);
    }

    public boolean transferSelection(
            ServerPlayer player,
            DatabaseScope sourceScope,
            DatabaseScope targetScope,
            List<DatabaseSelectionEntry> selectionEntries,
            String targetTabId
    ) {
        return PersonalDatabaseTransferHelper.transferSelection(this, player, sourceScope, targetScope, selectionEntries, targetTabId);
    }

    public void syncPublicViewers(MinecraftServer server) {
        PersonalDatabaseServiceViewerHelper.syncPublicViewers(server);
    }

    public void syncAllViewers(MinecraftServer server) {
        PersonalDatabaseServiceViewerHelper.syncAllViewers(server);
    }

    public void syncAllViewersAndNotifyCurrentScope(MinecraftServer server) {
        PersonalDatabaseServiceViewerHelper.syncAllViewersAndNotifyCurrentScope(server);
    }

    public void notifyViewerAboutUnresolvedEntries(ServerPlayer player, DatabaseScope scope) {
        PersonalDatabaseServiceViewerHelper.notifyViewerAboutUnresolvedEntries(player, scope);
    }

    private static long safeAddMovedItems(long currentTotal, ItemStack stack) {
        return PersonalDatabaseServiceStorageHelper.safeAddMovedItems(currentTotal, stack);
    }

    private static boolean isPrimaryStorageSlot(int slotIndex) {
        return PersonalDatabaseServiceStorageHelper.isPrimaryStorageSlot(slotIndex);
    }

    private StoredItemDatabase resolveDatabaseForView(ServerPlayer player, DatabaseScope scope) {
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.get(player.server);
        if (DatabaseScope.normalize(scope) == DatabaseScope.PUBLIC) {
            return storage.publicDatabase();
        }
        this.ensureLegacyPersonalMigration(player, storage);
        return storage.personalDatabaseView(player.getUUID());
    }

    private DatabaseTabDirectory resolveTabsForView(ServerPlayer player, DatabaseScope scope) {
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.get(player.server);
        if (DatabaseScope.normalize(scope) == DatabaseScope.PUBLIC) {
            storage.publicDatabase().ensureTabAssignments(storage.publicTabs());
            return storage.publicTabs();
        }
        this.ensureLegacyPersonalMigration(player, storage);
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
        this.ensureLegacyPersonalMigration(player, storage);
        StoredItemDatabase database = storage.personalDatabase(player.getUUID());
        database.ensureTabAssignments(storage.personalTabs(player.getUUID()));
        return database;
    }

    private DatabaseTabDirectory resolveTabsForMutation(ServerPlayer player, DatabaseScope scope) {
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.get(player.server);
        if (DatabaseScope.normalize(scope) == DatabaseScope.PUBLIC) {
            storage.publicDatabase().ensureTabAssignments(storage.publicTabs());
            return storage.publicTabs();
        }
        this.ensureLegacyPersonalMigration(player, storage);
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
    }

    private void ensureLegacyPersonalMigration(ServerPlayer player) {
        this.ensureLegacyPersonalMigration(player, DatabaseStorageSavedData.get(player.server));
    }

    private void ensureLegacyPersonalMigration(ServerPlayer player, DatabaseStorageSavedData storage) {
        PlayerDatabaseAttachment legacyDatabase = this.getLegacyPersonalDatabase(player);
        if (legacyDatabase.entryCount() == 0 && legacyDatabase.unresolvedEntryCount() == 0) {
            this.pruneStaleMigrationState(storage, player.getUUID());
            return;
        }
        UUID playerId = player.getUUID();
        int legacyEntryCount = legacyDatabase.entryCount() + legacyDatabase.unresolvedEntryCount();
        if (!storage.hasPersonalDatabase(playerId)) {
            storage.personalDatabase(playerId).mergeFrom(legacyDatabase);
            storage.recordMigrationState(playerId, new LegacyMigrationState(
                    LegacyMigrationState.Status.PENDING_CLEANUP,
                    System.currentTimeMillis(),
                    legacyEntryCount
            ));
            storage.setDirty();
            LOGGER.info("已将玩家 {} 的旧个人数据库导入统一存储，等待迁移备份完成后清理旧附件", player.getGameProfile().getName());
        }
        this.tryFinalizeLegacyCleanup(player, storage, legacyDatabase, legacyEntryCount);
    }

    private void tryFinalizeLegacyCleanup(
            ServerPlayer player,
            DatabaseStorageSavedData storage,
            PlayerDatabaseAttachment legacyDatabase,
            int legacyEntryCount
    ) {
        if (legacyDatabase.entryCount() == 0 && legacyDatabase.unresolvedEntryCount() == 0) {
            return;
        }
        UUID playerId = player.getUUID();
        LegacyMigrationState migrationState = storage.migrationState(playerId);
        if (!storage.hasPersonalDatabase(playerId)) {
            return;
        }
        if (migrationState == null) {
            storage.recordMigrationState(playerId, new LegacyMigrationState(
                    LegacyMigrationState.Status.SKIPPED_EXISTING_STORAGE,
                    System.currentTimeMillis(),
                    legacyEntryCount
            ));
            storage.setDirty();
            LOGGER.warn("玩家 {} 同时存在旧附件个人库与统一存储个人库，已跳过重复导入旧附件", player.getGameProfile().getName());
            return;
        }
        if (migrationState.status() == LegacyMigrationState.Status.SKIPPED_EXISTING_STORAGE) {
            return;
        }
        try {
            DatabaseBackupManager.createMigrationBackup(
                    player.server,
                    "legacy-personal-" + playerId,
                    storage.exportStorageTag(player.registryAccess())
            );
            legacyDatabase.clear();
            storage.clearMigrationState(playerId);
            storage.setDirty();
            LOGGER.info("已完成玩家 {} 的旧个人数据库迁移并清理旧附件", player.getGameProfile().getName());
        } catch (java.io.IOException exception) {
            LOGGER.error("为玩家 {} 生成旧个人数据库迁移备份失败，旧附件已保留", player.getGameProfile().getName(), exception);
        }
    }

    private void pruneStaleMigrationState(DatabaseStorageSavedData storage, UUID playerId) {
        if (storage == null || playerId == null) {
            return;
        }
        if (storage.clearMigrationState(playerId)) {
            storage.setDirty();
        }
    }

    static String entryTabId(StoredItemDatabase database, StoredStackKey key) {
        StoredStackEntry entry = database.entries().get(key);
        return entry == null ? DatabaseTabs.DEFAULT_TAB_ID : entry.tabId();
    }
}
