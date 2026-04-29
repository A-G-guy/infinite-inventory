package com.agguy.infiniteinventory.client;

import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DatabaseAmountCache} 增量更新测试。
 */
class DatabaseAmountCacheTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        DatabaseAmountCache.INSTANCE.update(List.of(), List.of());
    }

    @Test
    void applyDeltaShouldUpdateExistingEntry() {
        // 每次测试使用独立实例，避免状态污染
        DatabaseAmountCache cache = DatabaseAmountCache.INSTANCE;
        cache.update(List.of(new DatabaseAmountCache.Entry(new ItemStack(Items.STONE), "blocks", 4L)), List.of());

        cache.applyDelta(
                List.of(new DatabaseAmountCache.Delta(new ItemStack(Items.STONE), "building", 10L, false)),
                List.of()
        );

        DatabaseAmountCache.Entry entry = cache.getPersonal(new ItemStack(Items.STONE));
        assertNotNull(entry);
        assertEquals(10L, entry.amount());
        assertEquals("building", entry.tabName());
    }

    @Test
    void applyDeltaShouldAddNewEntry() {
        DatabaseAmountCache cache = DatabaseAmountCache.INSTANCE;
        cache.update(List.of(), List.of());

        cache.applyDelta(
                List.of(new DatabaseAmountCache.Delta(new ItemStack(Items.STONE), "blocks", 4L, false)),
                List.of()
        );

        assertEquals(4L, cache.getPersonal(new ItemStack(Items.STONE)).amount());
    }

    @Test
    void applyDeltaShouldRemoveEntry() {
        DatabaseAmountCache cache = DatabaseAmountCache.INSTANCE;
        cache.update(List.of(new DatabaseAmountCache.Entry(new ItemStack(Items.STONE), "blocks", 4L)), List.of());

        cache.applyDelta(
                List.of(new DatabaseAmountCache.Delta(new ItemStack(Items.STONE), "", 0L, true)),
                List.of()
        );

        assertNull(cache.getPersonal(new ItemStack(Items.STONE)));
    }

    @Test
    void applyDeltaShouldNotAffectUnchangedEntries() {
        DatabaseAmountCache cache = DatabaseAmountCache.INSTANCE;
        cache.update(List.of(
                new DatabaseAmountCache.Entry(new ItemStack(Items.STONE), "blocks", 4L),
                new DatabaseAmountCache.Entry(new ItemStack(Items.DIRT), "building", 2L)
        ), List.of());

        cache.applyDelta(
                List.of(new DatabaseAmountCache.Delta(new ItemStack(Items.STONE), "blocks", 8L, false)),
                List.of()
        );

        assertEquals(8L, cache.getPersonal(new ItemStack(Items.STONE)).amount());
        assertEquals(2L, cache.getPersonal(new ItemStack(Items.DIRT)).amount());
    }

    @Test
    void applyDeltaThenFullUpdateShouldReplaceAll() {
        DatabaseAmountCache cache = DatabaseAmountCache.INSTANCE;
        cache.update(List.of(new DatabaseAmountCache.Entry(new ItemStack(Items.STONE), "blocks", 4L)), List.of());

        cache.applyDelta(
                List.of(new DatabaseAmountCache.Delta(new ItemStack(Items.DIRT), "building", 2L, false)),
                List.of()
        );

        cache.update(List.of(new DatabaseAmountCache.Entry(new ItemStack(Items.APPLE), "food", 1L)), List.of());

        assertNull(cache.getPersonal(new ItemStack(Items.STONE)));
        assertNull(cache.getPersonal(new ItemStack(Items.DIRT)));
        assertEquals(1L, cache.getPersonal(new ItemStack(Items.APPLE)).amount());
    }

    @Test
    void fullUpdateThenApplyDeltaShouldMergeCorrectly() {
        DatabaseAmountCache cache = DatabaseAmountCache.INSTANCE;
        cache.update(List.of(new DatabaseAmountCache.Entry(new ItemStack(Items.STONE), "blocks", 4L)), List.of());

        cache.applyDelta(
                List.of(new DatabaseAmountCache.Delta(new ItemStack(Items.STONE), "blocks", 8L, false)),
                List.of()
        );

        assertEquals(8L, cache.getPersonal(new ItemStack(Items.STONE)).amount());
    }

    @Test
    void multipleDeltaApplicationsShouldAccumulate() {
        DatabaseAmountCache cache = DatabaseAmountCache.INSTANCE;
        cache.update(List.of(), List.of());

        cache.applyDelta(
                List.of(new DatabaseAmountCache.Delta(new ItemStack(Items.STONE), "blocks", 4L, false)),
                List.of()
        );
        cache.applyDelta(
                List.of(new DatabaseAmountCache.Delta(new ItemStack(Items.STONE), "blocks", 6L, false)),
                List.of()
        );
        cache.applyDelta(
                List.of(new DatabaseAmountCache.Delta(new ItemStack(Items.DIRT), "building", 2L, false)),
                List.of()
        );

        assertEquals(6L, cache.getPersonal(new ItemStack(Items.STONE)).amount());
        assertEquals(2L, cache.getPersonal(new ItemStack(Items.DIRT)).amount());
    }

    @Test
    void applyDeltaShouldUpdatePublicItems() {
        DatabaseAmountCache cache = DatabaseAmountCache.INSTANCE;
        cache.update(List.of(), List.of(new DatabaseAmountCache.Entry(new ItemStack(Items.DIAMOND), "ores", 16L)));

        cache.applyDelta(
                List.of(),
                List.of(new DatabaseAmountCache.Delta(new ItemStack(Items.DIAMOND), "gems", 32L, false))
        );

        DatabaseAmountCache.Entry entry = cache.getPublic(new ItemStack(Items.DIAMOND));
        assertNotNull(entry);
        assertEquals(32L, entry.amount());
        assertEquals("gems", entry.tabName());
    }

    @Test
    void applyDeltaShouldRemoveNonExistentEntryWithoutError() {
        DatabaseAmountCache cache = DatabaseAmountCache.INSTANCE;
        cache.update(List.of(), List.of());

        cache.applyDelta(
                List.of(new DatabaseAmountCache.Delta(new ItemStack(Items.STONE), "", 0L, true)),
                List.of()
        );

        assertNull(cache.getPersonal(new ItemStack(Items.STONE)));
    }

    @Test
    void isAvailableShouldReflectCacheState() {
        DatabaseAmountCache cache = DatabaseAmountCache.INSTANCE;
        cache.update(List.of(), List.of());
        assertFalse(cache.isAvailable());

        cache.update(List.of(new DatabaseAmountCache.Entry(new ItemStack(Items.STONE), "blocks", 4L)), List.of());
        assertTrue(cache.isAvailable());

        cache.update(List.of(), List.of(new DatabaseAmountCache.Entry(new ItemStack(Items.DIAMOND), "ores", 16L)));
        assertTrue(cache.isAvailable());

        cache.update(List.of(), List.of());
        assertFalse(cache.isAvailable());
    }

    @Test
    void getPublicShouldReturnPublicEntry() {
        DatabaseAmountCache cache = DatabaseAmountCache.INSTANCE;
        cache.update(List.of(), List.of(new DatabaseAmountCache.Entry(new ItemStack(Items.APPLE), "food", 8L)));

        DatabaseAmountCache.Entry entry = cache.getPublic(new ItemStack(Items.APPLE));
        assertNotNull(entry);
        assertEquals(8L, entry.amount());
        assertEquals("food", entry.tabName());
    }

    @Test
    void entryShouldDefensivelyCopyStackOnConstruction() {
        ItemStack original = new ItemStack(Items.DIAMOND_SWORD);
        original.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("原始名称"));

        DatabaseAmountCache.Entry entry = new DatabaseAmountCache.Entry(original, "combat", 1L);
        original.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("修改后名称"));

        assertEquals("原始名称", entry.stack().get(net.minecraft.core.component.DataComponents.CUSTOM_NAME).getString());
    }

    @Test
    void entryStackAccessorShouldReturnDefensiveCopy() {
        DatabaseAmountCache.Entry entry = new DatabaseAmountCache.Entry(new ItemStack(Items.DIAMOND_SWORD), "combat", 1L);
        ItemStack firstAccess = entry.stack();
        firstAccess.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("修改后"));

        assertTrue(entry.stack().get(net.minecraft.core.component.DataComponents.CUSTOM_NAME) == null);
    }

    @Test
    void deltaShouldDefensivelyCopyStackOnConstruction() {
        ItemStack original = new ItemStack(Items.APPLE);
        original.setCount(16);

        DatabaseAmountCache.Delta delta = new DatabaseAmountCache.Delta(original, "food", 16L, false);
        original.setCount(1);

        assertEquals(16, delta.stack().getCount());
    }
}
