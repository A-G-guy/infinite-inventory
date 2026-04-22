package com.agguy.infiniteinventory.compat.jei;

import com.agguy.infiniteinventory.client.PersonalDatabaseClient;
import com.agguy.infiniteinventory.database.DatabaseEnhancementOption;
import com.agguy.infiniteinventory.network.JeiCraftingExtractPayload;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * JEI 配方转移处理器。
 * 拦截 + 按钮点击，当背包材料不足时自动从数据库补充提取。
 */
final class JeiRecipeTransferHandler implements IUniversalRecipeTransferHandler<AbstractContainerMenu> {

    private final IRecipeTransferHandlerHelper helper;

    JeiRecipeTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
    }

    static <C extends AbstractContainerMenu, R> IRecipeTransferHandler<C, R> createSpecificHandler(
            Class<? extends C> containerClass,
            RecipeType<R> recipeType,
            IRecipeTransferHandlerHelper helper
    ) {
        return new SpecificHandler<>(containerClass, recipeType, helper);
    }

    static void register(IRecipeTransferRegistration registration) {
        IRecipeTransferHandlerHelper helper = registration.getTransferHelper();
        JeiRecipeTransferHandler universalHandler = new JeiRecipeTransferHandler(helper);
        registration.addUniversalRecipeTransferHandler(universalHandler);

        // 注册特定容器类型的处理器，覆盖 JEI 内置处理器以支持从数据库自动提取
        IRecipeTransferHandler<CraftingMenu, RecipeHolder<CraftingRecipe>> craftingHandler =
                new SpecificHandler<>(CraftingMenu.class, RecipeTypes.CRAFTING, helper);
        registration.addRecipeTransferHandler(craftingHandler, RecipeTypes.CRAFTING);

        IRecipeTransferHandler<InventoryMenu, RecipeHolder<CraftingRecipe>> inventoryHandler =
                new SpecificHandler<>(InventoryMenu.class, RecipeTypes.CRAFTING, helper);
        registration.addRecipeTransferHandler(inventoryHandler, RecipeTypes.CRAFTING);
    }

    @Override
    public Class<AbstractContainerMenu> getContainerClass() {
        return AbstractContainerMenu.class;
    }

    @Override
    public java.util.Optional<net.minecraft.world.inventory.MenuType<AbstractContainerMenu>> getMenuType() {
        return java.util.Optional.empty();
    }

    @Override
    public IRecipeTransferError transferRecipe(
            AbstractContainerMenu container,
            Object recipe,
            IRecipeSlotsView recipeSlots,
            Player player,
            boolean maxTransfer,
            boolean doTransfer
    ) {
        return handleTransfer(container, recipeSlots, player, doTransfer, this.helper);
    }

    private static IRecipeTransferError handleTransfer(
            AbstractContainerMenu container,
            IRecipeSlotsView recipeSlots,
            Player player,
            boolean doTransfer,
            IRecipeTransferHandlerHelper helper
    ) {
        if (!PersonalDatabaseClient.lastKnownEnhancementConfig().isEnabled(DatabaseEnhancementOption.JEI_AUTO_EXTRACT_FOR_CRAFTING)) {
            return null;
        }

        List<JeiCraftingExtractPayload.MaterialGap> gaps = computeGaps(recipeSlots, player);
        if (gaps.isEmpty()) {
            return null;
        }

        if (!doTransfer) {
            // 预览阶段：检查背包+数据库总数量是否足够
            List<JeiCraftingGapHelper.Gap> helperGaps = gaps.stream()
                    .map(gap -> new JeiCraftingGapHelper.Gap(gap.stack(), gap.needed()))
                    .toList();
            if (!JeiCraftingGapHelper.hasEnoughInDatabase(helperGaps, JeiAmountCache.INSTANCE)) {
                return null;
            }
            return null;
        }

        // 执行阶段：发送提取请求，返回提示阻止 JEI 默认转移（避免时序问题）
        PacketDistributor.sendToServer(new JeiCraftingExtractPayload(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("jei", "crafting"),
                gaps
        ));
        return helper.createUserErrorWithTooltip(
                Component.translatable("screen.infiniteinventory.jei.extracting_from_database")
        );
    }

    private static List<JeiCraftingExtractPayload.MaterialGap> computeGaps(
            IRecipeSlotsView recipeSlots,
            Player player
    ) {
        List<JeiCraftingGapHelper.SlotIngredient> ingredients = new ArrayList<>();
        for (var slotView : recipeSlots.getSlotViews()) {
            List<ItemStack> stacks = slotView.getItemStacks().toList();
            ItemStack first = stacks.isEmpty() ? ItemStack.EMPTY : stacks.getFirst();
            ingredients.add(new JeiCraftingGapHelper.SlotIngredient(first, slotView.isEmpty()));
        }

        return JeiCraftingGapHelper.computeGaps(ingredients, player.getInventory().items).stream()
                .map(gap -> new JeiCraftingExtractPayload.MaterialGap(gap.stack(), gap.needed()))
                .toList();
    }

    /**
     * 特定容器类型的配方转移处理器，委托到共享的转移逻辑。
     */
    private static final class SpecificHandler<C extends AbstractContainerMenu, R> implements IRecipeTransferHandler<C, R> {
        private final Class<? extends C> containerClass;
        private final RecipeType<R> recipeType;
        private final IRecipeTransferHandlerHelper helper;

        SpecificHandler(Class<? extends C> containerClass, RecipeType<R> recipeType, IRecipeTransferHandlerHelper helper) {
            this.containerClass = containerClass;
            this.recipeType = recipeType;
            this.helper = helper;
        }

        @Override
        public Class<? extends C> getContainerClass() {
            return this.containerClass;
        }

        @Override
        public java.util.Optional<net.minecraft.world.inventory.MenuType<C>> getMenuType() {
            return java.util.Optional.empty();
        }

        @Override
        public RecipeType<R> getRecipeType() {
            return this.recipeType;
        }

        @Override
        public IRecipeTransferError transferRecipe(
                C container,
                R recipe,
                IRecipeSlotsView recipeSlots,
                Player player,
                boolean maxTransfer,
                boolean doTransfer
        ) {
            return handleTransfer(container, recipeSlots, player, doTransfer, this.helper);
        }
    }
}
