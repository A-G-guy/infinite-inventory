package com.agguy.infiniteinventory.client.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RemixIconTest {

    @Test
    void forContextMenuKeyShouldMapTakeSingle() {
        assertEquals(RemixIcon.TAKE_SINGLE,
                RemixIcon.forContextMenuKey("screen.infiniteinventory.context.take_single"));
    }

    @Test
    void forContextMenuKeyShouldMapTakeOneEach() {
        assertEquals(RemixIcon.TAKE_SINGLE,
                RemixIcon.forContextMenuKey("screen.infiniteinventory.selection.take_one_each"));
    }

    @Test
    void forContextMenuKeyShouldMapTransfer() {
        assertEquals(RemixIcon.TRANSFER,
                RemixIcon.forContextMenuKey("screen.infiniteinventory.selection.transfer"));
    }

    @Test
    void forContextMenuKeyShouldMapStarActions() {
        assertEquals(RemixIcon.TOGGLE_STAR,
                RemixIcon.forContextMenuKey("screen.infiniteinventory.context.toggle_star"));
        assertEquals(RemixIcon.ADD_STAR,
                RemixIcon.forContextMenuKey("screen.infiniteinventory.context.add_star"));
        assertEquals(RemixIcon.REMOVE_STAR,
                RemixIcon.forContextMenuKey("screen.infiniteinventory.context.remove_star"));
        assertEquals(RemixIcon.STAR_MIXED,
                RemixIcon.forContextMenuKey("screen.infiniteinventory.context.toggle_star_mixed"));
        assertEquals(RemixIcon.STAR_ALL,
                RemixIcon.forContextMenuKey("screen.infiniteinventory.context.star_all"));
        assertEquals(RemixIcon.UNSTAR_ALL,
                RemixIcon.forContextMenuKey("screen.infiniteinventory.context.unstar_all"));
    }

    @Test
    void forContextMenuKeyShouldReturnNullForUnknownKey() {
        assertNull(RemixIcon.forContextMenuKey("screen.infiniteinventory.context.unknown"));
    }

    @Test
    void forTabContextMenuActionShouldMapAllActions() {
        for (PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction action
                : PersonalDatabaseScreenTabContextMenuItem.TabContextMenuAction.values()) {
            assertNotNull(RemixIcon.forTabContextMenuAction(action),
                    "Action " + action + " should have an icon mapping");
        }
    }

    @Test
    void locationShouldHaveCorrectNamespace() {
        for (RemixIcon icon : RemixIcon.values()) {
            assertEquals("infiniteinventory", icon.location().getNamespace());
        }
    }

    @Test
    void locationShouldHaveNonEmptyPath() {
        for (RemixIcon icon : RemixIcon.values()) {
            assertFalse(icon.location().getPath().isEmpty());
        }
    }

    @Test
    void locationShouldStartWithIconPrefix() {
        for (RemixIcon icon : RemixIcon.values()) {
            assertTrue(icon.location().getPath().startsWith("icon/"),
                    "Icon path should start with 'icon/': " + icon.location().getPath());
        }
    }
}
