package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.database.DatabaseScope;

/**
 * 屏幕生命周期辅助类，负责 init() 与 containerTick() 中的逻辑调度。
 *
 * <p>设计决策：将生命周期逻辑从屏幕主类剥离，避免其过度膨胀。
 */
final class PersonalDatabaseScreenLifecycleHelper {
    private PersonalDatabaseScreenLifecycleHelper() {
    }

    /**
     * 初始化屏幕尺寸与所有 UI 控件，在屏幕首次显示或尺寸变化时调用。
     *
     * <p>业务约束：
     * <ul>
     *   <li>若当前设备无饰品槽位，强制收起饰品面板并重置滚动位置
     *   <li>所有展开状态在初始化时重置为收起，防止跨会话状态泄漏
     *   <li>搜索同步冷却与待处理搜索文本清空，确保新会话从干净状态开始
     * </ul>
     */
    static void initScreen(PersonalDatabaseScreen screen) {
        PersonalDatabaseScreenLayoutHelper.rebuildLayout(screen);
        screen.pendingLayoutQuery = null;
        screen.sortDropdownExpanded = false;
        screen.pagePickerExpanded = false;
        screen.advancedSearchExpanded = false;
        screen.enhancementPanelExpanded = false;
        screen.viewSelectorExpanded = false;
        screen.moreTabsExpanded = false;
        screen.topTabActionPromptExpanded = false;
        screen.topTabReplaceExpanded = false;
        screen.targetSelectorExpanded = false;
        screen.tabManagementExpanded = false;
        screen.iconPickerExpanded = false;
        screen.customExtractOverlayExpanded = false;
        screen.noteOverlayExpanded = false;
        screen.depositConflictExpanded = false;
        screen.pendingDepositConflict = null;
        screen.customExtractValidationKey = "";
        screen.logPanelExpanded = false;
        screen.logPanelScrollIndex = 0;
        screen.statisticsPanelExpanded = false;
        screen.activeStatisticsTab = PersonalDatabaseScreenEnums.StatisticsPanelTab.OVERVIEW;
        screen.statisticsPanelScope = DatabaseScope.PERSONAL;
        screen.statisticsCategoryScrollIndex = 0;
        screen.statisticsModsScrollIndex = 0;
        screen.statisticsTabsScrollIndex = 0;
        screen.statisticsTrendsScrollIndex = 0;
        screen.statisticsLogScrollIndex = 0;
        screen.viewSelectorPersonalScrollIndex = 0;
        screen.viewSelectorPublicScrollIndex = 0;
        screen.managementPersonalScrollIndex = 0;
        screen.managementPublicScrollIndex = 0;
        screen.moreTabsScrollIndex = 0;
        screen.topTabReplaceScrollIndex = 0;
        screen.advancedSearchScrollIndex = 0;
        screen.enhancementScrollIndex = 0;
        screen.topTabScopeFilter = screen.databaseMenu.viewState().query().focusedTab().scope();
        screen.selectionGestureModel.clearSelectionGesture();
        screen.selectionGestureModel.releaseDiscardKey();
        screen.searchSyncCooldownTicks = 0;
        screen.activeSearchTab = null;
        screen.pendingSearchTexts.clear();
        screen.dispatchedSearchTexts.clear();
        PersonalDatabaseScreenTabHelper.closeTabContextMenu(screen);
        PersonalDatabaseScreenContextHelper.closeContextMenu(screen);
        PersonalDatabaseScreenWidgetHelper.buildWidgets(screen);
        PersonalDatabaseScreenWidgetHelper.syncWidgetsFromState(screen);
        PersonalDatabaseScreenLayoutHelper.ensureLayoutQuerySynced(screen);
    }

    /**
     * 每 tick 更新屏幕状态，负责布局刷新、控件同步、搜索防抖与覆盖层校验。
     */
    static void tickScreen(PersonalDatabaseScreen screen) {
        PersonalDatabaseScreenLayoutHelper.refreshUiStructureIfNeeded(screen);
        PersonalDatabaseScreenWidgetHelper.syncWidgetsFromState(screen);
        PersonalDatabaseScreenLayoutHelper.tickSearchSync(screen);
        PersonalDatabaseScreenCustomExtractOverlayHelper.validateOverlay(screen);
        PersonalDatabaseScreenNoteOverlayHelper.validateOverlay(screen);
        PersonalDatabaseScreenDepositConflictHelper.tryOpenDepositConflict(screen);
        PersonalDatabaseScreenLayoutHelper.ensureLayoutQuerySynced(screen);
    }
}
