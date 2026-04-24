package com.agguy.infiniteinventory.compat.jei.tests;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NoOpJeiCompatBridge 包级可见类的反射测试。
 */
class NoOpJeiCompatBridgeTest {

    private static Object bridge;
    private static Method isAvailable;
    private static Method onPlayerLogin;
    private static Method syncAmounts;

    @BeforeAll
    static void setUp() throws ReflectiveOperationException {
        Class<?> clazz = Class.forName("com.agguy.infiniteinventory.compat.jei.NoOpJeiCompatBridge");
        java.lang.reflect.Constructor<?> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        bridge = constructor.newInstance();
        isAvailable = clazz.getDeclaredMethod("isAvailable");
        isAvailable.setAccessible(true);
        onPlayerLogin = clazz.getDeclaredMethod("onPlayerLogin", net.minecraft.world.entity.player.Player.class);
        onPlayerLogin.setAccessible(true);
        syncAmounts = clazz.getDeclaredMethod("syncAmounts", net.minecraft.world.entity.player.Player.class, Map.class, Map.class);
        syncAmounts.setAccessible(true);
    }

    @Test
    void isAvailableShouldReturnFalse() throws ReflectiveOperationException {
        assertFalse((boolean) isAvailable.invoke(bridge));
    }

    @Test
    void onPlayerLoginShouldBeNoOp() throws ReflectiveOperationException {
        // 不应抛出异常
        onPlayerLogin.invoke(bridge, (net.minecraft.world.entity.player.Player) null);
    }

    @Test
    void syncAmountsShouldBeNoOp() throws ReflectiveOperationException {
        // 不应抛出异常
        syncAmounts.invoke(bridge, (net.minecraft.world.entity.player.Player) null, (Map<?, ?>) null, (Map<?, ?>) null);
    }
}
