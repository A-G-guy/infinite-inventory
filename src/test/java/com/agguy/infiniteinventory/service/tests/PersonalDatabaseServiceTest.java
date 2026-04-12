package com.agguy.infiniteinventory.service.tests;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.service.PersonalDatabaseService;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonalDatabaseServiceTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void depositMainInventoryShouldReturnLongToAvoidOverflowRegression() throws ReflectiveOperationException {
        var method = PersonalDatabaseService.class.getDeclaredMethod("depositMainInventory", ServerPlayer.class, DatabaseScope.class);

        assertEquals(long.class, method.getReturnType());
    }

    @Test
    void safeAddMovedItemsShouldCrossIntegerBoundaryWithoutOverflow() throws ReflectiveOperationException {
        var method = PersonalDatabaseService.class.getDeclaredMethod("safeAddMovedItems", long.class, ItemStack.class);
        method.setAccessible(true);

        long result = (long) method.invoke(null, (long) Integer.MAX_VALUE, new ItemStack(Items.STONE, 64));

        assertEquals((long) Integer.MAX_VALUE + 64L, result);
    }

    @Test
    void safeAddMovedItemsShouldSaturateAtLongMaxValue() throws ReflectiveOperationException {
        var method = PersonalDatabaseService.class.getDeclaredMethod("safeAddMovedItems", long.class, ItemStack.class);
        method.setAccessible(true);

        long result = (long) method.invoke(null, Long.MAX_VALUE - 1L, new ItemStack(Items.STONE, 64));

        assertEquals(Long.MAX_VALUE, result);
    }
}
