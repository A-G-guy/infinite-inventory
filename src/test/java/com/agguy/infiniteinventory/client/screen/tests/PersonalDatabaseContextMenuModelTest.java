package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.network.DatabaseClickAction;
import com.agguy.infiniteinventory.network.DatabaseSelectionAction;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonalDatabaseContextMenuModelTest {
    @Test
    void singleSelectionMenuShouldExposeSingleSlotActionsAndCustomExtractOverlay() {
        List<PersonalDatabaseContextMenuItem> items = PersonalDatabaseScreenCommonHelper.contextMenuItemsForSelectionCount(1);

        assertEquals(9, items.size());
        assertEquals(DatabaseClickAction.TAKE_SINGLE, items.get(0).clickAction());
        assertEquals(DatabaseClickAction.TAKE_HALF_STACK_TO_INVENTORY, items.get(1).clickAction());
        assertEquals(DatabaseClickAction.TAKE_STACK, items.get(2).clickAction());
        assertEquals(DatabaseClickAction.TAKE_HALF_ENTRY_TO_INVENTORY, items.get(3).clickAction());
        assertEquals(DatabaseClickAction.TAKE_ALL, items.get(4).clickAction());
        assertEquals(PersonalDatabaseContextMenuItem.LocalAction.OPEN_CUSTOM_EXTRACT_OVERLAY, items.get(5).localAction());
        assertEquals(PersonalDatabaseContextMenuItem.LocalAction.OPEN_NOTE_OVERLAY, items.get(6).localAction());
        assertEquals(PersonalDatabaseContextMenuItem.LocalAction.TOGGLE_STAR, items.get(7).localAction());
        assertEquals(DatabaseSelectionAction.TRANSFER_TO_TAB, items.get(8).selectionAction());
    }

    @Test
    void multiSelectionMenuShouldUseSelectionActionsAndHidePickupActions() {
        List<PersonalDatabaseContextMenuItem> items = PersonalDatabaseScreenCommonHelper.contextMenuItemsForSelectionCount(3);

        assertEquals(9, items.size());
        assertEquals(DatabaseSelectionAction.EXTRACT_ONE_TO_INVENTORY, items.get(0).selectionAction());
        assertEquals(DatabaseSelectionAction.EXTRACT_HALF_STACK_TO_INVENTORY, items.get(1).selectionAction());
        assertEquals(DatabaseSelectionAction.EXTRACT_STACK_TO_INVENTORY, items.get(2).selectionAction());
        assertEquals(DatabaseSelectionAction.EXTRACT_HALF_ENTRY_TO_INVENTORY, items.get(3).selectionAction());
        assertEquals(DatabaseSelectionAction.EXTRACT_ALL_TO_INVENTORY, items.get(4).selectionAction());
        assertEquals(PersonalDatabaseContextMenuItem.LocalAction.OPEN_CUSTOM_EXTRACT_OVERLAY, items.get(5).localAction());
        assertEquals(PersonalDatabaseContextMenuItem.LocalAction.OPEN_NOTE_OVERLAY, items.get(6).localAction());
        assertEquals(PersonalDatabaseContextMenuItem.LocalAction.TOGGLE_STAR, items.get(7).localAction());
        assertEquals(DatabaseSelectionAction.TRANSFER_TO_TAB, items.get(8).selectionAction());
        assertTrue(items.stream().noneMatch(item -> item.clickAction() == DatabaseClickAction.TAKE_SINGLE));
    }
}
