package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.network.DatabaseClickAction;
import com.agguy.infiniteinventory.network.DatabaseSelectionAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PersonalDatabaseContextMenuItemTest {

    @Test
    void clickShouldCreateItemWithClickAction() {
        PersonalDatabaseContextMenuItem item = PersonalDatabaseContextMenuItem.click(
                "menu.item.take", DatabaseClickAction.TAKE_STACK_TO_INVENTORY);

        assertEquals("menu.item.take", item.translationKey());
        assertEquals(DatabaseClickAction.TAKE_STACK_TO_INVENTORY, item.clickAction());
    }

    @Test
    void selectionShouldCreateItemWithSelectionAction() {
        PersonalDatabaseContextMenuItem item = PersonalDatabaseContextMenuItem.selection(
                "menu.item.extract", DatabaseSelectionAction.EXTRACT_ONE_TO_INVENTORY);

        assertEquals("menu.item.extract", item.translationKey());
        assertEquals(DatabaseSelectionAction.EXTRACT_ONE_TO_INVENTORY, item.selectionAction());
    }

    @Test
    void localShouldCreateItemWithLocalAction() {
        PersonalDatabaseContextMenuItem item = PersonalDatabaseContextMenuItem.local(
                "menu.item.star", PersonalDatabaseContextMenuItem.LocalAction.TOGGLE_STAR);

        assertEquals("menu.item.star", item.translationKey());
        assertEquals(PersonalDatabaseContextMenuItem.LocalAction.TOGGLE_STAR, item.localAction());
    }

    @Test
    void shouldThrowWhenNoActionSpecified() {
        assertThrows(IllegalArgumentException.class, () ->
                new PersonalDatabaseContextMenuItem("test", null, null, null));
    }

    @Test
    void shouldThrowWhenMultipleActionsSpecified() {
        assertThrows(IllegalArgumentException.class, () ->
                new PersonalDatabaseContextMenuItem("test",
                        DatabaseClickAction.TAKE_STACK_TO_INVENTORY, null,
                        PersonalDatabaseContextMenuItem.LocalAction.TOGGLE_STAR));
    }

    @Test
    void shouldDefaultNullTranslationKeyToEmpty() {
        PersonalDatabaseContextMenuItem item = PersonalDatabaseContextMenuItem.click(
                null, DatabaseClickAction.TAKE_STACK_TO_INVENTORY);

        assertEquals("", item.translationKey());
    }

    @Test
    void localActionShouldHaveFiveValues() {
        assertEquals(5, PersonalDatabaseContextMenuItem.LocalAction.values().length);
    }
}
