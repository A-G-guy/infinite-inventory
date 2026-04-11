package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabasePage;
import com.agguy.infiniteinventory.database.DatabasePageEntry;
import com.agguy.infiniteinventory.database.DatabasePagination;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseViewPreferencesAttachment;
import com.agguy.infiniteinventory.database.PlayerDatabaseAttachment;
import com.agguy.infiniteinventory.database.PublicDatabaseSavedData;
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
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class PersonalDatabaseService {
    public static final PersonalDatabaseService INSTANCE = new PersonalDatabaseService();

    private final DatabaseEntrySorter entrySorter = DatabaseEntrySorter.INSTANCE;
    private final DatabaseSearchEvaluator searchEvaluator = new DatabaseSearchEvaluator();

    private PersonalDatabaseService() {
    }

    public void open(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, ignored) -> {
                    PersonalDatabaseMenu menu = new PersonalDatabaseMenu(containerId, playerInventory, player);
                    menu.initializeFromPreferences(this.getViewPreferences(player));
                    return menu;
                },
                Component.translatable("screen.infiniteinventory.database.title")
        ));
        if (player.containerMenu instanceof PersonalDatabaseMenu menu) {
            menu.syncViewToClient();
        }
    }

    public PlayerDatabaseAttachment getPersonalDatabase(Player player) {
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
        this.resolveDatabase(player, scope).store(stack.copy());
        this.markScopeDirty(player, scope);
        slot.setByPlayer(ItemStack.EMPTY, stack.copy());
        slot.setChanged();
        return true;
    }

    public int depositMainInventory(ServerPlayer player, DatabaseScope scope) {
        Inventory inventory = player.getInventory();
        StoredItemDatabase database = this.resolveDatabase(player, scope);
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
        this.resolveDatabase(player, scope).store(stack);
        this.markScopeDirty(player, scope);
        return true;
    }

    public ItemStack extractToCarried(ServerPlayer player, DatabaseScope scope, StoredStackKey key, int requestedAmount) {
        ItemStack extracted = this.resolveDatabase(player, scope).extract(key, requestedAmount);
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
        StoredItemDatabase database = this.resolveDatabase(player, scope);
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
        List<QueryCandidate> filteredEntries = this.collectCandidates(this.resolveDatabase(player, normalizedQuery.scope()), normalizedQuery);
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
        for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
            if (onlinePlayer.containerMenu instanceof PersonalDatabaseMenu menu && menu.activeScope() == DatabaseScope.PUBLIC) {
                menu.syncViewToClient();
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

    private StoredItemDatabase resolveDatabase(ServerPlayer player, DatabaseScope scope) {
        if (DatabaseScope.normalize(scope) == DatabaseScope.PUBLIC) {
            return PublicDatabaseSavedData.get(player.server).database();
        }
        return this.getPersonalDatabase(player);
    }

    private void markScopeDirty(ServerPlayer player, DatabaseScope scope) {
        if (DatabaseScope.normalize(scope) == DatabaseScope.PUBLIC) {
            PublicDatabaseSavedData.get(player.server).setDirty();
        }
    }

    private record QueryCandidate(
            StoredStackKey key,
            StoredStackEntry entry,
            ItemStack stack,
            DatabaseSortSnapshot sortSnapshot
    ) {
    }
}
