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

public record DatabaseNotePayload(
        int containerId,
        long sessionId,
        DatabaseScope scope,
        List<ItemStack> targetStacks,
        String note
) implements CustomPacketPayload {
    public static final Type<DatabaseNotePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(InfiniteInventory.MODID, "database_note"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DatabaseNotePayload> STREAM_CODEC = StreamCodec.of(
            DatabaseNotePayload::write,
            DatabaseNotePayload::read
    );

    public DatabaseNotePayload {
        scope = DatabaseScope.normalize(scope);
        targetStacks = targetStacks == null ? List.of() : List.copyOf(targetStacks);
        note = note == null ? "" : note;
    }

    @Override
    public Type<DatabaseNotePayload> type() {
        return TYPE;
    }

    private static DatabaseNotePayload read(RegistryFriendlyByteBuf buffer) {
        int containerId = buffer.readVarInt();
        long sessionId = buffer.readVarLong();
        DatabaseScope scope = buffer.readEnum(DatabaseScope.class);
        int stackCount = buffer.readVarInt();
        NetworkConstants.checkListSize(stackCount, NetworkConstants.MAX_STACK_LIST_COUNT, "targetStacks");
        List<ItemStack> targetStacks = new ArrayList<>(stackCount);
        for (int index = 0; index < stackCount; index++) {
            targetStacks.add(ItemStack.STREAM_CODEC.decode(buffer));
        }
        String note = buffer.readUtf(256);
        return new DatabaseNotePayload(containerId, sessionId, scope, targetStacks, note);
    }

    private static void write(RegistryFriendlyByteBuf buffer, DatabaseNotePayload payload) {
        buffer.writeVarInt(payload.containerId);
        buffer.writeVarLong(payload.sessionId);
        buffer.writeEnum(payload.scope);
        buffer.writeVarInt(payload.targetStacks.size());
        for (ItemStack stack : payload.targetStacks) {
            ItemStack.STREAM_CODEC.encode(buffer, stack);
        }
        buffer.writeUtf(payload.note, 256);
    }
}
