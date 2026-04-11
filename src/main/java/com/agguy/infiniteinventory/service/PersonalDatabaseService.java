package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabasePage;
import com.agguy.infiniteinventory.database.DatabasePageEntry;
import com.agguy.infiniteinventory.database.DatabasePagination;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import com.agguy.infiniteinventory.database.PlayerDatabaseAttachment;
import com.agguy.infiniteinventory.database.StoredStackEntry;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.database.VisibleDatabaseEntry;
import com.agguy.infiniteinventory.menu.PersonalDatabaseMenu;
import com.agguy.infiniteinventory.registry.ModAttachments;
import com.agguy.infiniteinventory.registry.ModItems;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class PersonalDatabaseService {
    public static final PersonalDatabaseService INSTANCE = new PersonalDatabaseService();

    private PersonalDatabaseService() {
    }

    public void open(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, ignored) -> new PersonalDatabaseMenu(containerId, playerInventory, player),
                Component.translatable("screen.infiniteinventory.database.title")
        ));
        if (player.containerMenu instanceof PersonalDatabaseMenu menu) {
            menu.syncViewToClient();
        }
    }

    public PlayerDatabaseAttachment getDatabase(Player player) {
        return player.getData(ModAttachments.PERSONAL_DATABASE.get());
    }

    public boolean canStore(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() != ModItems.DATABASE_ACCESS_ITEM.get();
    }

    public boolean depositSlot(ServerPlayer player, Slot slot) {
        ItemStack stack = slot.getItem();
        if (!this.canStore(stack)) {
            return false;
        }
        this.getDatabase(player).store(stack.copy());
        slot.setByPlayer(ItemStack.EMPTY, stack.copy());
        slot.setChanged();
        return true;
    }

    public int depositMainInventory(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        int movedItems = 0;
        for (int slotIndex = 0; slotIndex < inventory.items.size(); slotIndex++) {
            ItemStack stack = inventory.items.get(slotIndex);
            if (!this.canStore(stack)) {
                continue;
            }
            movedItems += stack.getCount();
            this.getDatabase(player).store(stack.copy());
            inventory.items.set(slotIndex, ItemStack.EMPTY);
        }
        if (movedItems > 0) {
            inventory.setChanged();
        }
        return movedItems;
    }

    public long extractAllToInventory(Player player, StoredStackKey key) {
        return this.extractToInventory(player, key, Long.MAX_VALUE);
    }

    public long extractToInventory(Player player, StoredStackKey key, long requestedAmount) {
        if (requestedAmount <= 0L) {
            return 0L;
        }
        Inventory inventory = player.getInventory();
        PlayerDatabaseAttachment database = this.getDatabase(player);
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
            inventory.setChanged();
        }
        return movedItems;
    }

    public DatabasePage buildPage(ServerPlayer player, DatabaseQuery query) {
        DatabaseQuery normalizedQuery = query == null ? DatabaseQuery.defaultQuery() : query;
        List<QueryCandidate> filteredEntries = this.collectCandidates(player, normalizedQuery);
        filteredEntries.sort(this.comparatorFor(normalizedQuery.sortOption()));

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

    private List<QueryCandidate> collectCandidates(ServerPlayer player, DatabaseQuery query) {
        List<QueryCandidate> candidates = new ArrayList<>();
        String searchNeedle = query.searchText().toLowerCase(Locale.ROOT);
        for (Map.Entry<StoredStackKey, StoredStackEntry> mapEntry : this.getDatabase(player).entries().entrySet()) {
            StoredStackKey key = mapEntry.getKey();
            StoredStackEntry entry = mapEntry.getValue();
            if (query.category() != com.agguy.infiniteinventory.database.DatabaseCategory.ALL && entry.category() != query.category()) {
                continue;
            }
            ItemStack displayStack = key.displayStack();
            String displayName = displayStack.getHoverName().getString();
            String displayNameLower = displayName.toLowerCase(Locale.ROOT);
            String registryNameLower = key.registryName().toLowerCase(Locale.ROOT);
            if (!searchNeedle.isEmpty()
                    && !displayNameLower.contains(searchNeedle)
                    && !registryNameLower.contains(searchNeedle)
                    && !key.registryPath().toLowerCase(Locale.ROOT).contains(searchNeedle)) {
                continue;
            }
            candidates.add(new QueryCandidate(key, entry, displayStack, displayName, displayNameLower, registryNameLower));
        }
        return candidates;
    }

    private Comparator<QueryCandidate> comparatorFor(DatabaseSortOption sortOption) {
        Comparator<QueryCandidate> byName = Comparator.comparing(QueryCandidate::displayNameLower).thenComparing(QueryCandidate::registryNameLower);
        return switch (sortOption) {
            case NAME_ASC -> byName;
            case NAME_DESC -> byName.reversed();
            case COUNT_ASC -> Comparator.comparingLong((QueryCandidate candidate) -> candidate.entry().amount()).thenComparing(byName);
            case COUNT_DESC -> Comparator.comparingLong((QueryCandidate candidate) -> candidate.entry().amount()).reversed().thenComparing(byName);
            case RECENTLY_CHANGED -> Comparator.comparingLong((QueryCandidate candidate) -> candidate.entry().lastModified()).reversed().thenComparing(byName);
        };
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

    private record QueryCandidate(
            StoredStackKey key,
            StoredStackEntry entry,
            ItemStack stack,
            String displayName,
            String displayNameLower,
            String registryNameLower
    ) {
    }
}
