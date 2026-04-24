package com.agguy.infiniteinventory.compat.tests;

import com.agguy.infiniteinventory.compat.VanillaPlayerInventoryPaneProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VanillaPlayerInventoryPaneProvider 常量返回值测试。
 *
 * <p>render 方法依赖 Minecraft 客户端运行时，不在此测试。</p>
 */
class VanillaPlayerInventoryPaneProviderTest {

    private final VanillaPlayerInventoryPaneProvider provider = new VanillaPlayerInventoryPaneProvider();

    @Test
    void equipmentPanelWidthShouldReturn176() {
        assertEquals(176, provider.equipmentPanelWidth());
    }

    @Test
    void equipmentPanelHeightShouldReturn83() {
        assertEquals(83, provider.equipmentPanelHeight());
    }

    @Test
    void bottomInventoryWidthShouldReturn176() {
        assertEquals(176, provider.bottomInventoryWidth());
    }

    @Test
    void bottomInventoryHeightShouldReturn83() {
        assertEquals(83, provider.bottomInventoryHeight());
    }

    @Test
    void dimensionsShouldBeSymmetric() {
        // 装备面板和底部背包宽度一致
        assertEquals(provider.equipmentPanelWidth(), provider.bottomInventoryWidth());
    }
}
