package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.network.DatabaseAmountDeltaSyncPayload;
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
     * 将玩家在个人与公共仓库中的物品数量及所属分类**增量同步**到客户端。
     *
     * <p>只发送自上次同步以来发生变更的条目。若当前无 pending deltas，则不发送网络包。</p>
     *
     * <p>设计约束：本方法只 drain 与 {@code scope} 对应的数据库，避免将另一作用域的 deltas 误发给当前玩家。
     * 公共数据库因被多玩家共享，仍保持全量同步路径（{@link #syncFullAmountsToPlayer}）。</p>
     *
     * @param service 服务实例
     * @param player  目标玩家
     * @param scope   发生变更的作用域
     */
    /**
     * 将玩家在个人与公共仓库中的物品数量及所属分类**增量同步**到客户端。
     *
     * <p>只发送自上次同步以来发生变更的条目。若当前无 pending deltas，则不发送网络包。</p>
     *
     * <p>设计约束：本方法只 drain 与 {@code scope} 对应的数据库，避免将另一作用域的 deltas 误发给当前玩家。
     * 公共数据库因被多玩家共享，仍保持全量同步路径（{@link #syncFullAmountsToPlayer}）。</p>
     *
     * @param service 服务实例
     * @param player  目标玩家
     * @param scope   发生变更的作用域
     * @return 若实际发送了网络包则返回 {@code true}
     */
    static boolean syncAmountDeltasToPlayer(PersonalDatabaseService service, ServerPlayer player, DatabaseScope scope) {
        if (DatabaseScope.normalize(scope) == DatabaseScope.PUBLIC) {
            syncFullAmountsToPlayer(service, player);
            return true;
        }
        List<DatabaseAmountDeltaSyncPayload.DeltaEntry> personalDeltas = buildDeltaEntries(service, player, DatabaseScope.PERSONAL);
        if (personalDeltas.isEmpty()) {
            return false;
        }
        PacketDistributor.sendToPlayer(player, new DatabaseAmountDeltaSyncPayload(personalDeltas, List.of()));
        return true;
    }

    /**
     * 将玩家在个人与公共仓库中的物品数量及所属分类**全量同步**到客户端。
     *
     * <p>用于登录、强制刷新等需要完整基准数据的场景。</p>
     *
     * @param service 服务实例
     * @param player  目标玩家
     */
    static void syncFullAmountsToPlayer(PersonalDatabaseService service, ServerPlayer player) {
        // 全量同步前清理两个数据库的 pending deltas，防止增量队列无限累积
        StoredItemDatabase personalDb = service.resolveDatabaseForView(player, DatabaseScope.PERSONAL);
        personalDb.drainPendingAmountDeltas();
        StoredItemDatabase publicDb = service.resolveDatabaseForView(player, DatabaseScope.PUBLIC);
        publicDb.drainPendingAmountDeltas();

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

    private static List<DatabaseAmountDeltaSyncPayload.DeltaEntry> buildDeltaEntries(PersonalDatabaseService service, ServerPlayer player, DatabaseScope scope) {
        StoredItemDatabase database = service.resolveDatabaseForView(player, scope);
        List<StoredItemDatabase.AmountDelta> deltas = database.drainPendingAmountDeltas();
        if (deltas.isEmpty()) {
            return List.of();
        }
        DatabaseTabDirectory tabDirectory = service.resolveTabsForView(player, scope);
        List<DatabaseAmountDeltaSyncPayload.DeltaEntry> result = new ArrayList<>(deltas.size());
        for (StoredItemDatabase.AmountDelta delta : deltas) {
            String tabName = resolveTabDisplayName(tabDirectory, delta.tabId());
            result.add(new DatabaseAmountDeltaSyncPayload.DeltaEntry(
                    delta.key().displayStack(),
                    tabName,
                    delta.amount(),
                    delta.removed()
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
