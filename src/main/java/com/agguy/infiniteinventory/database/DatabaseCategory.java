package com.agguy.infiniteinventory.database;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MilkBucketItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.ArmorItem;

public enum DatabaseCategory {
    ALL("screen.infiniteinventory.category.all"),
    BLOCKS("screen.infiniteinventory.category.blocks"),
    TOOLS_WEAPONS("screen.infiniteinventory.category.tools_weapons"),
    EQUIPMENT("screen.infiniteinventory.category.equipment"),
    CONSUMABLES("screen.infiniteinventory.category.consumables"),
    MATERIALS("screen.infiniteinventory.category.materials"),
    OTHER("screen.infiniteinventory.category.other");

    private final String translationKey;

    DatabaseCategory(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public static DatabaseCategory classify(ItemStack stack) {
        if (stack.isEmpty()) {
            return OTHER;
        }
        Item item = stack.getItem();
        if (item instanceof BlockItem) {
            return BLOCKS;
        }
        if (isToolOrWeapon(item)) {
            return TOOLS_WEAPONS;
        }
        if (item instanceof ArmorItem || item instanceof ElytraItem) {
            return EQUIPMENT;
        }
        if (stack.has(DataComponents.FOOD) || stack.has(DataComponents.POTION_CONTENTS) || item instanceof MilkBucketItem) {
            return CONSUMABLES;
        }
        if (stack.getMaxStackSize() > 1) {
            return MATERIALS;
        }
        return OTHER;
    }

    private static boolean isToolOrWeapon(Item item) {
        return item instanceof DiggerItem
                || item instanceof SwordItem
                || item instanceof BowItem
                || item instanceof CrossbowItem
                || item instanceof TridentItem
                || item instanceof FishingRodItem
                || item instanceof ShearsItem
                || item instanceof ShieldItem;
    }
}
