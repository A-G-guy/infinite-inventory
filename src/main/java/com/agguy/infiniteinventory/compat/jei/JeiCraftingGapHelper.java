package com.agguy.infiniteinventory.compat.jei;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * JEI 合成缺口计算辅助类。
 * 将不依赖 JEI API 的纯逻辑提取出来，便于单元测试。
 */
final class JeiCraftingGapHelper {

    private JeiCraftingGapHelper() {
    }

    record SlotIngredient(ItemStack firstStack, boolean empty) {
    }

    record Gap(ItemStack stack, int needed) {
    }

    static List<Gap> computeGaps(List<SlotIngredient> ingredients, List<ItemStack> inventoryItems) {
        List<Gap> gaps = new ArrayList<>();
        for (SlotIngredient ingredient : ingredients) {
            if (ingredient.empty() || ingredient.firstStack().isEmpty()) {
                continue;
            }
            ItemStack needed = ingredient.firstStack();
            int required = needed.getCount();
            int available = countInInventory(inventoryItems, needed);
            if (available < required) {
                gaps.add(new Gap(needed.copyWithCount(1), required - available));
            }
        }
        return gaps;
    }

    static int countInInventory(List<ItemStack> inventoryItems, ItemStack needed) {
        int count = 0;
        for (ItemStack stack : inventoryItems) {
            if (ItemStack.isSameItemSameComponents(stack, needed)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    static boolean hasEnoughInDatabase(List<Gap> gaps, JeiAmountCache cache) {
        for (Gap gap : gaps) {
            long personal = cache.getPersonalAmount(gap.stack());
            long publicItems = cache.getPublicAmount(gap.stack());
            if (personal + publicItems < gap.needed()) {
                return false;
            }
        }
        return true;
    }
}
