package com.agguy.infiniteinventory.client.screen.tests;

import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PersonalDatabaseScreenGeometry 的单元测试。
 *
 * <p>由于目标类为包级可见，测试通过反射调用。
 */
class PersonalDatabaseScreenGeometryTest {

    private static final String CLASS_NAME =
            "com.agguy.infiniteinventory.client.screen.PersonalDatabaseScreenGeometry";

    private Class<?> geometryClass() throws ClassNotFoundException {
        return Class.forName(CLASS_NAME);
    }

    @Test
    void shouldHavePrivateConstructor() throws Exception {
        Constructor<?> constructor = geometryClass().getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()), "工具类构造器应为 private");
    }

    @Test
    void overlayCloseButtonRectShouldAccountForMarginAndSize() throws Exception {
        int panelX = 10;
        int panelY = 20;
        int panelW = 200;
        int panelH = 150;
        PersonalDatabaseLayout.Rect panelRect = new PersonalDatabaseLayout.Rect(panelX, panelY, panelW, panelH);

        int expectedSize = PersonalDatabaseLayout.CONTROL_HEIGHT;
        int expectedMargin = 4;
        int expectedX = panelRect.right() - expectedSize - expectedMargin;
        int expectedY = panelRect.y() + expectedMargin;

        Method method = geometryClass().getDeclaredMethod("overlayCloseButtonRect", PersonalDatabaseLayout.Rect.class);
        method.setAccessible(true);
        PersonalDatabaseLayout.Rect result = (PersonalDatabaseLayout.Rect) method.invoke(null, panelRect);

        assertEquals(expectedX, result.x());
        assertEquals(expectedY, result.y());
        assertEquals(expectedSize, result.width());
        assertEquals(expectedSize, result.height());
    }

    @Test
    void emptyRectShouldHaveZeroDimensions() {
        PersonalDatabaseLayout.Rect empty = PersonalDatabaseLayout.Rect.empty();

        assertEquals(0, empty.width());
        assertEquals(0, empty.height());
        assertNotNull(empty);
    }
}
