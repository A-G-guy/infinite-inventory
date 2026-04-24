package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.network.DatabaseNotePayload;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DatabaseNotePayload 序列化对称性测试。
 */
class DatabaseNotePayloadTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void payloadWithEmptyStacksShouldRoundTripThroughStreamCodec() {
        // ItemStack.STREAM_CODEC 需要完整注册表，无法用 RegistryAccess.EMPTY 序列化。
        // 此处测试非 ItemStack 字段的往返一致性。
        DatabaseNotePayload payload = new DatabaseNotePayload(
                1, 42L, DatabaseScope.PERSONAL,
                List.of(),
                "这是一条测试备注"
        );
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        DatabaseNotePayload.STREAM_CODEC.encode(buffer, payload);
        DatabaseNotePayload restored = DatabaseNotePayload.STREAM_CODEC.decode(buffer);

        assertEquals(payload.containerId(), restored.containerId());
        assertEquals(payload.sessionId(), restored.sessionId());
        assertEquals(payload.scope(), restored.scope());
        assertEquals(0, restored.targetStacks().size());
        assertEquals(payload.note(), restored.note());
    }

    @Test
    void payloadWithEmptyNoteShouldRoundTripThroughStreamCodec() {
        DatabaseNotePayload payload = new DatabaseNotePayload(
                99, 888L, DatabaseScope.PUBLIC,
                List.of(),
                ""
        );
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        DatabaseNotePayload.STREAM_CODEC.encode(buffer, payload);
        DatabaseNotePayload restored = DatabaseNotePayload.STREAM_CODEC.decode(buffer);

        assertEquals("", restored.note());
        assertEquals(0, restored.targetStacks().size());
    }

    @Test
    void payloadWithNullNoteShouldDefaultToEmptyString() {
        DatabaseNotePayload payload = new DatabaseNotePayload(
                1, 1L, DatabaseScope.PERSONAL, List.of(), null);
        assertEquals("", payload.note());
    }

    @Test
    void payloadShouldNormalizeScope() {
        DatabaseNotePayload payload = new DatabaseNotePayload(1, 1L, null, List.of(), "");
        assertEquals(DatabaseScope.PERSONAL, payload.scope());
    }

    @Test
    void payloadShouldNormalizeNullStackListToEmpty() {
        DatabaseNotePayload payload = new DatabaseNotePayload(1, 1L, DatabaseScope.PERSONAL, null, "");
        assertTrue(payload.targetStacks().isEmpty());
    }
}
