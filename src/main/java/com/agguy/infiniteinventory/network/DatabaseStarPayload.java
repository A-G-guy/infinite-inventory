package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.database.DatabaseScope;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record DatabaseStarPayload(
        int containerId,
        long sessionId,
        DatabaseScope scope,
        List<ItemStack> targetStacks,
        StarAction action
) implements CustomPacketPayload {
    public static final Type<DatabaseStarPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(InfiniteInventory.MODID, "database_star"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DatabaseStarPayload> STREAM_CODEC = StreamCodec.of(
            DatabaseStarPayload::write,
            DatabaseStarPayload::read
    );

    public DatabaseStarPayload {
        scope = DatabaseScope.normalize(scope);
        targetStacks = targetStacks == null ? List.of() : List.copyOf(targetStacks);
        action = action == null ? StarAction.TOGGLE : action;
    }

    @Override
    public Type<DatabaseStarPayload> type() {
        return TYPE;
    }

    public enum StarAction {
        TOGGLE,
        STAR_ALL,
        UNSTAR_ALL
    }

    private static DatabaseStarPayload read(RegistryFriendlyByteBuf buffer) {
        int containerId = buffer.readVarInt();
        long sessionId = buffer.readVarLong();
        DatabaseScope scope = buffer.readEnum(DatabaseScope.class);
        int stackCount = buffer.readVarInt();
        List<ItemStack> targetStacks = new ArrayList<>(stackCount);
        for (int index = 0; index < stackCount; index++) {
            targetStacks.add(ItemStack.STREAM_CODEC.decode(buffer));
        }
        StarAction action = buffer.readEnum(StarAction.class);
        return new DatabaseStarPayload(containerId, sessionId, scope, targetStacks, action);
    }

    private static void write(RegistryFriendlyByteBuf buffer, DatabaseStarPayload payload) {
        buffer.writeVarInt(payload.containerId);
        buffer.writeVarLong(payload.sessionId);
        buffer.writeEnum(payload.scope);
        buffer.writeVarInt(payload.targetStacks.size());
        for (ItemStack stack : payload.targetStacks) {
            ItemStack.STREAM_CODEC.encode(buffer, stack);
        }
        buffer.writeEnum(payload.action);
    }
}
