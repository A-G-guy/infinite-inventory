package com.agguy.infiniteinventory.client.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonalDatabasePrimaryClickModelTest {
    @Test
    void unselectedEntryShouldKeepReplaceSelectionBehavior() {
        assertEquals(
                PersonalDatabasePrimaryClickModel.Action.REPLACE_SELECTION,
                PersonalDatabasePrimaryClickModel.resolve(false, false, false)
        );
    }

    @Test
    void selectedEntryShouldTakeSingleOnPlainLeftClick() {
        assertEquals(
                PersonalDatabasePrimaryClickModel.Action.TAKE_SINGLE,
                PersonalDatabasePrimaryClickModel.resolve(false, false, true)
        );
    }

    @Test
    void ctrlShouldKeepSelectionGestureEvenWhenEntryIsAlreadySelected() {
        assertEquals(
                PersonalDatabasePrimaryClickModel.Action.CTRL_SELECTION,
                PersonalDatabasePrimaryClickModel.resolve(false, true, true)
        );
    }

    @Test
    void shiftShouldKeepExistingTakeStackPriority() {
        assertEquals(
                PersonalDatabasePrimaryClickModel.Action.TAKE_STACK_TO_INVENTORY,
                PersonalDatabasePrimaryClickModel.resolve(true, false, true)
        );
        assertEquals(
                PersonalDatabasePrimaryClickModel.Action.TAKE_STACK_TO_INVENTORY,
                PersonalDatabasePrimaryClickModel.resolve(true, true, true)
        );
    }
}
