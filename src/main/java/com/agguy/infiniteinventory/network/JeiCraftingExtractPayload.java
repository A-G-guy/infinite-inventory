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
 * C2S：请求从数据库提取材料以补充 JEI 合成配方所需的缺口。
 */
public record JeiCraftingExtractPayload(
        ResourceLocation recipeId,
        List<MaterialGap> gaps
) implements CustomPacketPayload {
    public static final Type<JeiCraftingExtractPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(InfiniteInventory.MODID, "jei_crafting_extract"));
    public static final StreamCodec<RegistryFriendlyByteBuf, JeiCraftingExtractPayload> STREAM_CODEC = StreamCodec.of(
            JeiCraftingExtractPayload::write,
            JeiCraftingExtractPayload::read
    );

    @Override
    public Type<JeiCraftingExtractPayload> type() {
        return TYPE;
    }

    private static JeiCraftingExtractPayload read(RegistryFriendlyByteBuf buffer) {
        ResourceLocation recipeId = ResourceLocation.STREAM_CODEC.decode(buffer);
        int count = buffer.readVarInt();
        List<MaterialGap> gaps = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
            int needed = buffer.readVarInt();
            gaps.add(new MaterialGap(stack, needed));
        }
        return new JeiCraftingExtractPayload(recipeId, gaps);
    }

    private static void write(RegistryFriendlyByteBuf buffer, JeiCraftingExtractPayload payload) {
        ResourceLocation.STREAM_CODEC.encode(buffer, payload.recipeId);
        buffer.writeVarInt(payload.gaps.size());
        for (MaterialGap gap : payload.gaps) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, gap.stack());
            buffer.writeVarInt(gap.needed());
        }
    }

    public record MaterialGap(ItemStack stack, int needed) {
    }
}
