package com.agguy.infiniteinventory.database.tests;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * StoredItemDatabaseHelper 包级可见方法的白盒测试。
 *
 * <p>覆盖安全加法与 firstAdded 合并等纯逻辑方法。</p>
 */
class StoredItemDatabaseHelperTest {
    private static final Method SAFE_ADD;
    private static final Method MERGE_FIRST_ADDED;

    static {
        try {
            Class<?> helperClass = Class.forName("com.agguy.infiniteinventory.database.StoredItemDatabaseHelper");
            SAFE_ADD = helperClass.getDeclaredMethod("safeAdd", long.class, long.class);
            SAFE_ADD.setAccessible(true);
            MERGE_FIRST_ADDED = helperClass.getDeclaredMethod("mergeFirstAdded", long.class, long.class);
            MERGE_FIRST_ADDED.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------- safeAdd ----------

    @Test
    void safeAddShouldReturnLeftWhenRightIsZero() throws ReflectiveOperationException {
        assertEquals(10L, SAFE_ADD.invoke(null, 10L, 0L));
    }

    @Test
    void safeAddShouldReturnLeftWhenRightIsNegative() throws ReflectiveOperationException {
        assertEquals(10L, SAFE_ADD.invoke(null, 10L, -5L));
    }

    @Test
    void safeAddShouldPerformNormalAddition() throws ReflectiveOperationException {
        assertEquals(15L, SAFE_ADD.invoke(null, 10L, 5L));
    }

    @Test
    void safeAddShouldSaturateAtLongMaxValue() throws ReflectiveOperationException {
        assertEquals(Long.MAX_VALUE, SAFE_ADD.invoke(null, Long.MAX_VALUE - 5L, 10L));
    }

    @Test
    void safeAddShouldSaturateAtLongMaxValueForMaxPlusMax() throws ReflectiveOperationException {
        assertEquals(Long.MAX_VALUE, SAFE_ADD.invoke(null, Long.MAX_VALUE, Long.MAX_VALUE));
    }

    // ---------- mergeFirstAdded ----------

    @Test
    void mergeFirstAddedShouldReturnIncomingWhenExistingIsZero() throws ReflectiveOperationException {
        assertEquals(5L, MERGE_FIRST_ADDED.invoke(null, 0L, 5L));
    }

    @Test
    void mergeFirstAddedShouldReturnExistingWhenIncomingIsZero() throws ReflectiveOperationException {
        assertEquals(3L, MERGE_FIRST_ADDED.invoke(null, 3L, 0L));
    }

    @Test
    void mergeFirstAddedShouldReturnMinimumWhenBothNonZero() throws ReflectiveOperationException {
        assertEquals(3L, MERGE_FIRST_ADDED.invoke(null, 3L, 10L));
        assertEquals(2L, MERGE_FIRST_ADDED.invoke(null, 10L, 2L));
    }

    @Test
    void mergeFirstAddedShouldNormalizeNegativeValues() throws ReflectiveOperationException {
        assertEquals(5L, MERGE_FIRST_ADDED.invoke(null, -1L, 5L));
        assertEquals(3L, MERGE_FIRST_ADDED.invoke(null, 3L, -1L));
    }
}
