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
 * S2C：同步数据库物品数量及所属分类名称到客户端，用于悬浮提示显示。
 */
public record DatabaseAmountSyncPayload(
        List<AmountEntry> personalEntries,
        List<AmountEntry> publicEntries
) implements CustomPacketPayload {
    public static final Type<DatabaseAmountSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(InfiniteInventory.MODID, "amount_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DatabaseAmountSyncPayload> STREAM_CODEC = StreamCodec.of(
            DatabaseAmountSyncPayload::write,
            DatabaseAmountSyncPayload::read
    );

    @Override
    public Type<DatabaseAmountSyncPayload> type() {
        return TYPE;
    }

    private static DatabaseAmountSyncPayload read(RegistryFriendlyByteBuf buffer) {
        return new DatabaseAmountSyncPayload(readEntries(buffer), readEntries(buffer));
    }

    private static void write(RegistryFriendlyByteBuf buffer, DatabaseAmountSyncPayload payload) {
        writeEntries(buffer, payload.personalEntries);
        writeEntries(buffer, payload.publicEntries);
    }

    private static List<AmountEntry> readEntries(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        NetworkConstants.checkListSize(count, NetworkConstants.MAX_AMOUNT_SYNC_ENTRY_COUNT, "amountSyncEntries");
        List<AmountEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
            String tabName = buffer.readUtf(128);
            long amount = buffer.readVarLong();
            entries.add(new AmountEntry(stack, tabName, amount));
        }
        return entries;
    }

    private static void writeEntries(RegistryFriendlyByteBuf buffer, List<AmountEntry> entries) {
        buffer.writeVarInt(entries.size());
        for (AmountEntry entry : entries) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, entry.stack());
            buffer.writeUtf(entry.tabName(), 128);
            buffer.writeVarLong(entry.amount());
        }
    }

    public record AmountEntry(ItemStack stack, String tabName, long amount) {
    }
}
