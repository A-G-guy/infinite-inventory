package com.agguy.infiniteinventory.client;

import com.agguy.infiniteinventory.localization.ViewerLanguage;
import com.agguy.infiniteinventory.database.DatabaseViewState;
import com.agguy.infiniteinventory.menu.PersonalDatabaseMenu;
import com.agguy.infiniteinventory.network.DatabaseViewerLocalePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PersonalDatabaseClient {
    private static int lastSyncedContainerId = Integer.MIN_VALUE;
    private static long lastSyncedSessionId = Long.MIN_VALUE;
    private static ViewerLanguage lastSyncedViewerLanguage;

    private PersonalDatabaseClient() {
    }

    public static void applySnapshot(DatabaseViewState viewState) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (minecraft.player.containerMenu instanceof PersonalDatabaseMenu menu
                && menu.containerId == viewState.containerId()
                && menu.sessionId() == viewState.sessionId()) {
            menu.applyViewState(viewState);
        }
    }

    public static void syncViewerLanguageIfNeeded() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(minecraft.player.containerMenu instanceof PersonalDatabaseMenu menu)) {
            clearViewerLanguageSyncState();
            return;
        }
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null) {
            clearViewerLanguageSyncState();
            return;
        }
        ViewerLanguage viewerLanguage = ViewerLanguage.resolve(minecraft.getLanguageManager().getSelected());
        if (!shouldSendViewerLanguageUpdate(
                menu.containerId,
                menu.sessionId(),
                viewerLanguage,
                lastSyncedContainerId,
                lastSyncedSessionId,
                lastSyncedViewerLanguage,
                connection.hasChannel(DatabaseViewerLocalePayload.TYPE)
        )) {
            return;
        }
        PacketDistributor.sendToServer(new DatabaseViewerLocalePayload(
                menu.containerId,
                menu.sessionId(),
                viewerLanguage.code()
        ));
        lastSyncedContainerId = menu.containerId;
        lastSyncedSessionId = menu.sessionId();
        lastSyncedViewerLanguage = viewerLanguage;
    }

    static boolean shouldSendViewerLanguageUpdate(
            int containerId,
            long sessionId,
            ViewerLanguage viewerLanguage,
            int lastContainerId,
            long lastSessionId,
            @Nullable ViewerLanguage lastViewerLanguage,
            boolean viewerLocaleChannelAvailable
    ) {
        if (!viewerLocaleChannelAvailable) {
            return false;
        }
        return containerId != lastContainerId
                || sessionId != lastSessionId
                || viewerLanguage != lastViewerLanguage;
    }

    private static void clearViewerLanguageSyncState() {
        lastSyncedContainerId = Integer.MIN_VALUE;
        lastSyncedSessionId = Long.MIN_VALUE;
        lastSyncedViewerLanguage = null;
    }
}
