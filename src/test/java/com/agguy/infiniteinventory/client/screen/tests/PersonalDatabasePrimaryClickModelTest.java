package com.agguy.infiniteinventory.client.screen;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonalDatabasePrimaryClickModelTest {

    @Test
    void primaryClickResolutionShouldOnlyDependOnModifierKeys() throws ReflectiveOperationException {
        Method resolveMethod = PersonalDatabasePrimaryClickModel.class.getDeclaredMethod(
                "resolve",
                boolean.class,
                boolean.class,
                boolean.class
        );

        assertEquals(PersonalDatabasePrimaryClickModel.Action.class, resolveMethod.getReturnType());
    }

    @Test
    void plainLeftClickShouldStartReplaceSelectionGesture() {
        assertEquals(
                PersonalDatabasePrimaryClickModel.Action.START_REPLACE_SELECTION,
                PersonalDatabasePrimaryClickModel.resolve(false, false, false)
        );
    }

    @Test
    void ctrlShouldStartAdditiveSelectionGesture() {
        assertEquals(
                PersonalDatabasePrimaryClickModel.Action.START_ADDITIVE_SELECTION,
                PersonalDatabasePrimaryClickModel.resolve(false, false, true)
        );
    }

    @Test
    void altShouldTakeSingleToInventory() {
        assertEquals(
                PersonalDatabasePrimaryClickModel.Action.TAKE_SINGLE_TO_INVENTORY,
                PersonalDatabasePrimaryClickModel.resolve(false, true, false)
        );
    }

    @Test
    void altShouldTakePriorityOverCtrl() {
        assertEquals(
                PersonalDatabasePrimaryClickModel.Action.TAKE_SINGLE_TO_INVENTORY,
                PersonalDatabasePrimaryClickModel.resolve(false, true, true)
        );
    }

    @Test
    void shiftShouldKeepExistingTakeStackPriority() {
        assertEquals(
                PersonalDatabasePrimaryClickModel.Action.TAKE_STACK_TO_INVENTORY,
                PersonalDatabasePrimaryClickModel.resolve(true, false, false)
        );
        assertEquals(
                PersonalDatabasePrimaryClickModel.Action.TAKE_STACK_TO_INVENTORY,
                PersonalDatabasePrimaryClickModel.resolve(true, false, true)
        );
        assertEquals(
                PersonalDatabasePrimaryClickModel.Action.TAKE_STACK_TO_INVENTORY,
                PersonalDatabasePrimaryClickModel.resolve(true, true, false)
        );
        assertEquals(
                PersonalDatabasePrimaryClickModel.Action.TAKE_STACK_TO_INVENTORY,
                PersonalDatabasePrimaryClickModel.resolve(true, true, true)
        );
    }
}
