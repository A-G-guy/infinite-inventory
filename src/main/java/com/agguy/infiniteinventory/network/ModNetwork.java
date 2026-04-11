package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.client.PersonalDatabaseClient;
import com.agguy.infiniteinventory.menu.PersonalDatabaseMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

public final class ModNetwork {
    private static final String NETWORK_VERSION = "1";

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToClient(DatabaseSnapshotPayload.TYPE, DatabaseSnapshotPayload.STREAM_CODEC, ModNetwork::handleSnapshot);
        registrar.playToServer(DatabaseQueryPayload.TYPE, DatabaseQueryPayload.STREAM_CODEC, ModNetwork::handleQuery);
        registrar.playToServer(DatabaseClickPayload.TYPE, DatabaseClickPayload.STREAM_CODEC, ModNetwork::handleDatabaseClick);
        registrar.playToServer(DepositAllPayload.TYPE, DepositAllPayload.STREAM_CODEC, ModNetwork::handleDepositAll);
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
        PersonalDatabaseMenu menu = resolveMenu(player, payload.containerId());
        if (menu != null) {
            menu.updateQuery(payload.query());
        }
    }

    private static void handleDatabaseClick(DatabaseClickPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        PersonalDatabaseMenu menu = resolveMenu(player, payload.containerId());
        if (menu != null) {
            menu.handleDatabaseClick(payload.pageSlotIndex(), payload.action());
        }
    }

    private static void handleDepositAll(DepositAllPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        PersonalDatabaseMenu menu = resolveMenu(player, payload.containerId());
        if (menu != null) {
            menu.depositAllFromMainInventory();
        }
    }

    @Nullable
    private static PersonalDatabaseMenu resolveMenu(ServerPlayer player, int containerId) {
        if (player.containerMenu instanceof PersonalDatabaseMenu menu && menu.containerId == containerId) {
            return menu;
        }
        return null;
    }
}
