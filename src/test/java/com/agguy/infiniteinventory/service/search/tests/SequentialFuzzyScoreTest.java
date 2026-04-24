package com.agguy.infiniteinventory.service.search.tests;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SequentialFuzzyScore 顺序模糊匹配评分白盒测试（包级可见，通过反射访问）。
 */
class SequentialFuzzyScoreTest {
    private static Constructor<?> CONSTRUCTOR;
    private static Method SCORE;

    private static Object scorer;

    @BeforeAll
    static void setUp() throws ReflectiveOperationException {
        Class<?> clazz = Class.forName("com.agguy.infiniteinventory.service.search.SequentialFuzzyScore");
        CONSTRUCTOR = clazz.getDeclaredConstructor(Locale.class);
        CONSTRUCTOR.setAccessible(true);
        SCORE = clazz.getDeclaredMethod("score", String.class, String.class);
        SCORE.setAccessible(true);
        scorer = CONSTRUCTOR.newInstance(Locale.ROOT);
    }

    private int invokeScore(Object scorerInstance, String candidate, String query) throws ReflectiveOperationException {
        return (int) SCORE.invoke(scorerInstance, candidate, query);
    }

    @Test
    void constructorShouldThrowForNullLocale() {
        assertThrows(Exception.class, () -> CONSTRUCTOR.newInstance((Locale) null));
    }

    @Test
    void scoreShouldReturnZeroForNullCandidate() throws ReflectiveOperationException {
        assertEquals(0, invokeScore(scorer, null, "query"));
    }

    @Test
    void scoreShouldReturnZeroForNullQuery() throws ReflectiveOperationException {
        assertEquals(0, invokeScore(scorer, "candidate", null));
    }

    @Test
    void scoreShouldReturnZeroForEmptyCandidate() throws ReflectiveOperationException {
        assertEquals(0, invokeScore(scorer, "", "query"));
    }

    @Test
    void scoreShouldReturnZeroForEmptyQuery() throws ReflectiveOperationException {
        assertEquals(0, invokeScore(scorer, "candidate", ""));
    }

    @Test
    void scoreShouldReturnZeroForNoMatch() throws ReflectiveOperationException {
        assertEquals(0, invokeScore(scorer, "abcdef", "xyz"));
    }

    @Test
    void scoreShouldReturnPositiveForExactMatch() throws ReflectiveOperationException {
        int score = invokeScore(scorer, "hello", "hello");
        assertTrue(score > 0);
    }

    @Test
    void scoreShouldAwardConsecutiveBonus() throws ReflectiveOperationException {
        int consecutiveScore = invokeScore(scorer, "abcdef", "abc");
        int scatteredScore = invokeScore(scorer, "abcdef", "ace");
        assertTrue(consecutiveScore > scatteredScore,
                "连续匹配得分 (" + consecutiveScore + ") 应高于分散匹配得分 (" + scatteredScore + ")");
    }

    @Test
    void scoreShouldBeCaseInsensitive() throws ReflectiveOperationException {
        assertEquals(invokeScore(scorer, "Hello", "hello"),
                invokeScore(scorer, "hello", "hello"));
    }

    @Test
    void scoreShouldHandlePartialMatchAtEnd() throws ReflectiveOperationException {
        int score = invokeScore(scorer, "minecraft", "craft");
        assertTrue(score > 0);
    }

    @Test
    void scoreShouldIncreaseWithMoreMatches() throws ReflectiveOperationException {
        int score2 = invokeScore(scorer, "minecraft", "mi");
        int score4 = invokeScore(scorer, "minecraft", "mine");
        assertTrue(score4 > score2);
    }

    @Test
    void scoreShouldHandleDifferentLocale() throws ReflectiveOperationException {
        Object turkishScorer = CONSTRUCTOR.newInstance(new Locale("tr"));
        int score = invokeScore(turkishScorer, "istanbul", "istanbul");
        assertTrue(score >= 0);
    }
}
