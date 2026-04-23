package com.agguy.infiniteinventory.event.tests;

import com.agguy.infiniteinventory.event.ModGameEvents;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ModGameEventsTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ModGameEvents> constructor = ModGameEvents.class.getDeclaredConstructor();

        assertTrue(Modifier.isPrivate(constructor.getModifiers()), "工具类构造器应为 private");
    }
}
