package com.agguy.infiniteinventory.client.screen;

import java.util.LinkedHashSet;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

final class DatabaseSelectionGestureModel {
    private final Set<Long> visitedDragSlots = new LinkedHashSet<>();
    @Nullable
    private SelectionPoint pendingCtrlClick;
    private boolean dragSelectionActive;
    private boolean discardKeyConsumed;

    void beginCtrlClick(int panelIndex, int slotIndex) {
        this.pendingCtrlClick = new SelectionPoint(panelIndex, slotIndex);
        this.dragSelectionActive = false;
        this.visitedDragSlots.clear();
    }

    boolean hasPendingCtrlClick() {
        return this.pendingCtrlClick != null;
    }

    boolean hasCtrlSelectionGesture() {
        return this.pendingCtrlClick != null || this.dragSelectionActive;
    }

    @Nullable
    SelectionPoint pendingCtrlClick() {
        return this.pendingCtrlClick;
    }

    boolean shouldPromoteToDrag(int panelIndex, int slotIndex) {
        if (this.pendingCtrlClick == null || this.dragSelectionActive) {
            return false;
        }
        return slotKey(panelIndex, slotIndex) != slotKey(this.pendingCtrlClick.panelIndex(), this.pendingCtrlClick.slotIndex());
    }

    boolean activateDragSelection() {
        if (this.pendingCtrlClick == null) {
            return false;
        }
        this.dragSelectionActive = true;
        this.visitedDragSlots.clear();
        this.visitedDragSlots.add(slotKey(this.pendingCtrlClick.panelIndex(), this.pendingCtrlClick.slotIndex()));
        return true;
    }

    boolean isDragSelectionActive() {
        return this.dragSelectionActive;
    }

    boolean recordDraggedSlot(int panelIndex, int slotIndex) {
        return this.dragSelectionActive && this.visitedDragSlots.add(slotKey(panelIndex, slotIndex));
    }

    void clearCtrlSelectionGesture() {
        this.pendingCtrlClick = null;
        this.dragSelectionActive = false;
        this.visitedDragSlots.clear();
    }

    boolean tryConsumeDiscardKey() {
        if (this.discardKeyConsumed) {
            return false;
        }
        this.discardKeyConsumed = true;
        return true;
    }

    void releaseDiscardKey() {
        this.discardKeyConsumed = false;
    }

    boolean isDiscardKeyConsumed() {
        return this.discardKeyConsumed;
    }

    static long slotKey(int panelIndex, int slotIndex) {
        return ((long) panelIndex << 32) | (slotIndex & 0xFFFFFFFFL);
    }

    record SelectionPoint(int panelIndex, int slotIndex) {
    }
}
