package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.database.DatabaseLogEntry;
import com.agguy.infiniteinventory.database.DatabaseScope;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DatabaseLogSnapshotPayload(DatabaseScope scope, List<DatabaseLogEntry> entries) implements CustomPacketPayload {
    public static final Type<DatabaseLogSnapshotPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(InfiniteInventory.MODID, "database_log_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DatabaseLogSnapshotPayload> STREAM_CODEC = StreamCodec.of(
            DatabaseLogSnapshotPayload::write,
            DatabaseLogSnapshotPayload::read
    );

    public DatabaseLogSnapshotPayload {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    @Override
    public Type<DatabaseLogSnapshotPayload> type() {
        return TYPE;
    }

    private static DatabaseLogSnapshotPayload read(RegistryFriendlyByteBuf buffer) {
        DatabaseScope scope = DatabaseScope.read(buffer.readUtf(), DatabaseScope.PERSONAL);
        int count = buffer.readVarInt();
        NetworkConstants.checkListSize(count, NetworkConstants.MAX_LOG_ENTRY_COUNT, "logEntries");
        List<DatabaseLogEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            DatabaseLogEntry entry = DatabaseLogEntry.read(buffer);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return new DatabaseLogSnapshotPayload(scope, entries);
    }

    private static void write(RegistryFriendlyByteBuf buffer, DatabaseLogSnapshotPayload payload) {
        buffer.writeUtf(payload.scope.name());
        buffer.writeVarInt(payload.entries.size());
        for (DatabaseLogEntry entry : payload.entries) {
            entry.write(buffer);
        }
    }
}
