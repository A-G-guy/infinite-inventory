package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseSelectionEntry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DatabaseSelectionPayload(
        int containerId,
        long sessionId,
        DatabaseSelectionAction action,
        String targetTabId,
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
        selectedEntries = selectedEntries == null ? List.of() : List.copyOf(selectedEntries);
    }

    @Override
    public Type<DatabaseSelectionPayload> type() {
        return TYPE;
    }

    private static DatabaseSelectionPayload read(RegistryFriendlyByteBuf buffer) {
        int containerId = buffer.readVarInt();
        long sessionId = buffer.readVarLong();
        DatabaseSelectionAction action = buffer.readEnum(DatabaseSelectionAction.class);
        String targetTabId = buffer.readUtf(DatabaseQuery.MAX_TAB_ID_LENGTH);
        int selectionCount = buffer.readVarInt();
        List<DatabaseSelectionEntry> selectedEntries = new ArrayList<>(selectionCount);
        for (int index = 0; index < selectionCount; index++) {
            selectedEntries.add(DatabaseSelectionEntry.read(buffer));
        }
        return new DatabaseSelectionPayload(
                containerId,
                sessionId,
                action,
                targetTabId,
                selectedEntries
        );
    }

    private static void write(RegistryFriendlyByteBuf buffer, DatabaseSelectionPayload payload) {
        buffer.writeVarInt(payload.containerId);
        buffer.writeVarLong(payload.sessionId);
        buffer.writeEnum(payload.action);
        buffer.writeUtf(payload.targetTabId, DatabaseQuery.MAX_TAB_ID_LENGTH);
        buffer.writeVarInt(payload.selectedEntries.size());
        for (DatabaseSelectionEntry selectedEntry : payload.selectedEntries) {
            selectedEntry.write(buffer);
        }
    }
}
