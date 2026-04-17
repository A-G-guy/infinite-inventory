package com.agguy.infiniteinventory.client.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSelectionGestureModelTest {
    @Test
    void ctrlClickShouldRemainPendingUntilRelease() {
        DatabaseSelectionGestureModel gestureModel = new DatabaseSelectionGestureModel();

        gestureModel.beginCtrlClick(1, 7);

        assertTrue(gestureModel.hasPendingCtrlClick());
        assertFalse(gestureModel.isDragSelectionActive());
        assertNotNull(gestureModel.pendingCtrlClick());
        assertEquals(1, gestureModel.pendingCtrlClick().panelIndex());
        assertEquals(7, gestureModel.pendingCtrlClick().slotIndex());
    }

    @Test
    void dragSelectionShouldPromoteOnlyAfterLeavingStartSlot() {
        DatabaseSelectionGestureModel gestureModel = new DatabaseSelectionGestureModel();

        gestureModel.beginCtrlClick(0, 3);

        assertFalse(gestureModel.shouldPromoteToDrag(0, 3));
        assertTrue(gestureModel.shouldPromoteToDrag(0, 4));
        assertTrue(gestureModel.activateDragSelection());
        assertTrue(gestureModel.isDragSelectionActive());
        assertFalse(gestureModel.recordDraggedSlot(0, 3));
        assertTrue(gestureModel.recordDraggedSlot(0, 4));
        assertFalse(gestureModel.recordDraggedSlot(0, 4));
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
