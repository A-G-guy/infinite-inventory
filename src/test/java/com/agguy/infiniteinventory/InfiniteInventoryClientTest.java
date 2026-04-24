package com.agguy.infiniteinventory;

import com.agguy.infiniteinventory.client.PersonalDatabaseClient;
import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InfiniteInventoryClientTest {
    static {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void canOpenEquippedDatabaseShouldReturnTrueWhenScreenIsNull() throws ReflectiveOperationException {
        Minecraft minecraft = mock(Minecraft.class);
        setScreenField(minecraft, null);
        assertTrue(invokeCanOpen(minecraft));
    }

    @Test
    void canOpenEquippedDatabaseShouldReturnFalseWhenScreenIsNotContainerScreen()
            throws ReflectiveOperationException {
        Minecraft minecraft = mock(Minecraft.class);
        Screen nonContainerScreen = mock(Screen.class);
        setScreenField(minecraft, nonContainerScreen);
        assertFalse(invokeCanOpen(minecraft));
    }

    @Test
    void canOpenEquippedDatabaseShouldReturnTrueForContainerScreenWithoutFocusedEditBox()
            throws ReflectiveOperationException {
        Minecraft minecraft = mock(Minecraft.class);
        AbstractContainerScreen<?> containerScreen = mock(AbstractContainerScreen.class);
        setScreenField(minecraft, containerScreen);
        assertTrue(invokeCanOpen(minecraft));
    }

    @Test
    void canOpenEquippedDatabaseShouldReturnFalseWhenFocusedWidgetIsEditBox() throws ReflectiveOperationException {
        Minecraft minecraft = mock(Minecraft.class);
        AbstractContainerScreen<?> containerScreen = mock(AbstractContainerScreen.class);
        when(containerScreen.getFocused()).thenReturn(mock(EditBox.class));
        setScreenField(minecraft, containerScreen);

        assertFalse(invokeCanOpen(minecraft));
    }

    private boolean invokeCanOpen(Minecraft minecraft) throws ReflectiveOperationException {
        var method = InfiniteInventoryClient.class.getDeclaredMethod("canOpenEquippedDatabase", Minecraft.class);
        method.setAccessible(true);
        return (boolean) method.invoke(null, minecraft);
    }

    private void setScreenField(Minecraft minecraft, Screen screen) throws ReflectiveOperationException {
        Field screenField = Minecraft.class.getDeclaredField("screen");
        screenField.setAccessible(true);
        screenField.set(minecraft, screen);
    }
}
