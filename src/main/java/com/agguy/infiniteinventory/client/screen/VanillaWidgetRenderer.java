package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

final class VanillaWidgetRenderer {
    private static final ResourceLocation PANEL_SPRITE = ResourceLocation.withDefaultNamespace("container/bundle/background");
    private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");
    private static final ResourceLocation TAB_SPRITE = ResourceLocation.withDefaultNamespace("widget/tab");
    private static final ResourceLocation TAB_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/tab_highlighted");
    private static final ResourceLocation TAB_SELECTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/tab_selected");
    private static final ResourceLocation TAB_SELECTED_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/tab_selected_highlighted");
    private static final ResourceLocation TEXT_FIELD_SPRITE = ResourceLocation.withDefaultNamespace("widget/text_field");
    private static final ResourceLocation TEXT_FIELD_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/text_field_highlighted");

    private VanillaWidgetRenderer() {
    }

    static void renderPanel(GuiGraphics guiGraphics, PersonalDatabaseLayout.Rect rect) {
        guiGraphics.blitSprite(PANEL_SPRITE, rect.x(), rect.y(), rect.width(), rect.height());
    }

    static void renderSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blitSprite(SLOT_SPRITE, x, y, PersonalDatabaseLayout.SLOT_SIZE, PersonalDatabaseLayout.SLOT_SIZE);
    }

    static void renderTab(GuiGraphics guiGraphics, PersonalDatabaseLayout.Rect rect, boolean selected, boolean hovered) {
        ResourceLocation sprite = selected
                ? hovered ? TAB_SELECTED_HIGHLIGHTED_SPRITE : TAB_SELECTED_SPRITE
                : hovered ? TAB_HIGHLIGHTED_SPRITE : TAB_SPRITE;
        guiGraphics.blitSprite(sprite, rect.x(), rect.y(), rect.width(), rect.height());
    }

    static void renderTextField(GuiGraphics guiGraphics, PersonalDatabaseLayout.Rect rect, boolean focused) {
        guiGraphics.blitSprite(focused ? TEXT_FIELD_HIGHLIGHTED_SPRITE : TEXT_FIELD_SPRITE, rect.x(), rect.y(), rect.width(), rect.height());
    }
}
