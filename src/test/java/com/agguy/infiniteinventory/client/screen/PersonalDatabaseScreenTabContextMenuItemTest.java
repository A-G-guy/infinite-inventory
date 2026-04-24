package com.agguy.infiniteinventory.client.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonalDatabaseScreenTabContextMenuItemTest {

    @Test
    void shouldStoreTranslationKeyAndAction() {
        PersonalDatabaseScreenTabContextMenuItem item = new PersonalDatabaseScreenTabContextMenuItem(
                "screen.infiniteinventory.tab_context.rename",
                PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.RENAME
        );

        assertEquals("screen.infiniteinventory.tab_context.rename", item.translationKey());
        assertEquals(PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.RENAME, item.action());
    }

    @Test
    void shouldNormalizeNullTranslationKeyToEmptyString() {
        PersonalDatabaseScreenTabContextMenuItem item = new PersonalDatabaseScreenTabContextMenuItem(
                null,
                PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.SINGLE_VIEW
        );

        assertEquals("", item.translationKey());
        assertEquals(PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.SINGLE_VIEW, item.action());
    }

    @Test
    void eachActionEnumShouldHaveDistinctIdentity() {
        for (PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction action
                : PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.values()) {
            PersonalDatabaseScreenTabContextMenuItem item = new PersonalDatabaseScreenTabContextMenuItem(
                    "key." + action.name().toLowerCase(),
                    action
            );
            assertEquals(action, item.action());
        }
    }
}
