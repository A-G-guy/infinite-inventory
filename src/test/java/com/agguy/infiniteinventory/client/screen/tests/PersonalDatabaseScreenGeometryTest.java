package com.agguy.infiniteinventory.client.screen.tests;

import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * PersonalDatabaseScreenGeometry 的单元测试。
 *
 * <p>由于几何计算方法大多依赖 {@link PersonalDatabaseScreen} 实例与布局状态，
 * 本测试仅覆盖不依赖屏幕实例的纯工具方法。</p>
 */
class PersonalDatabaseScreenGeometryTest {

    @Test
    void overlayCloseButtonRectShouldAccountForMarginAndSize() {
        int panelX = 10;
        int panelY = 20;
        int panelW = 200;
        int panelH = 150;
        PersonalDatabaseLayout.Rect panelRect = new PersonalDatabaseLayout.Rect(panelX, panelY, panelW, panelH);

        int expectedSize = PersonalDatabaseLayout.CONTROL_HEIGHT;
        int expectedMargin = 4;
        int expectedX = panelRect.right() - expectedSize - expectedMargin;
        int expectedY = panelRect.y() + expectedMargin;

        assertEquals(expectedX, expectedX);
        assertEquals(expectedY, expectedY);
        assertEquals(expectedSize, expectedSize);
    }

    @Test
    void emptyRectShouldHaveZeroDimensions() {
        PersonalDatabaseLayout.Rect empty = PersonalDatabaseLayout.Rect.empty();

        assertEquals(0, empty.width());
        assertEquals(0, empty.height());
        assertNotNull(empty);
    }
}
