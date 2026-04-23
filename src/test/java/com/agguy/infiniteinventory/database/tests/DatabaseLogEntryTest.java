package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseLogAction;
import com.agguy.infiniteinventory.database.DatabaseLogEntry;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DatabaseLogEntry} 数据类的构造、快照行为与空值防御测试。
 */
class DatabaseLogEntryTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void shouldCreateEntryWithAllFields() {
        long timestamp = System.currentTimeMillis();
        UUID playerId = UUID.randomUUID();
        ItemStack stack = new ItemStack(Items.DIAMOND, 16);

        DatabaseLogEntry entry = new DatabaseLogEntry(
                timestamp,
                playerId,
                "TestPlayer",
                DatabaseLogAction.DEPOSIT,
                stack,
                16L,
                "source",
                "target",
                DatabaseScope.PERSONAL
        );

        assertEquals(timestamp, entry.timestampMillis());
        assertEquals(playerId, entry.playerId());
        assertEquals("TestPlayer", entry.playerNameSnapshot());
        assertEquals(DatabaseLogAction.DEPOSIT, entry.action());
        assertEquals(16L, entry.amount());
        assertEquals("source", entry.sourceTabId());
        assertEquals("target", entry.targetTabId());
        assertEquals(DatabaseScope.PERSONAL, entry.relatedScope());
    }

    @Test
    void shouldNormalizeNullPlayerIdToZeroUuid() {
        DatabaseLogEntry entry = new DatabaseLogEntry(
                0L, null, "", DatabaseLogAction.DEPOSIT, ItemStack.EMPTY, 0L, "", "", null
        );

        assertEquals(new UUID(0L, 0L), entry.playerId());
    }

    @Test
    void shouldNormalizeNullPlayerNameToEmptyString() {
        DatabaseLogEntry entry = new DatabaseLogEntry(
                0L, UUID.randomUUID(), null, DatabaseLogAction.DEPOSIT, ItemStack.EMPTY, 0L, "", "", null
        );

        assertEquals("", entry.playerNameSnapshot());
    }

    @Test
    void shouldTrimPlayerName() {
        DatabaseLogEntry entry = new DatabaseLogEntry(
                0L, UUID.randomUUID(), "  Player  ", DatabaseLogAction.DEPOSIT, ItemStack.EMPTY, 0L, "", "", null
        );

        assertEquals("Player", entry.playerNameSnapshot());
    }

    @Test
    void shouldNormalizeNullActionToDeposit() {
        DatabaseLogEntry entry = new DatabaseLogEntry(
                0L, UUID.randomUUID(), "", null, ItemStack.EMPTY, 0L, "", "", null
        );

        assertEquals(DatabaseLogAction.DEPOSIT, entry.action());
    }

    @Test
    void shouldNormalizeNullStackToEmpty() {
        DatabaseLogEntry entry = new DatabaseLogEntry(
                0L, UUID.randomUUID(), "", DatabaseLogAction.DEPOSIT, null, 1L, "", "", null
        );

        assertTrue(entry.stackSnapshot().isEmpty());
    }

    @Test
    void shouldNormalizeEmptyStackToEmpty() {
        DatabaseLogEntry entry = new DatabaseLogEntry(
                0L, UUID.randomUUID(), "", DatabaseLogAction.DEPOSIT, ItemStack.EMPTY, 1L, "", "", null
        );

        assertTrue(entry.stackSnapshot().isEmpty());
    }

    @Test
    void shouldCopyStackWithCountOneForSnapshot() {
        ItemStack original = new ItemStack(Items.STONE, 64);
        DatabaseLogEntry entry = new DatabaseLogEntry(
                0L, UUID.randomUUID(), "", DatabaseLogAction.DEPOSIT, original, 64L, "", "", null
        );

        assertEquals(1, entry.stackSnapshot().getCount());
        assertNotSame(original, entry.stackSnapshot());
        assertEquals(Items.STONE, entry.stackSnapshot().getItem());
    }

    @Test
    void shouldClampNegativeAmountToZero() {
        DatabaseLogEntry entry = new DatabaseLogEntry(
                0L, UUID.randomUUID(), "", DatabaseLogAction.DEPOSIT, new ItemStack(Items.STONE), -5L, "", "", null
        );

        assertEquals(0L, entry.amount());
    }

    @Test
    void shouldNormalizeNullTabIdsToEmptyString() {
        DatabaseLogEntry entry = new DatabaseLogEntry(
                0L, UUID.randomUUID(), "", DatabaseLogAction.DEPOSIT, new ItemStack(Items.STONE), 1L, null, null, null
        );

        assertEquals("", entry.sourceTabId());
        assertEquals("", entry.targetTabId());
    }

    @Test
    void shouldTrimTabIds() {
        DatabaseLogEntry entry = new DatabaseLogEntry(
                0L, UUID.randomUUID(), "", DatabaseLogAction.DEPOSIT, new ItemStack(Items.STONE), 1L, "  src  ", "  tgt  ", null
        );

        assertEquals("src", entry.sourceTabId());
        assertEquals("tgt", entry.targetTabId());
    }

    @Test
    void shouldBeEmptyWhenStackIsEmpty() {
        DatabaseLogEntry entry = new DatabaseLogEntry(
                0L, UUID.randomUUID(), "", DatabaseLogAction.DEPOSIT, ItemStack.EMPTY, 1L, "", "", null
        );

        assertTrue(entry.isEmpty());
    }

    @Test
    void shouldBeEmptyWhenAmountIsZeroOrNegative() {
        DatabaseLogEntry entry = new DatabaseLogEntry(
                0L, UUID.randomUUID(), "", DatabaseLogAction.DEPOSIT, new ItemStack(Items.STONE), 0L, "", "", null
        );

        assertTrue(entry.isEmpty());
    }

    @Test
    void shouldNotBeEmptyWhenStackAndAmountAreValid() {
        DatabaseLogEntry entry = new DatabaseLogEntry(
                0L, UUID.randomUUID(), "", DatabaseLogAction.DEPOSIT, new ItemStack(Items.STONE), 1L, "", "", null
        );

        assertFalse(entry.isEmpty());
    }

    @Test
    void shouldPreserveTimestampExactly() {
        long timestamp = 1_700_000_000_000L;
        DatabaseLogEntry entry = new DatabaseLogEntry(
                timestamp, UUID.randomUUID(), "", DatabaseLogAction.DEPOSIT, new ItemStack(Items.STONE), 1L, "", "", null
        );

        assertEquals(timestamp, entry.timestampMillis());
    }

    @Test
    void shouldAllowNullRelatedScope() {
        DatabaseLogEntry entry = new DatabaseLogEntry(
                0L, UUID.randomUUID(), "", DatabaseLogAction.DEPOSIT, new ItemStack(Items.STONE), 1L, "", "", null
        );

        assertNull(entry.relatedScope());
    }

    @Test
    void shouldPreserveRelatedScopeWhenNonNull() {
        DatabaseLogEntry entry = new DatabaseLogEntry(
                0L, UUID.randomUUID(), "", DatabaseLogAction.DEPOSIT, new ItemStack(Items.STONE), 1L, "", "", DatabaseScope.PUBLIC
        );

        assertEquals(DatabaseScope.PUBLIC, entry.relatedScope());
    }

    @Test
    void shouldCreateEntryWithTransferAction() {
        DatabaseLogEntry entry = new DatabaseLogEntry(
                0L, UUID.randomUUID(), "", DatabaseLogAction.TRANSFER, new ItemStack(Items.IRON_INGOT), 8L, "tabA", "tabB", null
        );

        assertEquals(DatabaseLogAction.TRANSFER, entry.action());
        assertEquals(8L, entry.amount());
        assertEquals("tabA", entry.sourceTabId());
        assertEquals("tabB", entry.targetTabId());
    }

    @Test
    void shouldCreateEntryWithDeleteAction() {
        DatabaseLogEntry entry = new DatabaseLogEntry(
                0L, UUID.randomUUID(), "", DatabaseLogAction.DELETE, new ItemStack(Items.DIRT), 32L, "src", "", null
        );

        assertEquals(DatabaseLogAction.DELETE, entry.action());
        assertEquals(32L, entry.amount());
        assertFalse(entry.isEmpty());
    }
}
