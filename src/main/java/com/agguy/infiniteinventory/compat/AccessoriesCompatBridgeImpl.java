package com.agguy.infiniteinventory.compat;

import com.agguy.infiniteinventory.registry.ModItems;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.data.AccessoriesBaseData;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class AccessoriesCompatBridgeImpl implements AccessoriesCompatBridge {
    private static final Accessory DATABASE_TERMINAL_ACCESSORY = new DatabaseTerminalAccessory();

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void registerDatabaseTerminalAccessory() {
        AccessoriesAPI.registerAccessory(ModItems.DATABASE_ACCESS_ITEM.get(), DATABASE_TERMINAL_ACCESSORY);
    }

    @Override
    public boolean isBackSlotEquipped(Player player, Item item) {
        AccessoriesCapability capability = AccessoriesCapability.get(player);
        if (capability == null) {
            return false;
        }
        return capability.getEquipped(item).stream()
                .map(reference -> reference.reference())
                .anyMatch(reference -> AccessoriesBaseData.BACK_SLOT.equals(reference.slotName()));
    }

    @Override
    public List<AccessorySlotGroup> appendAccessorySlots(Player player, MenuSlotAdder slotAdder) {
        AccessoriesCapability capability = AccessoriesCapability.get(player);
        if (capability == null) {
            return List.of();
        }

        List<AccessorySlotGroup> groups = new ArrayList<>();
        List<SlotType> slotTypes = EntitySlotLoader.getEntitySlots(player).values().stream()
                .sorted(Comparator.naturalOrder())
                .toList();
        for (SlotType slotType : slotTypes) {
            var container = capability.getContainer(slotType);
            if (container == null || container.getSize() <= 0) {
                continue;
            }
            int firstSlotIndex = -1;
            int addedSlotCount = 0;
            for (int slot = 0; slot < container.getSize(); slot++) {
                AccessoriesBasedSlot accessoriesSlot = AccessoriesBasedSlot.of(player, slotType, slot, 0, 0);
                if (accessoriesSlot == null) {
                    continue;
                }
                int menuIndex = slotAdder.add(accessoriesSlot);
                if (firstSlotIndex < 0) {
                    firstSlotIndex = menuIndex;
                }
                addedSlotCount++;
            }
            if (firstSlotIndex >= 0 && addedSlotCount > 0) {
                groups.add(new AccessorySlotGroup(slotType.name(), slotType.translation(), firstSlotIndex, addedSlotCount));
            }
        }
        return List.copyOf(groups);
    }

    @SuppressWarnings("removal")
    private static final class DatabaseTerminalAccessory implements Accessory {
        @Override
        public boolean canEquipFromUse(ItemStack stack, SlotReference reference) {
            return false;
        }

        @Override
        public DropRule getDropRule(ItemStack stack, SlotReference reference, DamageSource source) {
            return DropRule.KEEP;
        }
    }
}
