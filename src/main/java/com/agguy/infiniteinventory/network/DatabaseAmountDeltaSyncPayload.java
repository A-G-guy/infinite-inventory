package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.InfiniteInventory;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * S2C：增量同步数据库物品数量及所属分类名称到客户端。
 *
 * <p>与 {@link DatabaseAmountSyncPayload}（全量同步）的区别在于只发送自上次同步以来
 * 发生变更的条目，将典型场景的数据传输量从 O(n) 降至 O(变更数)。</p>
 */
public record DatabaseAmountDeltaSyncPayload(
        List<DeltaEntry> personalDeltas,
        List<DeltaEntry> publicDeltas
) implements CustomPacketPayload {
    public static final Type<DatabaseAmountDeltaSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(InfiniteInventory.MODID, "amount_delta_sync")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DatabaseAmountDeltaSyncPayload> STREAM_CODEC = StreamCodec.of(
            DatabaseAmountDeltaSyncPayload::write,
            DatabaseAmountDeltaSyncPayload::read
    );

    @Override
    public Type<DatabaseAmountDeltaSyncPayload> type() {
        return TYPE;
    }

    private static DatabaseAmountDeltaSyncPayload read(RegistryFriendlyByteBuf buffer) {
        return new DatabaseAmountDeltaSyncPayload(readEntries(buffer), readEntries(buffer));
    }

    private static void write(RegistryFriendlyByteBuf buffer, DatabaseAmountDeltaSyncPayload payload) {
        writeEntries(buffer, payload.personalDeltas);
        writeEntries(buffer, payload.publicDeltas);
    }

    private static List<DeltaEntry> readEntries(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        NetworkConstants.checkListSize(count, NetworkConstants.MAX_AMOUNT_DELTA_SYNC_ENTRY_COUNT, "amountDeltaSyncEntries");
        List<DeltaEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
            String tabName = buffer.readUtf(128);
            long amount = buffer.readVarLong();
            boolean removed = buffer.readBoolean();
            entries.add(new DeltaEntry(stack, tabName, amount, removed));
        }
        return entries;
    }

    private static void writeEntries(RegistryFriendlyByteBuf buffer, List<DeltaEntry> entries) {
        buffer.writeVarInt(entries.size());
        for (DeltaEntry entry : entries) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, entry.stack());
            buffer.writeUtf(entry.tabName(), 128);
            buffer.writeVarLong(entry.amount());
            buffer.writeBoolean(entry.removed());
        }
    }

    public record DeltaEntry(ItemStack stack, String tabName, long amount, boolean removed) {
    }
}
