package com.agguy.infiniteinventory.compat.jei;

import com.agguy.infiniteinventory.network.JeiCraftingExtractPayload;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * JEI 配方转移处理器。
 * 拦截 + 按钮点击，当背包材料不足时自动从数据库补充提取。
 */
final class JeiRecipeTransferHandler implements IUniversalRecipeTransferHandler<AbstractContainerMenu> {

    private JeiRecipeTransferHandler() {
    }

    static void register(IRecipeTransferRegistration registration) {
        JeiRecipeTransferHandler handler = new JeiRecipeTransferHandler();
        registration.addUniversalRecipeTransferHandler(handler);
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
        if (!doTransfer) {
            return null;
        }
        List<JeiCraftingExtractPayload.MaterialGap> gaps = computeGaps(container, recipeSlots, player);
        if (gaps.isEmpty()) {
            return null;
        }
        PacketDistributor.sendToServer(new JeiCraftingExtractPayload(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("jei", "crafting"),
                gaps
        ));
        return null;
    }

    private static List<JeiCraftingExtractPayload.MaterialGap> computeGaps(
            AbstractContainerMenu container,
            IRecipeSlotsView recipeSlots,
            Player player
    ) {
        List<JeiCraftingExtractPayload.MaterialGap> gaps = new ArrayList<>();
        for (var slotView : recipeSlots.getSlotViews()) {
            if (slotView.isEmpty()) {
                continue;
            }
            List<ItemStack> ingredients = slotView.getItemStacks().toList();
            if (ingredients.isEmpty()) {
                continue;
            }
            ItemStack needed = ingredients.getFirst();
            int required = needed.getCount();
            int available = countInInventory(player, needed);
            if (available < required) {
                gaps.add(new JeiCraftingExtractPayload.MaterialGap(needed.copyWithCount(1), required - available));
            }
        }
        return gaps;
    }

    private static int countInInventory(Player player, ItemStack needed) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItemSameComponents(stack, needed)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
