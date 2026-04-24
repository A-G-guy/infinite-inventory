package com.agguy.infiniteinventory.service.tests;

import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseStorageSavedData;
import com.agguy.infiniteinventory.database.LegacyMigrationState;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackEntry;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.database.tests.DatabaseTestReflectionHelper;
import com.agguy.infiniteinventory.service.PersonalDatabaseService;
import com.agguy.infiniteinventory.service.PersonalDatabaseServiceHelper;
import com.agguy.infiniteinventory.service.PersonalDatabaseServiceMigrationHelper;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        Class<?> helperClass = Class.forName("com.agguy.infiniteinventory.service.PersonalDatabaseServiceStorageHelper");
        var method = helperClass.getDeclaredMethod("safeAddMovedItems", long.class, ItemStack.class);
        method.setAccessible(true);

        long result = (long) method.invoke(null, (long) Integer.MAX_VALUE, new ItemStack(Items.STONE, 64));

        assertEquals((long) Integer.MAX_VALUE + 64L, result);
    }

    @Test
    void safeAddMovedItemsShouldSaturateAtLongMaxValue() throws ReflectiveOperationException {
        Class<?> helperClass = Class.forName("com.agguy.infiniteinventory.service.PersonalDatabaseServiceStorageHelper");
        var method = helperClass.getDeclaredMethod("safeAddMovedItems", long.class, ItemStack.class);
        method.setAccessible(true);

        long result = (long) method.invoke(null, Long.MAX_VALUE - 1L, new ItemStack(Items.STONE, 64));

        assertEquals(Long.MAX_VALUE, result);
    }

    @Test
    void depositMainInventoryShouldSkipHotbarSlots() throws ReflectiveOperationException {
        var method = PersonalDatabaseServiceHelper.class.getDeclaredMethod("isPrimaryStorageSlot", int.class);
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

        var method = PersonalDatabaseServiceMigrationHelper.class.getDeclaredMethod("pruneStaleMigrationState", DatabaseStorageSavedData.class, UUID.class);
        method.setAccessible(true);
        method.invoke(null, storage, playerId);

        assertNull(storage.migrationState(playerId));
        assertTrue(storage.isDirty());
    }

    @Test
    void safeAddMovedItemsLongShouldAccumulateNormally() throws ReflectiveOperationException {
        var method = PersonalDatabaseServiceHelper.class.getDeclaredMethod("safeAddMovedItems", long.class, long.class);
        method.setAccessible(true);

        assertEquals(200L, (long) method.invoke(null, 100L, 100L));
    }

    @Test
    void safeAddMovedItemsLongShouldIgnoreZeroOrNegativeMovedItems() throws ReflectiveOperationException {
        var method = PersonalDatabaseServiceHelper.class.getDeclaredMethod("safeAddMovedItems", long.class, long.class);
        method.setAccessible(true);

        assertEquals(100L, (long) method.invoke(null, 100L, 0L));
        assertEquals(100L, (long) method.invoke(null, 100L, -1L));
    }

    @Test
    void safeAddMovedItemsLongShouldSaturateAtLongMaxValue() throws ReflectiveOperationException {
        var method = PersonalDatabaseServiceHelper.class.getDeclaredMethod("safeAddMovedItems", long.class, long.class);
        method.setAccessible(true);

        assertEquals(Long.MAX_VALUE, (long) method.invoke(null, Long.MAX_VALUE - 10L, 20L));
        assertEquals(Long.MAX_VALUE, (long) method.invoke(null, Long.MAX_VALUE, 1L));
    }

    @Test
    void entryTabIdShouldReturnDefaultForMissingKey() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        StoredStackKey key = DatabaseTestReflectionHelper.fakeKey("minecraft:stone");

        var method = PersonalDatabaseServiceHelper.class.getDeclaredMethod("entryTabId", StoredItemDatabase.class,
                StoredStackKey.class);
        method.setAccessible(true);

        assertEquals("__default", method.invoke(null, database, key));
    }

    @Test
    void entryTabIdShouldReturnActualTabIdForExistingEntry() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        StoredStackKey key = DatabaseTestReflectionHelper.fakeKey("minecraft:stone");
        DatabaseTestReflectionHelper.forceEntry(database, key,
                new StoredStackEntry("my_tab", 64L, 100L, 100L));

        var method = PersonalDatabaseServiceHelper.class.getDeclaredMethod("entryTabId", StoredItemDatabase.class,
                StoredStackKey.class);
        method.setAccessible(true);

        assertEquals("my_tab", method.invoke(null, database, key));
    }

    @Test
    void queryIncludesPublicScopeShouldReturnFalseForNullQuery() throws ReflectiveOperationException {
        Class<?> helperClass = Class.forName(
                "com.agguy.infiniteinventory.service.PersonalDatabaseServiceViewerHelper");
        var method = helperClass.getDeclaredMethod("queryIncludesPublicScope", DatabaseQuery.class);
        method.setAccessible(true);

        assertFalse((boolean) method.invoke(null, (Object) null));
    }

    @Test
    void queryIncludesPublicScopeShouldReturnTrueWhenPublicTabIsVisible() throws ReflectiveOperationException {
        DatabaseQuery query = new DatabaseQuery(
                DatabaseScopedTabRef.concreteTab(DatabaseScope.PUBLIC, "test"),
                List.of(DatabaseScopedTabRef.concreteTab(DatabaseScope.PUBLIC, "test")),
                new java.util.LinkedHashMap<>(),
                List.of()
        );

        Class<?> helperClass = Class.forName(
                "com.agguy.infiniteinventory.service.PersonalDatabaseServiceViewerHelper");
        var method = helperClass.getDeclaredMethod("queryIncludesPublicScope", DatabaseQuery.class);
        method.setAccessible(true);

        assertTrue((boolean) method.invoke(null, query));
    }

    @Test
    void queryIncludesPublicScopeShouldReturnFalseWhenOnlyPersonalTabsVisible() throws ReflectiveOperationException {
        DatabaseQuery query = new DatabaseQuery(
                DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "test"),
                List.of(DatabaseScopedTabRef.concreteTab(DatabaseScope.PERSONAL, "test")),
                new java.util.LinkedHashMap<>(),
                List.of()
        );

        Class<?> helperClass = Class.forName(
                "com.agguy.infiniteinventory.service.PersonalDatabaseServiceViewerHelper");
        var method = helperClass.getDeclaredMethod("queryIncludesPublicScope", DatabaseQuery.class);
        method.setAccessible(true);

        assertFalse((boolean) method.invoke(null, query));
    }
}
