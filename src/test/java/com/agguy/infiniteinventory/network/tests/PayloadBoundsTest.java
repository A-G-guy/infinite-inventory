package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.network.DatabaseNotePayload;
import com.agguy.infiniteinventory.network.DatabaseSelectionAction;
import com.agguy.infiniteinventory.network.DatabaseSelectionPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 网络 Payload 反序列化边界测试。
 *
 * <p>验证超长字符串、超量列表等恶意输入在反序列化时被拒绝，防止 OOM 或缓冲区溢出。</p>
 */
class PayloadBoundsTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void databaseNotePayloadShouldRejectOversizedStackList() {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        buffer.writeVarInt(1);
        buffer.writeVarLong(1L);
        buffer.writeEnum(DatabaseScope.PERSONAL);
        // 写入超出限制的列表长度
        buffer.writeVarInt(1001);

        Exception exception = assertThrows(Exception.class, () -> DatabaseNotePayload.STREAM_CODEC.decode(buffer));
        String message = exception.getMessage();
        assertTrue(
                message.contains("targetStacks") || message.contains("out of bounds"),
                "异常信息应包含边界超限提示，实际为: " + message
        );
    }

    @Test
    void databaseNotePayloadShouldRejectNegativeStackListCount() {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        buffer.writeVarInt(1);
        buffer.writeVarLong(1L);
        buffer.writeEnum(DatabaseScope.PERSONAL);
        buffer.writeVarInt(-1);

        Exception exception = assertThrows(Exception.class, () -> DatabaseNotePayload.STREAM_CODEC.decode(buffer));
        String message = exception.getMessage();
        assertTrue(
                message.contains("targetStacks") || message.contains("out of bounds"),
                "异常信息应包含边界超限提示，实际为: " + message
        );
    }

    @Test
    void databaseSelectionPayloadShouldRejectOversizedEntryList() {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        buffer.writeVarInt(1);
        buffer.writeVarLong(1L);
        buffer.writeEnum(DatabaseSelectionAction.EXTRACT_ALL_TO_INVENTORY);
        buffer.writeBoolean(false);
        buffer.writeUtf("default", 64);
        buffer.writeVarLong(0L);
        // 写入超出限制的列表长度
        buffer.writeVarInt(10001);

        Exception exception = assertThrows(Exception.class, () -> DatabaseSelectionPayload.STREAM_CODEC.decode(buffer));
        String message = exception.getMessage();
        assertTrue(
                message.contains("selectedEntries") || message.contains("out of bounds"),
                "异常信息应包含边界超限提示，实际为: " + message
        );
    }

    @Test
    void databaseSelectionPayloadShouldRejectNegativeEntryListCount() {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        buffer.writeVarInt(1);
        buffer.writeVarLong(1L);
        buffer.writeEnum(DatabaseSelectionAction.TRANSFER_TO_TAB);
        buffer.writeBoolean(true);
        buffer.writeEnum(DatabaseScope.PERSONAL);
        buffer.writeUtf("default", 64);
        buffer.writeVarLong(0L);
        buffer.writeVarInt(-1);

        Exception exception = assertThrows(Exception.class, () -> DatabaseSelectionPayload.STREAM_CODEC.decode(buffer));
        String message = exception.getMessage();
        assertTrue(
                message.contains("selectedEntries") || message.contains("out of bounds"),
                "异常信息应包含边界超限提示，实际为: " + message
        );
    }

    @Test
    void databaseNotePayloadShouldAcceptMaxLengthNote() {
        String maxLengthNote = "a".repeat(256);
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
        );
        buffer.writeVarInt(1);
        buffer.writeVarLong(1L);
        buffer.writeEnum(DatabaseScope.PERSONAL);
        buffer.writeVarInt(0); // stackCount = 0，避免 ItemStack 序列化
        buffer.writeUtf(maxLengthNote, 256);

        DatabaseNotePayload restored = DatabaseNotePayload.STREAM_CODEC.decode(buffer);
        assertEquals(maxLengthNote, restored.note());
        assertTrue(restored.targetStacks().isEmpty());
    }

    @Test
    void databaseNotePayloadShouldAcceptAtLimitStackListCount() {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
        );
        buffer.writeVarInt(1);
        buffer.writeVarLong(1L);
        buffer.writeEnum(DatabaseScope.PERSONAL);
        buffer.writeVarInt(1000); // 恰好在限制边界上
        // 不写入实际 ItemStack，decode 会在读取第一个 ItemStack 时失败，
        // 但此处仅验证 list size 校验本身不会拒绝 1000 这个边界值
        // 由于单元测试环境无法序列化 ItemStack，我们只验证 count 校验通过
        Exception exception = assertThrows(Exception.class,
                () -> DatabaseNotePayload.STREAM_CODEC.decode(buffer));
        // 如果 count 校验通过，异常应来自 ItemStack 解码（buffer 越界或格式错误），而非 "out of bounds"
        String message = exception.getMessage();
        assertTrue(
                message == null || !message.contains("out of bounds"),
                "count=1000 不应触发 out of bounds，实际异常: " + message
        );
    }

    @Test
    void databaseSelectionPayloadShouldAcceptAtLimitEntryListCount() {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
        );
        buffer.writeVarInt(1);
        buffer.writeVarLong(1L);
        buffer.writeEnum(DatabaseSelectionAction.EXTRACT_ALL_TO_INVENTORY);
        buffer.writeBoolean(false);
        buffer.writeUtf("default", 64);
        buffer.writeVarLong(0L);
        buffer.writeVarInt(10000); // 恰好在限制边界上

        Exception exception = assertThrows(Exception.class,
                () -> DatabaseSelectionPayload.STREAM_CODEC.decode(buffer));
        String message = exception.getMessage();
        assertTrue(
                message == null || !message.contains("out of bounds"),
                "count=10000 不应触发 out of bounds，实际异常: " + message
        );
    }

    private static void assertEquals(String expected, String actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
