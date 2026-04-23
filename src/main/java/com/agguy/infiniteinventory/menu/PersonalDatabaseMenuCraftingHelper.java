package com.agguy.infiniteinventory.menu;

import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * 合成网格相关操作的委托辅助类。
 *
 * <p>将 {@link PersonalDatabaseMenu} 中与配方书、合成槽位交互的方法抽取至此，
 * 使主菜单类专注于业务编排，而不必直接实现 RecipeBookMenu 的槽位契约。
 */
final class PersonalDatabaseMenuCraftingHelper {

    private static final int CRAFT_GRID_WIDTH = 2;
    private static final int CRAFT_GRID_HEIGHT = 2;
    private static final int CRAFT_GRID_SIZE = 5;

    private final PersonalDatabaseMenu menu;

    PersonalDatabaseMenuCraftingHelper(PersonalDatabaseMenu menu) {
        this.menu = menu;
    }

    void fillCraftSlotsStackedContents(net.minecraft.world.entity.player.StackedContents stackedContents) {
        this.menu.craftSlots.fillStackedContents(stackedContents);
    }

    void clearCraftingContent() {
        this.menu.resultSlots.clearContent();
        this.menu.craftSlots.clearContent();
    }

    boolean recipeMatches(RecipeHolder<CraftingRecipe> recipe) {
        return recipe.value().matches(this.menu.craftSlots.asCraftInput(), this.menu.owner.level());
    }

    int getResultSlotIndex() {
        return this.menu.resultSlotIndex;
    }

    int getGridWidth() {
        return CRAFT_GRID_WIDTH;
    }

    int getGridHeight() {
        return CRAFT_GRID_HEIGHT;
    }

    int getSize() {
        return CRAFT_GRID_SIZE;
    }

    RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    boolean shouldMoveToInventory(int slotIndex) {
        return slotIndex != this.menu.resultSlotIndex;
    }
}
