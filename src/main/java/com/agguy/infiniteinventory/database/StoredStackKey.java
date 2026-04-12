package com.agguy.infiniteinventory.database;

import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class StoredStackKey {
    private final ItemStack displayStack;
    private final int hashCode;
    private final String registryName;
    private final String registryNamespace;
    private final String registryPath;

    private StoredStackKey(ItemStack stack) {
        this.displayStack = stack.copyWithCount(1);
        this.hashCode = ItemStack.hashItemAndComponents(this.displayStack);
        ResourceLocation itemId = Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(this.displayStack.getItem()));
        this.registryName = itemId.toString();
        this.registryNamespace = itemId.getNamespace();
        this.registryPath = itemId.getPath();
    }

    public static StoredStackKey of(ItemStack stack) {
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Empty stack cannot be used as a database key");
        }
        return new StoredStackKey(stack);
    }

    public ItemStack displayStack() {
        return this.displayStack.copy();
    }

    public ItemStack toStack(int count) {
        if (count <= 0) {
            return ItemStack.EMPTY;
        }
        return this.displayStack.copyWithCount(this.clampToLegalStackSize(count));
    }

    public String registryName() {
        return this.registryName;
    }

    public String registryPath() {
        return this.registryPath;
    }

    public String registryNamespace() {
        return this.registryNamespace;
    }

    public int maxStackSize() {
        return this.displayStack.getMaxStackSize();
    }

    private int clampToLegalStackSize(int count) {
        return Math.min(count, Math.max(1, this.maxStackSize()));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof StoredStackKey key)) {
            return false;
        }
        return ItemStack.isSameItemSameComponents(this.displayStack, key.displayStack);
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }
}
