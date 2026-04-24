package com.agguy.infiniteinventory.service.search.tests;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenMatchTest {

    private static final Class<?> TOKEN_MATCH_CLASS;
    private static final Class<?> MATCH_LEVEL_CLASS;
    private static final Class<?> SEARCH_FIELD_CLASS;

    static {
        try {
            TOKEN_MATCH_CLASS = Class.forName("com.agguy.infiniteinventory.service.search.TokenMatch");
            MATCH_LEVEL_CLASS = Class.forName("com.agguy.infiniteinventory.service.search.MatchLevel");
            SEARCH_FIELD_CLASS = Class.forName("com.agguy.infiniteinventory.database.DatabaseSearchField");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object createTokenMatch(Object field, Object level, double score) throws Exception {
        Constructor<?> ctor = TOKEN_MATCH_CLASS.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        return ctor.newInstance(field, level, score);
    }

    private static Object callStatic(String methodName) throws Exception {
        Method method = TOKEN_MATCH_CLASS.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(null);
    }

    private static Object callMethod(Object instance, String methodName, Object... args) throws Exception {
        for (Method method : TOKEN_MATCH_CLASS.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                method.setAccessible(true);
                return method.invoke(instance, args);
            }
        }
        throw new NoSuchMethodException(methodName);
    }

    private static Object enumValue(Class<?> enumClass, String name) {
        for (Object e : enumClass.getEnumConstants()) {
            if (e.toString().equals(name)) {
                return e;
            }
        }
        return null;
    }

    @Test
    void noMatchShouldReturnNonMatched() throws Exception {
        Object noMatch = callStatic("noMatch");

        assertNotNull(noMatch);
        assertFalse((boolean) callMethod(noMatch, "matched"));
    }

    @Test
    void matchedShouldReturnTrueForNonNoneLevel() throws Exception {
        Object match = createTokenMatch(null, enumValue(MATCH_LEVEL_CLASS, "EXACT"), 1.0);

        assertTrue((boolean) callMethod(match, "matched"));
    }

    @Test
    void matchedShouldReturnFalseForNoneLevel() throws Exception {
        Object match = createTokenMatch(null, enumValue(MATCH_LEVEL_CLASS, "NONE"), 0.0);

        assertFalse((boolean) callMethod(match, "matched"));
    }

    @Test
    void noMatchShouldReturnSameWhenUnmatched() throws Exception {
        Object noMatch = callStatic("noMatch");
        Object result = callMethod(noMatch, "withField",
                enumValue(SEARCH_FIELD_CLASS, "DISPLAY_NAME"));

        assertEquals(noMatch, result);
    }

    @Test
    void withFieldShouldUpdateFieldWhenMatched() throws Exception {
        Object match = createTokenMatch(null, enumValue(MATCH_LEVEL_CLASS, "EXACT"), 1.0);
        Object updated = callMethod(match, "withField",
                enumValue(SEARCH_FIELD_CLASS, "DISPLAY_NAME"));

        Object field = callMethod(updated, "field");
        assertEquals(enumValue(SEARCH_FIELD_CLASS, "DISPLAY_NAME"), field);
    }

    @Test
    void betterOfShouldReturnNonNullWhenOtherIsNull() throws Exception {
        Object match = createTokenMatch(null, enumValue(MATCH_LEVEL_CLASS, "EXACT"), 1.0);
        Object result = callMethod(match, "betterOf", (Object) null);

        assertEquals(match, result);
    }

    @Test
    void betterOfShouldReturnHigherLevel() throws Exception {
        Object exact = createTokenMatch(null, enumValue(MATCH_LEVEL_CLASS, "EXACT"), 0.5);
        Object prefix = createTokenMatch(null, enumValue(MATCH_LEVEL_CLASS, "PREFIX"), 0.9);

        Object result = callMethod(exact, "betterOf", prefix);

        assertEquals(exact, result);
    }

    @Test
    void betterOfShouldReturnHigherScoreWhenLevelsEqual() throws Exception {
        Object highScore = createTokenMatch(null, enumValue(MATCH_LEVEL_CLASS, "EXACT"), 0.9);
        Object lowScore = createTokenMatch(null, enumValue(MATCH_LEVEL_CLASS, "EXACT"), 0.5);

        Object result = callMethod(highScore, "betterOf", lowScore);

        assertEquals(highScore, result);
    }

    @Test
    void isBetterThanShouldReturnTrueForStrictlyBetter() throws Exception {
        Object exact = createTokenMatch(null, enumValue(MATCH_LEVEL_CLASS, "EXACT"), 1.0);
        Object contains = createTokenMatch(null, enumValue(MATCH_LEVEL_CLASS, "CONTAINS"), 1.0);

        assertTrue((boolean) callMethod(exact, "isBetterThan", contains));
    }
}
