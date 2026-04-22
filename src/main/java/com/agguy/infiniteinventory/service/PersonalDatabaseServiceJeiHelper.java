package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.compat.jei.JeiCompat;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseStorageSavedData;
import com.agguy.infiniteinventory.database.DatabaseTabDirectory;
import com.agguy.infiniteinventory.database.JeiCraftingTabSourceConfig;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackEntry;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.network.JeiAmountSyncPayload;
import com.agguy.infiniteinventory.network.JeiCraftingExtractPayload;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class PersonalDatabaseServiceJeiHelper {
    private static final Logger LOGGER = LogManager.getLogger();

    private PersonalDatabaseServiceJeiHelper() {
    }

    static void syncAmountsToPlayer(ServerPlayer player) {
        if (!JeiCompat.isAvailable()) {
            return;
        }
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.get(player.server);
        Object2LongMap<ItemStack> personal = buildAmountMap(storage.personalDatabaseView(player.getUUID()));
        Object2LongMap<ItemStack> publicItems = buildAmountMap(storage.publicDatabase());
        PacketDistributor.sendToPlayer(player, JeiAmountSyncPayload.of(personal, publicItems));
    }

    static void syncAmountsToAllPlayers(ServerPlayer player) {
        if (!JeiCompat.isAvailable()) {
            return;
        }
        syncAmountsToPlayer(player);
    }

    static void setJeiCraftingTabSource(ServerPlayer player, DatabaseScope scope, String tabId, boolean enabled) {
        JeiCraftingTabSourceConfig current = PersonalDatabaseService.INSTANCE.getViewPreferences(player).jeiCraftingTabSources();
        JeiCraftingTabSourceConfig updated = current.withTabEnabled(scope, tabId, enabled);
        PersonalDatabaseService.INSTANCE.getViewPreferences(player).setJeiCraftingTabSources(updated);
    }

    static void extractForJeiCrafting(ServerPlayer player, java.util.List<JeiCraftingExtractPayload.MaterialGap> gaps) {
        if (gaps == null || gaps.isEmpty()) {
            return;
        }
        StoredItemDatabase personalDb = PersonalDatabaseService.INSTANCE.resolveDatabaseForMutation(player, DatabaseScope.PERSONAL);
        StoredItemDatabase publicDb = PersonalDatabaseService.INSTANCE.resolveDatabaseForMutation(player, DatabaseScope.PUBLIC);
        DatabaseTabDirectory personalTabs = PersonalDatabaseService.INSTANCE.resolveTabsForMutation(player, DatabaseScope.PERSONAL);
        DatabaseTabDirectory publicTabs = PersonalDatabaseService.INSTANCE.resolveTabsForMutation(player, DatabaseScope.PUBLIC);
        JeiCraftingTabSourceConfig config = PersonalDatabaseService.INSTANCE.getViewPreferences(player).jeiCraftingTabSources();

        for (JeiCraftingExtractPayload.MaterialGap gap : gaps) {
            if (gap.needed() <= 0) {
                continue;
            }
            int remaining = gap.needed();
            remaining -= tryExtractFromDatabase(player, personalDb, personalTabs, config, DatabaseScope.PERSONAL, gap.stack(), remaining);
            if (remaining > 0) {
                remaining -= tryExtractFromDatabase(player, publicDb, publicTabs, config, DatabaseScope.PUBLIC, gap.stack(), remaining);
            }
            if (remaining > 0) {
                LOGGER.debug("JEI 合成提取不足：{} 仍需 {}", gap.stack().getItem(), remaining);
            }
        }
    }

    private static int tryExtractFromDatabase(
            ServerPlayer player,
            StoredItemDatabase database,
            DatabaseTabDirectory tabDirectory,
            JeiCraftingTabSourceConfig config,
            DatabaseScope scope,
            ItemStack neededStack,
            int neededAmount
    ) {
        if (neededAmount <= 0) {
            return 0;
        }
        Set<String> enabledTabs = new HashSet<>(config.enabledTabIdsFor(scope));
        if (enabledTabs.isEmpty()) {
            return 0;
        }
        long totalAvailable = 0L;
        for (java.util.Map.Entry<StoredStackKey, StoredStackEntry> entry : database.entries().entrySet()) {
            if (!enabledTabs.contains(entry.getValue().tabId())) {
                continue;
            }
            ItemStack display = entry.getKey().displayStack();
            if (ItemStack.isSameItemSameComponents(display, neededStack)) {
                totalAvailable += entry.getValue().amount();
            }
        }
        if (totalAvailable <= 0L) {
            return 0;
        }
        int toExtract = (int) Math.min(neededAmount, totalAvailable);
        int extracted = 0;
        for (java.util.Map.Entry<StoredStackKey, StoredStackEntry> entry : database.entries().entrySet()) {
            if (!enabledTabs.contains(entry.getValue().tabId())) {
                continue;
            }
            ItemStack display = entry.getKey().displayStack();
            if (!ItemStack.isSameItemSameComponents(display, neededStack)) {
                continue;
            }
            int extractNow = (int) Math.min(toExtract - extracted, entry.getValue().amount());
            if (extractNow <= 0) {
                continue;
            }
            long moved = extractToInventory(player, scope, database, entry.getKey(), extractNow);
            extracted += moved;
            if (extracted >= toExtract) {
                break;
            }
        }
        return extracted;
    }

    private static long extractToInventory(ServerPlayer player, DatabaseScope scope, StoredItemDatabase database, StoredStackKey key, int amount) {
        return PersonalDatabaseExtractionHelper.extractToInventory(
                PersonalDatabaseService.INSTANCE, player, scope, database, key, amount
        );
    }

    private static Object2LongMap<ItemStack> buildAmountMap(StoredItemDatabase database) {
        Object2LongMap<ItemStack> map = new Object2LongOpenHashMap<>();
        for (java.util.Map.Entry<StoredStackKey, StoredStackEntry> entry : database.entries().entrySet()) {
            map.put(entry.getKey().displayStack(), entry.getValue().amount());
        }
        return map;
    }
}
