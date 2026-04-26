package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record DatabaseDepositResolvePayload(
        int containerId,
        long sessionId,
        @Nullable DatabaseScope scope,
        String targetTabId,
        String existingTabId,
        ItemStack stack,
        int slotIndex,
        DepositConflictAction action
) implements CustomPacketPayload {
    public static final Type<DatabaseDepositResolvePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(InfiniteInventory.MODID, "deposit_resolve"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DatabaseDepositResolvePayload> STREAM_CODEC = StreamCodec.of(
            DatabaseDepositResolvePayload::write,
            DatabaseDepositResolvePayload::read
    );

    @Override
    public Type<DatabaseDepositResolvePayload> type() {
        return TYPE;
    }

    private static DatabaseDepositResolvePayload read(RegistryFriendlyByteBuf buffer) {
        return new DatabaseDepositResolvePayload(
                buffer.readVarInt(),
                buffer.readVarLong(),
                buffer.readBoolean() ? buffer.readEnum(DatabaseScope.class) : null,
                buffer.readUtf(DatabaseQuery.MAX_TAB_ID_LENGTH),
                buffer.readUtf(DatabaseQuery.MAX_TAB_ID_LENGTH),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                buffer.readVarInt(),
                buffer.readEnum(DepositConflictAction.class)
        );
    }

    private static void write(RegistryFriendlyByteBuf buffer, DatabaseDepositResolvePayload payload) {
        buffer.writeVarInt(payload.containerId);
        buffer.writeVarLong(payload.sessionId);
        buffer.writeBoolean(payload.scope != null);
        if (payload.scope != null) {
            buffer.writeEnum(payload.scope);
        }
        buffer.writeUtf(payload.targetTabId, DatabaseQuery.MAX_TAB_ID_LENGTH);
        buffer.writeUtf(payload.existingTabId, DatabaseQuery.MAX_TAB_ID_LENGTH);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, payload.stack);
        buffer.writeVarInt(payload.slotIndex);
        buffer.writeEnum(payload.action);
    }
}
