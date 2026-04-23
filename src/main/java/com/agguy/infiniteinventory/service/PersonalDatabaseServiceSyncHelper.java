package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseStorageSavedData;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackEntry;
import com.agguy.infiniteinventory.database.StoredStackKey;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 负责 {@link PersonalDatabaseService} 中的 JEI 数量同步与多玩家视图刷新。
 *
 * <p>设计意图：将同步相关的独立职责从服务门面中剥离，降低原类复杂度。
 * 包含 JEI 数量同步、公共仓库视图刷新、全量视图刷新及未解析条目通知。</p>
 *
 * <p>本类为包级可见，仅由 PersonalDatabaseService 委托调用。</p>
 */
final class PersonalDatabaseServiceSyncHelper {

    private PersonalDatabaseServiceSyncHelper() {
    }

    /**
     * 将玩家在个人与公共仓库中的物品数量同步到 JEI，使其配方界面显示可用数量。
     *
     * @param service 服务实例
     * @param player  目标玩家
     */
    static void syncJeiAmountsToPlayer(PersonalDatabaseService service, ServerPlayer player) {
        Map<ItemStack, Long> personalAmounts = new LinkedHashMap<>();
        Map<ItemStack, Long> publicAmounts = new LinkedHashMap<>();
        for (Map.Entry<StoredStackKey, StoredStackEntry> entry : service.resolveDatabaseForView(player, DatabaseScope.PERSONAL).entries().entrySet()) {
            personalAmounts.put(entry.getKey().displayStack(), entry.getValue().amount());
        }
        for (Map.Entry<StoredStackKey, StoredStackEntry> entry : service.resolveDatabaseForView(player, DatabaseScope.PUBLIC).entries().entrySet()) {
            publicAmounts.put(entry.getKey().displayStack(), entry.getValue().amount());
        }
        com.agguy.infiniteinventory.compat.jei.JeiCompat.syncAmounts(player, personalAmounts, publicAmounts);
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
