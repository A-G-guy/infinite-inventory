package com.agguy.infiniteinventory.compat.tests;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CuriosCompatBridgeImplTest {

    private static final Class<?> IMPL_CLASS;

    static {
        try {
            IMPL_CLASS = Class.forName("com.agguy.infiniteinventory.compat.CuriosCompatBridgeImpl");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object createInstance() throws Exception {
        Constructor<?> ctor = IMPL_CLASS.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    @Test
    void isAvailableShouldReturnTrue() throws Exception {
        Object instance = createInstance();
        Method method = IMPL_CLASS.getDeclaredMethod("isAvailable");
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(instance);

        assertTrue(result);
    }

    @Test
    void registerDatabaseTerminalAccessoryMethodShouldExist() throws Exception {
        Method method = IMPL_CLASS.getDeclaredMethod("registerDatabaseTerminalAccessory");
        method.setAccessible(true);

        assertNotNull(method);
    }

    @Test
    void isBackSlotEquippedShouldReturnFalseForNullPlayer() throws Exception {
        Object instance = createInstance();
        Method method = IMPL_CLASS.getDeclaredMethod("isBackSlotEquipped",
                net.minecraft.world.entity.player.Player.class, net.minecraft.world.item.Item.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(instance, (Object) null, (Object) null);

        assertTrue(!result);
    }

    @Test
    void appendAccessorySlotsShouldReturnEmptyListForNullPlayer() throws Exception {
        Object instance = createInstance();
        Method method = IMPL_CLASS.getDeclaredMethod("appendAccessorySlots",
                net.minecraft.world.entity.player.Player.class,
                com.agguy.infiniteinventory.compat.MenuSlotAdder.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        java.util.List<?> result = (java.util.List<?>) method.invoke(instance, (Object) null, (Object) null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldImplementCuriosCompatBridge() throws Exception {
        Class<?> bridgeClass = Class.forName("com.agguy.infiniteinventory.compat.CuriosCompatBridge");

        assertTrue(bridgeClass.isAssignableFrom(IMPL_CLASS));
    }
}
