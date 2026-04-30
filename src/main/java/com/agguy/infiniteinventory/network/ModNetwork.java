package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.client.ClientPayloadHandlers;
import com.agguy.infiniteinventory.compat.AccessoriesCompat;
import com.agguy.infiniteinventory.compat.CuriosCompat;
import com.agguy.infiniteinventory.database.DatabaseLogEntry;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.localization.ViewerLanguage;
import com.agguy.infiniteinventory.menu.PersonalDatabaseMenu;
import com.agguy.infiniteinventory.registry.ModItems;
import com.agguy.infiniteinventory.service.PersonalDatabaseService;
import com.agguy.infiniteinventory.service.PersonalDatabaseTransferHelper;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

public final class ModNetwork {
    private static final String NETWORK_VERSION = "20";

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        PayloadRegistrar optionalRegistrar = registrar.optional();
        registrar.playToClient(DatabaseSnapshotPayload.TYPE, DatabaseSnapshotPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ClientPayloadHandlers.handleSnapshot(payload)));
        registrar.playToClient(DatabaseAmountSyncPayload.TYPE, DatabaseAmountSyncPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ClientPayloadHandlers.handleAmountSync(payload)));
        registrar.playToClient(DatabaseAmountDeltaSyncPayload.TYPE, DatabaseAmountDeltaSyncPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ClientPayloadHandlers.handleAmountDeltaSync(payload)));
        optionalRegistrar.playToServer(DatabaseViewerLocalePayload.TYPE, DatabaseViewerLocalePayload.STREAM_CODEC, ModNetwork::handleViewerLocale);
        registrar.playToServer(DatabaseQueryPayload.TYPE, DatabaseQueryPayload.STREAM_CODEC, ModNetwork::handleQuery);
        registrar.playToServer(DatabaseEnhancementPayload.TYPE, DatabaseEnhancementPayload.STREAM_CODEC, ModNetwork::handleEnhancementConfig);
        registrar.playToServer(DatabaseClickPayload.TYPE, DatabaseClickPayload.STREAM_CODEC, ModNetwork::handleDatabaseClick);
        registrar.playToServer(DatabaseSelectionPayload.TYPE, DatabaseSelectionPayload.STREAM_CODEC, ModNetwork::handleDatabaseSelection);
        registrar.playToServer(DatabaseQuickDepositPayload.TYPE, DatabaseQuickDepositPayload.STREAM_CODEC, ModNetwork::handleQuickDeposit);
        registrar.playToServer(DatabaseTabMutationPayload.TYPE, DatabaseTabMutationPayload.STREAM_CODEC, ModNetwork::handleTabMutation);
        registrar.playToServer(DepositAllPayload.TYPE, DepositAllPayload.STREAM_CODEC, ModNetwork::handleDepositAll);
        registrar.playToServer(DepositExistingByTabPayload.TYPE, DepositExistingByTabPayload.STREAM_CODEC, ModNetwork::handleDepositExistingByTab);
        registrar.playToServer(OpenEquippedDatabasePayload.TYPE, OpenEquippedDatabasePayload.STREAM_CODEC, ModNetwork::handleOpenEquippedDatabase);
        registrar.playToServer(DatabaseLogRequestPayload.TYPE, DatabaseLogRequestPayload.STREAM_CODEC, ModNetwork::handleLogRequest);
        registrar.playToServer(DatabaseStatisticsRequestPayload.TYPE, DatabaseStatisticsRequestPayload.STREAM_CODEC, ModNetwork::handleStatisticsRequest);
        registrar.playToServer(DatabaseNotePayload.TYPE, DatabaseNotePayload.STREAM_CODEC, ModNetwork::handleNoteUpdate);
        registrar.playToServer(DatabaseStarPayload.TYPE, DatabaseStarPayload.STREAM_CODEC, ModNetwork::handleStarToggle);
        registrar.playToClient(DatabaseLogSnapshotPayload.TYPE, DatabaseLogSnapshotPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ClientPayloadHandlers.handleLogSnapshot(payload)));
        registrar.playToClient(DatabaseStatisticsSnapshotPayload.TYPE, DatabaseStatisticsSnapshotPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ClientPayloadHandlers.handleStatisticsSnapshot(payload)));
        registrar.playToClient(DatabaseDepositConflictPayload.TYPE, DatabaseDepositConflictPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ClientPayloadHandlers.handleDepositConflict(payload)));
        registrar.playToServer(DatabaseDepositResolvePayload.TYPE, DatabaseDepositResolvePayload.STREAM_CODEC, ModNetwork::handleDepositResolve);
    }

    private static void handleViewerLocale(DatabaseViewerLocalePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            PersonalDatabaseMenu menu = resolveMenu(player, payload.containerId(), payload.sessionId());
            if (menu != null) {
                menu.updateViewerLanguage(ViewerLanguage.resolve(payload.languageCode()));
            }
        });
    }

    private static void handleQuery(DatabaseQueryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            PersonalDatabaseMenu menu = resolveMenu(player, payload.containerId(), payload.sessionId());
            if (menu != null) {
                menu.updateQuery(payload.query());
            }
        });
    }

    private static void handleEnhancementConfig(DatabaseEnhancementPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            PersonalDatabaseMenu menu = resolveMenu(player, payload.containerId(), payload.sessionId());
            if (menu != null) {
                menu.updateEnhancementConfig(payload.enhancementConfig(), payload.autoStoreTarget());
            }
        });
    }

    private static void handleDatabaseClick(DatabaseClickPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
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
        });
    }

    private static void handleDatabaseSelection(DatabaseSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
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
        });
    }

    private static void handleQuickDeposit(DatabaseQuickDepositPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            PersonalDatabaseMenu menu = resolveMenu(player, payload.containerId(), payload.sessionId());
            if (menu != null) {
                menu.depositInventorySlot(payload.slotIndex(), payload.targetScope(), payload.targetTabId());
            }
        });
    }

    private static void handleTabMutation(DatabaseTabMutationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
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
                case TOGGLE_TOP_VISIBILITY -> PersonalDatabaseService.INSTANCE.toggleTopTabVisibility(player, payload.scope(), payload.tabId());
            };
            if (!changed) {
                return;
            }
            if (payload.action() == DatabaseTabMutationAction.TOGGLE_TOP_VISIBILITY) {
                com.agguy.infiniteinventory.database.DatabaseViewPreferencesAttachment preferences = PersonalDatabaseService.INSTANCE.getViewPreferences(player);
                menu.updateQuery(preferences.query());
                return;
            }
            if (payload.action() == DatabaseTabMutationAction.TRANSFER) {
                syncAfterTransfer(menu, player, payload.scope(), payload.resolvedTargetScope());
                return;
            }
            if (payload.scope() == DatabaseScope.PUBLIC) {
                PersonalDatabaseService.INSTANCE.syncPublicViewers(player.server);
                menu.syncViewToClient();
            } else {
                menu.syncViewToClient();
            }
        });
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
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            PersonalDatabaseMenu menu = resolveMenu(player, payload.containerId(), payload.sessionId());
            if (menu != null) {
                menu.depositAllFromMainInventory(payload.targetScope(), payload.targetTabId());
            }
        });
    }

    private static void handleDepositExistingByTab(DepositExistingByTabPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            PersonalDatabaseMenu menu = resolveMenu(player, payload.containerId(), payload.sessionId());
            if (menu != null) {
                menu.depositExistingByTab(payload.targetScope());
            }
        });
    }

    private static void handleOpenEquippedDatabase(OpenEquippedDatabasePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            boolean backEquipped = AccessoriesCompat.isBackSlotEquipped(player, ModItems.DATABASE_ACCESS_ITEM.get())
                    || CuriosCompat.isBackSlotEquipped(player, ModItems.DATABASE_ACCESS_ITEM.get());
            if (!backEquipped) {
                return;
            }
            PersonalDatabaseService.INSTANCE.open(player);
        });
    }

    private static void handleLogRequest(DatabaseLogRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            DatabaseScope scope = DatabaseScope.normalize(payload.scope());
            if (scope == DatabaseScope.PUBLIC && !player.hasPermissions(2)) {
                context.reply(new DatabaseLogSnapshotPayload(scope, List.of()));
                return;
            }
            List<DatabaseLogEntry> entries = PersonalDatabaseService.INSTANCE.getLogEntries(player, scope);
            context.reply(new DatabaseLogSnapshotPayload(scope, entries));
        });
    }

    private static void handleStatisticsRequest(DatabaseStatisticsRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            DatabaseScope scope = DatabaseScope.normalize(payload.scope());
            if (scope == DatabaseScope.PUBLIC && !player.hasPermissions(2)) {
                context.reply(new DatabaseStatisticsSnapshotPayload(
                        new com.agguy.infiniteinventory.database.statistics.DatabaseStatisticsSnapshot(
                                scope, 0, 0, List.of(), List.of(), List.of(), List.of(), List.of()
                        )
                ));
                return;
            }
            var snapshot = PersonalDatabaseService.INSTANCE.getStatisticsSnapshot(player, scope);
            context.reply(new DatabaseStatisticsSnapshotPayload(snapshot));
        });
    }

    private static void handleStarToggle(DatabaseStarPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            PersonalDatabaseMenu menu = resolveMenu(player, payload.containerId(), payload.sessionId());
            if (menu != null) {
                menu.handleStarAction(payload.scope(), payload.targetStacks(), payload.action());
            }
        });
    }

    private static void handleNoteUpdate(DatabaseNotePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            PersonalDatabaseMenu menu = resolveMenu(player, payload.containerId(), payload.sessionId());
            if (menu != null) {
                menu.handleNoteUpdate(payload.scope(), payload.targetStacks(), payload.note());
            }
        });
    }

    private static void handleDepositResolve(DatabaseDepositResolvePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            PersonalDatabaseMenu menu = resolveMenu(player, payload.containerId(), payload.sessionId());
            if (menu != null) {
                menu.resolveDepositConflict(
                        payload.scope(),
                        payload.targetTabId(),
                        payload.existingTabId(),
                        payload.action(),
                        payload.slotIndex(),
                        payload.stack()
                );
            }
        });
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
