package com.agguy.infiniteinventory.command.tests;

import com.agguy.infiniteinventory.command.ModDatabaseCommands;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ModDatabaseCommandsTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ModDatabaseCommands> constructor = ModDatabaseCommands.class.getDeclaredConstructor();

        assertTrue(Modifier.isPrivate(constructor.getModifiers()), "工具类构造器应为 private");
    }
}
