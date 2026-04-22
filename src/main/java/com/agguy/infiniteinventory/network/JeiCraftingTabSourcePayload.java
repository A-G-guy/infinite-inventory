package com.agguy.infiniteinventory.network;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S：客户端修改 JEI 合成材料提取源页签配置。
 */
public record JeiCraftingTabSourcePayload(
        DatabaseScope scope,
        String tabId,
        boolean enabled
) implements CustomPacketPayload {
    public static final Type<JeiCraftingTabSourcePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(InfiniteInventory.MODID, "jei_crafting_tab_source"));
    public static final StreamCodec<RegistryFriendlyByteBuf, JeiCraftingTabSourcePayload> STREAM_CODEC = StreamCodec.of(
            JeiCraftingTabSourcePayload::write,
            JeiCraftingTabSourcePayload::read
    );

    @Override
    public Type<JeiCraftingTabSourcePayload> type() {
        return TYPE;
    }

    private static JeiCraftingTabSourcePayload read(RegistryFriendlyByteBuf buffer) {
        return new JeiCraftingTabSourcePayload(
                buffer.readEnum(DatabaseScope.class),
                buffer.readUtf(DatabaseQuery.MAX_TAB_ID_LENGTH),
                buffer.readBoolean()
        );
    }

    private static void write(RegistryFriendlyByteBuf buffer, JeiCraftingTabSourcePayload payload) {
        buffer.writeEnum(payload.scope);
        buffer.writeUtf(payload.tabId, DatabaseQuery.MAX_TAB_ID_LENGTH);
        buffer.writeBoolean(payload.enabled);
    }
}
