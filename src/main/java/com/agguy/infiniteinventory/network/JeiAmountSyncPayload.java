package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.InfiniteInventory;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * S2C：同步数据库物品数量到客户端，用于 JEI 面板叠加显示。
 */
public record JeiAmountSyncPayload(
        List<ItemAmountEntry> personalEntries,
        List<ItemAmountEntry> publicEntries
) implements CustomPacketPayload {
    public static final Type<JeiAmountSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(InfiniteInventory.MODID, "jei_amount_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, JeiAmountSyncPayload> STREAM_CODEC = StreamCodec.of(
            JeiAmountSyncPayload::write,
            JeiAmountSyncPayload::read
    );

    @Override
    public Type<JeiAmountSyncPayload> type() {
        return TYPE;
    }

    public static JeiAmountSyncPayload of(Object2LongMap<ItemStack> personal, Object2LongMap<ItemStack> publicItems) {
        return new JeiAmountSyncPayload(toList(personal), toList(publicItems));
    }

    public Object2LongMap<ItemStack> personalMap() {
        return toMap(this.personalEntries);
    }

    public Object2LongMap<ItemStack> publicMap() {
        return toMap(this.publicEntries);
    }

    private static List<ItemAmountEntry> toList(Object2LongMap<ItemStack> map) {
        List<ItemAmountEntry> result = new ArrayList<>(map.size());
        for (Object2LongMap.Entry<ItemStack> entry : map.object2LongEntrySet()) {
            result.add(new ItemAmountEntry(entry.getKey(), entry.getLongValue()));
        }
        return result;
    }

    private static Object2LongMap<ItemStack> toMap(List<ItemAmountEntry> list) {
        Object2LongMap<ItemStack> map = new Object2LongOpenHashMap<>(list.size());
        for (ItemAmountEntry entry : list) {
            map.put(entry.stack(), entry.amount());
        }
        return map;
    }

    private static JeiAmountSyncPayload read(RegistryFriendlyByteBuf buffer) {
        return new JeiAmountSyncPayload(readEntries(buffer), readEntries(buffer));
    }

    private static void write(RegistryFriendlyByteBuf buffer, JeiAmountSyncPayload payload) {
        writeEntries(buffer, payload.personalEntries);
        writeEntries(buffer, payload.publicEntries);
    }

    private static List<ItemAmountEntry> readEntries(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<ItemAmountEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
            long amount = buffer.readVarLong();
            entries.add(new ItemAmountEntry(stack, amount));
        }
        return entries;
    }

    private static void writeEntries(RegistryFriendlyByteBuf buffer, List<ItemAmountEntry> entries) {
        buffer.writeVarInt(entries.size());
        for (ItemAmountEntry entry : entries) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, entry.stack());
            buffer.writeVarLong(entry.amount());
        }
    }

    public record ItemAmountEntry(ItemStack stack, long amount) {
    }
}
