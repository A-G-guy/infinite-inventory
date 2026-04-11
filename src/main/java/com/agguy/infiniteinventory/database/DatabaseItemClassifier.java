package com.agguy.infiniteinventory.database;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ArmorItem;
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

public final class DatabaseItemClassifier {
    public static final DatabaseItemClassifier INSTANCE = new DatabaseItemClassifier();

    public static final int CURRENT_VERSION = 1;

    private DatabaseItemClassifier() {
    }

    public DatabaseCategory classify(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return DatabaseCategory.OTHER;
        }
        Item item = stack.getItem();
        if (item instanceof BlockItem) {
            return DatabaseCategory.BLOCKS;
        }
        if (isToolOrWeapon(item)) {
            return DatabaseCategory.TOOLS_WEAPONS;
        }
        if (item instanceof ArmorItem || item instanceof ElytraItem) {
            return DatabaseCategory.EQUIPMENT;
        }
        if (stack.has(DataComponents.FOOD) || stack.has(DataComponents.POTION_CONTENTS) || item instanceof MilkBucketItem) {
            return DatabaseCategory.CONSUMABLES;
        }
        if (stack.getMaxStackSize() > 1) {
            return DatabaseCategory.MATERIALS;
        }
        return DatabaseCategory.OTHER;
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
