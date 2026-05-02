package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.compat.PlayerInventoryPaneProvider;
import com.agguy.infiniteinventory.compat.VanillaPlayerInventoryPaneProvider;
import com.agguy.infiniteinventory.database.DatabaseCategory;
import com.agguy.infiniteinventory.database.DatabaseSelectionEntry;
import com.agguy.infiniteinventory.database.DatabaseEnhancementOption;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseViewState;
import com.agguy.infiniteinventory.database.DatabaseSearchField;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import com.agguy.infiniteinventory.menu.PersonalDatabaseMenu;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 个人数据库客户端渲染与交互屏幕，负责数据库 UI 的整体生命周期管理。
 *
 * <p>职责边界：响应式布局、覆盖层渲染、输入事件、自定义工具提示。
 * <p>设计决策：渲染与交互逻辑委托给专门的 Helper 类，本类仅维护 UI 状态机与事件路由。
 */
public final class PersonalDatabaseScreen extends AbstractContainerScreen<PersonalDatabaseMenu> {
    static final int OVERLAY_TEXT_COLOR = GuiTheme.OVERLAY_TEXT;
    static final int OVERLAY_MUTED_TEXT_COLOR = GuiTheme.OVERLAY_MUTED_TEXT;
    static final int OVERLAY_ACCENT_TEXT_COLOR = GuiTheme.OVERLAY_ACCENT_TEXT;
    static final int FRAME_TEXT_COLOR = GuiTheme.FRAME_TEXT;
    static final int FRAME_MUTED_TEXT_COLOR = GuiTheme.FRAME_MUTED_TEXT;
    static final int FRAME_ACCENT_TEXT_COLOR = GuiTheme.FRAME_ACCENT_TEXT;
    static final int FRAME_TEXT_BACKDROP_COLOR = GuiTheme.FRAME_TEXT_BACKDROP;
    static final int FRAME_TEXT_OUTLINE_COLOR = GuiTheme.FRAME_TEXT_OUTLINE;
    static final int TEXT_FIELD_TEXT_COLOR = GuiTheme.OVERLAY_TEXT;
    static final int TEXT_FIELD_MUTED_TEXT_COLOR = GuiTheme.OVERLAY_MUTED_TEXT;
    static final int SORT_BUTTON_TEXT_COLOR = GuiTheme.OVERLAY_TEXT;
    static final int SCROLLBAR_TRACK_COLOR = GuiTheme.SCROLLBAR_TRACK;
    static final int SCROLLBAR_THUMB_COLOR = GuiTheme.SCROLLBAR_THUMB;
    static final int SCROLLBAR_THUMB_HOVERED_COLOR = GuiTheme.SCROLLBAR_THUMB_HOVERED;
    static final int TOP_TAB_ACTIVE_TEXT_COLOR = GuiTheme.TOP_TAB_ACTIVE_TEXT;
    static final int TOP_TAB_INACTIVE_TEXT_COLOR = GuiTheme.TOP_TAB_INACTIVE_TEXT;
    static final int DROPDOWN_ROW_HEIGHT = 20;
    static final int SORT_DROPDOWN_MIN_WIDTH = 168;
    static final int SORT_DROPDOWN_SECTION_GAP = 6;
    static final int SORT_DIRECTION_BUTTON_GAP = 4;
    static final int CONTEXT_MENU_MIN_WIDTH = 112;
    static final int CONTEXT_MENU_ROW_HEIGHT = 20;
    static final int CONTEXT_MENU_MARGIN = 4;
    static final int PAGE_PICKER_MIN_WIDTH = 88;
    static final int PAGE_PICKER_ROW_HEIGHT = 20;
    static final int SEARCH_ICON_SIZE = 9;
    static final int TEXT_FIELD_LEFT_PADDING = 6, TEXT_FIELD_RIGHT_PADDING = 6;
    static final int SEARCH_TEXT_LEFT_PADDING = 22;
    static final int ADVANCED_SEARCH_PANEL_WIDTH = 236;
    static final int ADVANCED_SEARCH_PANEL_PADDING = 6;
    static final int ADVANCED_SEARCH_TITLE_HEIGHT = 22;
    static final int ADVANCED_SEARCH_ROW_HEIGHT = 20;
    static final int ADVANCED_SEARCH_ROW_GAP = 2;
    static final int ADVANCED_SEARCH_TOGGLE_WIDTH = 24;
    static final int ADVANCED_SEARCH_WEIGHT_WIDTH = 40;
    static final int ENHANCEMENT_PANEL_WIDTH = 236;
    static final int ENHANCEMENT_PANEL_PADDING = 6;
    static final int ENHANCEMENT_TITLE_HEIGHT = 22;
    static final int ENHANCEMENT_ROW_HEIGHT = 20;
    static final int ENHANCEMENT_ROW_GAP = 2;
    static final int ENHANCEMENT_TOGGLE_WIDTH = 24;
    static final int TAB_SELECTOR_WIDTH = 520;
    static final int TAB_SELECTOR_ROW_HEIGHT = 20;
    static final int MORE_TABS_WIDTH = 180;
    static final int TARGET_SELECTOR_WIDTH = 280;
    static final int TARGET_SELECTOR_ROW_HEIGHT = 20;
    static final int MANAGEMENT_PANEL_WIDTH = 520;
    static final int MANAGEMENT_PANEL_HEIGHT = 260;
    static final int TOP_TAB_ACTION_WIDTH = 300;
    static final int TOP_TAB_ACTION_ROW_HEIGHT = 20;
    static final int CUSTOM_EXTRACT_PANEL_WIDTH = 236;
    static final int CUSTOM_EXTRACT_PANEL_HEIGHT = 134;
    static final int MANAGEMENT_ROW_HEIGHT = 20;
    static final int MANAGEMENT_LIST_WIDTH = 124;
    static final int MANAGEMENT_BUTTON_WIDTH = 76;
    static final int ICON_PICKER_WIDTH = 900;
    static final int ICON_PICKER_HEIGHT = 620;
    static final int MANAGEMENT_PANEL_PADDING = 8;
    static final int OVERLAY_SECTION_TITLE_HEIGHT = 22;
    static final int INLINE_TAB_MIN_WIDTH = 88;
    static final int INLINE_TAB_MIN_WIDTH_WITH_MORE = 72;
    static final int INLINE_TAB_TIGHT_GAP = 2;
    static final int INLINE_TAB_MORE_WIDTH = 56;
    static final int STANDARD_ICON_PICKER_COLUMNS = 6;
    static final int COMPACT_ICON_PICKER_COLUMNS = 4;
    static final int ICON_PICKER_ROWS = 4;
    static final int ICON_PICKER_CELL_GAP = 6;
    static final int ICON_PICKER_CELL_MIN_HEIGHT = 42;
    static final int ICON_PICKER_CELL_MAX_HEIGHT = 62;
    static final int ICON_PICKER_LABEL_LINES = 2;
    static final int TAB_ICON_SIZE = 16;
    static final int TAB_ICON_LEFT_PADDING = 4;
    static final int TAB_TEXT_GAP = 3;
    static final int SETTINGS_NAV_WIDTH = 140;
    static final int SETTINGS_NAV_ITEM_HEIGHT = 28;
    static final int SETTINGS_NAV_PADDING = 8;
    static final int SETTINGS_CONTENT_PADDING = 12;

    final PlayerInventoryPaneProvider inventoryPaneProvider = new VanillaPlayerInventoryPaneProvider();
    final PersonalDatabaseMenu databaseMenu;
    final Component databaseScreenTitle;
    @Nullable
    PersonalDatabaseLayout layout;
    @Nullable
    DatabaseQuery pendingLayoutQuery;
    Button depositButton;
    Button depositExistingButton;
    Button settingsButton;
    Button viewSelectorButton;
    Button statisticsButton;
    Button personalScopeButton;
    Button publicScopeButton;
    Button accessoriesToggleButton;
    final List<EditBox> panelSearchBoxes = new ArrayList<>();
    final List<Button> panelSortButtons = new ArrayList<>();
    final List<Button> panelPreviousPageButtons = new ArrayList<>();
    final List<Button> panelPageButtons = new ArrayList<>();
    final List<Button> panelNextPageButtons = new ArrayList<>();
    @Nullable
    EditBox managementNameBox;
    @Nullable
    EditBox iconSearchBox;
    @Nullable
    EditBox customExtractAmountBox;
    @Nullable
    EditBox noteEditBox;
    final Map<DatabaseSearchField, Button> advancedSearchToggleButtons = new EnumMap<>(DatabaseSearchField.class);
    final Map<DatabaseSearchField, Button> advancedSearchWeightButtons = new EnumMap<>(DatabaseSearchField.class);
    final Map<DatabaseEnhancementOption, Button> enhancementToggleButtons = new EnumMap<>(DatabaseEnhancementOption.class);
    boolean syncingSearchBox;
    int searchSyncCooldownTicks;
    @Nullable
    DatabaseScopedTabRef activeSearchTab;
    final Map<DatabaseScopedTabRef, String> pendingSearchTexts = new LinkedHashMap<>();
    final Map<DatabaseScopedTabRef, String> dispatchedSearchTexts = new LinkedHashMap<>();
    boolean sortDropdownExpanded;
    boolean pagePickerExpanded;
    int activeSortPanelIndex = -1;
    int activePagePickerPanelIndex = -1;
    boolean advancedSearchExpanded;
    boolean enhancementPanelExpanded;
    boolean viewSelectorExpanded;
    boolean moreTabsExpanded;
    boolean topTabActionPromptExpanded;
    boolean topTabReplaceExpanded;
    boolean targetSelectorExpanded;
    boolean tabManagementExpanded;
    boolean iconPickerExpanded;
    boolean accessoriesExpanded;
    boolean contextMenuExpanded;
    boolean customExtractOverlayExpanded;
    boolean noteOverlayExpanded;
    boolean noteOverlayMixed;
    boolean logPanelExpanded;
    DatabaseScope logPanelScope = DatabaseScope.PERSONAL;
    int logPanelScrollIndex;
    boolean statisticsPanelExpanded;
    PersonalDatabaseScreenEnums.StatisticsPanelTab activeStatisticsTab = PersonalDatabaseScreenEnums.StatisticsPanelTab.OVERVIEW;
    DatabaseScope statisticsPanelScope = DatabaseScope.PERSONAL;
    int statisticsCategoryScrollIndex;
    int statisticsModsScrollIndex;
    int statisticsTabsScrollIndex;
    int statisticsTrendsScrollIndex;
    int statisticsLogScrollIndex;
    boolean settingsPanelExpanded;
    PersonalDatabaseScreenEnums.SettingsPanelTab activeSettingsTab = PersonalDatabaseScreenEnums.SettingsPanelTab.ADVANCED_SEARCH;
    boolean suppressVanillaTooltipRender;
    double lastMouseX;
    double lastMouseY;
    int accessoryScrollRow;
    int contextMenuPanelIndex = -1;
    int contextMenuSlotIndex = -1;
    int contextMenuX;
    int contextMenuY;
    ItemStack contextMenuEntryStack = ItemStack.EMPTY;
    final LinkedHashSet<DatabaseSelectionEntry> selectedDatabaseEntries = new LinkedHashSet<>();
    final DatabaseSelectionGestureModel selectionGestureModel = new DatabaseSelectionGestureModel();
    @Nullable
    DatabaseViewState selectionTrackedViewState;
    String pendingTargetSourceTabId = "";
    DatabaseScope pendingTargetSourceScope = DatabaseScope.PERSONAL;
    int pendingTargetPanelIndex = -1;
    int pendingQuickDepositSlotIndex = -1;
    boolean pendingQuickDepositSingleOnly = false;
    String managementSelectedTabId = com.agguy.infiniteinventory.database.DatabaseTabs.DEFAULT_TAB_ID;
    DatabaseScope managementSelectedScope = DatabaseScope.PERSONAL;
    String pendingIconItemId = com.agguy.infiniteinventory.database.DatabaseTabs.DEFAULT_CONCRETE_ICON_ITEM_ID;
    DatabaseScope managementSnapshotScope = DatabaseScope.PERSONAL;
    String managementSnapshotTabId = "";
    String managementSnapshotName = "";
    String managementSnapshotIconItemId = "";
    String customExtractValidationKey = "";
    DatabaseCategory iconPickerCategory = DatabaseCategory.ALL;
    int iconPickerPageIndex;
    String iconPickerOriginalItemId = com.agguy.infiniteinventory.database.DatabaseTabs.DEFAULT_CONCRETE_ICON_ITEM_ID;
    String lastUiSignature = "";
    boolean pendingTargetStoresSingle;
    PersonalDatabaseScreenEnums.TargetSelectorMode targetSelectorMode = PersonalDatabaseScreenEnums.TargetSelectorMode.NONE;
    int targetSelectorScrollIndex;
    DatabaseScope topTabScopeFilter = DatabaseScope.PERSONAL;
    @Nullable
    DatabaseScopedTabRef pendingTopTabActionTab;
    int viewSelectorPersonalScrollIndex;
    int viewSelectorPublicScrollIndex;
    int managementPersonalScrollIndex;
    int managementPublicScrollIndex;
    int moreTabsScrollIndex;
    int topTabReplaceScrollIndex;
    int advancedSearchScrollIndex;
    int enhancementScrollIndex;
    boolean tabContextMenuExpanded;
    @Nullable
    DatabaseScopedTabRef tabContextMenuTarget;
    int tabContextMenuX;
    int tabContextMenuY;
    int tabContextMenuHeight;
    boolean depositConflictExpanded;
    @Nullable
    com.agguy.infiniteinventory.network.DatabaseDepositConflictPayload pendingDepositConflict;
    boolean scrollbarDragging;
    int scrollbarDragStartY;
    int scrollbarDragStartScrollIndex;
    PersonalDatabaseScreenEnums.ScrollbarDragTarget scrollbarDragTarget = PersonalDatabaseScreenEnums.ScrollbarDragTarget.NONE;
    record DatabaseHitResult(int panelIndex, int slotIndex) {
    }

    record IconChoice(String itemId, ItemStack previewStack, String searchableText, DatabaseCategory category) {
    }
    /** 创建数据库屏幕实例。 */
    public PersonalDatabaseScreen(PersonalDatabaseMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.databaseMenu = menu;
        this.databaseScreenTitle = title;
        this.inventoryLabelY = Integer.MAX_VALUE;
        this.titleLabelY = Integer.MAX_VALUE;
    }

    /** 初始化屏幕尺寸与所有 UI 控件，在屏幕首次显示时调用。 */
    @Override
    protected void init() {
        this.imageWidth = this.width;
        this.imageHeight = this.height;
        super.init();
        if (!PersonalDatabaseScreenLayoutHelper.hasAccessorySlots(this)) {
            this.accessoriesExpanded = false;
            this.accessoryScrollRow = 0;
        }
        PersonalDatabaseScreenLifecycleHelper.initScreen(this);
    }

    /** 每 tick 更新屏幕状态，负责布局刷新、控件同步、搜索防抖与覆盖层校验。 */
    @Override
    public void containerTick() {
        super.containerTick();
        PersonalDatabaseScreenLifecycleHelper.tickScreen(this);
    }

    /**
     * 主渲染入口，按分层顺序绘制背景、原版容器、各覆盖层与自定义工具提示。
     *
     * <p>业务约束：若屏幕尺寸不足以支持完整 UI，则仅显示分辨率不足提示。
     */
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        if (!this.screenFitProfile().supportsFullUi()) {
            PersonalDatabaseScreenRenderHelper.renderUnsupportedScreen(this, guiGraphics);
            return;
        }
        this.suppressVanillaTooltipRender = true;
        try {
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        } finally {
            this.suppressVanillaTooltipRender = false;
        }
        PersonalDatabaseScreenRenderHelper.renderAccessorySlotHover(this, guiGraphics, mouseX, mouseY);
        PersonalDatabaseScreenRenderHelper.renderToolbarOverlays(this, guiGraphics);
        if (this.settingsPanelExpanded) {
            PersonalDatabaseScreenSettingsHelper.renderSettingsPanel(this, guiGraphics, mouseX, mouseY);
        }
        if (this.advancedSearchExpanded) {
            PersonalDatabaseScreenOverlayRenderHelper.renderAdvancedSearchPanel(this, guiGraphics, mouseX, mouseY);
        }
        if (this.enhancementPanelExpanded) {
            PersonalDatabaseScreenOverlayRenderHelper.renderEnhancementPanel(this, guiGraphics, mouseX, mouseY);
        }
        if (this.settingsPanelExpanded) {
            PersonalDatabaseScreenSettingsHelper.renderSettingsPanelCloseButton(this, guiGraphics, mouseX, mouseY);
        }
        if (this.sortDropdownExpanded) {
            PersonalDatabaseScreenOverlayRenderHelper.renderSortDropdown(this, guiGraphics, mouseX, mouseY);
        }
        if (this.pagePickerExpanded) {
            PersonalDatabaseScreenOverlayRenderHelper.renderPagePicker(this, guiGraphics, mouseX, mouseY);
        }
        if (this.viewSelectorExpanded) {
            PersonalDatabaseScreenTabHelper.renderViewSelector(this, guiGraphics, mouseX, mouseY);
        }
        if (this.moreTabsExpanded) {
            PersonalDatabaseScreenTabHelper.renderMoreTabsDropdown(this, guiGraphics, mouseX, mouseY);
        }
        if (this.topTabActionPromptExpanded) {
            PersonalDatabaseScreenTabHelper.renderTopTabActionPrompt(this, guiGraphics, mouseX, mouseY);
        }
        if (this.topTabReplaceExpanded) {
            PersonalDatabaseScreenTabHelper.renderTopTabReplacePrompt(this, guiGraphics, mouseX, mouseY);
        }
        if (this.tabManagementExpanded && !this.iconPickerExpanded && !this.targetSelectorExpanded) {
            PersonalDatabaseScreenManagementHelper.renderTabManagementPanel(this, guiGraphics, mouseX, mouseY);
        }
        if (this.iconPickerExpanded) {
            PersonalDatabaseScreenManagementHelper.renderIconPicker(this, guiGraphics, mouseX, mouseY);
        }
        if (this.targetSelectorExpanded) {
            PersonalDatabaseScreenTargetHelper.renderTargetSelector(this, guiGraphics, mouseX, mouseY);
        }
        if (this.contextMenuExpanded) {
            PersonalDatabaseScreenOverlayRenderHelper.renderContextMenu(this, guiGraphics, mouseX, mouseY);
        }
        if (this.tabContextMenuExpanded) {
            PersonalDatabaseScreenOverlayRenderHelper.renderTabContextMenu(this, guiGraphics, mouseX, mouseY);
        }
        if (this.depositConflictExpanded) {
            PersonalDatabaseScreenDepositConflictHelper.renderDepositConflict(this, guiGraphics, mouseX, mouseY);
        }
        if (this.customExtractOverlayExpanded) {
            PersonalDatabaseScreenCustomExtractOverlayHelper.renderOverlay(this, guiGraphics, mouseX, mouseY);
        }
        if (this.noteOverlayExpanded) {
            PersonalDatabaseScreenNoteOverlayHelper.renderOverlay(this, guiGraphics, mouseX, mouseY);
        }
        if (this.logPanelExpanded) {
            PersonalDatabaseScreenLogHelper.renderLogPanel(this, guiGraphics, mouseX, mouseY);
        }
        if (this.statisticsPanelExpanded) {
            PersonalDatabaseScreenStatisticsHelper.renderStatisticsPanel(this, guiGraphics, mouseX, mouseY);
        }
        PersonalDatabaseScreenRenderHelper.renderScreenTooltips(this, guiGraphics, mouseX, mouseY);
    }

    /** 渲染物品提示。当自定义提示渲染激活时跳过原版实现，防止双重绘制。 */
    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.suppressVanillaTooltipRender) {
            return;
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /** 渲染屏幕背景，包括数据库主体框架与各面板底色。 */
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        PersonalDatabaseScreenRenderHelper.renderBg(this, guiGraphics, partialTick, mouseX, mouseY);
    }

    /** 渲染容器标签。本屏幕使用完全自定义的文本绘制，故留空。 */
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    /** 渲染单个槽位，先由自定义 Helper 绘制附加装饰，再调用原版槽位渲染。 */
    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        if (this.settingsPanelExpanded || this.statisticsPanelExpanded) {
            return;
        }
        PersonalDatabaseScreenRenderHelper.renderSlot(this, guiGraphics, slot);
        super.renderSlot(guiGraphics, slot);
    }

    /** 渲染槽位悬停高亮。若 Helper 判定需要跳过，则抑制高亮以避免视觉干扰。 */
    @Override
    protected void renderSlotHighlight(GuiGraphics guiGraphics, Slot slot, int mouseX, int mouseY, float partialTick) {
        if (PersonalDatabaseScreenRenderHelper.shouldSkipSlotHighlight(this, slot)) {
            return;
        }
        super.renderSlotHighlight(guiGraphics, slot, mouseX, mouseY, partialTick);
    }

    /** 处理鼠标点击事件。 */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return PersonalDatabaseScreenInputDelegate.mouseClicked(this, mouseX, mouseY, button);
    }

    /** 处理鼠标滚轮事件。 */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return PersonalDatabaseScreenInputDelegate.mouseScrolled(this, mouseX, mouseY, scrollX, scrollY);
    }

    /** 处理鼠标拖拽事件。 */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return PersonalDatabaseScreenInputDelegate.mouseDragged(this, mouseX, mouseY, button, dragX, dragY);
    }

    /** 处理鼠标释放事件。 */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return PersonalDatabaseScreenInputDelegate.mouseReleased(this, mouseX, mouseY, button);
    }

    /** 处理键盘按下事件。 */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return PersonalDatabaseScreenInputDelegate.keyPressed(this, keyCode, scanCode, modifiers);
    }

    /** 处理键盘释放事件。 */
    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return PersonalDatabaseScreenInputDelegate.keyReleased(this, keyCode, scanCode, modifiers);
    }

    /** 处理字符输入事件。 */
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return PersonalDatabaseScreenInputDelegate.charTyped(this, codePoint, modifiers);
    }

    /** 返回当前屏幕使用的字体渲染器。 */
    Font screenFont() {
        return this.font;
    }

    /** 返回 Minecraft 客户端实例，可能为 null。 */
    @Nullable
    Minecraft minecraftClient() {
        return this.minecraft;
    }

    /** 返回当前鼠标悬停的槽位引用，可能为 null。 */
    @Nullable
    Slot hoveredSlotRef() {
        return this.hoveredSlot;
    }

    /** 返回当前屏幕宽度。 */
    int screenWidthValue() {
        return this.width;
    }

    /** 返回当前屏幕高度。 */
    int screenHeightValue() {
        return this.height;
    }

    /** 根据当前屏幕尺寸解析适配配置文件。 */
    PersonalDatabaseScreenFitProfile screenFitProfile() {
        return PersonalDatabaseScreenFitProfile.resolve(this.width, this.height);
    }

    /** 向屏幕添加一个按钮控件。 */
    Button addScreenButton(Button button) {
        return super.addRenderableWidget(button);
    }

    /** 向屏幕添加一个文本编辑框控件。 */
    EditBox addScreenEditBox(EditBox editBox) {
        return super.addRenderableWidget(editBox);
    }

    /** 清空屏幕所有已注册的渲染控件。 */
    void clearScreenWidgets() {
        super.clearWidgets();
    }

    boolean invokeSuperMouseClicked(double mouseX, double mouseY, int button) { return super.mouseClicked(mouseX, mouseY, button); }
    boolean invokeSuperMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) { return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY); }
    boolean invokeSuperMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) { return super.mouseDragged(mouseX, mouseY, button, dragX, dragY); }
    boolean invokeSuperMouseReleased(double mouseX, double mouseY, int button) { return super.mouseReleased(mouseX, mouseY, button); }
    boolean invokeSuperKeyPressed(int keyCode, int scanCode, int modifiers) { return super.keyPressed(keyCode, scanCode, modifiers); }
    boolean invokeSuperKeyReleased(int keyCode, int scanCode, int modifiers) { return super.keyReleased(keyCode, scanCode, modifiers); }
    boolean invokeSuperCharTyped(char codePoint, int modifiers) { return super.charTyped(codePoint, modifiers); }
    void invokeSuperRenderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) { super.renderTooltip(guiGraphics, mouseX, mouseY); }
    java.util.List<Component> containerTooltip(ItemStack stack) { return this.getTooltipFromContainerItem(stack); } void focusScreen(@Nullable GuiEventListener listener) { this.setFocused(listener); }
}
