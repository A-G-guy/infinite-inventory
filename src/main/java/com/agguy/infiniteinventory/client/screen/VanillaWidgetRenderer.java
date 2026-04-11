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
        guiGraphics.blitSprite(SLOT_SPRITE, x, y, PersonalDatabaseLayout.DATABASE_SLOT_SIZE, PersonalDatabaseLayout.DATABASE_SLOT_SIZE);
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

    static void renderSlotHighlight(GuiGraphics guiGraphics, PersonalDatabaseLayout.Rect rect) {
        guiGraphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), 0x22FFFFFF);
        guiGraphics.fill(rect.x(), rect.y(), rect.right(), rect.y() + 1, 0xE0FFF7D6);
        guiGraphics.fill(rect.x(), rect.bottom() - 1, rect.right(), rect.bottom(), 0xB05B5B5B);
        guiGraphics.fill(rect.x(), rect.y(), rect.x() + 1, rect.bottom(), 0xE0FFF7D6);
        guiGraphics.fill(rect.right() - 1, rect.y(), rect.right(), rect.bottom(), 0xB05B5B5B);
    }

    static void renderSearchGlyph(GuiGraphics guiGraphics, int x, int y, int color) {
        guiGraphics.fill(x + 1, y + 1, x + 5, y + 2, color);
        guiGraphics.fill(x, y + 2, x + 1, y + 5, color);
        guiGraphics.fill(x + 5, y + 2, x + 6, y + 5, color);
        guiGraphics.fill(x + 1, y + 5, x + 5, y + 6, color);
        guiGraphics.fill(x + 5, y + 5, x + 6, y + 6, color);
        guiGraphics.fill(x + 6, y + 6, x + 7, y + 7, color);
        guiGraphics.fill(x + 7, y + 7, x + 8, y + 8, color);
        guiGraphics.fill(x + 8, y + 8, x + 9, y + 9, color);
    }

    static void renderDropdownIndicator(GuiGraphics guiGraphics, int centerX, int centerY, int color) {
        guiGraphics.fill(centerX - 3, centerY - 1, centerX + 4, centerY, color);
        guiGraphics.fill(centerX - 2, centerY, centerX + 3, centerY + 1, color);
        guiGraphics.fill(centerX - 1, centerY + 1, centerX + 2, centerY + 2, color);
        guiGraphics.fill(centerX, centerY + 2, centerX + 1, centerY + 3, color);
    }
}
