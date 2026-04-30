package com.agguy.infiniteinventory.service.tests;

import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PersonalDatabaseServiceStatisticsHelper 纯逻辑方法测试。
 */
class PersonalDatabaseServiceStatisticsHelperTest {

    private static final Method SAFE_ADD;

    static {
        MinecraftTestBootstrap.ensureBootstrapped();
        try {
            Class<?> clazz = Class.forName("com.agguy.infiniteinventory.service.PersonalDatabaseServiceStatisticsHelper");
            SAFE_ADD = clazz.getDeclaredMethod("safeAdd", long.class, long.class);
            SAFE_ADD.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeAll
    static void beforeAll() {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void safeAddShouldReturnSumForNormalValues() throws ReflectiveOperationException {
        assertEquals(30L, SAFE_ADD.invoke(null, 10L, 20L));
    }

    @Test
    void safeAddShouldReturnLeftWhenRightIsZero() throws ReflectiveOperationException {
        assertEquals(100L, SAFE_ADD.invoke(null, 100L, 0L));
    }

    @Test
    void safeAddShouldReturnLeftWhenRightIsNegative() throws ReflectiveOperationException {
        assertEquals(100L, SAFE_ADD.invoke(null, 100L, -10L));
    }

    @Test
    void safeAddShouldCapAtMaxLongOnOverflow() throws ReflectiveOperationException {
        assertEquals(Long.MAX_VALUE, SAFE_ADD.invoke(null, Long.MAX_VALUE - 5L, 10L));
    }

    @Test
    void safeAddShouldHandleMaxLongExactly() throws ReflectiveOperationException {
        assertEquals(Long.MAX_VALUE, SAFE_ADD.invoke(null, Long.MAX_VALUE, 0L));
    }

    @Test
    void safeAddShouldReturnRightWhenLeftIsZero() throws ReflectiveOperationException {
        assertEquals(42L, SAFE_ADD.invoke(null, 0L, 42L));
    }
}
