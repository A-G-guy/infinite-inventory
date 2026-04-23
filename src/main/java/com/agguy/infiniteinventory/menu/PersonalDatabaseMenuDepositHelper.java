package com.agguy.infiniteinventory.menu;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.service.PersonalDatabaseService;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * 存取操作相关逻辑的委托辅助类。
 *
 * <p>将 {@link PersonalDatabaseMenu} 中“存入全部”与“存入单个槽位”的方法抽取至此，
 * 统一处理目标标签页解析、服务端校验与变更同步。
 */
final class PersonalDatabaseMenuDepositHelper {

    private final PersonalDatabaseMenu menu;

    PersonalDatabaseMenuDepositHelper(PersonalDatabaseMenu menu) {
        this.menu = menu;
    }

    /**
     * 将玩家主背包所有物品存入数据库指定标签页。
     *
     * <p>业务约束：若目标标签页未指定，则按当前自动存储目标解析；
     * 仅当实际存入数量大于 0 时才广播变更。
     *
     * @param targetScope 目标作用域，可为 null（按默认规则解析）
     * @param targetTabId 目标标签页标识
     */
    void depositAllFromMainInventory(@Nullable DatabaseScope targetScope, String targetTabId) {
        DatabaseScopedTabRef targetTab = this.menu.resolveStoreTarget(-1, targetScope, targetTabId);
        if (this.menu.owner instanceof ServerPlayer serverPlayer
                && PersonalDatabaseService.INSTANCE.depositMainInventory(serverPlayer, targetTab.scope(), targetTab.tabId()) > 0L) {
            this.menu.broadcastChanges();
            this.menu.syncAfterScopeMutation(serverPlayer, targetTab.scope());
        }
    }

    /**
     * 将指定背包槽位的物品存入数据库。
     *
     * <p>业务约束：会校验槽位索引有效性与是否允许快捷存入；
     * 若槽位为空或服务端玩家不合法则直接返回。
     *
     * @param slotIndex   背包槽位索引
     * @param targetScope 目标作用域，可为 null
     * @param targetTabId 目标标签页标识
     */
    void depositInventorySlot(int slotIndex, @Nullable DatabaseScope targetScope, String targetTabId) {
        if (!(this.menu.owner instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (slotIndex < 0 || slotIndex >= this.menu.slots.size() || !this.menu.shouldDepositQuickMovedSlot(slotIndex)) {
            return;
        }
        net.minecraft.world.inventory.Slot slot = this.menu.slots.get(slotIndex);
        if (!slot.hasItem()) {
            return;
        }
        DatabaseScopedTabRef targetTab = this.menu.resolveStoreTarget(-1, targetScope, targetTabId);
        if (PersonalDatabaseService.INSTANCE.depositSlot(serverPlayer, targetTab.scope(), targetTab.tabId(), slot)) {
            this.menu.broadcastChanges();
            this.menu.syncAfterScopeMutation(serverPlayer, targetTab.scope());
        }
    }
}
