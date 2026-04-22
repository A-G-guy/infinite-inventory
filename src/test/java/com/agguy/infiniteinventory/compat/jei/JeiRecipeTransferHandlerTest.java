package com.agguy.infiniteinventory.compat.jei;

import com.agguy.infiniteinventory.tests.MinecraftTestBootstrap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JeiRecipeTransferHandlerTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void universalHandlerShouldReturnAbstractContainerMenuClass() {
        IUniversalRecipeTransferHandler<AbstractContainerMenu> handler =
                new JeiRecipeTransferHandler(MockTransferHelper.INSTANCE);

        assertEquals(AbstractContainerMenu.class, handler.getContainerClass());
    }

    @Test
    @SuppressWarnings("unchecked")
    void specificCraftingHandlerShouldReturnCraftingMenuClass() {
        IRecipeTransferHandler<CraftingMenu, RecipeHolder<CraftingRecipe>> handler =
                JeiRecipeTransferHandler.createSpecificHandler(CraftingMenu.class, RecipeTypes.CRAFTING, MockTransferHelper.INSTANCE);

        assertEquals(CraftingMenu.class, handler.getContainerClass());
    }

    @Test
    @SuppressWarnings("unchecked")
    void specificInventoryHandlerShouldReturnInventoryMenuClass() {
        IRecipeTransferHandler<InventoryMenu, RecipeHolder<CraftingRecipe>> handler =
                JeiRecipeTransferHandler.createSpecificHandler(InventoryMenu.class, RecipeTypes.CRAFTING, MockTransferHelper.INSTANCE);

        assertEquals(InventoryMenu.class, handler.getContainerClass());
    }

    @Test
    void transferRecipeShouldNotCrashWithEmptySlots() {
        IUniversalRecipeTransferHandler<AbstractContainerMenu> handler =
                new JeiRecipeTransferHandler(MockTransferHelper.INSTANCE);

        IRecipeSlotsView emptySlots = new MockRecipeSlotsView(List.of());

        // 使用 null player 和 container 测试空配方不会崩溃
        // 实际游戏中不会传入 null，这里仅验证空槽位的健壮性
        IRecipeTransferError result = handler.transferRecipe(null, null, emptySlots, null, false, false);
        assertEquals(null, result);
    }

    static class MockRecipeSlotsView implements IRecipeSlotsView {
        private final List<IRecipeSlotView> slots;

        MockRecipeSlotsView(List<IRecipeSlotView> slots) {
            this.slots = slots;
        }

        @Override
        public List<IRecipeSlotView> getSlotViews() {
            return this.slots;
        }
    }

    static class MockRecipeSlotView implements IRecipeSlotView {
        private final List<ItemStack> stacks;
        private final boolean empty;

        MockRecipeSlotView(List<ItemStack> stacks, boolean empty) {
            this.stacks = stacks;
            this.empty = empty;
        }

        @Override
        public Stream<ITypedIngredient<?>> getAllIngredients() {
            return Stream.empty();
        }

        @Override
        public List<ITypedIngredient<?>> getAllIngredientsList() {
            return List.of();
        }

        @Override
        public Optional<ITypedIngredient<?>> getDisplayedIngredient() {
            return Optional.empty();
        }

        @Override
        public RecipeIngredientRole getRole() {
            return RecipeIngredientRole.INPUT;
        }

        @Override
        public void drawHighlight(GuiGraphics graphics, int color) {
        }

        @Override
        public Optional<String> getSlotName() {
            return Optional.empty();
        }

        @Override
        public Stream<ItemStack> getItemStacks() {
            return this.stacks.stream();
        }

        @Override
        public boolean isEmpty() {
            return this.empty;
        }
    }

    static class MockTransferHelper implements IRecipeTransferHandlerHelper {
        static final MockTransferHelper INSTANCE = new MockTransferHelper();

        @Override
        public IRecipeTransferError createInternalError() {
            return null;
        }

        @Override
        public IRecipeTransferError createUserErrorWithTooltip(Component tooltip) {
            return new IRecipeTransferError() {
                @Override
                public Type getType() {
                    return Type.USER_FACING;
                }
            };
        }

        @Override
        public IRecipeTransferError createUserErrorForMissingSlots(Component tooltip, java.util.Collection<IRecipeSlotView> missingSlots) {
            return null;
        }

        @Override
        public <C extends AbstractContainerMenu, R> mezz.jei.api.recipe.transfer.IRecipeTransferInfo<C, R> createBasicRecipeTransferInfo(
                Class<? extends C> containerClass,
                net.minecraft.world.inventory.MenuType<C> menuType,
                RecipeType<R> recipeType,
                int recipeSlotStart,
                int recipeSlotCount,
                int inventorySlotStart,
                int inventorySlotCount
        ) {
            return null;
        }

        @Override
        public <C extends AbstractContainerMenu, R> IRecipeTransferHandler<C, R> createUnregisteredRecipeTransferHandler(mezz.jei.api.recipe.transfer.IRecipeTransferInfo<C, R> transferInfo) {
            return null;
        }

        @Override
        public IRecipeSlotsView createRecipeSlotsView(List<IRecipeSlotView> slotViews) {
            return new MockRecipeSlotsView(slotViews);
        }

        @Override
        public boolean recipeTransferHasServerSupport() {
            return true;
        }

        @Override
        public java.util.Map<Integer, net.minecraft.world.item.crafting.Ingredient> getGuiSlotIndexToIngredientMap(RecipeHolder<CraftingRecipe> recipe) {
            return java.util.Map.of();
        }
    }
}
