package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

final class VanillaWidgetRenderer {
    private static final int OVERLAY_SHADOW_COLOR = 0x70000000;
    private static final int OVERLAY_OUTLINE_COLOR = 0xFF433A31;
    private static final int OVERLAY_BACKGROUND_COLOR = 0xFFF2ECDD;
    private static final int OVERLAY_TOP_EDGE_COLOR = 0x90FFFDF7;
    private static final int OVERLAY_BOTTOM_EDGE_COLOR = 0x50261E16;
    private static final int OVERLAY_ROW_COLOR = 0x80E1D9C9;
    private static final int OVERLAY_ROW_HOVERED_COLOR = 0xE0D8CFBD;
    private static final int OVERLAY_ROW_SELECTED_COLOR = 0xE0CDB27A;
    private static final int OVERLAY_ROW_DIVIDER_COLOR = 0x70A89E8C;
    private static final int OVERLAY_CHIP_ACTIVE_COLOR = 0xFFF5F0E4;
    private static final int OVERLAY_CHIP_HOVERED_COLOR = 0xFFE8DDC6;
    private static final int OVERLAY_CHIP_SELECTED_COLOR = 0xFFD6BB86;
    private static final int OVERLAY_CHIP_DISABLED_COLOR = 0xFFD3CCBE;
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

    static void renderSlot(GuiGraphics guiGraphics, PersonalDatabaseLayout.Rect rect) {
        guiGraphics.blitSprite(SLOT_SPRITE, rect.x(), rect.y(), rect.width(), rect.height());
    }

    static void renderMenuSlot(GuiGraphics guiGraphics, int slotX, int slotY) {
        guiGraphics.blitSprite(SLOT_SPRITE, slotX - 1, slotY - 1, PersonalDatabaseLayout.SLOT_SIZE, PersonalDatabaseLayout.SLOT_SIZE);
    }

    static void renderTab(GuiGraphics guiGraphics, PersonalDatabaseLayout.Rect rect, boolean selected, boolean hovered) {
        ResourceLocation sprite = selected
                ? hovered ? TAB_SELECTED_HIGHLIGHTED_SPRITE : TAB_SELECTED_SPRITE
                : hovered ? TAB_HIGHLIGHTED_SPRITE : TAB_SPRITE;
        guiGraphics.blitSprite(sprite, rect.x(), rect.y(), rect.width(), rect.height());
    }

    static void renderTextField(GuiGraphics guiGraphics, PersonalDatabaseLayout.Rect rect, boolean focused) {
        int outlineColor = focused ? 0xFFC49B60 : 0xFF2A2E35;
        int innerOutlineColor = focused ? 0xFF575E69 : 0xFF3D434B;
        int fillColor = focused ? 0xFF15191F : 0xFF101419;
        int topEdgeColor = focused ? 0xA07A8390 : 0x705C6674;
        guiGraphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), outlineColor);
        guiGraphics.fill(rect.x() + 1, rect.y() + 1, rect.right() - 1, rect.bottom() - 1, innerOutlineColor);
        guiGraphics.fill(rect.x() + 2, rect.y() + 2, rect.right() - 2, rect.bottom() - 2, fillColor);
        guiGraphics.fill(rect.x() + 2, rect.y() + 2, rect.right() - 2, rect.y() + 3, topEdgeColor);
    }

    static void renderSlotHighlight(GuiGraphics guiGraphics, PersonalDatabaseLayout.Rect rect) {
        guiGraphics.fill(rect.x() + 1, rect.y() + 1, rect.right() - 1, rect.bottom() - 1, 0x48000000);
    }

    static void renderSlotSelection(GuiGraphics guiGraphics, PersonalDatabaseLayout.Rect rect) {
        guiGraphics.fill(rect.x() + 1, rect.y() + 1, rect.right() - 1, rect.bottom() - 1, 0x40E3D0A4);
        guiGraphics.fill(rect.x() + 1, rect.y() + 1, rect.right() - 1, rect.y() + 2, 0xFFD6B86E);
        guiGraphics.fill(rect.x() + 1, rect.bottom() - 2, rect.right() - 1, rect.bottom() - 1, 0xFF8D6F28);
        guiGraphics.fill(rect.x() + 1, rect.y() + 1, rect.x() + 2, rect.bottom() - 1, 0xFFD6B86E);
        guiGraphics.fill(rect.right() - 2, rect.y() + 1, rect.right() - 1, rect.bottom() - 1, 0xFF8D6F28);
    }

    static void renderOverlayPanel(GuiGraphics guiGraphics, PersonalDatabaseLayout.Rect rect) {
        guiGraphics.fill(rect.x() + 2, rect.y() + 2, rect.right() + 2, rect.bottom() + 2, OVERLAY_SHADOW_COLOR);
        guiGraphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), OVERLAY_OUTLINE_COLOR);
        guiGraphics.fill(rect.x() + 1, rect.y() + 1, rect.right() - 1, rect.bottom() - 1, OVERLAY_BACKGROUND_COLOR);
        guiGraphics.fill(rect.x() + 2, rect.y() + 2, rect.right() - 2, rect.y() + 3, OVERLAY_TOP_EDGE_COLOR);
        guiGraphics.fill(rect.x() + 2, rect.bottom() - 3, rect.right() - 2, rect.bottom() - 2, OVERLAY_BOTTOM_EDGE_COLOR);
    }

    static void renderOverlayRow(GuiGraphics guiGraphics, PersonalDatabaseLayout.Rect rect, boolean hovered, boolean selected) {
        int fillColor = selected ? OVERLAY_ROW_SELECTED_COLOR : hovered ? OVERLAY_ROW_HOVERED_COLOR : OVERLAY_ROW_COLOR;
        guiGraphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), fillColor);
        guiGraphics.fill(rect.x(), rect.bottom() - 1, rect.right(), rect.bottom(), OVERLAY_ROW_DIVIDER_COLOR);
    }

    static void renderOverlayChip(
            GuiGraphics guiGraphics,
            PersonalDatabaseLayout.Rect rect,
            boolean hovered,
            boolean selected,
            boolean enabled
    ) {
        int fillColor = !enabled
                ? OVERLAY_CHIP_DISABLED_COLOR
                : selected ? OVERLAY_CHIP_SELECTED_COLOR
                : hovered ? OVERLAY_CHIP_HOVERED_COLOR
                : OVERLAY_CHIP_ACTIVE_COLOR;
        guiGraphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), OVERLAY_OUTLINE_COLOR);
        guiGraphics.fill(rect.x() + 1, rect.y() + 1, rect.right() - 1, rect.bottom() - 1, fillColor);
        guiGraphics.fill(rect.x() + 2, rect.y() + 2, rect.right() - 2, rect.y() + 3, OVERLAY_TOP_EDGE_COLOR);
    }

    static void renderSearchGlyph(GuiGraphics guiGraphics, int x, int y, int color) {
        // 使用 RemixIcon 替代手绘像素，color 参数保留以兼容调用方但不再使用
        renderRemixIcon(guiGraphics, RemixIcon.SEARCH, x, y, 9);
    }

    static void renderDropdownIndicator(GuiGraphics guiGraphics, int centerX, int centerY, int color) {
        guiGraphics.fill(centerX - 3, centerY - 1, centerX + 4, centerY, color);
        guiGraphics.fill(centerX - 2, centerY, centerX + 3, centerY + 1, color);
        guiGraphics.fill(centerX - 1, centerY + 1, centerX + 2, centerY + 2, color);
        guiGraphics.fill(centerX, centerY + 2, centerX + 1, centerY + 3, color);
    }

    static void renderSortDirectionIndicator(
            GuiGraphics guiGraphics,
            int centerX,
            int centerY,
            boolean ascending,
            int color
    ) {
        // color 参数保留以兼容调用方但不再使用
        RemixIcon icon = ascending ? RemixIcon.SORT_ASC : RemixIcon.SORT_DESC;
        renderRemixIcon(guiGraphics, icon, centerX - 6, centerY - 6, 12);
    }

    static void renderRemixIcon(GuiGraphics guiGraphics, @Nullable RemixIcon icon, int x, int y, int size) {
        if (icon == null) {
            return;
        }
        guiGraphics.blitSprite(icon.location(), x, y, size, size);
    }

    static void renderRemixIcon(GuiGraphics guiGraphics, @Nullable RemixIcon icon, int x, int y) {
        renderRemixIcon(guiGraphics, icon, x, y, 16);
    }

    static void renderRemixIconSmall(GuiGraphics guiGraphics, @Nullable RemixIcon icon, int x, int y) {
        renderRemixIcon(guiGraphics, icon, x, y, 12);
    }

    /**
     * 在浅色背景上渲染 Remix Icon，附加 1px 右下偏移的半透明阴影以提升可读性。
     */
    static void renderRemixIconWithShadow(GuiGraphics guiGraphics, @Nullable RemixIcon icon, int x, int y, int size) {
        if (icon == null) {
            return;
        }
        guiGraphics.setColor(0.0f, 0.0f, 0.0f, 0.35f);
        guiGraphics.blitSprite(icon.location(), x + 1, y + 1, size, size);
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        guiGraphics.blitSprite(icon.location(), x, y, size, size);
    }
}
