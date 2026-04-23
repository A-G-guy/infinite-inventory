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
 * <p>职责边界：
 * <ul>
 *   <li>响应式布局：根据屏幕尺寸动态计算各面板位置与尺寸
 *   <li>覆盖层渲染：搜索、排序、标签页选择器、高级搜索、增强配置等弹层面板
 *   <li>输入事件：鼠标点击/滚动/拖拽、键盘按键、字符输入的捕获与分发
 *   <li>自定义工具提示：替代原版提示渲染，支持多行富文本与动态内容
 * </ul>
 *
 * <p>设计决策：
 * 所有渲染逻辑委托给专门的 Helper 类（如 {@code PersonalDatabaseScreenRenderHelper}），
 * 本类仅维护 UI 状态机与事件路由，避免单个类过度膨胀。
 */
public final class PersonalDatabaseScreen extends AbstractContainerScreen<PersonalDatabaseMenu> {
    static final int OVERLAY_TEXT_COLOR = 0x231C16;
    static final int OVERLAY_MUTED_TEXT_COLOR = 0x605547;
    static final int OVERLAY_ACCENT_TEXT_COLOR = 0x234A64;
    static final int FRAME_TEXT_COLOR = 0xF5F1E6;
    static final int FRAME_MUTED_TEXT_COLOR = 0xE6DCC2;
    static final int FRAME_ACCENT_TEXT_COLOR = 0xFFE0A6;
    static final int FRAME_TEXT_BACKDROP_COLOR = 0x6A16120D;
    static final int FRAME_TEXT_OUTLINE_COLOR = 0x90765B3B;
    static final int TEXT_FIELD_TEXT_COLOR = 0xF1ECE3;
    static final int TEXT_FIELD_MUTED_TEXT_COLOR = 0xB2ABA1;
    static final int SORT_BUTTON_TEXT_COLOR = 0xECE5D8;
    static final int SCROLLBAR_TRACK_COLOR = 0x3051463B;
    static final int SCROLLBAR_THUMB_COLOR = 0xCC7A6447;
    static final int SCROLLBAR_THUMB_HOVERED_COLOR = 0xE09E825D;
    static final int TOP_TAB_ACTIVE_TEXT_COLOR = 0xFFF4D58A;
    static final int TOP_TAB_INACTIVE_TEXT_COLOR = 0xFFF9F4EA;
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
    static final int TEXT_FIELD_LEFT_PADDING = 6;
    static final int TEXT_FIELD_RIGHT_PADDING = 6;
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

    final PlayerInventoryPaneProvider inventoryPaneProvider = new VanillaPlayerInventoryPaneProvider();
    final PersonalDatabaseMenu databaseMenu;
    final Component databaseScreenTitle;
    @Nullable
    PersonalDatabaseLayout layout;
    @Nullable
    DatabaseQuery pendingLayoutQuery;
    Button depositButton;
    Button advancedSearchButton;
    Button enhancementButton;
    Button viewSelectorButton;
    Button tabManagementButton;
    Button logButton;
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
    TargetSelectorMode targetSelectorMode = TargetSelectorMode.NONE;
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

    enum TargetSelectorMode {
        NONE,
        DEPOSIT_ALL,
        CARRIED_STORE,
        QUICK_DEPOSIT,
        TRANSFER_TAB,
        TRANSFER_SELECTION,
        DELETE_TAB,
        AUTO_STORE_TARGET
    }

    record DatabaseHitResult(int panelIndex, int slotIndex) {
    }

    record IconChoice(String itemId, ItemStack previewStack, String searchableText, DatabaseCategory category) {
    }

    /**
     * 创建数据库屏幕实例。
     *
     * @param menu           关联的服务器端菜单
     * @param playerInventory 玩家背包，用于原版容器屏幕的基础初始化
     * @param title          屏幕标题组件
     */
    public PersonalDatabaseScreen(PersonalDatabaseMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.databaseMenu = menu;
        this.databaseScreenTitle = title;
        this.inventoryLabelY = Integer.MAX_VALUE;
        this.titleLabelY = Integer.MAX_VALUE;
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
    @Override
    protected void init() {
        this.imageWidth = this.width;
        this.imageHeight = this.height;
        super.init();

        if (!PersonalDatabaseScreenLayoutHelper.hasAccessorySlots(this)) {
            this.accessoriesExpanded = false;
            this.accessoryScrollRow = 0;
        }
        PersonalDatabaseScreenLayoutHelper.rebuildLayout(this);
        this.pendingLayoutQuery = null;
        this.sortDropdownExpanded = false;
        this.pagePickerExpanded = false;
        this.advancedSearchExpanded = false;
        this.enhancementPanelExpanded = false;
        this.viewSelectorExpanded = false;
        this.moreTabsExpanded = false;
        this.topTabActionPromptExpanded = false;
        this.topTabReplaceExpanded = false;
        this.targetSelectorExpanded = false;
        this.tabManagementExpanded = false;
        this.iconPickerExpanded = false;
        this.customExtractOverlayExpanded = false;
        this.noteOverlayExpanded = false;
        this.customExtractValidationKey = "";
        this.logPanelExpanded = false;
        this.logPanelScrollIndex = 0;
        this.viewSelectorPersonalScrollIndex = 0;
        this.viewSelectorPublicScrollIndex = 0;
        this.managementPersonalScrollIndex = 0;
        this.managementPublicScrollIndex = 0;
        this.moreTabsScrollIndex = 0;
        this.topTabReplaceScrollIndex = 0;
        this.advancedSearchScrollIndex = 0;
        this.enhancementScrollIndex = 0;
        this.topTabScopeFilter = this.databaseMenu.viewState().query().focusedTab().scope();
        this.selectionGestureModel.clearSelectionGesture();
        this.selectionGestureModel.releaseDiscardKey();
        this.searchSyncCooldownTicks = 0;
        this.activeSearchTab = null;
        this.pendingSearchTexts.clear();
        this.dispatchedSearchTexts.clear();
        PersonalDatabaseScreenContextHelper.closeContextMenu(this);
        PersonalDatabaseScreenWidgetHelper.buildWidgets(this);
        PersonalDatabaseScreenWidgetHelper.syncWidgetsFromState(this);
        PersonalDatabaseScreenLayoutHelper.ensureLayoutQuerySynced(this);
    }

    /**
     * 每 tick 更新屏幕状态，负责布局刷新、控件同步、搜索防抖与覆盖层校验。
     *
     * <p>设计决策：将各类周期性逻辑拆分到独立 Helper，保持屏幕类仅作为调度中心。
     */
    @Override
    public void containerTick() {
        super.containerTick();
        PersonalDatabaseScreenLayoutHelper.refreshUiStructureIfNeeded(this);
        PersonalDatabaseScreenWidgetHelper.syncWidgetsFromState(this);
        PersonalDatabaseScreenLayoutHelper.tickSearchSync(this);
        PersonalDatabaseScreenCustomExtractOverlayHelper.validateOverlay(this);
        PersonalDatabaseScreenNoteOverlayHelper.validateOverlay(this);
        PersonalDatabaseScreenLayoutHelper.ensureLayoutQuerySynced(this);
    }

    /**
     * 主渲染入口，按分层顺序绘制背景、原版容器、各覆盖层与自定义工具提示。
     *
     * <p>渲染顺序（后绘制的覆盖先绘制的）：
     * <ol>
     *   <li>背景与原版容器槽位
     *   <li>饰品槽位悬停高亮与工具栏覆盖层
     *   <li>各类弹层面板（高级搜索、增强配置、排序下拉、页码选择器等）
     *   <li>自定义工具提示（替代原版渲染以支持富文本）
     * </ol>
     *
     * <p>业务约束：若屏幕尺寸不足以支持完整 UI，则仅显示分辨率不足提示。
     *
     * @param guiGraphics 图形绘制上下文
     * @param mouseX      当前鼠标 X 坐标
     * @param mouseY      当前鼠标 Y 坐标
     * @param partialTick 部分 tick 时间，用于动画插值
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
        if (this.advancedSearchExpanded) {
            PersonalDatabaseScreenOverlayRenderHelper.renderAdvancedSearchPanel(this, guiGraphics, mouseX, mouseY);
        }
        if (this.enhancementPanelExpanded) {
            PersonalDatabaseScreenOverlayRenderHelper.renderEnhancementPanel(this, guiGraphics, mouseX, mouseY);
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
        if (this.customExtractOverlayExpanded) {
            PersonalDatabaseScreenCustomExtractOverlayHelper.renderOverlay(this, guiGraphics, mouseX, mouseY);
        }
        if (this.noteOverlayExpanded) {
            PersonalDatabaseScreenNoteOverlayHelper.renderOverlay(this, guiGraphics, mouseX, mouseY);
        }
        if (this.logPanelExpanded) {
            PersonalDatabaseScreenLogHelper.renderLogPanel(this, guiGraphics, mouseX, mouseY);
        }
        PersonalDatabaseScreenRenderHelper.renderScreenTooltips(this, guiGraphics, mouseX, mouseY);
    }

    /**
     * 渲染物品提示。当自定义提示渲染激活时跳过原版实现，防止双重绘制。
     *
     * @param guiGraphics 图形绘制上下文
     * @param mouseX      鼠标 X 坐标
     * @param mouseY      鼠标 Y 坐标
     */
    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.suppressVanillaTooltipRender) {
            return;
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /**
     * 渲染屏幕背景，包括数据库主体框架与各面板底色。
     *
     * @param guiGraphics 图形绘制上下文
     * @param partialTick 部分 tick 时间
     * @param mouseX      鼠标 X 坐标
     * @param mouseY      鼠标 Y 坐标
     */
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        PersonalDatabaseScreenRenderHelper.renderBg(this, guiGraphics, partialTick, mouseX, mouseY);
    }

    /**
     * 渲染容器标签。本屏幕使用完全自定义的文本绘制，故留空。
     */
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    /**
     * 渲染单个槽位，先由自定义 Helper 绘制附加装饰（如选中框、数量覆盖），再调用原版槽位渲染。
     *
     * @param guiGraphics 图形绘制上下文
     * @param slot        待渲染的槽位
     */
    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        PersonalDatabaseScreenRenderHelper.renderSlot(this, guiGraphics, slot);
        super.renderSlot(guiGraphics, slot);
    }

    /**
     * 渲染槽位悬停高亮。若 Helper 判定需要跳过（如多选拖拽期间），则抑制高亮以避免视觉干扰。
     *
     * @param guiGraphics 图形绘制上下文
     * @param slot        目标槽位
     * @param mouseX      鼠标 X 坐标
     * @param mouseY      鼠标 Y 坐标
     * @param partialTick 部分 tick 时间
     */
    @Override
    protected void renderSlotHighlight(GuiGraphics guiGraphics, Slot slot, int mouseX, int mouseY, float partialTick) {
        if (PersonalDatabaseScreenRenderHelper.shouldSkipSlotHighlight(this, slot)) {
            return;
        }
        super.renderSlotHighlight(guiGraphics, slot, mouseX, mouseY, partialTick);
    }

    /**
     * 处理鼠标点击事件，分发给交互 Helper 以支持覆盖层优先、多选手势等自定义逻辑。
     *
     * @param mouseX 鼠标 X 坐标
     * @param mouseY 鼠标 Y 坐标
     * @param button 鼠标按键编码
     * @return true 表示事件已被消费
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return PersonalDatabaseScreenInteractionHelper.mouseClicked(this, mouseX, mouseY, button);
    }

    /**
     * 处理鼠标滚轮事件，用于面板滚动、下拉列表滚动等。
     *
     * @param mouseX 鼠标 X 坐标
     * @param mouseY 鼠标 Y 坐标
     * @param scrollX 水平滚动量
     * @param scrollY 垂直滚动量
     * @return true 表示事件已被消费
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return PersonalDatabaseScreenInteractionHelper.mouseScrolled(this, mouseX, mouseY, scrollX, scrollY);
    }

    /**
     * 处理鼠标拖拽事件，主要用于多选框手势与槽位拖拽。
     *
     * @param mouseX 鼠标当前 X 坐标
     * @param mouseY 鼠标当前 Y 坐标
     * @param button 拖拽按键编码
     * @param dragX  本次拖拽 X 偏移
     * @param dragY  本次拖拽 Y 偏移
     * @return true 表示事件已被消费
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return PersonalDatabaseScreenInteractionHelper.mouseDragged(this, mouseX, mouseY, button, dragX, dragY);
    }

    /**
     * 处理鼠标释放事件，用于结束多选手势、关闭临时覆盖层等。
     *
     * @param mouseX 鼠标 X 坐标
     * @param mouseY 鼠标 Y 坐标
     * @param button 释放的按键编码
     * @return true 表示事件已被消费
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return PersonalDatabaseScreenInteractionHelper.mouseReleased(this, mouseX, mouseY, button);
    }

    /**
     * 处理键盘按下事件，支持快捷键（如 ESC 关闭覆盖层、Ctrl+A 全选）。
     *
     * @param keyCode   按键编码
     * @param scanCode  扫描码
     * @param modifiers 修饰键位掩码
     * @return true 表示事件已被消费
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return PersonalDatabaseScreenInteractionHelper.keyPressed(this, keyCode, scanCode, modifiers);
    }

    /**
     * 处理键盘释放事件，用于修饰键状态跟踪（如多选丢弃键释放检测）。
     *
     * @param keyCode   按键编码
     * @param scanCode  扫描码
     * @param modifiers 修饰键位掩码
     * @return true 表示事件已被消费
     */
    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return PersonalDatabaseScreenInteractionHelper.keyReleased(this, keyCode, scanCode, modifiers);
    }

    /**
     * 处理字符输入事件，用于搜索框、备注编辑框等文本控件的输入。
     *
     * @param codePoint 输入的 Unicode 码点
     * @param modifiers 修饰键位掩码
     * @return true 表示事件已被消费
     */
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return PersonalDatabaseScreenInteractionHelper.charTyped(this, codePoint, modifiers);
    }

    /**
     * 返回当前屏幕使用的字体渲染器，供外部 Helper 统一文本绘制。
     *
     * @return 屏幕字体实例
     */
    Font screenFont() {
        return this.font;
    }

    /**
     * 返回 Minecraft 客户端实例，供网络包发送等操作使用。
     *
     * @return 客户端实例，可能为 null（如屏幕未完全初始化时）
     */
    @Nullable
    Minecraft minecraftClient() {
        return this.minecraft;
    }

    /**
     * 返回当前鼠标悬停的槽位引用，供覆盖层判断与提示渲染使用。
     *
     * @return 悬停槽位，可能为 null
     */
    @Nullable
    Slot hoveredSlotRef() {
        return this.hoveredSlot;
    }

    /**
     * 返回当前屏幕宽度。
     *
     * @return 屏幕宽度（像素）
     */
    int screenWidthValue() {
        return this.width;
    }

    /**
     * 返回当前屏幕高度。
     *
     * @return 屏幕高度（像素）
     */
    int screenHeightValue() {
        return this.height;
    }

    /**
     * 根据当前屏幕尺寸解析适配配置文件，决定 UI 布局策略。
     *
     * @return 当前屏幕的适配配置
     */
    PersonalDatabaseScreenFitProfile screenFitProfile() {
        return PersonalDatabaseScreenFitProfile.resolve(this.width, this.height);
    }

    /**
     * 向屏幕添加一个按钮控件，封装父类方法以便 Helper 类调用。
     *
     * @param button 待添加的按钮
     * @return 添加后的按钮实例
     */
    Button addScreenButton(Button button) {
        return super.addRenderableWidget(button);
    }

    /**
     * 向屏幕添加一个文本编辑框控件，封装父类方法以便 Helper 类调用。
     *
     * @param editBox 待添加的编辑框
     * @return 添加后的编辑框实例
     */
    EditBox addScreenEditBox(EditBox editBox) {
        return super.addRenderableWidget(editBox);
    }

    /**
     * 清空屏幕所有已注册的渲染控件，通常在重新初始化布局前调用。
     */
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
    java.util.List<Component> containerTooltip(ItemStack stack) { return this.getTooltipFromContainerItem(stack); }
    void focusScreen(@Nullable GuiEventListener listener) { this.setFocused(listener); }
}
