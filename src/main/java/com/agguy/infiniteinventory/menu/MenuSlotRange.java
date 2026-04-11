package com.agguy.infiniteinventory.menu;

import com.agguy.infiniteinventory.compat.AccessorySlotGroup;
import java.util.List;

record MenuSlotRange(int firstIndex, int slotCount) {
    static MenuSlotRange of(int firstIndex, int slotCount) {
        return new MenuSlotRange(firstIndex, Math.max(0, slotCount));
    }

    static MenuSlotRange empty() {
        return new MenuSlotRange(0, 0);
    }

    static MenuSlotRange span(MenuSlotRange firstRange, MenuSlotRange secondRange) {
        if ((firstRange == null || firstRange.isEmpty()) && (secondRange == null || secondRange.isEmpty())) {
            return empty();
        }
        if (firstRange == null || firstRange.isEmpty()) {
            return of(secondRange.firstIndex(), secondRange.slotCount());
        }
        if (secondRange == null || secondRange.isEmpty()) {
            return of(firstRange.firstIndex(), firstRange.slotCount());
        }
        int firstIndex = Math.min(firstRange.firstIndex(), secondRange.firstIndex());
        int lastIndexExclusive = Math.max(firstRange.lastIndexExclusive(), secondRange.lastIndexExclusive());
        return of(firstIndex, lastIndexExclusive - firstIndex);
    }

    static MenuSlotRange fromGroups(List<AccessorySlotGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return empty();
        }
        AccessorySlotGroup firstGroup = groups.getFirst();
        AccessorySlotGroup lastGroup = groups.getLast();
        return of(firstGroup.firstSlotIndex(), lastGroup.lastSlotIndexExclusive() - firstGroup.firstSlotIndex());
    }

    int lastIndexExclusive() {
        return this.firstIndex + this.slotCount;
    }

    boolean contains(int slotIndex) {
        return slotIndex >= this.firstIndex && slotIndex < this.lastIndexExclusive();
    }

    boolean isEmpty() {
        return this.slotCount <= 0;
    }
}
