package com.agguy.infiniteinventory.service.tests;

import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PersonalDatabaseServiceHelper 与 PersonalDatabaseServiceStorageHelper 的纯逻辑方法测试。
 */
class PersonalDatabaseServiceHelperTest {
    private static final Method ENTRY_TAB_ID;
    private static final Method SERVICE_HELPER_SAFE_ADD;
    private static final Method STORAGE_HELPER_SAFE_ADD;
    private static final Method IS_PRIMARY_STORAGE_SLOT;

    static {
        MinecraftTestBootstrap.ensureBootstrapped();
        try {
            Class<?> helperClass = Class.forName("com.agguy.infiniteinventory.service.PersonalDatabaseServiceHelper");
            ENTRY_TAB_ID = helperClass.getDeclaredMethod("entryTabId", StoredItemDatabase.class, StoredStackKey.class);
            ENTRY_TAB_ID.setAccessible(true);
            SERVICE_HELPER_SAFE_ADD = helperClass.getDeclaredMethod("safeAddMovedItems", long.class, long.class);
            SERVICE_HELPER_SAFE_ADD.setAccessible(true);

            Class<?> storageHelperClass = Class.forName("com.agguy.infiniteinventory.service.PersonalDatabaseServiceStorageHelper");
            STORAGE_HELPER_SAFE_ADD = storageHelperClass.getDeclaredMethod("safeAddMovedItems", long.class, ItemStack.class);
            STORAGE_HELPER_SAFE_ADD.setAccessible(true);
            IS_PRIMARY_STORAGE_SLOT = storageHelperClass.getDeclaredMethod("isPrimaryStorageSlot", int.class);
            IS_PRIMARY_STORAGE_SLOT.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------- entryTabId ----------

    @Test
    void entryTabIdShouldReturnDefaultForMissingKey() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));
        assertEquals(DatabaseTabs.DEFAULT_TAB_ID, ENTRY_TAB_ID.invoke(null, database, key));
    }

    @Test
    void entryTabIdShouldReturnTabIdForExistingEntry() throws ReflectiveOperationException {
        StoredItemDatabase database = new StoredItemDatabase();
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));
        database.store(new ItemStack(Items.STONE, 1), "custom_tab");
        assertEquals("custom_tab", ENTRY_TAB_ID.invoke(null, database, key));
    }

    // ---------- safeAddMovedItems (long + long) ----------

    @Test
    void serviceHelperSafeAddShouldReturnCurrentTotalWhenMovedIsZero() throws ReflectiveOperationException {
        assertEquals(10L, (long) SERVICE_HELPER_SAFE_ADD.invoke(null, 10L, 0L));
    }

    @Test
    void serviceHelperSafeAddShouldReturnCurrentTotalWhenMovedIsNegative() throws ReflectiveOperationException {
        assertEquals(10L, (long) SERVICE_HELPER_SAFE_ADD.invoke(null, 10L, -3L));
    }

    @Test
    void serviceHelperSafeAddShouldAddNormally() throws ReflectiveOperationException {
        assertEquals(25L, (long) SERVICE_HELPER_SAFE_ADD.invoke(null, 10L, 15L));
    }

    @Test
    void serviceHelperSafeAddShouldSaturateAtMaxValue() throws ReflectiveOperationException {
        assertEquals(Long.MAX_VALUE, (long) SERVICE_HELPER_SAFE_ADD.invoke(null, Long.MAX_VALUE - 5L, 10L));
    }

    // ---------- safeAddMovedItems (long + ItemStack) ----------

    @Test
    void storageHelperSafeAddShouldReturnCurrentTotalForNullStack() throws ReflectiveOperationException {
        assertEquals(10L, (long) STORAGE_HELPER_SAFE_ADD.invoke(null, 10L, (ItemStack) null));
    }

    @Test
    void storageHelperSafeAddShouldReturnCurrentTotalForEmptyStack() throws ReflectiveOperationException {
        assertEquals(10L, (long) STORAGE_HELPER_SAFE_ADD.invoke(null, 10L, ItemStack.EMPTY));
    }

    @Test
    void storageHelperSafeAddShouldAddStackCount() throws ReflectiveOperationException {
        assertEquals(74L, (long) STORAGE_HELPER_SAFE_ADD.invoke(null, 10L, new ItemStack(Items.STONE, 64)));
    }

    @Test
    void storageHelperSafeAddShouldSaturateAtMaxValue() throws ReflectiveOperationException {
        assertEquals(Long.MAX_VALUE, (long) STORAGE_HELPER_SAFE_ADD.invoke(null, Long.MAX_VALUE - 10L, new ItemStack(Items.STONE, 64)));
    }

    // ---------- isPrimaryStorageSlot ----------

    @Test
    void isPrimaryStorageSlotShouldReturnFalseForHotbarOnly() throws ReflectiveOperationException {
        assertFalse((boolean) IS_PRIMARY_STORAGE_SLOT.invoke(null, 0));
        assertFalse((boolean) IS_PRIMARY_STORAGE_SLOT.invoke(null, 8));
    }

    @Test
    void isPrimaryStorageSlotShouldReturnTrueForMainInventory() throws ReflectiveOperationException {
        assertTrue((boolean) IS_PRIMARY_STORAGE_SLOT.invoke(null, 9));
        assertTrue((boolean) IS_PRIMARY_STORAGE_SLOT.invoke(null, 35));
    }
}
