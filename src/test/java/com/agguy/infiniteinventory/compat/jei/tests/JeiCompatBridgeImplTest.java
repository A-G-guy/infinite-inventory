package com.agguy.infiniteinventory.compat.jei.tests;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JeiCompatBridgeImplTest {

    private static final Class<?> IMPL_CLASS;

    static {
        try {
            IMPL_CLASS = Class.forName("com.agguy.infiniteinventory.compat.jei.JeiCompatBridgeImpl");
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
    void onPlayerLoginShouldNotThrow() throws Exception {
        Object instance = createInstance();
        Method method = IMPL_CLASS.getDeclaredMethod("onPlayerLogin",
                net.minecraft.world.entity.player.Player.class);
        method.setAccessible(true);

        // Pass null player - method is a no-op
        method.invoke(instance, (Object) null);
    }

    @Test
    void shouldImplementJeiCompatBridge() {
        Class<?> bridgeClass = com.agguy.infiniteinventory.compat.jei.JeiCompatBridge.class;

        assertTrue(bridgeClass.isAssignableFrom(IMPL_CLASS));
    }
}
