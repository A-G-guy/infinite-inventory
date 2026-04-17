package com.agguy.infiniteinventory.client.screen;

import java.util.LinkedHashSet;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

final class DatabaseSelectionGestureModel {
    private final Set<Long> visitedDragSlots = new LinkedHashSet<>();
    @Nullable
    private SelectionGesture pendingSelectionGesture;
    private boolean dragSelectionActive;
    private boolean discardKeyConsumed;

    void beginSelectionGesture(SelectionMode mode, PointerTarget startTarget, boolean clickedEntrySelected) {
        this.pendingSelectionGesture = new SelectionGesture(mode, startTarget, clickedEntrySelected);
        this.dragSelectionActive = false;
        this.visitedDragSlots.clear();
    }

    boolean hasPendingSelectionGesture() {
        return this.pendingSelectionGesture != null;
    }

    boolean hasSelectionGesture() {
        return this.pendingSelectionGesture != null || this.dragSelectionActive;
    }

    @Nullable
    SelectionGesture pendingSelectionGesture() {
        return this.pendingSelectionGesture;
    }

    boolean shouldPromoteToDrag(PointerTarget currentTarget) {
        if (this.pendingSelectionGesture == null || this.dragSelectionActive) {
            return false;
        }
        return !this.pendingSelectionGesture.startTarget().matches(currentTarget);
    }

    boolean activateDragSelection() {
        if (this.pendingSelectionGesture == null) {
            return false;
        }
        this.dragSelectionActive = true;
        this.visitedDragSlots.clear();
        return true;
    }

    boolean isDragSelectionActive() {
        return this.dragSelectionActive;
    }

    boolean recordDraggedSlot(int panelIndex, int slotIndex) {
        return this.dragSelectionActive && this.visitedDragSlots.add(slotKey(panelIndex, slotIndex));
    }

    void clearSelectionGesture() {
        this.pendingSelectionGesture = null;
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

    enum SelectionMode {
        REPLACE,
        ADDITIVE
    }

    enum PointerTargetType {
        FILLED_SLOT,
        EMPTY_SLOT,
        PANEL_BACKGROUND,
        OUTSIDE_PANEL
    }

    record PointerTarget(int panelIndex, int slotIndex, PointerTargetType type) {
        static PointerTarget filledSlot(int panelIndex, int slotIndex) {
            return new PointerTarget(panelIndex, slotIndex, PointerTargetType.FILLED_SLOT);
        }

        static PointerTarget emptySlot(int panelIndex, int slotIndex) {
            return new PointerTarget(panelIndex, slotIndex, PointerTargetType.EMPTY_SLOT);
        }

        static PointerTarget panelBackground(int panelIndex) {
            return new PointerTarget(panelIndex, -1, PointerTargetType.PANEL_BACKGROUND);
        }

        static PointerTarget outsidePanel() {
            return new PointerTarget(-1, -1, PointerTargetType.OUTSIDE_PANEL);
        }

        boolean isFilledSlot() {
            return this.type == PointerTargetType.FILLED_SLOT;
        }

        boolean isWithinPanel() {
            return this.type != PointerTargetType.OUTSIDE_PANEL;
        }

        boolean matches(PointerTarget other) {
            return this.panelIndex == other.panelIndex
                    && this.slotIndex == other.slotIndex
                    && this.type == other.type;
        }
    }

    record SelectionGesture(SelectionMode mode, PointerTarget startTarget, boolean clickedEntrySelected) {
    }
}
