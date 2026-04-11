package com.agguy.infiniteinventory.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

public interface PlayerInventoryPaneProvider {
    int panelWidth();

    int panelHeight();

    void render(GuiGraphics guiGraphics, Player player, int left, int top, int mouseX, int mouseY);
}
