package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.compat.AccessorySlotGroup;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

final class PersonalDatabaseScreenAccessoryPanelHelper {
    private PersonalDatabaseScreenAccessoryPanelHelper() {
    }

    static void renderAccessoriesPanel(PersonalDatabaseScreen screen, GuiGraphics guiGraphics) {
        if (screen.layout == null || screen.layout.accessoriesPanelRect().height() <= 0) {
            return;
        }
        PersonalDatabaseLayout.Rect panelRect = screen.layout.accessoriesPanelRect();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 220.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        guiGraphics.drawString(
                screen.screenFont(),
                Component.translatable("screen.infiniteinventory.accessories_panel"),
                panelRect.x() + PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING,
                panelRect.y() + PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING,
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                false
        );
        guiGraphics.fill(
                panelRect.x() + PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING,
                panelRect.y() + PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING + PersonalDatabaseLayout.ACCESSORY_DRAWER_TITLE_HEIGHT + 1,
                panelRect.right() - PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING,
                panelRect.y() + PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING + PersonalDatabaseLayout.ACCESSORY_DRAWER_TITLE_HEIGHT + 2,
                0x70A89E8C
        );
        for (int index = 0; index < screen.layout.accessoryGroupLayouts().size(); index++) {
            PersonalDatabaseLayout.AccessoryGroupLayout groupLayout = screen.layout.accessoryGroupLayouts().get(index);
            if (!groupLayout.visible()) {
                continue;
            }
            renderAccessoryGroupBody(guiGraphics, groupLayout.bodyRect(), index);
            renderAccessoryGroupHeader(screen, guiGraphics, groupLayout.group(), groupLayout.headerRect());
        }
        guiGraphics.pose().popPose();
    }

    static void renderAccessorySlotHover(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.AccessorySlotLayout hoveredSlot = PersonalDatabaseScreenGeometry.findHoveredAccessorySlot(
                screen,
                mouseX,
                mouseY
        );
        if (hoveredSlot == null) {
            return;
        }
        PersonalDatabaseLayout.Rect slotRect = hoveredSlot.slotRect();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 230.0F);
        guiGraphics.fill(slotRect.x(), slotRect.y(), slotRect.x() + 16, slotRect.y() + 16, 0x52000000);
        guiGraphics.pose().popPose();
    }

    private static void renderAccessoryGroupBody(
            GuiGraphics guiGraphics,
            PersonalDatabaseLayout.Rect bodyRect,
            int groupIndex
    ) {
        if (bodyRect.width() <= 0 || bodyRect.height() <= 0) {
            return;
        }
        int fillColor = groupIndex % 2 == 0 ? 0x2ADCCFB9 : 0x20D4C7B2;
        guiGraphics.fill(bodyRect.x(), bodyRect.y(), bodyRect.right(), bodyRect.bottom(), fillColor);
        guiGraphics.fill(bodyRect.x(), bodyRect.y(), bodyRect.right(), bodyRect.y() + 1, 0x708F7D63);
        guiGraphics.fill(bodyRect.x(), bodyRect.bottom() - 1, bodyRect.right(), bodyRect.bottom(), 0x60433731);
        guiGraphics.fill(bodyRect.x(), bodyRect.y(), bodyRect.x() + 1, bodyRect.bottom(), 0x608F7D63);
        guiGraphics.fill(bodyRect.right() - 1, bodyRect.y(), bodyRect.right(), bodyRect.bottom(), 0x50433731);
        for (int x = bodyRect.x() + PersonalDatabaseLayout.SLOT_SIZE; x < bodyRect.right(); x += PersonalDatabaseLayout.SLOT_SIZE) {
            guiGraphics.fill(x, bodyRect.y(), x + 1, bodyRect.bottom(), 0x12000000);
        }
        for (int y = bodyRect.y() + PersonalDatabaseLayout.SLOT_SIZE; y < bodyRect.bottom(); y += PersonalDatabaseLayout.SLOT_SIZE) {
            guiGraphics.fill(bodyRect.x(), y, bodyRect.right(), y + 1, 0x18000000);
        }
    }

    private static void renderAccessoryGroupHeader(
            PersonalDatabaseScreen screen,
            GuiGraphics guiGraphics,
            AccessorySlotGroup group,
            PersonalDatabaseLayout.Rect headerRect
    ) {
        if (headerRect.width() <= 0 || headerRect.height() <= 0) {
            return;
        }
        Component label = I18n.exists(group.translationKey())
                ? Component.translatable(group.translationKey())
                : Component.literal(group.slotName());
        int labelX = headerRect.x() + 2;
        int labelY = headerRect.y() + 5;
        guiGraphics.drawString(
                screen.screenFont(),
                label,
                labelX,
                labelY,
                PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR,
                true
        );
        int dividerX = Math.min(headerRect.right() - 2, labelX + screen.screenFont().width(label) + 6);
        if (dividerX < headerRect.right() - 2) {
            guiGraphics.fill(dividerX, headerRect.y() + 9, headerRect.right() - 2, headerRect.y() + 10, 0x70A89E8C);
        }
    }
}
