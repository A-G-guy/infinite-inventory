package com.agguy.infiniteinventory.menu;

import com.agguy.infiniteinventory.database.DatabaseAutoStoreTarget;
import com.agguy.infiniteinventory.database.DatabaseEnhancementConfig;
import com.agguy.infiniteinventory.database.DatabasePage;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseViewState;
import com.agguy.infiniteinventory.network.DatabaseSnapshotPayload;
import com.agguy.infiniteinventory.service.PersonalDatabaseService;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

final class PersonalDatabaseMenuSyncHelper {
    private PersonalDatabaseMenuSyncHelper() {
    }

    static void syncViewToClient(PersonalDatabaseMenu menu) {
        if (!(menu.owner instanceof ServerPlayer serverPlayer)) {
            return;
        }
        DatabaseQuery activeQuery = PersonalDatabaseService.INSTANCE.sanitizeQuery(serverPlayer, menu.currentQuery());
        java.util.ArrayList<DatabasePage> rebuiltPages = new java.util.ArrayList<>(activeQuery.visibleTabs().size());
        DatabaseQuery adjustedQuery = activeQuery;
        for (DatabaseScopedTabRef visibleTab : activeQuery.visibleTabs()) {
            DatabasePage page = PersonalDatabaseService.INSTANCE.buildPage(serverPlayer, adjustedQuery, visibleTab, menu.viewerLanguage());
            rebuiltPages.add(page);
            adjustedQuery = adjustedQuery
                    .withPageIndex(page.scopedTab(), page.pageIndex())
                    .withPageSize(page.scopedTab(), page.pageSize());
        }
        menu.currentPages = List.copyOf(rebuiltPages);
        menu.setActiveQuery(adjustedQuery);
        menu.autoStoreTarget = PersonalDatabaseService.INSTANCE.resolveAutoStoreTarget(serverPlayer);
        menu.persistPreferences(serverPlayer);
        menu.viewState = new DatabaseViewState(
                menu.containerId,
                menu.sessionId,
                menu.query,
                menu.enhancementConfig,
                menu.autoStoreTarget,
                PersonalDatabaseService.INSTANCE.tabsForScope(serverPlayer, DatabaseScope.PERSONAL),
                PersonalDatabaseService.INSTANCE.tabsForScope(serverPlayer, DatabaseScope.PUBLIC),
                menu.currentPages.stream().map(DatabasePage::toPanelView).toList()
        );
        PacketDistributor.sendToPlayer(serverPlayer, new DatabaseSnapshotPayload(menu.viewState));
    }

    static void updateQuery(PersonalDatabaseMenu menu, DatabaseQuery newQuery) {
        DatabaseScope previousScope = menu.query.scope();
        menu.setActiveQuery(newQuery == null ? menu.currentQuery() : newQuery);
        if (menu.owner instanceof ServerPlayer serverPlayer) {
            menu.persistPreferences(serverPlayer);
        }
        syncViewToClient(menu);
        if (menu.owner instanceof ServerPlayer serverPlayer && previousScope != menu.query.scope()) {
            PersonalDatabaseService.INSTANCE.notifyViewerAboutUnresolvedEntries(serverPlayer, menu.query.scope());
        }
    }

    static void updateEnhancementConfig(
            PersonalDatabaseMenu menu,
            DatabaseEnhancementConfig newConfig,
            DatabaseAutoStoreTarget newAutoStoreTarget
    ) {
        menu.enhancementConfig = newConfig == null ? DatabaseEnhancementConfig.defaultConfig() : newConfig;
        menu.autoStoreTarget = newAutoStoreTarget == null ? DatabaseAutoStoreTarget.defaultTarget() : newAutoStoreTarget;
        if (menu.owner instanceof ServerPlayer serverPlayer) {
            menu.persistPreferences(serverPlayer);
        }
        syncViewToClient(menu);
    }

    static void syncAfterScopeMutation(PersonalDatabaseMenu menu, ServerPlayer player, @Nullable DatabaseScope scope) {
        syncAfterScopedMutations(
                menu,
                player,
                DatabaseScope.normalize(scope) == DatabaseScope.PERSONAL,
                DatabaseScope.normalize(scope) == DatabaseScope.PUBLIC
        );
    }

    static void syncAfterScopedMutations(
            PersonalDatabaseMenu menu,
            ServerPlayer player,
            boolean personalChanged,
            boolean publicChanged
    ) {
        if (personalChanged) {
            syncViewToClient(menu);
        }
        if (publicChanged) {
            PersonalDatabaseService.INSTANCE.syncPublicViewers(player.server);
        }
    }
}
