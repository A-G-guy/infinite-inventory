package com.agguy.infiniteinventory.compat;

import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class VanillaPlayerInventoryPaneProvider implements PlayerInventoryPaneProvider {
    private static final ResourceLocation INVENTORY_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png");

    @Override
    public int panelWidth() {
        return PersonalDatabaseLayout.PLAYER_PANEL_WIDTH;
    }

    @Override
    public int panelHeight() {
        return PersonalDatabaseLayout.PLAYER_PANEL_HEIGHT;
    }

    @Override
    public void render(GuiGraphics guiGraphics, Player player, int left, int top, int mouseX, int mouseY) {
        guiGraphics.blit(INVENTORY_TEXTURE, left, top, 0, 0, this.panelWidth(), this.panelHeight());
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics,
                left + 26,
                top + 8,
                left + 75,
                top + 78,
                30,
                0.0625F,
                mouseX,
                mouseY,
                player
        );
    }
}
