package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.network.DatabaseAmountDeltaSyncPayload;
import com.agguy.infiniteinventory.network.NetworkConstants;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DatabaseAmountDeltaSyncPayload} 序列化对称性测试。
 *
 * <p>注意：ItemStack.STREAM_CODEC 在单元测试环境（无完整注册表同步）中无法序列化。
 * 涉及 ItemStack 的完整往返测试在集成环境中验证；此处保留空列表与边界测试。</p>
 */
class DatabaseAmountDeltaSyncPayloadTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void emptyDeltasShouldRoundTrip() {
        DatabaseAmountDeltaSyncPayload payload = new DatabaseAmountDeltaSyncPayload(List.of(), List.of());
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        DatabaseAmountDeltaSyncPayload.STREAM_CODEC.encode(buffer, payload);
        DatabaseAmountDeltaSyncPayload restored = DatabaseAmountDeltaSyncPayload.STREAM_CODEC.decode(buffer);

        assertTrue(restored.personalDeltas().isEmpty());
        assertTrue(restored.publicDeltas().isEmpty());
    }

    @Test
    void payloadFieldsShouldBeAccessible() {
        List<DatabaseAmountDeltaSyncPayload.DeltaEntry> personal = List.of(
                new DatabaseAmountDeltaSyncPayload.DeltaEntry(new ItemStack(Items.STONE), "blocks", 4L, false),
                new DatabaseAmountDeltaSyncPayload.DeltaEntry(new ItemStack(Items.DIRT), "building", 0L, true)
        );
        List<DatabaseAmountDeltaSyncPayload.DeltaEntry> publicDeltas = List.of(
                new DatabaseAmountDeltaSyncPayload.DeltaEntry(new ItemStack(Items.DIAMOND), "ores", 16L, false)
        );
        DatabaseAmountDeltaSyncPayload payload = new DatabaseAmountDeltaSyncPayload(personal, publicDeltas);

        assertEquals(2, payload.personalDeltas().size());
        assertEquals(1, payload.publicDeltas().size());
        assertEquals("blocks", payload.personalDeltas().get(0).tabName());
        assertEquals(4L, payload.personalDeltas().get(0).amount());
        assertFalse(payload.personalDeltas().get(0).removed());
        assertTrue(payload.personalDeltas().get(1).removed());
        assertEquals("ores", payload.publicDeltas().get(0).tabName());
        assertEquals(16L, payload.publicDeltas().get(0).amount());
    }

    @Test
    void deltaEntryFieldsShouldBeAccessible() {
        DatabaseAmountDeltaSyncPayload.DeltaEntry entry = new DatabaseAmountDeltaSyncPayload.DeltaEntry(
                new ItemStack(Items.STONE), "blocks", 99L, true
        );

        assertEquals(Items.STONE, entry.stack().getItem());
        assertEquals("blocks", entry.tabName());
        assertEquals(99L, entry.amount());
        assertTrue(entry.removed());
    }

    @Test
    void oversizedPayloadShouldThrow() {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        buffer.writeVarInt(NetworkConstants.MAX_AMOUNT_DELTA_SYNC_ENTRY_COUNT + 1);

        assertThrows(IllegalStateException.class, () -> DatabaseAmountDeltaSyncPayload.STREAM_CODEC.decode(buffer));
    }
}
