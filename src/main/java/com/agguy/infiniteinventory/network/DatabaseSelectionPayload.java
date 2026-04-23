package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSelectionEntry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record DatabaseSelectionPayload(
        int containerId,
        long sessionId,
        DatabaseSelectionAction action,
        @Nullable DatabaseScope targetScope,
        String targetTabId,
        long requestedAmount,
        List<DatabaseSelectionEntry> selectedEntries
) implements CustomPacketPayload {
    public static final Type<DatabaseSelectionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(InfiniteInventory.MODID, "database_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DatabaseSelectionPayload> STREAM_CODEC = StreamCodec.of(
            DatabaseSelectionPayload::write,
            DatabaseSelectionPayload::read
    );

    public DatabaseSelectionPayload {
        targetTabId = targetTabId == null ? "" : targetTabId;
        requestedAmount = action == null ? 0L : action.normalizeRequestedAmount(requestedAmount);
        selectedEntries = selectedEntries == null ? List.of() : List.copyOf(selectedEntries);
    }

    public DatabaseSelectionPayload(
            int containerId,
            long sessionId,
            DatabaseSelectionAction action,
            String targetTabId,
            long requestedAmount,
            List<DatabaseSelectionEntry> selectedEntries
    ) {
        this(containerId, sessionId, action, null, targetTabId, requestedAmount, selectedEntries);
    }

    public DatabaseSelectionPayload(
            int containerId,
            long sessionId,
            DatabaseSelectionAction action,
            @Nullable DatabaseScope targetScope,
            String targetTabId,
            List<DatabaseSelectionEntry> selectedEntries
    ) {
        this(containerId, sessionId, action, targetScope, targetTabId, 0L, selectedEntries);
    }

    public DatabaseSelectionPayload(
            int containerId,
            long sessionId,
            DatabaseSelectionAction action,
            String targetTabId,
            List<DatabaseSelectionEntry> selectedEntries
    ) {
        this(containerId, sessionId, action, null, targetTabId, 0L, selectedEntries);
    }

    public DatabaseScope resolvedTargetScope(DatabaseScope fallbackScope) {
        return this.targetScope == null ? DatabaseScope.normalize(fallbackScope) : DatabaseScope.normalize(this.targetScope);
    }

    @Override
    public Type<DatabaseSelectionPayload> type() {
        return TYPE;
    }

    private static DatabaseSelectionPayload read(RegistryFriendlyByteBuf buffer) {
        int containerId = buffer.readVarInt();
        long sessionId = buffer.readVarLong();
        DatabaseSelectionAction action = buffer.readEnum(DatabaseSelectionAction.class);
        DatabaseScope targetScope = buffer.readBoolean() ? buffer.readEnum(DatabaseScope.class) : null;
        String targetTabId = buffer.readUtf(DatabaseQuery.MAX_TAB_ID_LENGTH);
        long requestedAmount = buffer.readVarLong();
        int selectionCount = buffer.readVarInt();
        NetworkConstants.checkListSize(selectionCount, NetworkConstants.MAX_SELECTION_ENTRY_COUNT, "selectedEntries");
        List<DatabaseSelectionEntry> selectedEntries = new ArrayList<>(selectionCount);
        for (int index = 0; index < selectionCount; index++) {
            selectedEntries.add(DatabaseSelectionEntry.read(buffer));
        }
        return new DatabaseSelectionPayload(
                containerId,
                sessionId,
                action,
                targetScope,
                targetTabId,
                requestedAmount,
                selectedEntries
        );
    }

    private static void write(RegistryFriendlyByteBuf buffer, DatabaseSelectionPayload payload) {
        buffer.writeVarInt(payload.containerId);
        buffer.writeVarLong(payload.sessionId);
        buffer.writeEnum(payload.action);
        buffer.writeBoolean(payload.targetScope != null);
        if (payload.targetScope != null) {
            buffer.writeEnum(payload.targetScope);
        }
        buffer.writeUtf(payload.targetTabId, DatabaseQuery.MAX_TAB_ID_LENGTH);
        buffer.writeVarLong(payload.requestedAmount);
        buffer.writeVarInt(payload.selectedEntries.size());
        for (DatabaseSelectionEntry selectedEntry : payload.selectedEntries) {
            selectedEntry.write(buffer);
        }
    }
}
