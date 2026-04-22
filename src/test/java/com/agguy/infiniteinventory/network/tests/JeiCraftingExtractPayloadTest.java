package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.network.JeiCraftingExtractPayload;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JeiCraftingExtractPayloadTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void shouldRoundTripEmptyGaps() {
        JeiCraftingExtractPayload payload = new JeiCraftingExtractPayload(
                ResourceLocation.fromNamespaceAndPath("minecraft", "stone_pickaxe"),
                List.of()
        );
        RegistryAccess registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);

        JeiCraftingExtractPayload.STREAM_CODEC.encode(buffer, payload);
        JeiCraftingExtractPayload restored = JeiCraftingExtractPayload.STREAM_CODEC.decode(buffer);

        assertEquals(payload, restored);
        assertEquals(ResourceLocation.fromNamespaceAndPath("minecraft", "stone_pickaxe"), restored.recipeId());
        assertEquals(0, restored.gaps().size());
    }

    @Test
    void shouldStoreRecipeIdAndGapsCorrectly() {
        ItemStack wheat = new ItemStack(Items.WHEAT);
        ItemStack stone = new ItemStack(Items.STONE, 2);
        JeiCraftingExtractPayload payload = new JeiCraftingExtractPayload(
                ResourceLocation.fromNamespaceAndPath("minecraft", "bread"),
                List.of(
                        new JeiCraftingExtractPayload.MaterialGap(wheat, 3),
                        new JeiCraftingExtractPayload.MaterialGap(stone, 8)
                )
        );

        assertEquals(ResourceLocation.fromNamespaceAndPath("minecraft", "bread"), payload.recipeId());
        assertEquals(2, payload.gaps().size());
        assertEquals(3, payload.gaps().get(0).needed());
        assertEquals(Items.WHEAT, payload.gaps().get(0).stack().getItem());
        assertEquals(8, payload.gaps().get(1).needed());
        assertEquals(Items.STONE, payload.gaps().get(1).stack().getItem());
    }
}
