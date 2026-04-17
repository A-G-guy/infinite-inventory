package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class PersonalDatabaseScreenViewSelectorHelper {
    private static final int VIEW_PREVIEW_GAP = 6;

    private PersonalDatabaseScreenViewSelectorHelper() {
    }

    static void renderViewSelector(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenGeometry.viewSelectorRect(screen);
        if (panelRect.width() <= 0 || panelRect.height() <= 0) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 252.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        guiGraphics.drawString(
                screen.screenFont(),
                Component.translatable("screen.infiniteinventory.visible_tabs_title"),
                panelRect.x() + 8,
                panelRect.y() + 8,
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                false
        );
        guiGraphics.fill(
                panelRect.x() + 8,
                panelRect.y() + 8 + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT,
                panelRect.right() - 8,
                panelRect.y() + 9 + PersonalDatabaseScreen.OVERLAY_SECTION_TITLE_HEIGHT,
                0x70A89E8C
        );
        PersonalDatabaseScreenOverlayRenderHelper.renderOverlayCloseButton(screen, guiGraphics, panelRect, mouseX, mouseY);

        renderViewSelectorPreview(screen, guiGraphics, mouseX, mouseY);
        renderViewSelectorColumn(screen, guiGraphics, mouseX, mouseY, true);
        renderViewSelectorColumn(screen, guiGraphics, mouseX, mouseY, false);
        guiGraphics.pose().popPose();
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
        for (int index = 0; index < tabs.size(); index++) {
            DatabaseScopedTabRef scopedTab = tabs.get(index);
            DatabaseTab tab = PersonalDatabaseScreenCommonHelper.findTab(screen, scopedTab);
            PersonalDatabaseLayout.Rect rowRect = new PersonalDatabaseLayout.Rect(
                    panelRect.x() + 2,
                    panelRect.y() + 2 + index * PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT,
                    panelRect.width() - 4,
                    PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT - 1
            );
            boolean hovered = rowRect.contains(mouseX, mouseY);
            boolean selected = screen.databaseMenu.viewState().query().focusedTab().equals(scopedTab);
            VanillaWidgetRenderer.renderOverlayRow(guiGraphics, rowRect, hovered, selected);
            guiGraphics.renderItem(PersonalDatabaseScreenCommonHelper.tabIcon(screen, tab), rowRect.x() + 3, rowRect.y() + 2);
            guiGraphics.drawString(
                    screen.screenFont(),
                    PersonalDatabaseScreenGeometry.truncateToWidth(
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
        guiGraphics.pose().popPose();
    }

    static boolean handleViewSelectorClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        if (!screen.viewSelectorExpanded) {
            return false;
        }
        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenGeometry.viewSelectorRect(screen);
        if (PersonalDatabaseScreenOverlayRenderHelper.isOverlayCloseClicked(panelRect, mouseX, mouseY)) {
            screen.viewSelectorExpanded = false;
            return true;
        }
        if (screen.layout != null && screen.layout.viewSelectorButtonRect().contains(mouseX, mouseY)) {
            screen.viewSelectorExpanded = false;
            return true;
        }
        if (!panelRect.contains(mouseX, mouseY)) {
            screen.viewSelectorExpanded = false;
            return true;
        }
        if (handleViewSelectorColumnClick(screen, mouseX, mouseY, true)) {
            return true;
        }
        if (handleViewSelectorColumnClick(screen, mouseX, mouseY, false)) {
            return true;
        }
        return true;
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
        for (int index = 0; index < tabs.size(); index++) {
            PersonalDatabaseLayout.Rect rowRect = new PersonalDatabaseLayout.Rect(
                    panelRect.x() + 2,
                    panelRect.y() + 2 + index * PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT,
                    panelRect.width() - 4,
                    PersonalDatabaseScreen.TAB_SELECTOR_ROW_HEIGHT - 1
            );
            if (!rowRect.contains(mouseX, mouseY)) {
                continue;
            }
            screen.moreTabsExpanded = false;
            PersonalDatabaseScreenTopTabPromptHelper.handleTopTabSelection(screen, tabs.get(index));
            return true;
        }
        return true;
    }

    private static void renderViewSelectorPreview(
            PersonalDatabaseScreen screen,
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY
    ) {
        PersonalDatabaseLayout.Rect previewRect = PersonalDatabaseScreenGeometry.viewSelectorPreviewRect(screen);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, previewRect);
        guiGraphics.drawString(
                screen.screenFont(),
                Component.translatable("screen.infiniteinventory.view_selector.preview"),
                previewRect.x() + 6,
                previewRect.y() + 6,
                PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR,
                false
        );
        List<DatabaseScopedTabRef> visibleTabs = screen.databaseMenu.viewState().query().visibleTabs();
        List<PersonalDatabaseLayout.Rect> previewPanels = previewPanelRects(previewRect, Math.max(1, visibleTabs.size()));
        for (int index = 0; index < previewPanels.size(); index++) {
            PersonalDatabaseLayout.Rect panelRect = previewPanels.get(index);
            boolean filled = index < visibleTabs.size();
            VanillaWidgetRenderer.renderOverlayRow(guiGraphics, panelRect, panelRect.contains(mouseX, mouseY), filled);
            if (!filled) {
                PersonalDatabaseScreenCommonHelper.drawCenteredShadow(
                        screen,
                        guiGraphics,
                        Component.literal("+"),
                        panelRect.x(),
                        panelRect.right(),
                        panelRect.y() + Math.max(6, panelRect.height() / 2 - 4),
                        PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR
                );
                continue;
            }
            DatabaseScopedTabRef scopedTab = visibleTabs.get(index);
            DatabaseTab tab = PersonalDatabaseScreenCommonHelper.findTab(screen, scopedTab);
            guiGraphics.renderItem(PersonalDatabaseScreenCommonHelper.tabIcon(screen, tab), panelRect.x() + 6, panelRect.y() + 6);
            guiGraphics.drawString(
                    screen.screenFont(),
                    Integer.toString(index + 1),
                    panelRect.right() - 12,
                    panelRect.y() + 6,
                    PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR,
                    false
            );
            guiGraphics.drawString(
                    screen.screenFont(),
                    PersonalDatabaseScreenGeometry.truncateToWidth(
                            screen,
                            PersonalDatabaseScreenCommonHelper.scopedTabLabel(screen, scopedTab).getString(),
                            Math.max(0, panelRect.width() - 18)
                    ),
                    panelRect.x() + 6,
                    panelRect.y() + 28,
                    PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                    false
            );
            guiGraphics.drawString(
                    screen.screenFont(),
                    PersonalDatabaseScreenCommonHelper.scopeLabel(scopedTab.scope()),
                    panelRect.x() + 6,
                    panelRect.bottom() - 14,
                    PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR,
                    false
            );
        }
    }

    private static void renderViewSelectorColumn(
            PersonalDatabaseScreen screen,
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            boolean personalColumn
    ) {
        DatabaseScope scope = personalColumn ? DatabaseScope.PERSONAL : DatabaseScope.PUBLIC;
        PersonalDatabaseLayout.Rect headerRect = PersonalDatabaseScreenGeometry.viewSelectorScopeHeaderRect(screen, personalColumn);
        guiGraphics.drawString(
                screen.screenFont(),
                PersonalDatabaseScreenCommonHelper.scopeLabel(scope),
                headerRect.x(),
                headerRect.y() + 6,
                PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR,
                false
        );
        List<DatabaseTab> tabs = PersonalDatabaseScreenCommonHelper.tabsForScope(screen, scope);
        DatabaseQuery query = screen.databaseMenu.viewState().query();
        for (int index = 0; index < tabs.size(); index++) {
            DatabaseTab tab = tabs.get(index);
            DatabaseScopedTabRef scopedTab = DatabaseScopedTabRef.concreteTab(scope, tab.id());
            PersonalDatabaseLayout.Rect rowRect = PersonalDatabaseScreenGeometry.viewSelectorScopeRowRect(screen, personalColumn, index);
            boolean selected = query.visibleTabs().contains(scopedTab);
            boolean enabled = selected || query.visibleTabs().size() < DatabaseTabs.MAX_VISIBLE_TAB_COUNT;
            boolean hovered = rowRect.contains(mouseX, mouseY);
            VanillaWidgetRenderer.renderOverlayRow(guiGraphics, rowRect, hovered, selected);
            guiGraphics.renderItem(PersonalDatabaseScreenCommonHelper.tabIcon(screen, tab), rowRect.x() + 3, rowRect.y() + 2);
            guiGraphics.drawString(
                    screen.screenFont(),
                    PersonalDatabaseScreenGeometry.truncateToWidth(
                            screen,
                            PersonalDatabaseScreenCommonHelper.scopedTabLabel(screen, scopedTab).getString(),
                            Math.max(0, rowRect.width() - 52)
                    ),
                    rowRect.x() + 24,
                    rowRect.y() + 6,
                    query.focusedTab().equals(scopedTab)
                            ? PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR
                            : PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                    false
            );
            Component statusLabel = selected
                    ? Component.literal(Integer.toString(query.visibleTabs().indexOf(scopedTab) + 1))
                    : Component.literal("□");
            PersonalDatabaseScreenCommonHelper.drawCenteredShadow(
                    screen,
                    guiGraphics,
                    statusLabel,
                    rowRect.right() - 26,
                    rowRect.right() - 4,
                    rowRect.y() + 6,
                    enabled ? PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR : PersonalDatabaseScreen.OVERLAY_MUTED_TEXT_COLOR
            );
        }
    }

    private static boolean handleViewSelectorColumnClick(
            PersonalDatabaseScreen screen,
            double mouseX,
            double mouseY,
            boolean personalColumn
    ) {
        DatabaseScope scope = personalColumn ? DatabaseScope.PERSONAL : DatabaseScope.PUBLIC;
        List<DatabaseTab> tabs = PersonalDatabaseScreenCommonHelper.tabsForScope(screen, scope);
        for (int index = 0; index < tabs.size(); index++) {
            PersonalDatabaseLayout.Rect rowRect = PersonalDatabaseScreenGeometry.viewSelectorScopeRowRect(screen, personalColumn, index);
            if (!rowRect.contains(mouseX, mouseY)) {
                continue;
            }
            toggleViewSelection(screen, DatabaseScopedTabRef.concreteTab(scope, tabs.get(index).id()));
            return true;
        }
        return false;
    }

    private static void toggleViewSelection(PersonalDatabaseScreen screen, DatabaseScopedTabRef scopedTab) {
        DatabaseQuery query = screen.databaseMenu.viewState().query();
        if (query.visibleTabs().contains(scopedTab)) {
            if (query.visibleTabs().size() <= 1) {
                return;
            }
            List<DatabaseScopedTabRef> nextVisibleTabs = query.visibleTabs().stream()
                    .filter(existingTab -> !existingTab.equals(scopedTab))
                    .toList();
            PersonalDatabaseScreenLayoutHelper.sendQuery(screen, query.withVisibleTabs(nextVisibleTabs));
            return;
        }
        if (query.visibleTabs().size() >= DatabaseTabs.MAX_VISIBLE_TAB_COUNT) {
            return;
        }
        LinkedHashSet<DatabaseScopedTabRef> nextVisibleTabs = new LinkedHashSet<>(query.visibleTabs());
        nextVisibleTabs.add(scopedTab);
        PersonalDatabaseScreenLayoutHelper.sendQuery(
                screen,
                query.withVisibleTabs(new ArrayList<>(nextVisibleTabs)).withFocusedTab(scopedTab)
        );
    }

    private static List<PersonalDatabaseLayout.Rect> previewPanelRects(PersonalDatabaseLayout.Rect previewRect, int visiblePanelCount) {
        PersonalDatabaseLayout.Rect canvas = new PersonalDatabaseLayout.Rect(
                previewRect.x() + 6,
                previewRect.y() + 24,
                previewRect.width() - 12,
                previewRect.height() - 30
        );
        int panelCount = Math.max(1, Math.min(DatabaseTabs.MAX_VISIBLE_TAB_COUNT, visiblePanelCount));
        return switch (panelCount) {
            case 1 -> List.of(canvas);
            case 2 -> previewTwoPanels(canvas);
            case 3 -> previewThreePanels(canvas);
            case 4 -> previewFourPanels(canvas);
            default -> List.of(canvas);
        };
    }

    private static List<PersonalDatabaseLayout.Rect> previewTwoPanels(PersonalDatabaseLayout.Rect canvas) {
        int width = Math.max(1, (canvas.width() - VIEW_PREVIEW_GAP) / 2);
        PersonalDatabaseLayout.Rect left = new PersonalDatabaseLayout.Rect(canvas.x(), canvas.y(), width, canvas.height());
        PersonalDatabaseLayout.Rect right = new PersonalDatabaseLayout.Rect(
                left.right() + VIEW_PREVIEW_GAP,
                canvas.y(),
                canvas.right() - left.right() - VIEW_PREVIEW_GAP,
                canvas.height()
        );
        return List.of(left, right);
    }

    private static List<PersonalDatabaseLayout.Rect> previewThreePanels(PersonalDatabaseLayout.Rect canvas) {
        int leftWidth = Math.max(1, (canvas.width() - VIEW_PREVIEW_GAP) * 3 / 5);
        int rightWidth = Math.max(1, canvas.width() - leftWidth - VIEW_PREVIEW_GAP);
        int topHeight = Math.max(1, (canvas.height() - VIEW_PREVIEW_GAP) / 2);
        PersonalDatabaseLayout.Rect left = new PersonalDatabaseLayout.Rect(canvas.x(), canvas.y(), leftWidth, canvas.height());
        PersonalDatabaseLayout.Rect topRight = new PersonalDatabaseLayout.Rect(
                left.right() + VIEW_PREVIEW_GAP,
                canvas.y(),
                rightWidth,
                topHeight
        );
        PersonalDatabaseLayout.Rect bottomRight = new PersonalDatabaseLayout.Rect(
                topRight.x(),
                topRight.bottom() + VIEW_PREVIEW_GAP,
                rightWidth,
                canvas.bottom() - topRight.bottom() - VIEW_PREVIEW_GAP
        );
        return List.of(left, topRight, bottomRight);
    }

    private static List<PersonalDatabaseLayout.Rect> previewFourPanels(PersonalDatabaseLayout.Rect canvas) {
        int width = Math.max(1, (canvas.width() - VIEW_PREVIEW_GAP) / 2);
        int height = Math.max(1, (canvas.height() - VIEW_PREVIEW_GAP) / 2);
        PersonalDatabaseLayout.Rect topLeft = new PersonalDatabaseLayout.Rect(canvas.x(), canvas.y(), width, height);
        PersonalDatabaseLayout.Rect topRight = new PersonalDatabaseLayout.Rect(
                topLeft.right() + VIEW_PREVIEW_GAP,
                canvas.y(),
                canvas.right() - topLeft.right() - VIEW_PREVIEW_GAP,
                height
        );
        PersonalDatabaseLayout.Rect bottomLeft = new PersonalDatabaseLayout.Rect(
                canvas.x(),
                topLeft.bottom() + VIEW_PREVIEW_GAP,
                width,
                canvas.bottom() - topLeft.bottom() - VIEW_PREVIEW_GAP
        );
        PersonalDatabaseLayout.Rect bottomRight = new PersonalDatabaseLayout.Rect(
                topRight.x(),
                bottomLeft.y(),
                topRight.width(),
                bottomLeft.height()
        );
        return List.of(topLeft, topRight, bottomLeft, bottomRight);
    }
}
