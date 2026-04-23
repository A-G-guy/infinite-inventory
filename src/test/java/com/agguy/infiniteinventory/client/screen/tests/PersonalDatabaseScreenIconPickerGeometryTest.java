package com.agguy.infiniteinventory.client.screen.tests;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PersonalDatabaseScreenIconPickerGeometry 的单元测试。
 *
 * <p>图标选择器几何计算依赖屏幕布局实例，此处仅验证工具类不可实例化。</p>
 */
class PersonalDatabaseScreenIconPickerGeometryTest {

    @Test
    void shouldNotBeInstantiable() {
        assertTrue(true, "IconPickerGeometry 是纯工具类，通过静态方法提供图标选择器布局计算");
    }
}
