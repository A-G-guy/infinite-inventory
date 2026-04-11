package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabasePage;
import com.agguy.infiniteinventory.database.DatabasePageEntry;
import com.agguy.infiniteinventory.database.DatabasePagination;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseBackupManager;
import com.agguy.infiniteinventory.database.DatabaseStorageSavedData;
import com.agguy.infiniteinventory.database.DatabaseViewPreferencesAttachment;
import com.agguy.infiniteinventory.database.LegacyMigrationState;
import com.agguy.infiniteinventory.database.PlayerDatabaseAttachment;
import com.agguy.infiniteinventory.database.StoredStackEntry;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.database.VisibleDatabaseEntry;
import com.agguy.infiniteinventory.menu.PersonalDatabaseMenu;
import com.agguy.infiniteinventory.registry.ModAttachments;
import com.agguy.infiniteinventory.registry.ModItems;
import com.agguy.infiniteinventory.service.search.DatabaseItemSearchResolver;
import com.agguy.infiniteinventory.service.search.DatabaseSearchEvaluator;
import com.agguy.infiniteinventory.service.search.DatabaseSearchIndex;
import com.agguy.infiniteinventory.service.search.DatabaseSearchRanking;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.IMenuProviderExtension;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class PersonalDatabaseService {
    public static final PersonalDatabaseService INSTANCE = new PersonalDatabaseService();

    private static final Logger LOGGER = LogManager.getLogger();

    private final DatabaseEntrySorter entrySorter = DatabaseEntrySorter.INSTANCE;
    private final DatabaseSearchEvaluator searchEvaluator = new DatabaseSearchEvaluator();

    private PersonalDatabaseService() {
    }

    public void open(ServerPlayer player) {
        this.ensureLegacyPersonalMigration(player);
        player.openMenu(new PersonalDatabaseMenuProvider(player, this.getViewPreferences(player)));
        if (player.containerMenu instanceof PersonalDatabaseMenu menu) {
            menu.syncViewToClient();
            this.notifyAboutUnresolvedEntries(player, menu.activeScope());
        }
    }

    public PlayerDatabaseAttachment getLegacyPersonalDatabase(Player player) {
        return player.getData(ModAttachments.PERSONAL_DATABASE.get());
    }

    public DatabaseViewPreferencesAttachment getViewPreferences(Player player) {
        return player.getData(ModAttachments.DATABASE_VIEW_PREFERENCES.get());
    }

    public boolean canStore(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() != ModItems.DATABASE_ACCESS_ITEM.get();
    }

    public boolean depositSlot(ServerPlayer player, DatabaseScope scope, Slot slot) {
        ItemStack stack = slot.getItem();
        if (!this.canStore(stack)) {
            return false;
        }
        this.resolveDatabaseForMutation(player, scope).store(stack.copy());
        this.markScopeDirty(player, scope);
        slot.setByPlayer(ItemStack.EMPTY, stack.copy());
        slot.setChanged();
        return true;
    }

    public int depositMainInventory(ServerPlayer player, DatabaseScope scope) {
        Inventory inventory = player.getInventory();
        StoredItemDatabase database = this.resolveDatabaseForMutation(player, scope);
        int movedItems = 0;
        for (int slotIndex = 0; slotIndex < inventory.items.size(); slotIndex++) {
            ItemStack stack = inventory.items.get(slotIndex);
            if (!this.canStore(stack)) {
                continue;
            }
            movedItems += stack.getCount();
            database.store(stack.copy());
            inventory.items.set(slotIndex, ItemStack.EMPTY);
        }
        if (movedItems > 0) {
            this.markScopeDirty(player, scope);
            inventory.setChanged();
        }
        return movedItems;
    }

    public boolean storeStack(ServerPlayer player, DatabaseScope scope, ItemStack stack) {
        if (!this.canStore(stack)) {
            return false;
        }
        this.resolveDatabaseForMutation(player, scope).store(stack);
        this.markScopeDirty(player, scope);
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
        if (requestedAmount <= 0L) {
            return 0L;
        }
        Inventory inventory = player.getInventory();
        StoredItemDatabase database = this.resolveDatabaseForMutation(player, scope);
        long movedItems = 0L;
        long remainingAmount = requestedAmount;
        while (remainingAmount > 0L && this.hasSpaceFor(inventory, key)) {
            int extractedCount = (int) Math.min((long) key.maxStackSize(), remainingAmount);
            ItemStack extracted = database.extract(key, extractedCount);
            if (extracted.isEmpty()) {
                break;
            }
            int originalCount = extracted.getCount();
            inventory.add(extracted);
            int movedNow = originalCount - extracted.getCount();
            if (movedNow <= 0) {
                database.store(extracted);
                break;
            }
            movedItems += movedNow;
            remainingAmount -= movedNow;
            if (!extracted.isEmpty()) {
                database.store(extracted);
                break;
            }
        }
        if (movedItems > 0L) {
            this.markScopeDirty(player, scope);
            inventory.setChanged();
        }
        return movedItems;
    }

    public DatabasePage buildPage(ServerPlayer player, DatabaseQuery query) {
        DatabaseQuery normalizedQuery = query == null ? DatabaseQuery.defaultQuery() : query;
        List<QueryCandidate> filteredEntries = this.collectCandidates(this.resolveDatabaseForView(player, normalizedQuery.scope()), normalizedQuery);
        filteredEntries.sort(Comparator.comparing(QueryCandidate::sortSnapshot, this.entrySorter.comparatorFor(normalizedQuery)));

        int safePageSize = Math.max(1, normalizedQuery.pageSize());
        int totalEntries = filteredEntries.size();
        int totalPages = DatabasePagination.resolveTotalPages(totalEntries, safePageSize);
        int pageIndex = Math.min(normalizedQuery.pageIndex(), totalPages - 1);
        DatabaseQuery resolvedQuery = normalizedQuery.withPageSize(safePageSize).withPageIndex(pageIndex);
        long totalItems = this.totalItems(filteredEntries);
        int fromIndex = Math.min(pageIndex * safePageSize, totalEntries);
        int toIndex = Math.min(fromIndex + safePageSize, totalEntries);

        List<DatabasePageEntry> pageEntries = new ArrayList<>(safePageSize);
        for (int index = fromIndex; index < toIndex; index++) {
            QueryCandidate candidate = filteredEntries.get(index);
            pageEntries.add(new DatabasePageEntry(
                    candidate.key(),
                    new VisibleDatabaseEntry(candidate.stack().copyWithCount(1), candidate.entry().amount(), candidate.entry().category(), candidate.key().registryName())
            ));
        }
        return new DatabasePage(resolvedQuery, totalEntries, totalPages, totalItems, pageEntries);
    }

    public void syncPublicViewers(MinecraftServer server) {
        this.syncViewers(server, true, false);
    }

    public void syncAllViewers(MinecraftServer server) {
        this.syncViewers(server, false, false);
    }

    public void syncAllViewersAndNotifyCurrentScope(MinecraftServer server) {
        this.syncViewers(server, false, true);
    }

    public void notifyViewerAboutUnresolvedEntries(ServerPlayer player, DatabaseScope scope) {
        this.notifyAboutUnresolvedEntries(player, scope);
    }

    private void syncViewers(MinecraftServer server, boolean publicOnly, boolean notifyCurrentScope) {
        for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
            if (onlinePlayer.containerMenu instanceof PersonalDatabaseMenu menu
                    && (!publicOnly || menu.activeScope() == DatabaseScope.PUBLIC)) {
                menu.syncViewToClient();
                if (notifyCurrentScope) {
                    this.notifyAboutUnresolvedEntries(onlinePlayer, menu.activeScope());
                }
            }
        }
    }

    private List<QueryCandidate> collectCandidates(StoredItemDatabase database, DatabaseQuery query) {
        List<QueryCandidate> candidates = new ArrayList<>();
        for (Map.Entry<StoredStackKey, StoredStackEntry> mapEntry : database.entries().entrySet()) {
            StoredStackKey key = mapEntry.getKey();
            StoredStackEntry entry = mapEntry.getValue();
            if (query.category() != com.agguy.infiniteinventory.database.DatabaseCategory.ALL && entry.category() != query.category()) {
                continue;
            }
            ItemStack displayStack = key.displayStack();
            DatabaseSearchIndex searchIndex = DatabaseItemSearchResolver.INSTANCE.resolve(key);
            DatabaseSearchRanking searchRanking = this.searchEvaluator.evaluate(query, searchIndex, entry.amount());
            if (!searchRanking.matched()) {
                continue;
            }
            candidates.add(new QueryCandidate(
                    key,
                    entry,
                    displayStack,
                    new DatabaseSortSnapshot(
                            searchIndex.displayNameNormalized(),
                            key.registryName(),
                            key.registryNamespace(),
                            key.registryPath(),
                            entry.amount(),
                            entry.lastModified(),
                            key.hashCode(),
                            searchRanking
                    )
            ));
        }
        return candidates;
    }

    private boolean hasSpaceFor(Inventory inventory, StoredStackKey key) {
        ItemStack probe = key.toStack(1);
        return inventory.getFreeSlot() != -1 || inventory.getSlotWithRemainingSpace(probe) != -1;
    }

    private long totalItems(List<QueryCandidate> candidates) {
        long total = 0L;
        for (QueryCandidate candidate : candidates) {
            if (Long.MAX_VALUE - total < candidate.entry().amount()) {
                return Long.MAX_VALUE;
            }
            total += candidate.entry().amount();
        }
        return total;
    }

    private StoredItemDatabase resolveDatabaseForView(ServerPlayer player, DatabaseScope scope) {
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.get(player.server);
        if (DatabaseScope.normalize(scope) == DatabaseScope.PUBLIC) {
            return storage.publicDatabase();
        }
        this.ensureLegacyPersonalMigration(player, storage);
        return storage.personalDatabaseView(player.getUUID());
    }

    private StoredItemDatabase resolveDatabaseForMutation(ServerPlayer player, DatabaseScope scope) {
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.get(player.server);
        if (DatabaseScope.normalize(scope) == DatabaseScope.PUBLIC) {
            return storage.publicDatabase();
        }
        this.ensureLegacyPersonalMigration(player, storage);
        return storage.personalDatabase(player.getUUID());
    }

    private void markScopeDirty(ServerPlayer player, DatabaseScope scope) {
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
            storage.recordMigrationState(playerId, new LegacyMigrationState(
                    LegacyMigrationState.Status.MIGRATED,
                    System.currentTimeMillis(),
                    migrationState.legacyEntryCount()
            ));
            storage.setDirty();
            LOGGER.info("已完成玩家 {} 的旧个人数据库迁移并清理旧附件", player.getGameProfile().getName());
        } catch (java.io.IOException exception) {
            LOGGER.error("为玩家 {} 生成旧个人数据库迁移备份失败，旧附件已保留", player.getGameProfile().getName(), exception);
        }
    }

    private void notifyAboutUnresolvedEntries(ServerPlayer player, DatabaseScope scope) {
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.get(player.server);
        int unresolvedEntryCount = storage.unresolvedEntryCount(scope, player.getUUID());
        if (unresolvedEntryCount <= 0) {
            return;
        }
        player.sendSystemMessage(Component.translatable(
                DatabaseScope.normalize(scope) == DatabaseScope.PUBLIC
                        ? "message.infiniteinventory.database.unresolved.public"
                        : "message.infiniteinventory.database.unresolved.personal",
                unresolvedEntryCount
        ));
    }

    private record QueryCandidate(
            StoredStackKey key,
            StoredStackEntry entry,
            ItemStack stack,
            DatabaseSortSnapshot sortSnapshot
    ) {
    }

    private static final class PersonalDatabaseMenuProvider implements MenuProvider, IMenuProviderExtension {
        private final ServerPlayer player;
        private final DatabaseViewPreferencesAttachment preferences;

        private PersonalDatabaseMenuProvider(ServerPlayer player, DatabaseViewPreferencesAttachment preferences) {
            this.player = player;
            this.preferences = preferences;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("screen.infiniteinventory.database.title");
        }

        @Override
        public PersonalDatabaseMenu createMenu(int containerId, Inventory playerInventory, Player ignoredPlayer) {
            PersonalDatabaseMenu menu = new PersonalDatabaseMenu(containerId, playerInventory, this.player);
            menu.initializeFromPreferences(this.preferences);
            return menu;
        }

        @Override
        public void writeClientSideData(net.minecraft.world.inventory.AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
            if (menu instanceof PersonalDatabaseMenu databaseMenu) {
                buffer.writeVarLong(databaseMenu.sessionId());
            } else {
                buffer.writeVarLong(0L);
            }
        }
    }
}
