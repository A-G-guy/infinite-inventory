package com.agguy.infiniteinventory.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

/**
 * 提供对 {@link CraftingMenu} 受保护静态方法的包级访问。
 *
 * <p>设计意图：将内部类从 {@link PersonalDatabaseMenuSupport} 抽取以控制父类规模。
 */
final class PersonalDatabaseCraftingMenuAccess extends CraftingMenu {
    private PersonalDatabaseCraftingMenuAccess(int containerId, Inventory playerInventory) {
        super(containerId, playerInventory);
    }

    static void updateResult(
            AbstractContainerMenu menu,
            net.minecraft.world.level.Level level,
            Player player,
            CraftingContainer craftingSlots,
            ResultContainer resultSlots,
            @Nullable RecipeHolder<CraftingRecipe> recipe
    ) {
        slotChangedCraftingGrid(menu, level, player, craftingSlots, resultSlots, recipe);
    }
}
