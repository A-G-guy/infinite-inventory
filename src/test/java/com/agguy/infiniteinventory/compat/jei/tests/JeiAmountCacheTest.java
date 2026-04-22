package com.agguy.infiniteinventory.compat.jei.tests;

import com.agguy.infiniteinventory.compat.jei.JeiAmountCache;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JeiAmountCacheTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @BeforeEach
    void setUp() {
        JeiAmountCache.INSTANCE.update(new Object2LongOpenHashMap<>(), new Object2LongOpenHashMap<>());
    }

    @Test
    void shouldReturnZeroForUnknownItem() {
        assertEquals(0L, JeiAmountCache.INSTANCE.getPersonalAmount(new ItemStack(Items.STONE)));
        assertEquals(0L, JeiAmountCache.INSTANCE.getPublicAmount(new ItemStack(Items.DIRT)));
    }

    @Test
    void shouldReturnAmountForMatchingStack() {
        Object2LongMap<ItemStack> personal = new Object2LongOpenHashMap<>();
        personal.put(new ItemStack(Items.STONE), 42L);
        personal.put(new ItemStack(Items.DIRT), 7L);

        JeiAmountCache.INSTANCE.update(personal, new Object2LongOpenHashMap<>());

        assertEquals(42L, JeiAmountCache.INSTANCE.getPersonalAmount(new ItemStack(Items.STONE)));
        assertEquals(7L, JeiAmountCache.INSTANCE.getPersonalAmount(new ItemStack(Items.DIRT)));
        assertEquals(0L, JeiAmountCache.INSTANCE.getPublicAmount(new ItemStack(Items.STONE)));
    }

    @Test
    void shouldReturnAmountForPublicStack() {
        Object2LongMap<ItemStack> publicItems = new Object2LongOpenHashMap<>();
        publicItems.put(new ItemStack(Items.DIAMOND), 99L);

        JeiAmountCache.INSTANCE.update(new Object2LongOpenHashMap<>(), publicItems);

        assertEquals(99L, JeiAmountCache.INSTANCE.getPublicAmount(new ItemStack(Items.DIAMOND)));
        assertEquals(0L, JeiAmountCache.INSTANCE.getPersonalAmount(new ItemStack(Items.DIAMOND)));
    }

    @Test
    void updateShouldReplacePreviousContents() {
        Object2LongMap<ItemStack> first = new Object2LongOpenHashMap<>();
        first.put(new ItemStack(Items.STONE), 10L);
        JeiAmountCache.INSTANCE.update(first, new Object2LongOpenHashMap<>());

        Object2LongMap<ItemStack> second = new Object2LongOpenHashMap<>();
        second.put(new ItemStack(Items.DIRT), 20L);
        JeiAmountCache.INSTANCE.update(second, new Object2LongOpenHashMap<>());

        assertEquals(0L, JeiAmountCache.INSTANCE.getPersonalAmount(new ItemStack(Items.STONE)));
        assertEquals(20L, JeiAmountCache.INSTANCE.getPersonalAmount(new ItemStack(Items.DIRT)));
    }

    @Test
    void shouldMatchStackWithDifferentInstance() {
        Object2LongMap<ItemStack> personal = new Object2LongOpenHashMap<>();
        personal.put(new ItemStack(Items.OAK_LOG, 3), 15L);

        JeiAmountCache.INSTANCE.update(personal, new Object2LongOpenHashMap<>());

        ItemStack lookup = new ItemStack(Items.OAK_LOG, 1);
        assertEquals(15L, JeiAmountCache.INSTANCE.getPersonalAmount(lookup));
    }
}
