package com.agguy.infiniteinventory;

import com.agguy.infiniteinventory.client.screen.PersonalDatabaseScreen;
import com.agguy.infiniteinventory.network.OpenEquippedDatabasePayload;
import com.agguy.infiniteinventory.registry.ModMenus;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
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
