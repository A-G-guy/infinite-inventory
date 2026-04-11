package com.agguy.infiniteinventory.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class VanillaPlayerInventoryPaneProvider implements PlayerInventoryPaneProvider {
    private static final ResourceLocation INVENTORY_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png");
    private static final int SECTION_WIDTH = 176;
    private static final int TOP_SECTION_HEIGHT = 83;
    private static final int BOTTOM_SECTION_HEIGHT = 83;

    @Override
    public int equipmentPanelWidth() {
        return SECTION_WIDTH;
    }

    @Override
    public int equipmentPanelHeight() {
        return TOP_SECTION_HEIGHT;
    }

    @Override
    public int bottomInventoryWidth() {
        return SECTION_WIDTH;
    }

    @Override
    public int bottomInventoryHeight() {
        return BOTTOM_SECTION_HEIGHT;
    }

    @Override
    public void renderEquipmentPanel(GuiGraphics guiGraphics, Player player, int left, int top, int mouseX, int mouseY) {
        guiGraphics.blit(INVENTORY_TEXTURE, left, top, 0, 0, this.equipmentPanelWidth(), this.equipmentPanelHeight());
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

    @Override
    public void renderBottomInventory(GuiGraphics guiGraphics, int left, int top) {
        guiGraphics.blit(INVENTORY_TEXTURE, left, top, 0, 83, this.bottomInventoryWidth(), this.bottomInventoryHeight());
    }
}
