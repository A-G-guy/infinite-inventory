package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * 客户端 → 服务端：玩家槽位快存到数据库的请求包。
 *
 * <p>{@code storeSingleOnly} 为 true 时仅从槽位中取出 1 个物品存入数据库；
 * 为 false 时（默认 Shift+左键 路径）将整个槽位的堆叠存入数据库。
 * 当槽位中物品数量已为 1 时，两者行为一致。
 */
public record DatabaseQuickDepositPayload(
        int containerId,
        long sessionId,
        int slotIndex,
        @Nullable DatabaseScope targetScope,
        String targetTabId,
        boolean storeSingleOnly
) implements CustomPacketPayload {
    public static final Type<DatabaseQuickDepositPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(InfiniteInventory.MODID, "database_quick_deposit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DatabaseQuickDepositPayload> STREAM_CODEC = StreamCodec.of(
            DatabaseQuickDepositPayload::write,
            DatabaseQuickDepositPayload::read
    );

    @Override
    public Type<DatabaseQuickDepositPayload> type() {
        return TYPE;
    }

    private static DatabaseQuickDepositPayload read(RegistryFriendlyByteBuf buffer) {
        return new DatabaseQuickDepositPayload(
                buffer.readVarInt(),
                buffer.readVarLong(),
                buffer.readVarInt(),
                buffer.readBoolean() ? buffer.readEnum(DatabaseScope.class) : null,
                buffer.readUtf(DatabaseQuery.MAX_TAB_ID_LENGTH),
                buffer.readBoolean()
        );
    }

    private static void write(RegistryFriendlyByteBuf buffer, DatabaseQuickDepositPayload payload) {
        buffer.writeVarInt(payload.containerId);
        buffer.writeVarLong(payload.sessionId);
        buffer.writeVarInt(payload.slotIndex);
        buffer.writeBoolean(payload.targetScope != null);
        if (payload.targetScope != null) {
            buffer.writeEnum(payload.targetScope);
        }
        buffer.writeUtf(payload.targetTabId, DatabaseQuery.MAX_TAB_ID_LENGTH);
        buffer.writeBoolean(payload.storeSingleOnly);
    }
}
