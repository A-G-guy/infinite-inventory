package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseViewState;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestScreenFull {
    @Test
    void testUnsafeAllocateAndSetFields() throws Exception {
        java.lang.reflect.Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) f.get(null);
        PersonalDatabaseScreen screen = (PersonalDatabaseScreen) unsafe.allocateInstance(PersonalDatabaseScreen.class);
        
        // 设置 layout 字段
        java.lang.reflect.Field layoutField = PersonalDatabaseScreen.class.getDeclaredField("layout");
        layoutField.setAccessible(true);
        PersonalDatabaseLayout layout = PersonalDatabaseLayout.create(800, 600, 176, 76, 176, 76, java.util.List.of());
        layoutField.set(screen, layout);
        
        // 设置 databaseMenu 字段
        java.lang.reflect.Field menuField = PersonalDatabaseScreen.class.getDeclaredField("databaseMenu");
        menuField.setAccessible(true);
        // 不设置 menu，测试 layout != null 但 menu == null 的情况
        
        // 测试 overlayCloseButtonRect（不依赖 screen.layout）
        PersonalDatabaseLayout.Rect panel = new PersonalDatabaseLayout.Rect(100, 100, 200, 150);
        PersonalDatabaseLayout.Rect close = PersonalDatabaseScreenGeometry.overlayCloseButtonRect(panel);
        assertEquals(276, close.x());
        assertEquals(104, close.y());
        assertEquals(20, close.width());
        assertEquals(20, close.height());
        
        // 测试 centeredOverlayRect（依赖 screen.layout）
        PersonalDatabaseLayout.Rect centered = PersonalDatabaseScreenGeometry.centeredOverlayRect(screen, 100, 50);
        assertNotNull(centered);
        assertTrue(centered.width() > 0);
    }
}
