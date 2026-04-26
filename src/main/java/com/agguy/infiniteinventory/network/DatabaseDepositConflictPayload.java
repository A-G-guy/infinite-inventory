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

public record DatabaseDepositConflictPayload(
        @Nullable DatabaseScope scope,
        String targetTabId,
        String existingTabId,
        ItemStack stack,
        int slotIndex
) implements CustomPacketPayload {
    public static final Type<DatabaseDepositConflictPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(InfiniteInventory.MODID, "deposit_conflict"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DatabaseDepositConflictPayload> STREAM_CODEC = StreamCodec.of(
            DatabaseDepositConflictPayload::write,
            DatabaseDepositConflictPayload::read
    );

    @Override
    public Type<DatabaseDepositConflictPayload> type() {
        return TYPE;
    }

    private static DatabaseDepositConflictPayload read(RegistryFriendlyByteBuf buffer) {
        return new DatabaseDepositConflictPayload(
                buffer.readBoolean() ? buffer.readEnum(DatabaseScope.class) : null,
                buffer.readUtf(DatabaseQuery.MAX_TAB_ID_LENGTH),
                buffer.readUtf(DatabaseQuery.MAX_TAB_ID_LENGTH),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                buffer.readVarInt()
        );
    }

    private static void write(RegistryFriendlyByteBuf buffer, DatabaseDepositConflictPayload payload) {
        buffer.writeBoolean(payload.scope != null);
        if (payload.scope != null) {
            buffer.writeEnum(payload.scope);
        }
        buffer.writeUtf(payload.targetTabId, DatabaseQuery.MAX_TAB_ID_LENGTH);
        buffer.writeUtf(payload.existingTabId, DatabaseQuery.MAX_TAB_ID_LENGTH);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, payload.stack);
        buffer.writeVarInt(payload.slotIndex);
    }
}
