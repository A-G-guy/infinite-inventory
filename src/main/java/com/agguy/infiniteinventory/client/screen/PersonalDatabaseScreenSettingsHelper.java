package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 设置面板渲染与交互辅助类。
 *
 * <p>职责：绘制设置面板框架（左侧导航栏 + 右侧内容区），处理导航切换与面板开关。
 */
final class PersonalDatabaseScreenSettingsHelper {
    private static final int NAV_BACKGROUND_COLOR = 0xFFE8E0D0;
    private static final int NAV_ACTIVE_BACKGROUND = 0xFFD6CBB8;
    private static final int NAV_HOVER_BACKGROUND = 0xFFF0E8D8;
    private static final int NAV_ACTIVE_INDICATOR_COLOR = 0xFFF4D58A;
    private static final int DIVIDER_COLOR = 0x80A89E8C;
    private static final int TITLE_DIVIDER_COLOR = 0x70A89E8C;

    private PersonalDatabaseScreenSettingsHelper() {
    }

    /**
     * 渲染设置面板整体框架。
     */
    static void renderSettingsPanel(PersonalDatabaseScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (screen.layout == null) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 230.0F);

        PersonalDatabaseLayout.Rect panelRect = PersonalDatabaseScreenSettingsGeometry.settingsPanelRect(screen);
        PersonalDatabaseLayout.Rect navRect = PersonalDatabaseScreenSettingsGeometry.settingsNavRect(screen);
        PersonalDatabaseLayout.Rect contentRect = PersonalDatabaseScreenSettingsGeometry.settingsContentRect(screen);

        // 整体背景
        VanillaWidgetRenderer.renderPanel(guiGraphics, panelRect);

        // 导航栏背景
        guiGraphics.fill(navRect.x(), navRect.y(), navRect.right(), navRect.bottom(), NAV_BACKGROUND_COLOR);

        // 导航栏与内容区分隔线
        guiGraphics.fill(navRect.right(), navRect.y(), navRect.right() + 1, navRect.bottom(), DIVIDER_COLOR);

        // 渲染导航栏
        renderNavBar(screen, guiGraphics, navRect, mouseX, mouseY);

        guiGraphics.pose().popPose();
    }

    /**
     * 处理设置面板点击事件。
     *
     * @return 如果点击被处理（导航项切换），返回 true
     */
    static boolean handleSettingsPanelClick(PersonalDatabaseScreen screen, double mouseX, double mouseY) {
        int clickedIndex = PersonalDatabaseScreenSettingsGeometry.clickedNavItemIndex(screen, mouseX, mouseY);
        if (clickedIndex >= 0) {
            PersonalDatabaseScreen.SettingsPanelTab[] tabs = PersonalDatabaseScreen.SettingsPanelTab.values();
            if (clickedIndex < tabs.length) {
                PersonalDatabaseScreen.SettingsPanelTab nextTab = tabs[clickedIndex];
                if (screen.activeSettingsTab != nextTab) {
                    screen.activeSettingsTab = nextTab;
                    syncSettingsSubPanelStates(screen);
                    PersonalDatabaseScreenCommonHelper.playButtonClickSound(screen);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 打开设置面板并初始化状态。
     */
    static void openSettingsPanel(PersonalDatabaseScreen screen, PersonalDatabaseScreen.SettingsPanelTab initialTab) {
        PersonalDatabaseScreenTargetHelper.closeTransientOverlays(screen);
        screen.settingsPanelExpanded = true;
        screen.activeSettingsTab = initialTab;
        syncSettingsSubPanelStates(screen);
    }

    /**
     * 关闭设置面板并清理所有子面板状态。
     */
    static void closeSettingsPanel(PersonalDatabaseScreen screen) {
        screen.settingsPanelExpanded = false;
        screen.advancedSearchExpanded = false;
        screen.enhancementPanelExpanded = false;
        screen.viewSelectorExpanded = false;
        screen.tabManagementExpanded = false;
        screen.logPanelExpanded = false;
        PersonalDatabaseScreenManagementHelper.closeTabManagementOverlays(screen);
    }

    /**
     * 根据当前激活的设置标签同步各子面板的展开状态。
     */
    static void syncSettingsSubPanelStates(PersonalDatabaseScreen screen) {
        screen.advancedSearchExpanded = false;
        screen.enhancementPanelExpanded = false;
        screen.viewSelectorExpanded = false;
        screen.tabManagementExpanded = false;
        screen.logPanelExpanded = false;

        switch (screen.activeSettingsTab) {
            case ADVANCED_SEARCH -> screen.advancedSearchExpanded = true;
            case ENHANCEMENT -> screen.enhancementPanelExpanded = true;
            case VIEW_SELECTOR -> {
                screen.viewSelectorExpanded = true;
                screen.viewSelectorPersonalScrollIndex = 0;
                screen.viewSelectorPublicScrollIndex = 0;
            }
            case MANAGEMENT -> {
                screen.tabManagementExpanded = true;
                screen.managementPersonalScrollIndex = 0;
                screen.managementPublicScrollIndex = 0;
                PersonalDatabaseScreenManagementHelper.ensureManagementWidgets(screen);
                PersonalDatabaseScreenManagementHelper.loadManagementDrafts(
                        screen,
                        PersonalDatabaseScreenManagementHelper.preferredManagementTab(screen)
                );
            }
            case LOG -> {
                screen.logPanelExpanded = true;
                screen.logPanelScope = screen.databaseMenu.viewState().query().focusedTab().scope();
                screen.logPanelScrollIndex = 0;
                PacketDistributor.sendToServer(
                        new com.agguy.infiniteinventory.network.DatabaseLogRequestPayload(screen.logPanelScope));
            }
        }
    }

    private static void renderNavBar(PersonalDatabaseScreen screen, GuiGraphics guiGraphics,
            PersonalDatabaseLayout.Rect navRect, int mouseX, int mouseY) {
        // 标题
        PersonalDatabaseLayout.Rect titleRect = PersonalDatabaseScreenSettingsGeometry.settingsNavTitleRect(navRect);
        guiGraphics.drawString(
                screen.screenFont(),
                Component.translatable("screen.infiniteinventory.settings_title"),
                titleRect.x(),
                titleRect.y() + 5,
                PersonalDatabaseScreen.OVERLAY_TEXT_COLOR,
                false
        );

        // 标题下分割线
        guiGraphics.fill(
                titleRect.x(),
                titleRect.bottom() + 2,
                titleRect.right(),
                titleRect.bottom() + 3,
                TITLE_DIVIDER_COLOR
        );

        // 导航项
        PersonalDatabaseScreen.SettingsPanelTab[] tabs = PersonalDatabaseScreen.SettingsPanelTab.values();
        for (int i = 0; i < tabs.length; i++) {
            PersonalDatabaseLayout.Rect itemRect = PersonalDatabaseScreenSettingsGeometry.settingsNavItemRect(screen, i);
            if (itemRect.y() >= navRect.bottom() - PersonalDatabaseScreen.SETTINGS_NAV_PADDING) {
                break;
            }
            boolean isActive = screen.activeSettingsTab == tabs[i];
            boolean isHovered = itemRect.contains(mouseX, mouseY);

            // 背景
            if (isActive) {
                guiGraphics.fill(itemRect.x(), itemRect.y(), itemRect.right(), itemRect.bottom(), NAV_ACTIVE_BACKGROUND);
                // 左侧金色指示条
                guiGraphics.fill(itemRect.x(), itemRect.y(), itemRect.x() + 3, itemRect.bottom(), NAV_ACTIVE_INDICATOR_COLOR);
            } else if (isHovered) {
                guiGraphics.fill(itemRect.x(), itemRect.y(), itemRect.right(), itemRect.bottom(), NAV_HOVER_BACKGROUND);
            }

            // 文本
            int textColor = isActive ? PersonalDatabaseScreen.OVERLAY_ACCENT_TEXT_COLOR : PersonalDatabaseScreen.OVERLAY_TEXT_COLOR;
            guiGraphics.drawString(
                    screen.screenFont(),
                    Component.translatable(tabs[i].translationKey()),
                    itemRect.x() + 8,
                    itemRect.y() + (itemRect.height() - screen.screenFont().lineHeight) / 2 + 1,
                    textColor,
                    false
            );
        }
    }
}
