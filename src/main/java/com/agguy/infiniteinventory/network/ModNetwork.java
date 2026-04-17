package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.client.PersonalDatabaseClient;
import com.agguy.infiniteinventory.compat.AccessoriesCompat;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.menu.PersonalDatabaseMenu;
import com.agguy.infiniteinventory.registry.ModItems;
import com.agguy.infiniteinventory.service.PersonalDatabaseService;
import com.agguy.infiniteinventory.service.PersonalDatabaseTransferHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

public final class ModNetwork {
    private static final String NETWORK_VERSION = "16";

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToClient(DatabaseSnapshotPayload.TYPE, DatabaseSnapshotPayload.STREAM_CODEC, ModNetwork::handleSnapshot);
        registrar.playToServer(DatabaseQueryPayload.TYPE, DatabaseQueryPayload.STREAM_CODEC, ModNetwork::handleQuery);
        registrar.playToServer(DatabaseEnhancementPayload.TYPE, DatabaseEnhancementPayload.STREAM_CODEC, ModNetwork::handleEnhancementConfig);
        registrar.playToServer(DatabaseClickPayload.TYPE, DatabaseClickPayload.STREAM_CODEC, ModNetwork::handleDatabaseClick);
        registrar.playToServer(DatabaseSelectionPayload.TYPE, DatabaseSelectionPayload.STREAM_CODEC, ModNetwork::handleDatabaseSelection);
        registrar.playToServer(DatabaseQuickDepositPayload.TYPE, DatabaseQuickDepositPayload.STREAM_CODEC, ModNetwork::handleQuickDeposit);
        registrar.playToServer(DatabaseTabMutationPayload.TYPE, DatabaseTabMutationPayload.STREAM_CODEC, ModNetwork::handleTabMutation);
        registrar.playToServer(DepositAllPayload.TYPE, DepositAllPayload.STREAM_CODEC, ModNetwork::handleDepositAll);
        registrar.playToServer(OpenEquippedDatabasePayload.TYPE, OpenEquippedDatabasePayload.STREAM_CODEC, ModNetwork::handleOpenEquippedDatabase);
    }

    private static void handleSnapshot(DatabaseSnapshotPayload payload, IPayloadContext context) {
        if (Minecraft.getInstance().player != null) {
            PersonalDatabaseClient.applySnapshot(payload.viewState());
        }
    }

    private static void handleQuery(DatabaseQueryPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        PersonalDatabaseMenu menu = resolveMenu(player, payload.containerId(), payload.sessionId());
        if (menu != null) {
            menu.updateQuery(payload.query());
        }
    }

    private static void handleEnhancementConfig(DatabaseEnhancementPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        PersonalDatabaseMenu menu = resolveMenu(player, payload.containerId(), payload.sessionId());
        if (menu != null) {
            menu.updateEnhancementConfig(payload.enhancementConfig(), payload.autoStoreTarget());
        }
    }

    private static void handleDatabaseClick(DatabaseClickPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        PersonalDatabaseMenu menu = resolveMenu(player, payload.containerId(), payload.sessionId());
        if (menu != null) {
            menu.handleDatabaseClick(
                    payload.panelIndex(),
                    payload.pageSlotIndex(),
                    payload.action(),
                    payload.targetScope(),
                    payload.targetTabId()
            );
        }
    }

    private static void handleDatabaseSelection(DatabaseSelectionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        PersonalDatabaseMenu menu = resolveMenu(player, payload.containerId(), payload.sessionId());
        if (menu != null) {
            menu.handleSelectionAction(
                    payload.action(),
                    payload.selectedEntries(),
                    payload.targetScope(),
                    payload.targetTabId(),
                    payload.requestedAmount()
            );
        }
    }

    private static void handleQuickDeposit(DatabaseQuickDepositPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        PersonalDatabaseMenu menu = resolveMenu(player, payload.containerId(), payload.sessionId());
        if (menu != null) {
            menu.depositInventorySlot(payload.slotIndex(), payload.targetScope(), payload.targetTabId());
        }
    }

    private static void handleTabMutation(DatabaseTabMutationPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        PersonalDatabaseMenu menu = resolveMenu(player, payload.containerId(), payload.sessionId());
        if (menu == null) {
            return;
        }
        boolean changed = switch (payload.action()) {
            case ADD -> PersonalDatabaseService.INSTANCE.createTab(player, payload.scope(), payload.name(), payload.iconItemId());
            case RENAME -> PersonalDatabaseService.INSTANCE.renameTab(player, payload.scope(), payload.tabId(), payload.name());
            case CHANGE_ICON -> PersonalDatabaseService.INSTANCE.updateTabIcon(player, payload.scope(), payload.tabId(), payload.iconItemId());
            case MOVE_LEFT -> PersonalDatabaseService.INSTANCE.moveTab(player, payload.scope(), payload.tabId(), -1);
            case MOVE_RIGHT -> PersonalDatabaseService.INSTANCE.moveTab(player, payload.scope(), payload.tabId(), 1);
            case DELETE -> PersonalDatabaseService.INSTANCE.deleteTab(player, payload.scope(), payload.tabId(), payload.targetTabId());
            case TRANSFER -> PersonalDatabaseService.INSTANCE.transferTab(
                    player,
                    payload.scope(),
                    payload.resolvedTargetScope(),
                    payload.tabId(),
                    payload.targetTabId()
            );
        };
        if (!changed) {
            return;
        }
        if (payload.action() == DatabaseTabMutationAction.TRANSFER) {
            syncAfterTransfer(menu, player, payload.scope(), payload.resolvedTargetScope());
            return;
        }
        if (payload.scope() == DatabaseScope.PUBLIC) {
            PersonalDatabaseService.INSTANCE.syncPublicViewers(player.server);
        } else {
            menu.syncViewToClient();
        }
    }

    private static void syncAfterTransfer(PersonalDatabaseMenu menu, ServerPlayer player, DatabaseScope sourceScope, DatabaseScope targetScope) {
        DatabaseScope normalizedSourceScope = DatabaseScope.normalize(sourceScope);
        DatabaseScope normalizedTargetScope = PersonalDatabaseTransferHelper.resolveTargetScope(sourceScope, targetScope);
        if (!PersonalDatabaseTransferHelper.affectsPublicScope(normalizedSourceScope, normalizedTargetScope)) {
            menu.syncViewToClient();
            return;
        }
        if (normalizedSourceScope != DatabaseScope.PUBLIC) {
            menu.syncViewToClient();
        }
        PersonalDatabaseService.INSTANCE.syncPublicViewers(player.server);
    }

    private static void handleDepositAll(DepositAllPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        PersonalDatabaseMenu menu = resolveMenu(player, payload.containerId(), payload.sessionId());
        if (menu != null) {
            menu.depositAllFromMainInventory(payload.targetScope(), payload.targetTabId());
        }
    }

    private static void handleOpenEquippedDatabase(OpenEquippedDatabasePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!AccessoriesCompat.isBackSlotEquipped(player, ModItems.DATABASE_ACCESS_ITEM.get())) {
            return;
        }
        PersonalDatabaseService.INSTANCE.open(player);
    }

    @Nullable
    private static PersonalDatabaseMenu resolveMenu(ServerPlayer player, int containerId, long sessionId) {
        if (player.containerMenu instanceof PersonalDatabaseMenu menu
                && menu.containerId == containerId
                && menu.sessionId() == sessionId) {
            return menu;
        }
        return null;
    }
}
