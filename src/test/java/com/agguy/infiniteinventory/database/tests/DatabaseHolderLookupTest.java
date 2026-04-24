package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import net.minecraft.core.HolderLookup;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DatabaseHolderLookupTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    private static final Class<?> LOOKUP_CLASS;

    static {
        try {
            LOOKUP_CLASS = Class.forName("com.agguy.infiniteinventory.database.DatabaseHolderLookup");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object invokeStatic(String methodName, Object... args) throws Exception {
        for (Method method : LOOKUP_CLASS.getDeclaredMethods()) {
            if (method.getName().equals(methodName)
                    && method.getParameterCount() == args.length) {
                method.setAccessible(true);
                return method.invoke(null, args);
            }
        }
        throw new NoSuchMethodException(methodName);
    }

    @Test
    void resolveShouldReturnNonNullProviderDirectly() throws Exception {
        HolderLookup.Provider mockProvider = mock(HolderLookup.Provider.class);
        Object result = invokeStatic("resolve", mockProvider);

        assertSame(mockProvider, result);
    }

    @Test
    void resolveShouldFallbackForNullProvider() throws Exception {
        Object result = invokeStatic("resolve", (Object) null);

        assertNotNull(result);
        assertTrue(result instanceof HolderLookup.Provider);
    }

    @Test
    void requireShouldReturnProviderWhenNonNull() throws Exception {
        HolderLookup.Provider mockProvider = mock(HolderLookup.Provider.class);
        Object result = invokeStatic("require", mockProvider, "testAction");

        assertSame(mockProvider, result);
    }

    @Test
    void requireShouldFallbackWhenNullProviderGiven() throws Exception {
        Object result = invokeStatic("require", (Object) null, "testAction");

        assertNotNull(result);
        assertTrue(result instanceof HolderLookup.Provider);
    }
}
