package com.agguy.infiniteinventory.client;

import com.agguy.infiniteinventory.localization.ViewerLanguage;
import com.agguy.infiniteinventory.database.DatabaseEnhancementConfig;
import com.agguy.infiniteinventory.database.DatabaseLogEntry;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseViewState;
import com.agguy.infiniteinventory.menu.PersonalDatabaseMenu;
import com.agguy.infiniteinventory.network.DatabaseLogSnapshotPayload;
import com.agguy.infiniteinventory.network.DatabaseViewerLocalePayload;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PersonalDatabaseClient {
    private static int lastSyncedContainerId = Integer.MIN_VALUE;
    private static long lastSyncedSessionId = Long.MIN_VALUE;
    private static ViewerLanguage lastSyncedViewerLanguage;
    private static final Map<DatabaseScope, List<DatabaseLogEntry>> cachedLogEntries = new EnumMap<>(DatabaseScope.class);
    private static DatabaseEnhancementConfig lastKnownEnhancementConfig = DatabaseEnhancementConfig.defaultConfig();

    private PersonalDatabaseClient() {
    }

    public static void applySnapshot(DatabaseViewState viewState) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        lastKnownEnhancementConfig = viewState.enhancementConfig();
        if (minecraft.player.containerMenu instanceof PersonalDatabaseMenu menu
                && menu.containerId == viewState.containerId()
                && menu.sessionId() == viewState.sessionId()) {
            menu.applyViewState(viewState);
        }
    }

    public static DatabaseEnhancementConfig lastKnownEnhancementConfig() {
        return lastKnownEnhancementConfig;
    }

    public static void applyLogSnapshot(DatabaseLogSnapshotPayload payload) {
        cachedLogEntries.put(DatabaseScope.normalize(payload.scope()), List.copyOf(payload.entries()));
    }

    public static List<DatabaseLogEntry> getLogEntries(DatabaseScope scope) {
        List<DatabaseLogEntry> entries = cachedLogEntries.get(DatabaseScope.normalize(scope));
        return entries == null ? List.of() : entries;
    }

    public static void clearLogCache() {
        cachedLogEntries.clear();
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
