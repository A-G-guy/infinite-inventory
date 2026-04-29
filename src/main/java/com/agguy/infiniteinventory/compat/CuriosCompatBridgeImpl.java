package com.agguy.infiniteinventory.compat;

import com.agguy.infiniteinventory.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

final class CuriosCompatBridgeImpl implements CuriosCompatBridge {
    private static final ICurioItem DATABASE_TERMINAL_CURIO = new DatabaseTerminalCurio();

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void registerDatabaseTerminalAccessory() {
        CuriosApi.registerCurio(ModItems.DATABASE_ACCESS_ITEM.get(), DATABASE_TERMINAL_CURIO);
    }

    @Override
    public boolean isBackSlotEquipped(Player player, Item item) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> isBackSlotEquipped(handler, item))
                .orElse(false);
    }

    private boolean isBackSlotEquipped(ICuriosItemHandler handler, Item item) {
        return handler.findCurios("back").stream()
                .anyMatch(result -> result.stack().is(item));
    }

    @Override
    public List<AccessorySlotGroup> appendAccessorySlots(Player player, MenuSlotAdder slotAdder) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> appendSlots(handler, player, slotAdder))
                .orElse(List.of());
    }

    private List<AccessorySlotGroup> appendSlots(ICuriosItemHandler handler, Player player, MenuSlotAdder slotAdder) {
        List<AccessorySlotGroup> groups = new ArrayList<>();
        Map<String, ICurioStacksHandler> curios = handler.getCurios();
        if (curios.isEmpty()) {
            return List.of();
        }

        // 获取玩家实体槽位类型并按 Curios 定义的顺序排序
        Map<String, ISlotType> entitySlots = CuriosApi.getEntitySlots(player);
        List<String> sortedSlotIds = entitySlots.values().stream()
                .sorted()
                .map(ISlotType::getIdentifier)
                .filter(curios::containsKey)
                .toList();

        for (String slotTypeId : sortedSlotIds) {
            ICurioStacksHandler stacksHandler = curios.get(slotTypeId);
            if (stacksHandler == null) {
                continue;
            }
            var itemHandler = stacksHandler.getStacks();
            if (itemHandler == null || itemHandler.getSlots() <= 0) {
                continue;
            }
            int firstSlotIndex = -1;
            int addedSlotCount = 0;
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                SlotItemHandler curiosSlot = new SlotItemHandler(itemHandler, slot, 0, 0);
                int menuIndex = slotAdder.add(curiosSlot);
                if (firstSlotIndex < 0) {
                    firstSlotIndex = menuIndex;
                }
                addedSlotCount++;
            }
            if (firstSlotIndex >= 0 && addedSlotCount > 0) {
                String translationKey = "curios.identifier." + slotTypeId;
                groups.add(new AccessorySlotGroup(slotTypeId, translationKey, firstSlotIndex, addedSlotCount));
            }
        }
        return List.copyOf(groups);
    }

    private static final class DatabaseTerminalCurio implements ICurioItem {
        @Override
        public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
            return false;
        }

        @Override
        public ICurio.DropRule getDropRule(SlotContext slotContext, DamageSource source, boolean recentlyHit, ItemStack stack) {
            return ICurio.DropRule.ALWAYS_KEEP;
        }
    }
}
