package com.agguy.infiniteinventory.client.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSelectionGestureModelTest {
    @Test
    void replaceSelectionGestureShouldKeepSelectedEntryPendingUntilRelease() {
        DatabaseSelectionGestureModel gestureModel = new DatabaseSelectionGestureModel();
        DatabaseSelectionGestureModel.PointerTarget startTarget = DatabaseSelectionGestureModel.PointerTarget.filledSlot(1, 7);

        gestureModel.beginSelectionGesture(DatabaseSelectionGestureModel.SelectionMode.REPLACE, startTarget, true);

        assertTrue(gestureModel.hasPendingSelectionGesture());
        assertTrue(gestureModel.hasSelectionGesture());
        assertFalse(gestureModel.isDragSelectionActive());
        assertFalse(gestureModel.shouldPromoteToDrag(DatabaseSelectionGestureModel.PointerTarget.filledSlot(1, 7)));

        DatabaseSelectionGestureModel.SelectionGesture pendingGesture = gestureModel.pendingSelectionGesture();
        assertNotNull(pendingGesture);
        assertEquals(DatabaseSelectionGestureModel.SelectionMode.REPLACE, pendingGesture.mode());
        assertEquals(startTarget, pendingGesture.startTarget());
        assertTrue(pendingGesture.clickedEntrySelected());
    }

    @Test
    void additiveDragSelectionShouldPromoteOnlyAfterPointerLeavesStartTarget() {
        DatabaseSelectionGestureModel gestureModel = new DatabaseSelectionGestureModel();

        gestureModel.beginSelectionGesture(
                DatabaseSelectionGestureModel.SelectionMode.ADDITIVE,
                DatabaseSelectionGestureModel.PointerTarget.filledSlot(0, 3),
                false
        );

        assertFalse(gestureModel.shouldPromoteToDrag(DatabaseSelectionGestureModel.PointerTarget.filledSlot(0, 3)));
        assertTrue(gestureModel.shouldPromoteToDrag(DatabaseSelectionGestureModel.PointerTarget.filledSlot(0, 4)));
        assertTrue(gestureModel.activateDragSelection());
        assertTrue(gestureModel.isDragSelectionActive());
        assertTrue(gestureModel.recordDraggedSlot(0, 3));
        assertTrue(gestureModel.recordDraggedSlot(0, 4));
        assertFalse(gestureModel.recordDraggedSlot(0, 4));
    }

    @Test
    void replaceDragSelectionShouldAlsoPromoteFromEmptySlotAndPanelBackground() {
        DatabaseSelectionGestureModel gestureModel = new DatabaseSelectionGestureModel();

        gestureModel.beginSelectionGesture(
                DatabaseSelectionGestureModel.SelectionMode.REPLACE,
                DatabaseSelectionGestureModel.PointerTarget.emptySlot(2, 14),
                false
        );

        assertFalse(gestureModel.shouldPromoteToDrag(DatabaseSelectionGestureModel.PointerTarget.emptySlot(2, 14)));
        assertTrue(gestureModel.shouldPromoteToDrag(DatabaseSelectionGestureModel.PointerTarget.filledSlot(2, 13)));

        gestureModel.clearSelectionGesture();
        gestureModel.beginSelectionGesture(
                DatabaseSelectionGestureModel.SelectionMode.REPLACE,
                DatabaseSelectionGestureModel.PointerTarget.panelBackground(1),
                false
        );

        assertFalse(gestureModel.shouldPromoteToDrag(DatabaseSelectionGestureModel.PointerTarget.panelBackground(1)));
        assertTrue(gestureModel.shouldPromoteToDrag(DatabaseSelectionGestureModel.PointerTarget.filledSlot(1, 0)));
    }

    @Test
    void clearSelectionGestureShouldResetPendingStateAndVisitedSlots() {
        DatabaseSelectionGestureModel gestureModel = new DatabaseSelectionGestureModel();

        gestureModel.beginSelectionGesture(
                DatabaseSelectionGestureModel.SelectionMode.REPLACE,
                DatabaseSelectionGestureModel.PointerTarget.filledSlot(3, 5),
                false
        );
        assertTrue(gestureModel.activateDragSelection());
        assertTrue(gestureModel.recordDraggedSlot(3, 5));

        gestureModel.clearSelectionGesture();

        assertFalse(gestureModel.hasPendingSelectionGesture());
        assertFalse(gestureModel.hasSelectionGesture());
        assertFalse(gestureModel.isDragSelectionActive());
        assertFalse(gestureModel.recordDraggedSlot(3, 6));
    }

    @Test
    void discardKeyShouldRequireReleaseBeforeSecondConsume() {
        DatabaseSelectionGestureModel gestureModel = new DatabaseSelectionGestureModel();

        assertTrue(gestureModel.tryConsumeDiscardKey());
        assertFalse(gestureModel.tryConsumeDiscardKey());

        gestureModel.releaseDiscardKey();

        assertTrue(gestureModel.tryConsumeDiscardKey());
    }
}
