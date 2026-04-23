package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

final class PersonalDatabaseScreenMoreTabsHelper {
    private PersonalDatabaseScreenMoreTabsHelper() {
    }

    static void renderMoreTabsDropdown(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenGeometry.moreTabsDropdownRect(screen);
        if (panelRect.width() <= 0 || panelRect.height() <= 0) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 253.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        List<DatabaseScopedTabRef> tabs = PersonalDatabaseScreenTabHelper.hiddenTopTabs(screen);
        PersonalDatabaseLayout.Rect bodyRect = dropdownBodyRect(panelRect);
        PersonalDatabaseScreenListHelper.VisibleRange visibleRange = PersonalDatabaseScreenListHelper.visibleRange(
                tabs.size(),
                screen.moreTabsScrollIndex,
                PersonalDatabaseScreenListHelper.maxVisibleRows(bodyRect, PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT)
        );
        screen.moreTabsScrollIndex = visibleRange.scrollIndex();
        PersonalDatabaseScreenListHelper.enableScissor(guiGraphics, bodyRect);
        for (int index = visibleRange.fromIndex(); index < visibleRange.toIndex(); index++) {
            DatabaseScopedTabRef scopedTab = tabs.get(index);
            DatabaseTab tab = PersonalDatabaseScreenCommonHelper.findTab(screen, scopedTab);
            int rowIndex = index - visibleRange.fromIndex();
            PersonalDatabaseLayout.Rect rowRect = new PersonalDatabaseLayout.Rect(
                    bodyRect.x(),
                    bodyRect.y() + rowIndex * PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT,
                    bodyRect.width(),
                    PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT - 1
            );
            boolean hovered = rowRect.contains(mouseX, mouseY);
            boolean selected = screen.databaseMenu.viewState().query().focusedTab().equals(scopedTab);
            VanillaWidgetRenderer.renderOverlayRow(guiGraphics, rowRect, hovered, selected);
            guiGraphics.renderItem(PersonalDatabaseScreenCommonHelper.tabIcon(screen, tab), rowRect.x() + 3, rowRect.y() + 2);
            guiGraphics.drawString(
                    screen.screenFont(),
                    PersonalDatabaseScreenCommonHelper.truncateToWidth(
                            screen,
                            PersonalDatabaseScreenCommonHelper.scopedTabLabel(screen, scopedTab).getString(),
                            Math.max(0, rowRect.width() - 28)
                    ),
                    rowRect.x() + 24,
                    rowRect.y() + 6,
                    selected ? PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR : PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                    false
            );
        }
        guiGraphics.disableScissor();
        PersonalDatabaseScreenListHelper.renderScrollIndicators(screen, guiGraphics, bodyRect, visibleRange);
        guiGraphics.pose().popPose();
    }

    static boolean handleMoreTabsClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        if (!screen.moreTabsExpanded) {
            return false;
        }
        if (PersonalDatabaseScreenTabHelper.moreTabsButtonRect(screen).contains(mouseX, mouseY)) {
            screen.moreTabsExpanded = false;
            return true;
        }
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenGeometry.moreTabsDropdownRect(screen);
        if (!panelRect.contains(mouseX, mouseY)) {
            screen.moreTabsExpanded = false;
            return true;
        }
        List<DatabaseScopedTabRef> tabs = PersonalDatabaseScreenTabHelper.hiddenTopTabs(screen);
        PersonalDatabaseLayout.Rect bodyRect = dropdownBodyRect(panelRect);
        PersonalDatabaseScreenListHelper.VisibleRange visibleRange = PersonalDatabaseScreenListHelper.visibleRange(
                tabs.size(),
                screen.moreTabsScrollIndex,
                PersonalDatabaseScreenListHelper.maxVisibleRows(bodyRect, PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT)
        );
        screen.moreTabsScrollIndex = visibleRange.scrollIndex();
        for (int index = visibleRange.fromIndex(); index < visibleRange.toIndex(); index++) {
            int rowIndex = index - visibleRange.fromIndex();
            PersonalDatabaseLayout.Rect rowRect = new PersonalDatabaseLayout.Rect(
                    bodyRect.x(),
                    bodyRect.y() + rowIndex * PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT,
                    bodyRect.width(),
                    PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT - 1
            );
            if (!rowRect.contains(mouseX, mouseY)) {
                continue;
            }
            screen.moreTabsExpanded = false;
            PersonalDatabaseScreenCommonHelper.playButtonClickSound(screen);
            PersonalDatabaseScreenTopTabPromptHelper.handleTopTabSelection(screen, tabs.get(index));
            return true;
        }
        return true;
    }

    static boolean scrollMoreTabsDropdown(PersonalDatabaseScreen screen, double mouseX, double mouseY, int deltaRows) {
        if (!screen.moreTabsExpanded || deltaRows == 0) {
            return false;
        }
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenGeometry.moreTabsDropdownRect(screen);
        PersonalDatabaseLayout.Rect bodyRect = dropdownBodyRect(panelRect);
        if (!bodyRect.contains(mouseX, mouseY)) {
            return false;
        }
        List<DatabaseScopedTabRef> tabs = PersonalDatabaseScreenTabHelper.hiddenTopTabs(screen);
        PersonalDatabaseScreenListHelper.VisibleRange visibleRange = PersonalDatabaseScreenListHelper.visibleRange(
                tabs.size(),
                screen.moreTabsScrollIndex,
                PersonalDatabaseScreenListHelper.maxVisibleRows(bodyRect, PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT)
        );
        int nextScrollIndex = PersonalDatabaseScreenListHelper.clampScrollIndex(visibleRange, deltaRows);
        if (nextScrollIndex == screen.moreTabsScrollIndex) {
            return false;
        }
        screen.moreTabsScrollIndex = nextScrollIndex;
        return true;
    }

    private static PersonalDatabaseLayout.Rect dropdownBodyRect(PersonalDatabaseLayout.Rect panelRect) {
        return new PersonalDatabaseLayout.Rect(
                panelRect.x() + 2,
                panelRect.y() + 2,
                Math.max(0, panelRect.width() - 4),
                Math.max(0, panelRect.height() - 4)
        );
    }
}
