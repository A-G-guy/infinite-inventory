package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.StoredStackKey;
import net.minecraft.server.level.ServerPlayer;

/**
 * 负责 {@link PersonalDatabaseService} 中的收藏（星标）与备注管理。
 *
 * <p>设计意图：将星标/备注的增删查改独立职责从服务门面中剥离，降低原类复杂度。
 * 备注按物品键维度存储，同一物品在不同标签页共享同一条备注。</p>
 *
 * <p>本类为包级可见，仅由 PersonalDatabaseService 委托调用。</p>
 */
final class PersonalDatabaseServiceStarNoteHelper {

    private PersonalDatabaseServiceStarNoteHelper() {
    }

    /**
     * 为指定物品设置备注文本。
     *
     * @param service 服务实例
     * @param player  执行操作的玩家
     * @param scope   物品所在作用域
     * @param key     物品唯一键；若为 null 则直接返回，不做任何操作
     * @param note    备注内容；可为空字符串
     */
    static void setNote(PersonalDatabaseService service, ServerPlayer player, DatabaseScope scope, StoredStackKey key, String note) {
        if (key == null) {
            return;
        }
        service.resolveDatabaseForMutation(player, scope).setNote(key, note);
        service.markScopeDirty(player, scope);
    }

    /**
     * 查询指定物品的当前备注文本。
     *
     * @param service 服务实例
     * @param player  请求玩家
     * @param scope   物品所在作用域
     * @param key     物品唯一键；若为 null 返回空字符串
     * @return 当前备注内容；无备注时返回空字符串
     */
    static String noteFor(PersonalDatabaseService service, ServerPlayer player, DatabaseScope scope, StoredStackKey key) {
        return key == null ? "" : service.resolveDatabaseForView(player, scope).noteFor(key);
    }

    /**
     * 切换指定物品的星标状态。
     *
     * @param service 服务实例
     * @param player  执行操作的玩家
     * @param scope   物品所在作用域
     * @param key     物品唯一键；若为 null 返回 false
     * @return 若星标状态发生实际变化返回 true；否则返回 false
     */
    static boolean toggleStar(PersonalDatabaseService service, ServerPlayer player, DatabaseScope scope, StoredStackKey key) {
        if (key == null) {
            return false;
        }
        boolean changed = service.resolveDatabaseForMutation(player, scope).toggleStar(key);
        if (changed) {
            service.markScopeDirty(player, scope);
        }
        return changed;
    }

    /**
     * 显式设置指定物品的星标状态（而非切换）。
     *
     * @param service 服务实例
     * @param player  执行操作的玩家
     * @param scope   物品所在作用域
     * @param key     物品唯一键；若为 null 返回 false
     * @param starred 目标星标状态：true 为星标，false 为取消星标
     * @return 若星标状态发生实际变化返回 true；否则返回 false
     */
    static boolean setStarred(PersonalDatabaseService service, ServerPlayer player, DatabaseScope scope, StoredStackKey key, boolean starred) {
        if (key == null) {
            return false;
        }
        boolean changed = service.resolveDatabaseForMutation(player, scope).setStarred(key, starred);
        if (changed) {
            service.markScopeDirty(player, scope);
        }
        return changed;
    }

    /**
     * 判断指定物品是否已被星标。
     *
     * @param service 服务实例
     * @param player  请求玩家
     * @param scope   物品所在作用域
     * @param key     物品唯一键；若为 null 返回 false
     * @return 已星标返回 true；否则返回 false
     */
    static boolean isStarred(PersonalDatabaseService service, ServerPlayer player, DatabaseScope scope, StoredStackKey key) {
        return key != null && service.resolveDatabaseForView(player, scope).isStarred(key);
    }
}
