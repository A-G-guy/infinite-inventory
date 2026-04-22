package com.agguy.infiniteinventory;

import com.agguy.infiniteinventory.client.PersonalDatabaseClient;
import com.agguy.infiniteinventory.client.screen.PersonalDatabaseScreen;
import com.agguy.infiniteinventory.compat.jei.JeiAmountCache;
import com.agguy.infiniteinventory.database.DatabaseEnhancementOption;
import com.agguy.infiniteinventory.network.OpenEquippedDatabasePayload;
import com.agguy.infiniteinventory.registry.ModMenus;
import com.agguy.infiniteinventory.util.CompactNumberFormatter;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class InfiniteInventoryClient {
    private static final String KEY_CATEGORY = "key.categories.infiniteinventory";
    private static final KeyMapping OPEN_EQUIPPED_DATABASE_KEY = new KeyMapping(
            "key.infiniteinventory.open_equipped_database",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            KEY_CATEGORY
    );

    private InfiniteInventoryClient() {
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.PERSONAL_DATABASE_MENU.get(), PersonalDatabaseScreen::new);
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_EQUIPPED_DATABASE_KEY);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        PersonalDatabaseClient.syncViewerLanguageIfNeeded();
        if (minecraft.player == null) {
            return;
        }
        while (OPEN_EQUIPPED_DATABASE_KEY.consumeClick()) {
            if (!canOpenEquippedDatabase(minecraft)) {
                continue;
            }
            PacketDistributor.sendToServer(new OpenEquippedDatabasePayload());
        }
    }

    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!JeiAmountCache.isAvailable()) {
            return;
        }
        if (!PersonalDatabaseClient.lastKnownEnhancementConfig().isEnabled(DatabaseEnhancementOption.SHOW_JEI_AMOUNT_IN_TOOLTIP)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof PersonalDatabaseScreen) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        long personal = JeiAmountCache.INSTANCE.getPersonalAmount(stack);
        long publicItems = JeiAmountCache.INSTANCE.getPublicAmount(stack);
        if (personal <= 0 && publicItems <= 0) {
            return;
        }
        List<Component> tooltip = event.getToolTip();
        int insertIndex = tooltip.size();
        for (int i = 1; i < tooltip.size(); i++) {
            String text = tooltip.get(i).getString();
            if (text.contains("JEI") || text.contains("REI") || text.contains("EMI")) {
                insertIndex = i;
                break;
            }
        }
        if (personal > 0) {
            MutableComponent line = Component.translatable(
                    "screen.infiniteinventory.jei.amount.personal",
                    CompactNumberFormatter.format(personal)
            );
            tooltip.add(insertIndex, line.withColor(0x55FFFF));
            insertIndex++;
        }
        if (publicItems > 0) {
            MutableComponent line = Component.translatable(
                    "screen.infiniteinventory.jei.amount.public",
                    CompactNumberFormatter.format(publicItems)
            );
            tooltip.add(insertIndex, line.withColor(0xFFAA00));
        }
    }

    private static boolean canOpenEquippedDatabase(Minecraft minecraft) {
        if (minecraft.screen == null) {
            return true;
        }
        if (!(minecraft.screen instanceof AbstractContainerScreen<?>)) {
            return false;
        }
        if (minecraft.screen instanceof PersonalDatabaseScreen) {
            return false;
        }
        return !(minecraft.screen.getFocused() instanceof EditBox);
    }
}
