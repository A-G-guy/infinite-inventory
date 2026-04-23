package com.agguy.infiniteinventory.client.screen.tests;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PersonalDatabaseScreenIconPickerGeometry 的单元测试。
 *
 * <p>由于目标类为包级可见，测试通过反射调用。
 */
class PersonalDatabaseScreenIconPickerGeometryTest {

    private static final String CLASS_NAME =
            "com.agguy.infiniteinventory.client.screen.PersonalDatabaseScreenIconPickerGeometry";

    private Class<?> geometryClass() throws ClassNotFoundException {
        return Class.forName(CLASS_NAME);
    }

    @Test
    void shouldHavePrivateConstructor() throws Exception {
        Constructor<?> constructor = geometryClass().getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()), "工具类构造器应为 private");
    }
}
