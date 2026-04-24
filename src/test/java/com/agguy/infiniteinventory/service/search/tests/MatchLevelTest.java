package com.agguy.infiniteinventory.service.search.tests;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MatchLevel 匹配级别枚举测试（包级可见，通过反射访问）。
 */
class MatchLevelTest {
    private static Class<?> MATCH_LEVEL_CLASS;

    @BeforeAll
    static void setUp() throws ReflectiveOperationException {
        MATCH_LEVEL_CLASS = Class.forName("com.agguy.infiniteinventory.service.search.MatchLevel");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumValue(String name) throws ReflectiveOperationException {
        return Enum.valueOf((Class) MATCH_LEVEL_CLASS, name);
    }

    private static int rankOf(Object enumInstance) throws ReflectiveOperationException {
        Field rankField = MATCH_LEVEL_CLASS.getDeclaredField("rank");
        rankField.setAccessible(true);
        return rankField.getInt(enumInstance);
    }

    @Test
    void noneShouldHaveRankZero() throws ReflectiveOperationException {
        assertEquals(0, rankOf(enumValue("NONE")));
    }

    @Test
    void fuzzyShouldHaveRankOne() throws ReflectiveOperationException {
        assertEquals(1, rankOf(enumValue("FUZZY")));
    }

    @Test
    void containsShouldHaveRankTwo() throws ReflectiveOperationException {
        assertEquals(2, rankOf(enumValue("CONTAINS")));
    }

    @Test
    void prefixShouldHaveRankThree() throws ReflectiveOperationException {
        assertEquals(3, rankOf(enumValue("PREFIX")));
    }

    @Test
    void exactShouldHaveRankFour() throws ReflectiveOperationException {
        assertEquals(4, rankOf(enumValue("EXACT")));
    }

    @Test
    void ranksShouldBeInIncreasingOrder() throws ReflectiveOperationException {
        java.lang.reflect.Method valuesMethod = MATCH_LEVEL_CLASS.getDeclaredMethod("values");
        valuesMethod.setAccessible(true);
        Object[] levels = (Object[]) valuesMethod.invoke(null);
        for (int i = 1; i < levels.length; i++) {
            assertTrue(rankOf(levels[i]) > rankOf(levels[i - 1]),
                    levels[i] + " 的 rank 应大于 " + levels[i - 1]);
        }
    }

    @Test
    void valueOfShouldReturnCorrectEnum() throws ReflectiveOperationException {
        assertEquals(enumValue("NONE"), enumValue("NONE"));
        assertEquals(enumValue("FUZZY"), enumValue("FUZZY"));
        assertEquals(enumValue("CONTAINS"), enumValue("CONTAINS"));
        assertEquals(enumValue("PREFIX"), enumValue("PREFIX"));
        assertEquals(enumValue("EXACT"), enumValue("EXACT"));
    }
}
