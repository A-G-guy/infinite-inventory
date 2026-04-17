package com.agguy.infiniteinventory.service.tests;

import com.agguy.infiniteinventory.database.DatabaseStorageSavedData;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.LegacyMigrationState;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.service.PersonalDatabaseService;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonalDatabaseServiceTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void depositMainInventoryShouldReturnLongToAvoidOverflowRegression() throws ReflectiveOperationException {
        var method = PersonalDatabaseService.class.getDeclaredMethod("depositMainInventory", ServerPlayer.class, DatabaseScope.class, String.class);

        assertEquals(long.class, method.getReturnType());
    }

    @Test
    void extractToWorldShouldReturnLongToPreserveDroppedCountContract() throws ReflectiveOperationException {
        var method = PersonalDatabaseService.class.getDeclaredMethod("extractToWorld", ServerPlayer.class, DatabaseScope.class, StoredStackKey.class, long.class);

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

    @Test
    void depositMainInventoryShouldSkipHotbarSlots() throws ReflectiveOperationException {
        var method = PersonalDatabaseService.class.getDeclaredMethod("isPrimaryStorageSlot", int.class);
        method.setAccessible(true);

        assertEquals(false, method.invoke(null, 0));
        assertEquals(false, method.invoke(null, 8));
        assertEquals(true, method.invoke(null, 9));
        assertEquals(true, method.invoke(null, 35));
    }

    @Test
    void pruneStaleMigrationStateShouldClearRetainedMigrationEntries() throws ReflectiveOperationException {
        DatabaseStorageSavedData storage = DatabaseStorageSavedData.fromTag(new CompoundTag(), null);
        UUID playerId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        storage.recordMigrationState(playerId, new LegacyMigrationState(
                LegacyMigrationState.Status.MIGRATED,
                123L,
                8
        ));

        var method = PersonalDatabaseService.class.getDeclaredMethod("pruneStaleMigrationState", DatabaseStorageSavedData.class, UUID.class);
        method.setAccessible(true);
        method.invoke(PersonalDatabaseService.INSTANCE, storage, playerId);

        assertNull(storage.migrationState(playerId));
        assertTrue(storage.isDirty());
    }
}
