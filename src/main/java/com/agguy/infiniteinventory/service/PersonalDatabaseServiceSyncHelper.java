package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.network.DatabaseAmountSyncPayload;
import com.agguy.infiniteinventory.database.DatabaseStorageSavedData;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabDirectory;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackEntry;
import com.agguy.infiniteinventory.database.StoredStackKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 负责 {@link PersonalDatabaseService} 中的数量同步与多玩家视图刷新。
 *
 * <p>设计意图：将同步相关的独立职责从服务门面中剥离，降低原类复杂度。
 * 包含数量同步、公共仓库视图刷新、全量视图刷新及未解析条目通知。</p>
 *
 * <p>本类为包级可见，仅由 PersonalDatabaseService 委托调用。</p>
 */
final class PersonalDatabaseServiceSyncHelper {

    private PersonalDatabaseServiceSyncHelper() {
    }

    /**
     * 将玩家在个人与公共仓库中的物品数量及所属分类同步到客户端，使其悬浮提示显示可用数量。
     *
     * @param service 服务实例
     * @param player  目标玩家
     */
    static void syncAmountsToPlayer(PersonalDatabaseService service, ServerPlayer player) {
        List<DatabaseAmountSyncPayload.AmountEntry> personalEntries = buildAmountEntries(service, player, DatabaseScope.PERSONAL);
        List<DatabaseAmountSyncPayload.AmountEntry> publicEntries = buildAmountEntries(service, player, DatabaseScope.PUBLIC);
        PacketDistributor.sendToPlayer(player, new DatabaseAmountSyncPayload(personalEntries, publicEntries));
    }

    private static List<DatabaseAmountSyncPayload.AmountEntry> buildAmountEntries(PersonalDatabaseService service, ServerPlayer player, DatabaseScope scope) {
        List<DatabaseAmountSyncPayload.AmountEntry> result = new ArrayList<>();
        StoredItemDatabase database = service.resolveDatabaseForView(player, scope);
        DatabaseTabDirectory tabDirectory = service.resolveTabsForView(player, scope);
        for (Map.Entry<StoredStackKey, StoredStackEntry> entry : database.entries().entrySet()) {
            String tabName = resolveTabDisplayName(tabDirectory, entry.getValue().tabId());
            result.add(new DatabaseAmountSyncPayload.AmountEntry(
                    entry.getKey().displayStack(),
                    tabName,
                    entry.getValue().amount()
            ));
        }
        return result;
    }

    private static String resolveTabDisplayName(DatabaseTabDirectory tabDirectory, String tabId) {
        DatabaseTab tab = tabDirectory.find(tabId).orElse(null);
        if (tab == null) {
            return tabId;
        }
        if (!tab.customName().isBlank()) {
            return tab.customName();
        }
        if (!tab.translationKey().isBlank()) {
            return Component.translatable(tab.translationKey()).getString();
        }
        return tabId;
    }

    /**
     * 同步所有正在查看公共仓库的玩家视图。
     *
     * @param server 当前 Minecraft 服务端实例
     */
    static void syncPublicViewers(MinecraftServer server) {
        PersonalDatabaseServiceViewerHelper.syncPublicViewers(server);
    }

    /**
     * 同步所有正在查看无限仓库的玩家视图（含个人与公共）。
     *
     * @param server 当前 Minecraft 服务端实例
     */
    static void syncAllViewers(MinecraftServer server) {
        PersonalDatabaseServiceViewerHelper.syncAllViewers(server);
    }

    /**
     * 同步所有玩家视图，并额外通知当前正在查看仓库的玩家其所在作用域的变更。
     *
     * @param server 当前 Minecraft 服务端实例
     */
    static void syncAllViewersAndNotifyCurrentScope(MinecraftServer server) {
        PersonalDatabaseServiceViewerHelper.syncAllViewersAndNotifyCurrentScope(server);
    }

    /**
     * 通知指定玩家当前作用域下是否存在未解析的日志条目（如其他玩家造成的变更）。
     *
     * @param player 目标玩家
     * @param scope  当前作用域
     */
    static void notifyViewerAboutUnresolvedEntries(ServerPlayer player, DatabaseScope scope) {
        PersonalDatabaseServiceViewerHelper.notifyViewerAboutUnresolvedEntries(player, scope);
    }
}
