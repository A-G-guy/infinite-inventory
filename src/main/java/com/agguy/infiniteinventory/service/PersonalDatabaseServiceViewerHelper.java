package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseStorageSavedData;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.menu.PersonalDatabaseMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

final class PersonalDatabaseServiceViewerHelper {
    private PersonalDatabaseServiceViewerHelper() {
    }

    static void syncPublicViewers(MinecraftServer server) {
        syncViewers(server, true, false);
    }

    static void syncAllViewers(MinecraftServer server) {
        syncViewers(server, false, false);
    }

    static void syncAllViewersAndNotifyCurrentScope(MinecraftServer server) {
        syncViewers(server, false, true);
    }

    static void notifyViewerAboutUnresolvedEntries(ServerPlayer player, DatabaseScope scope) {
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

    private static void syncViewers(MinecraftServer server, boolean publicOnly, boolean notifyCurrentScope) {
        for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
            if (onlinePlayer.containerMenu instanceof PersonalDatabaseMenu menu
                    && (!publicOnly || queryIncludesPublicScope(menu.viewState().query()))) {
                menu.syncViewToClient();
                if (notifyCurrentScope) {
                    notifyViewerAboutUnresolvedEntries(onlinePlayer, menu.activeScope());
                }
            }
            PersonalDatabaseServiceJeiHelper.syncAmountsToPlayer(onlinePlayer);
        }
    }

    private static boolean queryIncludesPublicScope(DatabaseQuery query) {
        if (query == null) {
            return false;
        }
        for (DatabaseScopedTabRef visibleTab : query.visibleTabs()) {
            if (visibleTab.scope() == DatabaseScope.PUBLIC) {
                return true;
            }
        }
        return false;
    }
}
