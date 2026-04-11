package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.database.DatabaseViewState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DatabaseSnapshotPayload(DatabaseViewState viewState) implements CustomPacketPayload {
    public static final Type<DatabaseSnapshotPayload> TYPE = CustomPacketPayload.createType(InfiniteInventory.MODID + ":database_snapshot");
    public static final StreamCodec<RegistryFriendlyByteBuf, DatabaseSnapshotPayload> STREAM_CODEC = StreamCodec.of(
            DatabaseSnapshotPayload::write,
            DatabaseSnapshotPayload::read
    );

    @Override
    public Type<DatabaseSnapshotPayload> type() {
        return TYPE;
    }

    private static DatabaseSnapshotPayload read(RegistryFriendlyByteBuf buffer) {
        return new DatabaseSnapshotPayload(DatabaseViewState.read(buffer));
    }

    private static void write(RegistryFriendlyByteBuf buffer, DatabaseSnapshotPayload payload) {
        payload.viewState.write(buffer);
    }
}
