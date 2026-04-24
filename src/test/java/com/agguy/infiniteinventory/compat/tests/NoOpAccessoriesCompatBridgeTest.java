package com.agguy.infiniteinventory.compat.tests;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NoOpAccessoriesCompatBridge 包级可见类的反射测试。
 */
class NoOpAccessoriesCompatBridgeTest {

    private static Object bridge;
    private static Method isAvailable;
    private static Method registerDatabaseTerminalAccessory;
    private static Method isBackSlotEquipped;
    private static Method appendAccessorySlots;

    @BeforeAll
    static void setUp() throws ReflectiveOperationException {
        Class<?> clazz = Class.forName("com.agguy.infiniteinventory.compat.NoOpAccessoriesCompatBridge");
        java.lang.reflect.Constructor<?> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        bridge = constructor.newInstance();
        isAvailable = clazz.getDeclaredMethod("isAvailable");
        isAvailable.setAccessible(true);
        registerDatabaseTerminalAccessory = clazz.getDeclaredMethod("registerDatabaseTerminalAccessory");
        registerDatabaseTerminalAccessory.setAccessible(true);
        isBackSlotEquipped = clazz.getDeclaredMethod("isBackSlotEquipped", net.minecraft.world.entity.player.Player.class, net.minecraft.world.item.Item.class);
        isBackSlotEquipped.setAccessible(true);
        appendAccessorySlots = clazz.getDeclaredMethod("appendAccessorySlots", net.minecraft.world.entity.player.Player.class, com.agguy.infiniteinventory.compat.MenuSlotAdder.class);
        appendAccessorySlots.setAccessible(true);
    }

    @Test
    void isAvailableShouldReturnFalse() throws ReflectiveOperationException {
        assertFalse((boolean) isAvailable.invoke(bridge));
    }

    @Test
    void registerDatabaseTerminalAccessoryShouldBeNoOp() throws ReflectiveOperationException {
        // 不应抛出异常
        registerDatabaseTerminalAccessory.invoke(bridge);
    }

    @Test
    void isBackSlotEquippedShouldReturnFalseForNullPlayer() throws ReflectiveOperationException {
        assertFalse((boolean) isBackSlotEquipped.invoke(bridge, (net.minecraft.world.entity.player.Player) null, (net.minecraft.world.item.Item) null));
    }

    @Test
    void appendAccessorySlotsShouldReturnEmptyListForNullArgs() throws ReflectiveOperationException {
        @SuppressWarnings("unchecked")
        List<?> result = (List<?>) appendAccessorySlots.invoke(bridge, (net.minecraft.world.entity.player.Player) null, (com.agguy.infiniteinventory.compat.MenuSlotAdder) null);
        assertTrue(result.isEmpty());
    }
}
