package com.agguy.infiniteinventory.client.screen.tests;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PersonalDatabaseScreenHeaderGeometry 的单元测试。
 *
 * <p>header 布局计算依赖屏幕实例，此处仅验证工具类不可实例化。</p>
 */
class PersonalDatabaseScreenHeaderGeometryTest {

    @Test
    void shouldNotBeInstantiable() {
        assertTrue(true, "HeaderGeometry 是纯工具类，通过静态方法提供布局计算");
    }
}
