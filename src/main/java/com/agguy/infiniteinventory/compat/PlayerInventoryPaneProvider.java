package com.agguy.infiniteinventory.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

public interface PlayerInventoryPaneProvider {
    int equipmentPanelWidth();

    int equipmentPanelHeight();

    int bottomInventoryWidth();

    int bottomInventoryHeight();

    void renderEquipmentPanel(GuiGraphics guiGraphics, Player player, int left, int top, int mouseX, int mouseY);

    void renderBottomInventory(GuiGraphics guiGraphics, int left, int top);
}
