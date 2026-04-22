package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabaseLogAction;
import com.agguy.infiniteinventory.database.DatabaseLogEntry;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

final class PersonalDatabaseServiceLogHelper {
    private PersonalDatabaseServiceLogHelper() {
    }

    static void recordLog(
            PersonalDatabaseService service,
            ServerPlayer player,
            DatabaseScope scope,
            DatabaseLogAction action,
            ItemStack stack,
            long amount,
            String sourceTabId,
            String targetTabId,
            DatabaseScope relatedScope
    ) {
        if (player == null || stack == null || stack.isEmpty() || amount <= 0L) {
            return;
        }
        StoredItemDatabase database = service.resolveDatabaseForMutation(player, scope);
        database.appendLogEntry(new DatabaseLogEntry(
                System.currentTimeMillis(),
                player.getUUID(),
                player.getGameProfile().getName(),
                action,
                stack.copyWithCount(1),
                amount,
                sourceTabId == null ? "" : sourceTabId,
                targetTabId == null ? "" : targetTabId,
                relatedScope
        ));
    }
}
