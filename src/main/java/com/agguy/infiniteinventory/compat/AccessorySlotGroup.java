package com.agguy.infiniteinventory.compat;

public record AccessorySlotGroup(String slotName, String translationKey, int firstSlotIndex, int slotCount) {
    public int lastSlotIndexExclusive() {
        return this.firstSlotIndex + this.slotCount;
    }

    public boolean containsSlotIndex(int slotIndex) {
        return slotIndex >= this.firstSlotIndex && slotIndex < this.lastSlotIndexExclusive();
    }
}
