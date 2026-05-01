package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import com.agguy.infiniteinventory.network.DatabaseDepositResolvePayload;
import com.agguy.infiniteinventory.network.DepositConflictAction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 存款冲突提示框的渲染与交互辅助类。
 *
 * <p>当用户尝试将物品存入与当前存放页签不同的目标页签时，弹出三选项提示框。
 */
final class PersonalDatabaseScreenDepositConflictHelper {
    private static final int PANEL_WIDTH = 360;
    private static final int ROW_HEIGHT = 24;
    private static final int ITEM_ICON_SIZE = 16;

    private PersonalDatabaseScreenDepositConflictHelper() {
    }

    static void renderDepositConflict(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!screen.depositConflictExpanded || screen.pendingDepositConflict == null) {
            return;
        }
        PersonalDatabaseLayout.Rect panelRect = depositConflictRect(screen);
        if (panelRect.width() <= 0 || panelRect.height() <= 0) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 260.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        PersonalDatabaseScreenOverlayRenderHelper.renderOverlayCloseButton(screen, guiGraphics, panelRect, mouseX, mouseY);

        guiGraphics.drawString(
                screen.screenFont(),
                Component.translatable("screen.infiniteinventory.deposit_conflict.title"),
                panelRect.x() + 8,
                panelRect.y() + 8,
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                false
        );
        int dividerY1 = panelRect.y() + 8 + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT;
        guiGraphics.fill(panelRect.x() + 8, dividerY1, panelRect.right() - 8, dividerY1 + 1, GuiTheme.DIVIDER);

        int itemY = dividerY1 + 8;
        guiGraphics.renderItem(screen.pendingDepositConflict.stack(), panelRect.x() + 12, itemY);
        guiGraphics.drawString(
                screen.screenFont(),
                screen.pendingDepositConflict.stack().getHoverName(),
                panelRect.x() + 12 + ITEM_ICON_SIZE + 6,
                itemY + 4,
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                false
        );

        int descY = itemY + ITEM_ICON_SIZE + 6;
        String existingTabId = screen.pendingDepositConflict.existingTabId();
        Component existingTabLabel = PersonalDatabaseScreenCommonHelper.tabLabelById(screen, existingTabId);
        Component desc = Component.translatable(
                "screen.infiniteinventory.deposit_conflict.description",
                existingTabLabel
        );
        guiGraphics.drawString(
                screen.screenFont(),
                desc,
                panelRect.x() + 12,
                descY,
                PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR,
                false
        );

        int dividerY2 = descY + screen.screenFont().lineHeight + 8;
        guiGraphics.fill(panelRect.x() + 8, dividerY2, panelRect.right() - 8, dividerY2 + 1, GuiTheme.DIVIDER);

        Component[] optionLabels = {
                Component.translatable("screen.infiniteinventory.deposit_conflict.cancel"),
                Component.translatable("screen.infiniteinventory.deposit_conflict.move_to_target"),
                Component.translatable("screen.infiniteinventory.deposit_conflict.keep_in_source")
        };
        for (int index = 0; index < optionLabels.length; index++) {
            PersonalDatabaseLayout.Rect rowRect = conflictOptionRowRect(panelRect, index, dividerY2 + 8);
            boolean hovered = rowRect.contains(mouseX, mouseY);
            VanillaWidgetRenderer.renderOverlayRow(guiGraphics, rowRect, hovered, false);
            guiGraphics.drawString(
                    screen.screenFont(),
                    optionLabels[index],
                    rowRect.x() + 6,
                    rowRect.y() + 6,
                    PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                    false
            );
        }
        guiGraphics.pose().popPose();
    }

    static boolean handleDepositConflictClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        if (!screen.depositConflictExpanded || screen.pendingDepositConflict == null) {
            return false;
        }
        PersonalDatabaseLayout.Rect panelRect = depositConflictRect(screen);
        if (PersonalDatabaseScreenOverlayRenderHelper.isOverlayCloseClicked(panelRect, mouseX, mouseY)) {
            closeDepositConflict(screen);
            return true;
        }
        if (!panelRect.contains(mouseX, mouseY)) {
            closeDepositConflict(screen);
            return true;
        }
        int dividerY2 = panelRect.y() + 8 + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT
                + 8 + ITEM_ICON_SIZE + 6 + screen.screenFont().lineHeight + 8;
        for (int index = 0; index < 3; index++) {
            PersonalDatabaseLayout.Rect rowRect = conflictOptionRowRect(panelRect, index, dividerY2 + 8);
            if (!rowRect.contains(mouseX, mouseY)) {
                continue;
            }
            DepositConflictAction action = switch (index) {
                case 0 -> DepositConflictAction.CANCEL;
                case 1 -> DepositConflictAction.MOVE_TO_TARGET;
                case 2 -> DepositConflictAction.KEEP_IN_SOURCE;
                default -> DepositConflictAction.CANCEL;
            };
            if (action != DepositConflictAction.CANCEL) {
                sendDepositResolve(screen, action);
            }
            closeDepositConflict(screen);
            return true;
        }
        return true;
    }

    static void closeDepositConflict(PersonalDatabaseScreen screen) {
        screen.depositConflictExpanded = false;
        screen.pendingDepositConflict = null;
    }

    static void tryOpenDepositConflict(PersonalDatabaseScreen screen) {
        com.agguy.infiniteinventory.network.DatabaseDepositConflictPayload conflict =
                com.agguy.infiniteinventory.client.PersonalDatabaseClient.pendingDepositConflict();
        if (conflict == null) {
            return;
        }
        PersonalDatabaseScreenTargetHelper.closeTransientOverlays(screen);
        screen.pendingDepositConflict = conflict;
        screen.depositConflictExpanded = true;
    }

    private static void sendDepositResolve(PersonalDatabaseScreen screen, DepositConflictAction action) {
        if (screen.pendingDepositConflict == null) {
            return;
        }
        PacketDistributor.sendToServer(new DatabaseDepositResolvePayload(
                screen.databaseMenu.containerId,
                screen.databaseMenu.viewState().sessionId(),
                screen.pendingDepositConflict.scope(),
                screen.pendingDepositConflict.targetTabId(),
                screen.pendingDepositConflict.existingTabId(),
                screen.pendingDepositConflict.stack(),
                screen.pendingDepositConflict.slotIndex(),
                action
        ));
    }

    static PersonalDatabaseLayout.Rect depositConflictRect(PersonalDatabaseScreen screen) {
        if (screen.layout == null) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        int height = PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING * 2
                + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT
                + 8 + ITEM_ICON_SIZE + 6 + screen.screenFont().lineHeight + 8
                + 3 * ROW_HEIGHT + 8;
        return PersonalDatabaseScreenGeometry.centeredOverlayRect(screen, PANEL_WIDTH, height);
    }

    private static PersonalDatabaseLayout.Rect conflictOptionRowRect(PersonalDatabaseLayout.Rect panelRect, int index, int firstRowY) {
        return new PersonalDatabaseLayout.Rect(
                panelRect.x() + PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING,
                firstRowY + index * ROW_HEIGHT,
                panelRect.width() - PersonalDatabaseScreen.MANAGEMENT_PANEL_PADDING * 2,
                ROW_HEIGHT - 1
        );
    }
}
