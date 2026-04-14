package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.compat.PlayerInventoryPaneProvider;
import com.agguy.infiniteinventory.compat.VanillaPlayerInventoryPaneProvider;
import com.agguy.infiniteinventory.database.DatabaseEnhancementConfig;
import com.agguy.infiniteinventory.database.DatabaseEnhancementOption;
import com.agguy.infiniteinventory.database.DatabasePanelView;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseSearchConfig;
import com.agguy.infiniteinventory.database.DatabaseSearchField;
import com.agguy.infiniteinventory.database.DatabaseSearchWeight;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import com.agguy.infiniteinventory.database.DatabaseTab;
import com.agguy.infiniteinventory.database.DatabaseTabs;
import com.agguy.infiniteinventory.database.DatabaseViewState;
import com.agguy.infiniteinventory.database.VisibleDatabaseEntry;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import com.agguy.infiniteinventory.menu.PersonalDatabaseMenu;
import com.agguy.infiniteinventory.network.DatabaseClickAction;
import com.agguy.infiniteinventory.network.DatabaseClickPayload;
import com.agguy.infiniteinventory.network.DatabaseEnhancementPayload;
import com.agguy.infiniteinventory.network.DatabaseQuickDepositPayload;
import com.agguy.infiniteinventory.network.DatabaseQueryPayload;
import com.agguy.infiniteinventory.network.DatabaseTabMutationAction;
import com.agguy.infiniteinventory.network.DatabaseTabMutationPayload;
import com.agguy.infiniteinventory.network.DepositAllPayload;
import com.agguy.infiniteinventory.util.CompactNumberFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public final class PersonalDatabaseScreen extends AbstractContainerScreen<PersonalDatabaseMenu> {
    private static final int OVERLAY_TEXT_COLOR = 0x3D342B;
    private static final int OVERLAY_MUTED_TEXT_COLOR = 0x6B6257;
    private static final int OVERLAY_ACCENT_TEXT_COLOR = 0x5A4523;
    private static final int DROPDOWN_ROW_HEIGHT = 20;
    private static final int SORT_DROPDOWN_WIDTH = 168;
    private static final int CONTEXT_MENU_MIN_WIDTH = 112;
    private static final int CONTEXT_MENU_ROW_HEIGHT = 20;
    private static final int CONTEXT_MENU_MARGIN = 4;
    private static final int PAGE_PICKER_MIN_WIDTH = 88;
    private static final int PAGE_PICKER_ROW_HEIGHT = 20;
    private static final int SEARCH_ICON_SIZE = 9;
    private static final int SEARCH_TEXT_LEFT_PADDING = 18;
    private static final int ADVANCED_SEARCH_PANEL_WIDTH = 236;
    private static final int ADVANCED_SEARCH_PANEL_PADDING = 6;
    private static final int ADVANCED_SEARCH_TITLE_HEIGHT = 12;
    private static final int ADVANCED_SEARCH_ROW_HEIGHT = 20;
    private static final int ADVANCED_SEARCH_ROW_GAP = 2;
    private static final int ADVANCED_SEARCH_TOGGLE_WIDTH = 24;
    private static final int ADVANCED_SEARCH_WEIGHT_WIDTH = 40;
    private static final int ENHANCEMENT_PANEL_WIDTH = 236;
    private static final int ENHANCEMENT_PANEL_PADDING = 6;
    private static final int ENHANCEMENT_TITLE_HEIGHT = 12;
    private static final int ENHANCEMENT_ROW_HEIGHT = 20;
    private static final int ENHANCEMENT_ROW_GAP = 2;
    private static final int ENHANCEMENT_TOGGLE_WIDTH = 24;
    private static final int TAB_SELECTOR_WIDTH = 220;
    private static final int TAB_SELECTOR_ROW_HEIGHT = 20;
    private static final int MORE_TABS_WIDTH = 180;
    private static final int TARGET_SELECTOR_WIDTH = 180;
    private static final int TARGET_SELECTOR_ROW_HEIGHT = 20;
    private static final int MANAGEMENT_PANEL_WIDTH = 320;
    private static final int MANAGEMENT_PANEL_HEIGHT = 260;
    private static final int MANAGEMENT_ROW_HEIGHT = 20;
    private static final int MANAGEMENT_LIST_WIDTH = 124;
    private static final int MANAGEMENT_BUTTON_WIDTH = 76;
    private static final int ICON_PICKER_WIDTH = 280;
    private static final int ICON_PICKER_HEIGHT = 220;
    private static final int MANAGEMENT_PANEL_PADDING = 8;
    private static final int OVERLAY_SECTION_TITLE_HEIGHT = 12;
    private static final int INLINE_TAB_MIN_WIDTH = 88;
    private static final int INLINE_TAB_MIN_WIDTH_WITH_MORE = 72;
    private static final int INLINE_TAB_TIGHT_GAP = 2;
    private static final int INLINE_TAB_MORE_WIDTH = 56;
    private static final int ICON_PICKER_COLUMNS = 6;
    private static final int ICON_PICKER_ROWS = 4;
    private static final int ICON_PICKER_CELL_SIZE = 40;
    private static final int ICON_PICKER_MAX_RESULTS = ICON_PICKER_COLUMNS * ICON_PICKER_ROWS;
    private static final int TAB_ICON_SIZE = 16;
    private static final int TAB_ICON_LEFT_PADDING = 4;
    private static final int TAB_TEXT_GAP = 3;
    private static final DatabaseClickAction[] CONTEXT_MENU_ACTIONS = {
            DatabaseClickAction.TAKE_SINGLE,
            DatabaseClickAction.TAKE_HALF_STACK_TO_INVENTORY,
            DatabaseClickAction.TAKE_STACK,
            DatabaseClickAction.TAKE_HALF_ENTRY_TO_INVENTORY,
            DatabaseClickAction.TAKE_ALL
    };

    private final PlayerInventoryPaneProvider inventoryPaneProvider = new VanillaPlayerInventoryPaneProvider();
    @Nullable
    private PersonalDatabaseLayout layout;
    @Nullable
    private DatabaseQuery pendingLayoutQuery;
    private EditBox searchBox;
    private Button depositButton;
    private Button previousPageButton;
    private Button nextPageButton;
    private Button sortButton;
    private Button advancedSearchButton;
    private Button enhancementButton;
    private Button viewSelectorButton;
    private Button tabManagementButton;
    private Button personalScopeButton;
    private Button publicScopeButton;
    private Button accessoriesToggleButton;
    @Nullable
    private EditBox managementNameBox;
    @Nullable
    private EditBox iconSearchBox;
    private final Map<DatabaseSearchField, Button> advancedSearchToggleButtons = new EnumMap<>(DatabaseSearchField.class);
    private final Map<DatabaseSearchField, Button> advancedSearchWeightButtons = new EnumMap<>(DatabaseSearchField.class);
    private final Map<DatabaseEnhancementOption, Button> enhancementToggleButtons = new EnumMap<>(DatabaseEnhancementOption.class);
    private boolean syncingSearchBox;
    private boolean sortDropdownExpanded;
    private boolean pagePickerExpanded;
    private boolean advancedSearchExpanded;
    private boolean enhancementPanelExpanded;
    private boolean viewSelectorExpanded;
    private boolean moreTabsExpanded;
    private boolean targetSelectorExpanded;
    private boolean tabManagementExpanded;
    private boolean iconPickerExpanded;
    private boolean accessoriesExpanded;
    private boolean contextMenuExpanded;
    private boolean suppressVanillaTooltipRender;
    private int accessoryScrollRow;
    private int contextMenuPanelIndex = -1;
    private int contextMenuSlotIndex = -1;
    private int contextMenuX;
    private int contextMenuY;
    private ItemStack contextMenuEntryStack = ItemStack.EMPTY;
    private String pendingTargetSourceTabId = "";
    private int pendingTargetPanelIndex = -1;
    private int pendingQuickDepositSlotIndex = -1;
    private String managementSelectedTabId = DatabaseTabs.DEFAULT_TAB_ID;
    private String pendingIconItemId = DatabaseTabs.DEFAULT_CONCRETE_ICON_ITEM_ID;
    private boolean pendingTargetStoresSingle;
    private TargetSelectorMode targetSelectorMode = TargetSelectorMode.NONE;

    private enum TargetSelectorMode {
        NONE,
        DEPOSIT_ALL,
        CARRIED_STORE,
        QUICK_DEPOSIT,
        TRANSFER_TAB,
        DELETE_TAB,
        AUTO_STORE_TARGET
    }

    private record DatabaseHitResult(int panelIndex, int slotIndex) {
    }

    private record IconChoice(String itemId, ItemStack previewStack, String searchableText) {
    }

    public PersonalDatabaseScreen(PersonalDatabaseMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.inventoryLabelY = Integer.MAX_VALUE;
        this.titleLabelY = Integer.MAX_VALUE;
    }

    @Override
    protected void init() {
        this.imageWidth = this.width;
        this.imageHeight = this.height;
        super.init();

        if (!this.hasAccessorySlots()) {
            this.accessoriesExpanded = false;
            this.accessoryScrollRow = 0;
        }
        this.rebuildLayout();
        this.pendingLayoutQuery = null;
        this.sortDropdownExpanded = false;
        this.pagePickerExpanded = false;
        this.advancedSearchExpanded = false;
        this.enhancementPanelExpanded = false;
        this.viewSelectorExpanded = false;
        this.moreTabsExpanded = false;
        this.targetSelectorExpanded = false;
        this.tabManagementExpanded = false;
        this.iconPickerExpanded = false;
        this.closeContextMenu();
        this.buildWidgets();
        this.syncWidgetsFromState();
        this.ensureLayoutQuerySynced();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.syncWidgetsFromState();
        this.ensureLayoutQuerySynced();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        this.suppressVanillaTooltipRender = true;
        try {
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        } finally {
            this.suppressVanillaTooltipRender = false;
        }
        this.renderAccessorySlotHover(guiGraphics, mouseX, mouseY);
        this.renderToolbarOverlays(guiGraphics);
        if (this.advancedSearchExpanded) {
            this.renderAdvancedSearchPanel(guiGraphics, mouseX, mouseY);
        }
        if (this.enhancementPanelExpanded) {
            this.renderEnhancementPanel(guiGraphics, mouseX, mouseY);
        }
        this.renderSearchHint(guiGraphics);
        if (this.sortDropdownExpanded) {
            this.renderSortDropdown(guiGraphics, mouseX, mouseY);
        }
        if (this.pagePickerExpanded) {
            this.renderPagePicker(guiGraphics, mouseX, mouseY);
        }
        if (this.viewSelectorExpanded) {
            this.renderViewSelector(guiGraphics, mouseX, mouseY);
        }
        if (this.moreTabsExpanded) {
            this.renderMoreTabsDropdown(guiGraphics, mouseX, mouseY);
        }
        if (this.targetSelectorExpanded) {
            this.renderTargetSelector(guiGraphics, mouseX, mouseY);
        }
        if (this.tabManagementExpanded) {
            this.renderTabManagementPanel(guiGraphics, mouseX, mouseY);
        }
        if (this.iconPickerExpanded) {
            this.renderIconPicker(guiGraphics, mouseX, mouseY);
        }
        if (this.contextMenuExpanded) {
            this.renderContextMenu(guiGraphics, mouseX, mouseY);
        }
        this.renderScreenTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.suppressVanillaTooltipRender) {
            return;
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        if (this.layout == null) {
            return;
        }
        VanillaWidgetRenderer.renderPanel(guiGraphics, this.layout.frameRect());
        this.renderTabs(guiGraphics, mouseX, mouseY);
        VanillaWidgetRenderer.renderTextField(guiGraphics, this.layout.searchFieldRect(), this.searchBox != null && this.searchBox.isFocused());
        this.renderDatabaseScaffold(guiGraphics);

        if (this.minecraft != null && this.minecraft.player != null) {
            this.inventoryPaneProvider.renderEquipmentPanel(
                    guiGraphics,
                    this.minecraft.player,
                    this.layout.equipmentPanelRect().x(),
                    this.layout.equipmentPanelRect().y(),
                    mouseX,
                    mouseY
            );
        }
        this.inventoryPaneProvider.renderBottomInventory(
                guiGraphics,
                this.layout.bottomInventoryRect().x(),
                this.layout.bottomInventoryRect().y()
        );
        if (this.accessoriesExpanded) {
            this.renderAccessoriesPanel(guiGraphics, mouseX, mouseY);
        }

        this.renderDatabaseSlots(guiGraphics);
        this.renderDatabaseEntries(guiGraphics, mouseX, mouseY);
        this.renderEmptyState(guiGraphics);
        this.renderFrameText(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        PersonalDatabaseLayout.AccessorySlotLayout accessorySlotLayout = this.resolveAccessorySlotLayout(slot);
        if (accessorySlotLayout != null && accessorySlotLayout.visible()) {
            PersonalDatabaseLayout.Rect slotRect = accessorySlotLayout.slotRect();
            VanillaWidgetRenderer.renderMenuSlot(guiGraphics, slotRect.x(), slotRect.y());
        }
        super.renderSlot(guiGraphics, slot);
    }

    @Override
    protected void renderSlotHighlight(GuiGraphics guiGraphics, Slot slot, int mouseX, int mouseY, float partialTick) {
        if (this.resolveAccessorySlotLayout(slot) != null) {
            return;
        }
        super.renderSlotHighlight(guiGraphics, slot, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.advancedSearchExpanded && !this.isWithinAdvancedSearchPanel(mouseX, mouseY)) {
            this.advancedSearchExpanded = false;
        }
        if (this.enhancementPanelExpanded && !this.isWithinEnhancementPanel(mouseX, mouseY)) {
            this.enhancementPanelExpanded = false;
        }
        if (this.iconPickerExpanded && this.handleIconPickerClick(mouseX, mouseY)) {
            return true;
        }
        if (this.tabManagementExpanded && this.handleTabManagementClick(mouseX, mouseY)) {
            return true;
        }
        if (this.targetSelectorExpanded && this.handleTargetSelectorClick(mouseX, mouseY)) {
            return true;
        }
        if (this.moreTabsExpanded && this.handleMoreTabsClick(mouseX, mouseY)) {
            return true;
        }
        if (this.viewSelectorExpanded && this.handleViewSelectorClick(mouseX, mouseY)) {
            return true;
        }
        if (this.pagePickerExpanded && this.handlePagePickerClick(mouseX, mouseY)) {
            return true;
        }
        if (this.advancedSearchExpanded && this.isWithinAdvancedSearchPanel(mouseX, mouseY)) {
            super.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        if (this.enhancementPanelExpanded && this.isWithinEnhancementPanel(mouseX, mouseY)) {
            if (this.handleEnhancementPanelClick(mouseX, mouseY)) {
                return true;
            }
            super.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        if (this.contextMenuExpanded && this.handleContextMenuClick(mouseX, mouseY)) {
            return true;
        }
        if (this.sortDropdownExpanded && this.handleSortDropdownClick(mouseX, mouseY)) {
            return true;
        }
        if (this.handlePageLabelClick(mouseX, mouseY)) {
            return true;
        }
        if (this.accessoriesExpanded && this.isWithinAccessoriesPanel(mouseX, mouseY)) {
            super.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        if (this.handleTabClick(mouseX, mouseY)) {
            return true;
        }
        if (this.handleQuickDepositClick(mouseX, mouseY, button)) {
            return true;
        }
        if (this.handleDatabaseClick(mouseX, mouseY, button)) {
            return true;
        }
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (!handled) {
            this.sortDropdownExpanded = false;
            this.pagePickerExpanded = false;
            this.enhancementPanelExpanded = false;
            this.viewSelectorExpanded = false;
            this.moreTabsExpanded = false;
            this.targetSelectorExpanded = false;
            this.closeContextMenu();
        }
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.accessoriesExpanded && this.isWithinAccessoriesPanel(mouseX, mouseY) && this.scrollAccessories((int) -Math.signum(scrollY))) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (hasControlDown() && keyCode == GLFW.GLFW_KEY_F && this.searchBox != null) {
            this.setFocused(this.searchBox);
            this.searchBox.setFocused(true);
            return true;
        }
        if (this.iconPickerExpanded && this.iconSearchBox != null && this.iconSearchBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (this.tabManagementExpanded && this.managementNameBox != null && this.managementNameBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (this.searchBox != null && this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.iconPickerExpanded && this.iconSearchBox != null && this.iconSearchBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        if (this.tabManagementExpanded && this.managementNameBox != null && this.managementNameBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        if (this.searchBox != null && this.searchBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void rebuildLayout() {
        int visiblePanelCount = Math.max(1, Math.min(DatabaseTabs.MAX_VISIBLE_TAB_COUNT, this.menu.viewState().query().visibleTabIds().size()));
        this.layout = PersonalDatabaseLayout.create(
                this.width,
                this.height,
                this.inventoryPaneProvider.equipmentPanelWidth(),
                this.inventoryPaneProvider.equipmentPanelHeight(),
                this.inventoryPaneProvider.bottomInventoryWidth(),
                this.inventoryPaneProvider.bottomInventoryHeight(),
                this.menu.accessorySlotGroups(),
                visiblePanelCount,
                this.accessoriesExpanded,
                this.accessoryScrollRow
        );
        this.accessoryScrollRow = this.layout.accessoryScrollRow();
        this.menu.applySlotLayout(this.layout);
    }

    private boolean hasAccessorySlots() {
        return !this.menu.accessorySlotGroups().isEmpty();
    }

    private void buildWidgets() {
        if (this.layout == null) {
            return;
        }
        DatabaseQuery query = this.menu.viewState().query();
        PersonalDatabaseLayout.Rect searchRect = this.layout.searchFieldRect();
        this.searchBox = new EditBox(
                this.font,
                searchRect.x() + SEARCH_TEXT_LEFT_PADDING,
                searchRect.y() + 4,
                Math.max(1, searchRect.width() - SEARCH_TEXT_LEFT_PADDING - 4),
                12,
                Component.translatable("screen.infiniteinventory.search")
        );
        this.searchBox.setMaxLength(DatabaseQuery.MAX_SEARCH_LENGTH);
        this.searchBox.setBordered(false);
        this.searchBox.setTextColor(0x303030);
        this.searchBox.setTextColorUneditable(0x606060);
        this.syncingSearchBox = true;
        this.searchBox.setValue(query.searchText());
        this.syncingSearchBox = false;
        this.searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(this.searchBox);

        PersonalDatabaseLayout.Rect advancedSearchRect = this.layout.advancedSearchButtonRect();
        this.advancedSearchButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.infiniteinventory.search_advanced_button"), button -> {
                    this.closeContextMenu();
                    this.sortDropdownExpanded = false;
                    this.pagePickerExpanded = false;
                    this.enhancementPanelExpanded = false;
                    this.advancedSearchExpanded = !this.advancedSearchExpanded;
                })
                .bounds(advancedSearchRect.x(), advancedSearchRect.y(), advancedSearchRect.width(), advancedSearchRect.height())
                .build());

        this.buildAdvancedSearchButtons();

        PersonalDatabaseLayout.Rect enhancementRect = this.layout.enhancementButtonRect();
        this.enhancementButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.infiniteinventory.enhancement_button"), button -> {
                    this.closeContextMenu();
                    this.sortDropdownExpanded = false;
                    this.pagePickerExpanded = false;
                    this.viewSelectorExpanded = false;
                    this.moreTabsExpanded = false;
                    this.targetSelectorExpanded = false;
                    this.tabManagementExpanded = false;
                    this.advancedSearchExpanded = false;
                    this.enhancementPanelExpanded = !this.enhancementPanelExpanded;
                })
                .bounds(enhancementRect.x(), enhancementRect.y(), enhancementRect.width(), enhancementRect.height())
                .build());

        this.buildEnhancementButtons();

        PersonalDatabaseLayout.Rect viewSelectorRect = this.layout.viewSelectorButtonRect();
        this.viewSelectorButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.infiniteinventory.visible_tabs_button"), button -> {
                    this.closeContextMenu();
                    this.sortDropdownExpanded = false;
                    this.pagePickerExpanded = false;
                    this.advancedSearchExpanded = false;
                    this.enhancementPanelExpanded = false;
                    this.moreTabsExpanded = false;
                    this.tabManagementExpanded = false;
                    this.targetSelectorExpanded = false;
                    this.viewSelectorExpanded = !this.viewSelectorExpanded;
                })
                .bounds(viewSelectorRect.x(), viewSelectorRect.y(), viewSelectorRect.width(), viewSelectorRect.height())
                .build());

        PersonalDatabaseLayout.Rect tabManagementRect = this.layout.tabManagementButtonRect();
        this.tabManagementButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.infiniteinventory.tab_management_button"), button -> {
                    this.closeContextMenu();
                    this.sortDropdownExpanded = false;
                    this.pagePickerExpanded = false;
                    this.advancedSearchExpanded = false;
                    this.enhancementPanelExpanded = false;
                    this.moreTabsExpanded = false;
                    this.targetSelectorExpanded = false;
                    this.viewSelectorExpanded = false;
                    this.tabManagementExpanded = !this.tabManagementExpanded;
                    this.iconPickerExpanded = false;
                    this.ensureManagementWidgets();
                })
                .bounds(tabManagementRect.x(), tabManagementRect.y(), tabManagementRect.width(), tabManagementRect.height())
                .build());

        PersonalDatabaseLayout.Rect sortRect = this.layout.sortButtonRect();
        this.sortButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
                    this.closeContextMenu();
                    this.advancedSearchExpanded = false;
                    this.enhancementPanelExpanded = false;
                    this.viewSelectorExpanded = false;
                    this.moreTabsExpanded = false;
                    this.targetSelectorExpanded = false;
                    this.tabManagementExpanded = false;
                    this.pagePickerExpanded = false;
                    this.sortDropdownExpanded = !this.sortDropdownExpanded;
                })
                .bounds(sortRect.x(), sortRect.y(), sortRect.width(), sortRect.height())
                .build());

        PersonalDatabaseLayout.Rect personalScopeRect = this.layout.personalScopeButtonRect();
        this.personalScopeButton = this.addRenderableWidget(Button.builder(
                        Component.translatable(DatabaseScope.PERSONAL.translationKey()),
                        button -> this.switchScope(DatabaseScope.PERSONAL)
                )
                .bounds(personalScopeRect.x(), personalScopeRect.y(), personalScopeRect.width(), personalScopeRect.height())
                .build());

        PersonalDatabaseLayout.Rect publicScopeRect = this.layout.publicScopeButtonRect();
        this.publicScopeButton = this.addRenderableWidget(Button.builder(
                        Component.translatable(DatabaseScope.PUBLIC.translationKey()),
                        button -> this.switchScope(DatabaseScope.PUBLIC)
                )
                .bounds(publicScopeRect.x(), publicScopeRect.y(), publicScopeRect.width(), publicScopeRect.height())
                .build());

        PersonalDatabaseLayout.Rect depositRect = this.layout.depositButtonRect();
        this.depositButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.infiniteinventory.deposit_all"), button -> {
                    this.closeContextMenu();
                    this.sortDropdownExpanded = false;
                    this.pagePickerExpanded = false;
                    this.enhancementPanelExpanded = false;
                    String directTargetTabId = this.resolveSingleStoreTargetTabId();
                    if (directTargetTabId != null) {
                        PacketDistributor.sendToServer(new DepositAllPayload(this.menu.containerId, this.menu.viewState().sessionId(), directTargetTabId));
                    } else {
                        this.openTargetSelector(TargetSelectorMode.DEPOSIT_ALL, -1, -1, "");
                    }
                })
                .bounds(depositRect.x(), depositRect.y(), depositRect.width(), depositRect.height())
                .build());

        PersonalDatabaseLayout.Rect previousRect = this.layout.previousPageButtonRect();
        this.previousPageButton = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> this.changePage(-1))
                .bounds(previousRect.x(), previousRect.y(), previousRect.width(), previousRect.height())
                .build());

        PersonalDatabaseLayout.Rect nextRect = this.layout.nextPageButtonRect();
        this.nextPageButton = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> this.changePage(1))
                .bounds(nextRect.x(), nextRect.y(), nextRect.width(), nextRect.height())
                .build());

        this.accessoriesToggleButton = null;
        if (this.hasAccessorySlots() && this.layout.accessoryToggleRect().height() > 0) {
            PersonalDatabaseLayout.Rect accessoryToggleRect = this.layout.accessoryToggleRect();
            this.accessoriesToggleButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> this.toggleAccessoriesPanel())
                    .bounds(accessoryToggleRect.x(), accessoryToggleRect.y(), accessoryToggleRect.width(), accessoryToggleRect.height())
                    .build());
        }
        this.ensureManagementWidgets();
    }

    private void buildAdvancedSearchButtons() {
        this.advancedSearchToggleButtons.clear();
        this.advancedSearchWeightButtons.clear();
        if (this.layout == null) {
            return;
        }
        for (DatabaseSearchField field : DatabaseSearchField.values()) {
            PersonalDatabaseLayout.Rect rowRect = this.advancedSearchRowRect(field);
            Button toggleButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> this.toggleAdvancedSearchField(field))
                    .bounds(rowRect.x(), rowRect.y(), ADVANCED_SEARCH_TOGGLE_WIDTH, ADVANCED_SEARCH_ROW_HEIGHT)
                    .build());
            Button weightButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> this.cycleAdvancedSearchWeight(field))
                    .bounds(rowRect.right() - ADVANCED_SEARCH_WEIGHT_WIDTH, rowRect.y(), ADVANCED_SEARCH_WEIGHT_WIDTH, ADVANCED_SEARCH_ROW_HEIGHT)
                    .build());
            this.advancedSearchToggleButtons.put(field, toggleButton);
            this.advancedSearchWeightButtons.put(field, weightButton);
        }
    }

    private void buildEnhancementButtons() {
        this.enhancementToggleButtons.clear();
        if (this.layout == null) {
            return;
        }
        for (DatabaseEnhancementOption option : DatabaseEnhancementOption.orderedValues()) {
            PersonalDatabaseLayout.Rect rowRect = this.enhancementRowRect(option);
            Button toggleButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> this.toggleEnhancementOption(option))
                    .bounds(rowRect.x(), rowRect.y(), ENHANCEMENT_TOGGLE_WIDTH, ENHANCEMENT_ROW_HEIGHT)
                    .build());
            this.enhancementToggleButtons.put(option, toggleButton);
        }
    }

    private void syncAdvancedSearchButtons(DatabaseQuery query) {
        DatabaseSearchConfig searchConfig = query.searchConfig();
        int enabledTextFieldCount = this.enabledTextFieldCount(searchConfig);
        for (DatabaseSearchField field : DatabaseSearchField.values()) {
            DatabaseSearchWeight weight = searchConfig.weightFor(field);
            Button toggleButton = this.advancedSearchToggleButtons.get(field);
            if (toggleButton != null) {
                toggleButton.visible = this.advancedSearchExpanded;
                toggleButton.active = !field.isTextField() || weight == DatabaseSearchWeight.OFF || enabledTextFieldCount > 1;
                toggleButton.setMessage(Component.empty());
            }
            Button weightButton = this.advancedSearchWeightButtons.get(field);
            if (weightButton != null) {
                weightButton.visible = this.advancedSearchExpanded;
                weightButton.active = weight != DatabaseSearchWeight.OFF;
                weightButton.setMessage(Component.empty());
            }
        }
    }

    private void syncEnhancementButtons(DatabaseEnhancementConfig config) {
        for (DatabaseEnhancementOption option : DatabaseEnhancementOption.orderedValues()) {
            Button toggleButton = this.enhancementToggleButtons.get(option);
            if (toggleButton != null) {
                toggleButton.visible = this.enhancementPanelExpanded;
                toggleButton.active = true;
                toggleButton.setMessage(Component.empty());
            }
        }
    }

    private void toggleAdvancedSearchField(DatabaseSearchField field) {
        DatabaseQuery currentQuery = this.menu.viewState().query();
        DatabaseSearchConfig searchConfig = currentQuery.searchConfig();
        DatabaseSearchWeight currentWeight = searchConfig.weightFor(field);
        DatabaseSearchWeight nextWeight = currentWeight == DatabaseSearchWeight.OFF ? field.defaultWeight() : DatabaseSearchWeight.OFF;
        if (field.isTextField() && currentWeight != DatabaseSearchWeight.OFF && this.enabledTextFieldCount(searchConfig) <= 1) {
            return;
        }
        this.sendSearchConfig(currentQuery, searchConfig.withWeight(field, nextWeight));
    }

    private void cycleAdvancedSearchWeight(DatabaseSearchField field) {
        DatabaseQuery currentQuery = this.menu.viewState().query();
        DatabaseSearchConfig searchConfig = currentQuery.searchConfig();
        DatabaseSearchWeight currentWeight = searchConfig.weightFor(field);
        if (currentWeight == DatabaseSearchWeight.OFF) {
            return;
        }
        this.sendSearchConfig(currentQuery, searchConfig.withWeight(field, this.nextWeight(currentWeight)));
    }

    private void toggleEnhancementOption(DatabaseEnhancementOption option) {
        DatabaseEnhancementConfig currentConfig = this.menu.viewState().enhancementConfig();
        this.sendEnhancementConfig(currentConfig.withOption(option, !currentConfig.isEnabled(option)));
    }

    private void sendSearchConfig(DatabaseQuery currentQuery, DatabaseSearchConfig newSearchConfig) {
        if (currentQuery.searchConfig().equals(newSearchConfig)) {
            return;
        }
        this.sendQuery(currentQuery.withSearchConfig(newSearchConfig));
    }

    private void sendEnhancementConfig(DatabaseEnhancementConfig newConfig) {
        DatabaseEnhancementConfig currentConfig = this.menu.viewState().enhancementConfig();
        String autoStoreTargetTabId = this.menu.viewState().autoStoreTargetTabId();
        if (currentConfig.equals(newConfig)) {
            return;
        }
        PacketDistributor.sendToServer(new DatabaseEnhancementPayload(
                this.menu.containerId,
                this.menu.viewState().sessionId(),
                newConfig,
                autoStoreTargetTabId
        ));
    }

    private DatabaseSearchWeight nextWeight(DatabaseSearchWeight currentWeight) {
        return switch (currentWeight) {
            case OFF -> DatabaseSearchWeight.LOW;
            case LOW -> DatabaseSearchWeight.MEDIUM;
            case MEDIUM -> DatabaseSearchWeight.HIGH;
            case HIGH -> DatabaseSearchWeight.LOW;
        };
    }

    private int enabledTextFieldCount(DatabaseSearchConfig searchConfig) {
        int count = 0;
        for (DatabaseSearchField field : DatabaseSearchField.values()) {
            if (field.isTextField() && searchConfig.weightFor(field) != DatabaseSearchWeight.OFF) {
                count++;
            }
        }
        return count;
    }

    private void syncWidgetsFromState() {
        DatabaseViewState viewState = this.menu.viewState();
        DatabaseQuery query = viewState.query();
        DatabaseScope activeScope = query.scope();
        if (this.searchBox != null && !this.searchBox.isFocused() && !this.searchBox.getValue().equals(query.searchText())) {
            this.syncingSearchBox = true;
            this.searchBox.setValue(query.searchText());
            this.syncingSearchBox = false;
        }
        if (this.advancedSearchButton != null) {
            this.advancedSearchButton.setMessage(Component.translatable("screen.infiniteinventory.search_advanced_button"));
        }
        if (this.enhancementButton != null) {
            this.enhancementButton.setMessage(Component.translatable("screen.infiniteinventory.enhancement_button"));
        }
        if (this.viewSelectorButton != null) {
            this.viewSelectorButton.setMessage(Component.translatable("screen.infiniteinventory.visible_tabs_button"));
        }
        if (this.tabManagementButton != null) {
            this.tabManagementButton.setMessage(Component.translatable("screen.infiniteinventory.tab_management_button"));
        }
        if (this.sortButton != null) {
            this.sortButton.setMessage(Component.translatable(query.sortOption().translationKey()));
        }
        if (this.previousPageButton != null) {
            this.previousPageButton.active = query.pageIndex() > 0;
        }
        if (this.nextPageButton != null) {
            this.nextPageButton.active = query.pageIndex() + 1 < viewState.totalPages();
        }
        if (this.depositButton != null) {
            this.depositButton.active = this.minecraft != null && this.minecraft.player != null;
        }
        if (this.personalScopeButton != null) {
            this.personalScopeButton.active = activeScope != DatabaseScope.PERSONAL;
        }
        if (this.publicScopeButton != null) {
            this.publicScopeButton.active = activeScope != DatabaseScope.PUBLIC;
        }
        if (this.accessoriesToggleButton != null) {
            this.accessoriesToggleButton.visible = this.hasAccessorySlots() && this.layout != null && this.layout.accessoryToggleRect().height() > 0;
            this.accessoriesToggleButton.active = this.hasAccessorySlots();
            this.accessoriesToggleButton.setMessage(Component.translatable(
                    this.accessoriesExpanded
                            ? "screen.infiniteinventory.accessories_toggle.collapse"
                            : "screen.infiniteinventory.accessories_toggle.expand"
            ));
        }
        this.syncAdvancedSearchButtons(query);
        this.syncEnhancementButtons(viewState.enhancementConfig());
        this.syncManagementWidgets();
        if (!this.menu.getCarried().isEmpty()) {
            this.pagePickerExpanded = false;
            this.enhancementPanelExpanded = false;
            this.closeContextMenu();
            return;
        }
        this.validateContextMenu(viewState);
    }

    private void ensureLayoutQuerySynced() {
        if (this.layout == null) {
            return;
        }
        DatabaseQuery currentQuery = this.menu.viewState().query();
        if (this.pendingLayoutQuery != null) {
            boolean allSynced = true;
            for (String visibleTabId : currentQuery.visibleTabIds()) {
                if (currentQuery.pageSizeFor(visibleTabId) != this.pendingLayoutQuery.pageSizeFor(visibleTabId)) {
                    allSynced = false;
                    break;
                }
            }
            if (allSynced) {
                this.pendingLayoutQuery = null;
            } else {
                return;
            }
        }
        Map<String, Integer> nextPageIndexes = new java.util.LinkedHashMap<>(currentQuery.pageIndexes());
        Map<String, Integer> nextPageSizes = new java.util.LinkedHashMap<>(currentQuery.pageSizes());
        boolean changed = false;
        for (int panelIndex = 0; panelIndex < currentQuery.visibleTabIds().size(); panelIndex++) {
            String visibleTabId = currentQuery.visibleTabIds().get(panelIndex);
            int targetPageSize = Math.max(1, this.layout.visibleDatabaseSlotCount(panelIndex));
            int currentPageSize = currentQuery.pageSizeFor(visibleTabId);
            if (currentPageSize == targetPageSize) {
                continue;
            }
            long firstVisibleEntryIndex = (long) currentQuery.pageIndexFor(visibleTabId) * Math.max(1, currentPageSize);
            int adjustedPageIndex = (int) Math.min(Integer.MAX_VALUE, firstVisibleEntryIndex / targetPageSize);
            nextPageIndexes.put(visibleTabId, adjustedPageIndex);
            nextPageSizes.put(visibleTabId, targetPageSize);
            changed = true;
        }
        if (!changed) {
            return;
        }
        DatabaseQuery adjustedQuery = currentQuery.withPanelLayout(nextPageIndexes, nextPageSizes);
        this.pendingLayoutQuery = adjustedQuery;
        this.prepareForServerQuery();
        this.dispatchQuery(adjustedQuery);
    }

    private void onSearchChanged(String value) {
        if (this.syncingSearchBox) {
            return;
        }
        DatabaseQuery currentQuery = this.menu.viewState().query();
        if (currentQuery.searchText().equals(value)) {
            return;
        }
        this.sendQuery(currentQuery.withSearchText(value));
    }

    private void changePage(int delta) {
        DatabaseViewState viewState = this.menu.viewState();
        int nextPage = Math.max(0, Math.min(viewState.totalPages() - 1, viewState.query().pageIndex() + delta));
        if (nextPage == viewState.query().pageIndex()) {
            return;
        }
        this.sendQuery(viewState.query().withPageIndex(nextPage));
    }

    private void sendQuery(DatabaseQuery query) {
        if (query.equals(this.menu.viewState().query())) {
            return;
        }
        this.pendingLayoutQuery = null;
        this.prepareForServerQuery();
        this.dispatchQuery(query);
    }

    private void dispatchQuery(DatabaseQuery query) {
        PacketDistributor.sendToServer(new DatabaseQueryPayload(this.menu.containerId, this.menu.viewState().sessionId(), query));
    }

    private void sendDatabaseClick(int panelIndex, int slotIndex, DatabaseClickAction action, String targetTabId) {
        PacketDistributor.sendToServer(new DatabaseClickPayload(
                this.menu.containerId,
                this.menu.viewState().sessionId(),
                panelIndex,
                slotIndex,
                action,
                targetTabId == null ? "" : targetTabId
        ));
    }

    private void prepareForServerQuery() {
        this.closeContextMenu();
        this.sortDropdownExpanded = false;
        this.pagePickerExpanded = false;
        this.enhancementPanelExpanded = false;
        this.viewSelectorExpanded = false;
        this.moreTabsExpanded = false;
        this.targetSelectorExpanded = false;
    }

    private void switchScope(DatabaseScope scope) {
        DatabaseScope normalizedScope = DatabaseScope.normalize(scope);
        if (normalizedScope == this.menu.viewState().query().scope()) {
            return;
        }
        this.sendQuery(this.menu.viewState().queryForScope(normalizedScope));
    }

    private void toggleAccessoriesPanel() {
        if (!this.hasAccessorySlots()) {
            return;
        }
        this.accessoriesExpanded = !this.accessoriesExpanded;
        if (!this.accessoriesExpanded) {
            this.accessoryScrollRow = 0;
        }
        this.closeContextMenu();
        this.sortDropdownExpanded = false;
        this.pagePickerExpanded = false;
        this.enhancementPanelExpanded = false;
        this.rebuildLayout();
    }

    private boolean scrollAccessories(int deltaRows) {
        if (deltaRows == 0 || this.layout == null || !this.accessoriesExpanded) {
            return false;
        }
        int nextScrollRow = Mth.clamp(this.accessoryScrollRow + deltaRows, 0, this.layout.accessoryMaxScrollRow());
        if (nextScrollRow == this.accessoryScrollRow) {
            return false;
        }
        this.accessoryScrollRow = nextScrollRow;
        this.rebuildLayout();
        return true;
    }

    private void renderTabs(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.layout == null) {
            return;
        }
        List<DatabaseTab> visibleTabs = this.visibleTopTabs();
        DatabaseQuery query = this.menu.viewState().query();
        for (int index = 0; index < visibleTabs.size(); index++) {
            DatabaseTab tab = visibleTabs.get(index);
            PersonalDatabaseLayout.Rect tabRect = this.topTabRect(index, visibleTabs.size(), !this.hiddenTopTabs().isEmpty());
            boolean hovered = tabRect.contains(mouseX, mouseY);
            boolean selected = query.visibleTabIds().contains(tab.id());
            VanillaWidgetRenderer.renderTab(guiGraphics, tabRect, selected, hovered);
            guiGraphics.renderItem(this.tabIcon(tab), tabRect.x() + TAB_ICON_LEFT_PADDING, tabRect.y() + 4);
            int labelX = tabRect.x() + TAB_ICON_LEFT_PADDING + TAB_ICON_SIZE + TAB_TEXT_GAP;
            int labelWidth = Math.max(0, tabRect.right() - 4 - labelX);
            int color = query.focusedTabId().equals(tab.id()) ? 0x404040 : 0xFFFFFF;
            guiGraphics.drawString(
                    this.font,
                    this.truncateToWidth(this.tabLabel(tab).getString(), labelWidth),
                    labelX,
                    tabRect.y() + 8,
                    color,
                    true
            );
        }
        if (!this.hiddenTopTabs().isEmpty()) {
            PersonalDatabaseLayout.Rect moreRect = this.moreTabsButtonRect();
            boolean hovered = moreRect.contains(mouseX, mouseY);
            VanillaWidgetRenderer.renderTab(guiGraphics, moreRect, this.moreTabsExpanded, hovered);
            this.drawCenteredShadow(guiGraphics, Component.translatable("screen.infiniteinventory.tab.more"), moreRect.x(), moreRect.right(), moreRect.y() + 8, hovered ? 0x404040 : 0xFFFFFF);
        }
    }

    private void renderDatabaseSlots(GuiGraphics guiGraphics) {
        if (this.layout == null) {
            return;
        }
        for (int panelIndex = 0; panelIndex < this.currentPanels().size(); panelIndex++) {
            int visibleSlotCount = this.layout.visibleDatabaseSlotCount(panelIndex);
            for (int slotIndex = 0; slotIndex < visibleSlotCount; slotIndex++) {
                PersonalDatabaseLayout.Rect slotRect = this.layout.visibleDatabaseSlotBounds(panelIndex, slotIndex);
                VanillaWidgetRenderer.renderSlot(guiGraphics, slotRect.x(), slotRect.y());
            }
        }
    }

    private void renderAccessoriesPanel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.layout == null || this.layout.accessoriesPanelRect().height() <= 0) {
            return;
        }
        PersonalDatabaseLayout.Rect panelRect = this.layout.accessoriesPanelRect();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 220.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.infiniteinventory.accessories_panel"),
                panelRect.x() + PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING,
                panelRect.y() + PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING,
                OVERLAY_TEXT_COLOR,
                true
        );
        guiGraphics.fill(
                panelRect.x() + PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING,
                panelRect.y() + PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING + PersonalDatabaseLayout.ACCESSORY_DRAWER_TITLE_HEIGHT + 1,
                panelRect.right() - PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING,
                panelRect.y() + PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING + PersonalDatabaseLayout.ACCESSORY_DRAWER_TITLE_HEIGHT + 2,
                0x70A89E8C
        );
        guiGraphics.pose().popPose();
    }

    private void renderAccessorySlotHover(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.AccessorySlotLayout hoveredSlot = this.findHoveredAccessorySlot(mouseX, mouseY);
        if (hoveredSlot == null) {
            return;
        }
        PersonalDatabaseLayout.Rect slotRect = hoveredSlot.slotRect();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 230.0F);
        guiGraphics.fill(slotRect.x(), slotRect.y(), slotRect.x() + 16, slotRect.y() + 16, 0x52000000);
        guiGraphics.pose().popPose();
    }

    private void renderDatabaseEntries(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.layout == null) {
            return;
        }
        for (int panelIndex = 0; panelIndex < this.currentPanels().size(); panelIndex++) {
            DatabasePanelView panel = this.currentPanels().get(panelIndex);
            PersonalDatabaseLayout.DatabaseViewportLayout viewportLayout = this.layout.databaseViewportLayout(panelIndex);
            if (viewportLayout.panelRect().contains(mouseX, mouseY)) {
                guiGraphics.fill(viewportLayout.panelRect().x(), viewportLayout.panelRect().y(), viewportLayout.panelRect().right(), viewportLayout.panelRect().bottom(), 0x12000000);
            }
            for (int slotIndex = 0; slotIndex < this.layout.visibleDatabaseSlotCount(panelIndex); slotIndex++) {
                PersonalDatabaseLayout.Rect slotRect = this.layout.visibleDatabaseSlotBounds(panelIndex, slotIndex);
                if (slotRect.contains(mouseX, mouseY)) {
                    VanillaWidgetRenderer.renderSlotHighlight(guiGraphics, slotRect);
                }
                if (slotIndex < panel.entries().size()) {
                    VisibleDatabaseEntry entry = panel.entries().get(slotIndex);
                    ItemStack stack = entry.stack();
                    int itemX = slotRect.x() + (PersonalDatabaseLayout.DATABASE_SLOT_SIZE - 16) / 2;
                    int itemY = slotRect.y() + (PersonalDatabaseLayout.DATABASE_SLOT_SIZE - 16) / 2;
                    guiGraphics.renderItem(stack, itemX, itemY);
                    guiGraphics.renderItemDecorations(this.font, stack, itemX, itemY, CompactNumberFormatter.format(entry.amount()));
                }
            }
        }
    }

    private void renderEmptyState(GuiGraphics guiGraphics) {
        if (this.layout == null) {
            return;
        }
        for (int panelIndex = 0; panelIndex < this.currentPanels().size(); panelIndex++) {
            DatabasePanelView panel = this.currentPanels().get(panelIndex);
            if (!panel.entries().isEmpty()) {
                continue;
            }
            DatabaseQuery query = this.menu.viewState().query();
            Component message = Component.translatable(query.searchText().isEmpty()
                    ? query.scope().emptyTranslationKey()
                    : "screen.infiniteinventory.no_results");
            PersonalDatabaseLayout.Rect gridRect = this.layout.databaseViewportLayout(panelIndex).gridRect();
            int y = gridRect.y() + Math.max(0, gridRect.height() / 2 - 4);
            this.drawCenteredShadow(guiGraphics, message, gridRect.x(), gridRect.right(), y, 0x7A7A7A);
        }
    }

    private void renderFrameText(GuiGraphics guiGraphics) {
        if (this.layout == null) {
            return;
        }
        DatabaseViewState viewState = this.menu.viewState();
        guiGraphics.drawString(this.font, this.title, this.layout.titleRect().x(), this.layout.titleRect().y() + 6, 0x404040, true);

        for (int panelIndex = 0; panelIndex < this.currentPanels().size(); panelIndex++) {
            DatabasePanelView panel = this.currentPanels().get(panelIndex);
            PersonalDatabaseLayout.DatabaseViewportLayout viewportLayout = this.layout.databaseViewportLayout(panelIndex);
            guiGraphics.drawString(
                    this.font,
                    this.tabLabel(panel.tab()),
                    viewportLayout.headerRect().x(),
                    viewportLayout.headerRect().y() + 4,
                    viewState.query().focusedTabId().equals(panel.tab().id()) ? 0x404040 : OVERLAY_TEXT_COLOR,
                    true
            );
        }

        Component footerStats = Component.translatable(
                "screen.infiniteinventory.footer_stats",
                Component.translatable(viewState.query().scope().translationKey()),
                viewState.totalEntries(),
                CompactNumberFormatter.format(viewState.totalItems())
        );
        guiGraphics.drawString(
                this.font,
                footerStats,
                this.layout.databaseFooterRect().x(),
                this.layout.databaseFooterRect().y() + 6,
                0x404040,
                true
        );

        Component pageLabel = Component.translatable("screen.infiniteinventory.page_compact", viewState.query().pageIndex() + 1, viewState.totalPages());
        this.drawCenteredShadow(
                guiGraphics,
                pageLabel,
                this.layout.pageLabelRect().x(),
                this.layout.pageLabelRect().right(),
                this.layout.pageLabelRect().y() + 6,
                0x404040
        );
    }

    private void renderSearchHint(GuiGraphics guiGraphics) {
        if (this.layout == null || this.searchBox == null || this.searchBox.isFocused() || !this.searchBox.getValue().isEmpty()) {
            return;
        }
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.infiniteinventory.search_hint"),
                this.layout.searchFieldRect().x() + SEARCH_TEXT_LEFT_PADDING,
                this.layout.searchFieldRect().y() + 6,
                0x777777,
                false
        );
    }

    private void renderAdvancedSearchPanel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.Rect panelRect = this.advancedSearchPanelRect();
        if (panelRect.width() <= 0 || panelRect.height() <= 0) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 240.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.infiniteinventory.search_advanced_title"),
                panelRect.x() + ADVANCED_SEARCH_PANEL_PADDING,
                panelRect.y() + ADVANCED_SEARCH_PANEL_PADDING,
                OVERLAY_TEXT_COLOR,
                true
        );
        guiGraphics.fill(
                panelRect.x() + ADVANCED_SEARCH_PANEL_PADDING,
                panelRect.y() + ADVANCED_SEARCH_PANEL_PADDING + ADVANCED_SEARCH_TITLE_HEIGHT - 2,
                panelRect.right() - ADVANCED_SEARCH_PANEL_PADDING,
                panelRect.y() + ADVANCED_SEARCH_PANEL_PADDING + ADVANCED_SEARCH_TITLE_HEIGHT - 1,
                0x70A89E8C
        );
        DatabaseSearchConfig searchConfig = this.menu.viewState().query().searchConfig();
        for (DatabaseSearchField field : DatabaseSearchField.values()) {
            PersonalDatabaseLayout.Rect rowRect = this.advancedSearchRowRect(field);
            PersonalDatabaseLayout.Rect toggleRect = new PersonalDatabaseLayout.Rect(rowRect.x(), rowRect.y(), ADVANCED_SEARCH_TOGGLE_WIDTH, ADVANCED_SEARCH_ROW_HEIGHT);
            PersonalDatabaseLayout.Rect weightRect = new PersonalDatabaseLayout.Rect(
                    rowRect.right() - ADVANCED_SEARCH_WEIGHT_WIDTH,
                    rowRect.y(),
                    ADVANCED_SEARCH_WEIGHT_WIDTH,
                    ADVANCED_SEARCH_ROW_HEIGHT
            );
            DatabaseSearchWeight weight = searchConfig.weightFor(field);
            boolean enabled = weight != DatabaseSearchWeight.OFF;
            VanillaWidgetRenderer.renderOverlayRow(guiGraphics, rowRect, rowRect.contains(mouseX, mouseY), false);
            VanillaWidgetRenderer.renderOverlayChip(
                    guiGraphics,
                    toggleRect,
                    toggleRect.contains(mouseX, mouseY),
                    enabled,
                    this.isAdvancedToggleClickable(field, searchConfig)
            );
            VanillaWidgetRenderer.renderOverlayChip(
                    guiGraphics,
                    weightRect,
                    weightRect.contains(mouseX, mouseY),
                    enabled,
                    enabled
            );
            this.drawCenteredShadow(
                    guiGraphics,
                    Component.literal(enabled ? "ON" : "OFF"),
                    toggleRect.x(),
                    toggleRect.right(),
                    toggleRect.y() + 6,
                    enabled ? OVERLAY_ACCENT_TEXT_COLOR : OVERLAY_MUTED_TEXT_COLOR
            );
            int labelX = rowRect.x() + ADVANCED_SEARCH_TOGGLE_WIDTH + 6;
            int labelWidth = Math.max(0, rowRect.width() - ADVANCED_SEARCH_TOGGLE_WIDTH - ADVANCED_SEARCH_WEIGHT_WIDTH - 12);
            guiGraphics.drawString(
                    this.font,
                    this.truncateToWidth(Component.translatable(field.translationKey()).getString(), labelWidth),
                    labelX,
                    rowRect.y() + 6,
                    enabled ? OVERLAY_TEXT_COLOR : OVERLAY_MUTED_TEXT_COLOR,
                    true
            );
            this.drawCenteredShadow(
                    guiGraphics,
                    enabled ? Component.translatable(weight.translationKey()) : Component.literal("OFF"),
                    weightRect.x(),
                    weightRect.right(),
                    weightRect.y() + 6,
                    enabled ? OVERLAY_ACCENT_TEXT_COLOR : OVERLAY_MUTED_TEXT_COLOR
            );
        }
        guiGraphics.pose().popPose();
    }

    private void renderEnhancementPanel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.Rect panelRect = this.enhancementPanelRect();
        if (panelRect.width() <= 0 || panelRect.height() <= 0) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 245.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.infiniteinventory.enhancement_title"),
                panelRect.x() + ENHANCEMENT_PANEL_PADDING,
                panelRect.y() + ENHANCEMENT_PANEL_PADDING,
                OVERLAY_TEXT_COLOR,
                true
        );
        guiGraphics.fill(
                panelRect.x() + ENHANCEMENT_PANEL_PADDING,
                panelRect.y() + ENHANCEMENT_PANEL_PADDING + ENHANCEMENT_TITLE_HEIGHT - 2,
                panelRect.right() - ENHANCEMENT_PANEL_PADDING,
                panelRect.y() + ENHANCEMENT_PANEL_PADDING + ENHANCEMENT_TITLE_HEIGHT - 1,
                0x70A89E8C
        );
        DatabaseEnhancementConfig config = this.menu.viewState().enhancementConfig();
        for (DatabaseEnhancementOption option : DatabaseEnhancementOption.orderedValues()) {
            PersonalDatabaseLayout.Rect rowRect = this.enhancementRowRect(option);
            PersonalDatabaseLayout.Rect toggleRect = new PersonalDatabaseLayout.Rect(rowRect.x(), rowRect.y(), ENHANCEMENT_TOGGLE_WIDTH, ENHANCEMENT_ROW_HEIGHT);
            boolean enabled = config.isEnabled(option);
            VanillaWidgetRenderer.renderOverlayRow(guiGraphics, rowRect, rowRect.contains(mouseX, mouseY), false);
            VanillaWidgetRenderer.renderOverlayChip(
                    guiGraphics,
                    toggleRect,
                    toggleRect.contains(mouseX, mouseY),
                    enabled,
                    true
            );
            this.drawCenteredShadow(
                    guiGraphics,
                    Component.literal(enabled ? "ON" : "OFF"),
                    toggleRect.x(),
                    toggleRect.right(),
                    toggleRect.y() + 6,
                    enabled ? OVERLAY_ACCENT_TEXT_COLOR : OVERLAY_MUTED_TEXT_COLOR
            );
            int labelX = rowRect.x() + ENHANCEMENT_TOGGLE_WIDTH + 6;
            int labelWidth = Math.max(0, rowRect.width() - ENHANCEMENT_TOGGLE_WIDTH - 8);
            guiGraphics.drawString(
                    this.font,
                    this.truncateToWidth(Component.translatable(option.translationKey()).getString(), labelWidth),
                    labelX,
                    rowRect.y() + 6,
                    enabled ? OVERLAY_TEXT_COLOR : OVERLAY_MUTED_TEXT_COLOR,
                    true
            );
        }
        PersonalDatabaseLayout.Rect autoStoreRowRect = this.enhancementAutoStoreRowRect();
        boolean hovered = autoStoreRowRect.contains(mouseX, mouseY);
        VanillaWidgetRenderer.renderOverlayRow(guiGraphics, autoStoreRowRect, hovered, false);
        guiGraphics.drawString(
                this.font,
                this.truncateToWidth(Component.translatable("screen.infiniteinventory.enhancement.auto_store_target").getString(), Math.max(0, autoStoreRowRect.width() - 70)),
                autoStoreRowRect.x() + 6,
                autoStoreRowRect.y() + 6,
                OVERLAY_TEXT_COLOR,
                true
        );
        this.drawCenteredShadow(
                guiGraphics,
                this.tabLabel(this.findTab(this.menu.viewState().autoStoreTargetTabId())),
                autoStoreRowRect.right() - 62,
                autoStoreRowRect.right() - 6,
                autoStoreRowRect.y() + 6,
                OVERLAY_ACCENT_TEXT_COLOR
        );
        guiGraphics.pose().popPose();
    }

    private void renderSortDropdown(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.Rect dropdownRect = this.sortDropdownRect();
        if (dropdownRect == null) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 250.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, dropdownRect);
        DatabaseSortOption currentSort = this.menu.viewState().query().sortOption();
        List<DatabaseSortOption> sortOptions = DatabaseSortOption.orderedValues();
        for (int index = 0; index < sortOptions.size(); index++) {
            DatabaseSortOption option = sortOptions.get(index);
            PersonalDatabaseLayout.Rect rowRect = new PersonalDatabaseLayout.Rect(
                    dropdownRect.x() + 2,
                    dropdownRect.y() + index * DROPDOWN_ROW_HEIGHT + 2,
                    dropdownRect.width() - 4,
                    DROPDOWN_ROW_HEIGHT - 1
            );
            boolean hovered = rowRect.contains(mouseX, mouseY);
            boolean selected = currentSort == option;
            VanillaWidgetRenderer.renderOverlayRow(guiGraphics, rowRect, hovered, selected);
            Component label = selected
                    ? Component.translatable(option.translationKey()).withStyle(ChatFormatting.GOLD)
                    : Component.translatable(option.translationKey());
            guiGraphics.drawString(this.font, label, rowRect.x() + 6, rowRect.y() + 5, selected ? OVERLAY_ACCENT_TEXT_COLOR : OVERLAY_TEXT_COLOR, true);
        }
        guiGraphics.pose().popPose();
    }

    private void renderPagePicker(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.Rect pickerRect = this.pagePickerRect();
        if (pickerRect == null) {
            return;
        }
        List<DatabasePagePickerModel.PageOption> options = this.pagePickerOptions();
        int currentPageIndex = this.menu.viewState().query().pageIndex();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 255.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, pickerRect);
        for (int index = 0; index < options.size(); index++) {
            DatabasePagePickerModel.PageOption option = options.get(index);
            PersonalDatabaseLayout.Rect rowRect = new PersonalDatabaseLayout.Rect(
                    pickerRect.x() + 2,
                    pickerRect.y() + index * PAGE_PICKER_ROW_HEIGHT + 2,
                    pickerRect.width() - 4,
                    PAGE_PICKER_ROW_HEIGHT - 1
            );
            boolean hovered = rowRect.contains(mouseX, mouseY);
            boolean selected = option.pageIndex() == currentPageIndex;
            VanillaWidgetRenderer.renderOverlayRow(guiGraphics, rowRect, hovered, selected);
            Component label = selected
                    ? this.pagePickerLabel(option).copy().withStyle(ChatFormatting.GOLD)
                    : this.pagePickerLabel(option);
            guiGraphics.drawString(this.font, label, rowRect.x() + 6, rowRect.y() + 5, selected ? OVERLAY_ACCENT_TEXT_COLOR : OVERLAY_TEXT_COLOR, true);
        }
        guiGraphics.pose().popPose();
    }

    private void renderContextMenu(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!this.contextMenuExpanded) {
            return;
        }
        int menuWidth = this.contextMenuWidth();
        PersonalDatabaseLayout.Rect menuRect = new PersonalDatabaseLayout.Rect(
                this.contextMenuX,
                this.contextMenuY,
                menuWidth,
                CONTEXT_MENU_ACTIONS.length * CONTEXT_MENU_ROW_HEIGHT
        );
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 260.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, menuRect);
        for (int index = 0; index < CONTEXT_MENU_ACTIONS.length; index++) {
            PersonalDatabaseLayout.Rect rowRect = new PersonalDatabaseLayout.Rect(
                    menuRect.x() + 2,
                    menuRect.y() + index * CONTEXT_MENU_ROW_HEIGHT + 2,
                    menuRect.width() - 4,
                    CONTEXT_MENU_ROW_HEIGHT - 1
            );
            boolean hovered = rowRect.contains(mouseX, mouseY);
            VanillaWidgetRenderer.renderOverlayRow(guiGraphics, rowRect, hovered, false);
            guiGraphics.drawString(this.font, this.contextMenuLabel(CONTEXT_MENU_ACTIONS[index]), rowRect.x() + 6, rowRect.y() + 5, OVERLAY_TEXT_COLOR, true);
        }
        guiGraphics.pose().popPose();
    }

    private boolean handleTabClick(double mouseX, double mouseY) {
        if (this.layout == null) {
            return false;
        }
        List<DatabaseTab> visibleTabs = this.visibleTopTabs();
        for (int index = 0; index < visibleTabs.size(); index++) {
            DatabaseTab tab = visibleTabs.get(index);
            PersonalDatabaseLayout.Rect tabRect = this.topTabRect(index, visibleTabs.size(), !this.hiddenTopTabs().isEmpty());
            if (!tabRect.contains(mouseX, mouseY)) {
                continue;
            }
            DatabaseQuery currentQuery = this.menu.viewState().query();
            this.sendQuery(currentQuery.withSingleVisibleTab(tab.id()).withFocusedTabId(tab.id()));
            return true;
        }
        if (!this.hiddenTopTabs().isEmpty() && this.moreTabsButtonRect().contains(mouseX, mouseY)) {
            this.moreTabsExpanded = !this.moreTabsExpanded;
            return true;
        }
        return false;
    }

    private boolean handleSortDropdownClick(double mouseX, double mouseY) {
        PersonalDatabaseLayout.Rect dropdownRect = this.sortDropdownRect();
        if (dropdownRect == null) {
            return false;
        }
        List<DatabaseSortOption> sortOptions = DatabaseSortOption.orderedValues();
        for (int index = 0; index < sortOptions.size(); index++) {
            int rowY = dropdownRect.y() + index * DROPDOWN_ROW_HEIGHT;
            if (mouseX < dropdownRect.x() || mouseX >= dropdownRect.right() || mouseY < rowY || mouseY >= rowY + DROPDOWN_ROW_HEIGHT) {
                continue;
            }
            this.sendQuery(this.menu.viewState().query().withSortOption(sortOptions.get(index)));
            return true;
        }
        if (!this.layout.sortButtonRect().contains(mouseX, mouseY)) {
            this.sortDropdownExpanded = false;
        }
        return false;
    }

    private boolean handlePageLabelClick(double mouseX, double mouseY) {
        if (this.layout == null || !this.layout.pageLabelRect().contains(mouseX, mouseY)) {
            return false;
        }
        if (this.menu.viewState().totalPages() <= 1) {
            return true;
        }
        this.closeContextMenu();
        this.sortDropdownExpanded = false;
        this.advancedSearchExpanded = false;
        this.enhancementPanelExpanded = false;
        this.pagePickerExpanded = !this.pagePickerExpanded;
        return true;
    }

    private boolean handlePagePickerClick(double mouseX, double mouseY) {
        if (!this.pagePickerExpanded) {
            return false;
        }
        if (this.layout != null && this.layout.pageLabelRect().contains(mouseX, mouseY)) {
            this.pagePickerExpanded = false;
            return true;
        }
        PersonalDatabaseLayout.Rect pickerRect = this.pagePickerRect();
        if (pickerRect == null) {
            this.pagePickerExpanded = false;
            return false;
        }
        List<DatabasePagePickerModel.PageOption> options = this.pagePickerOptions();
        for (int index = 0; index < options.size(); index++) {
            int rowY = pickerRect.y() + index * PAGE_PICKER_ROW_HEIGHT;
            if (mouseX < pickerRect.x() || mouseX >= pickerRect.right() || mouseY < rowY || mouseY >= rowY + PAGE_PICKER_ROW_HEIGHT) {
                continue;
            }
            DatabasePagePickerModel.PageOption option = options.get(index);
            this.pagePickerExpanded = false;
            if (option.pageIndex() != this.menu.viewState().query().pageIndex()) {
                this.sendQuery(this.menu.viewState().query().withPageIndex(option.pageIndex()));
            }
            return true;
        }
        if (pickerRect.contains(mouseX, mouseY)) {
            return true;
        }
        this.pagePickerExpanded = false;
        return false;
    }

    private boolean handleContextMenuClick(double mouseX, double mouseY) {
        if (!this.contextMenuExpanded) {
            return false;
        }
        int menuWidth = this.contextMenuWidth();
        for (int index = 0; index < CONTEXT_MENU_ACTIONS.length; index++) {
            int rowY = this.contextMenuY + index * CONTEXT_MENU_ROW_HEIGHT;
            if (mouseX < this.contextMenuX || mouseX >= this.contextMenuX + menuWidth || mouseY < rowY || mouseY >= rowY + CONTEXT_MENU_ROW_HEIGHT) {
                continue;
            }
            this.sendDatabaseClick(this.contextMenuPanelIndex, this.contextMenuSlotIndex, CONTEXT_MENU_ACTIONS[index], "");
            this.closeContextMenu();
            return true;
        }
        if (this.isWithinContextMenu(mouseX, mouseY)) {
            return true;
        }
        this.closeContextMenu();
        return false;
    }

    private boolean handleDatabaseClick(double mouseX, double mouseY, int button) {
        DatabaseHitResult hitResult = this.findDatabaseSlot(mouseX, mouseY);
        int panelIndex = hitResult == null ? this.findDatabasePanel(mouseX, mouseY) : hitResult.panelIndex();
        boolean carryingStack = !this.menu.getCarried().isEmpty();
        if (panelIndex < 0) {
            return false;
        }
        DatabasePanelView panel = this.currentPanels().get(panelIndex);
        if (carryingStack) {
            DatabaseClickAction action;
            if (button == 0) {
                action = DatabaseClickAction.STORE_STACK;
            } else if (button == 1) {
                action = DatabaseClickAction.STORE_SINGLE;
            } else {
                return false;
            }
            this.closeContextMenu();
            this.sortDropdownExpanded = false;
            this.pagePickerExpanded = false;
            if (panel.tab().isAllTab()) {
                this.pendingTargetStoresSingle = action == DatabaseClickAction.STORE_SINGLE;
                this.openTargetSelector(TargetSelectorMode.CARRIED_STORE, panelIndex, -1, "");
                return true;
            }
            this.sendDatabaseClick(panelIndex, hitResult == null ? 0 : hitResult.slotIndex(), action, panel.tab().id());
            return true;
        }
        if (hitResult == null) {
            if (button == 0 && !this.menu.viewState().query().focusedTabId().equals(panel.tab().id())) {
                this.sendQuery(this.menu.viewState().query().withFocusedTabId(panel.tab().id()));
                return true;
            }
            return false;
        }
        if (hitResult.slotIndex() >= panel.entries().size()) {
            return false;
        }
        if (button == 0) {
            this.closeContextMenu();
            this.sortDropdownExpanded = false;
            this.pagePickerExpanded = false;
            this.enhancementPanelExpanded = false;
            DatabaseClickAction action = hasShiftDown()
                    ? DatabaseClickAction.TAKE_STACK_TO_INVENTORY
                    : DatabaseClickAction.TAKE_SINGLE;
            this.sendDatabaseClick(panelIndex, hitResult.slotIndex(), action, "");
            return true;
        }
        if (button == 1) {
            this.sortDropdownExpanded = false;
            this.pagePickerExpanded = false;
            this.enhancementPanelExpanded = false;
            this.openContextMenu(panelIndex, hitResult.slotIndex());
            return true;
        }
        return false;
    }

    private void renderScreenTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.contextMenuExpanded
                || this.sortDropdownExpanded
                || this.pagePickerExpanded
                || this.advancedSearchExpanded
                || this.enhancementPanelExpanded
                || this.viewSelectorExpanded
                || this.moreTabsExpanded
                || this.tabManagementExpanded
                || this.iconPickerExpanded
                || this.targetSelectorExpanded) {
            return;
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        PersonalDatabaseLayout.AccessorySlotLayout accessorySlotLayout = this.findHoveredAccessorySlot(mouseX, mouseY);
        if (accessorySlotLayout != null) {
            ItemStack hoveredStack = accessorySlotLayout.slotIndex() >= 0 && accessorySlotLayout.slotIndex() < this.menu.slots.size()
                    ? this.menu.getSlot(accessorySlotLayout.slotIndex()).getItem()
                    : ItemStack.EMPTY;
            if (hoveredStack.isEmpty()) {
                Component slotLabel = I18n.exists(accessorySlotLayout.group().translationKey())
                        ? Component.translatable(accessorySlotLayout.group().translationKey())
                        : Component.literal(accessorySlotLayout.group().slotName());
                guiGraphics.renderTooltip(
                        this.font,
                        List.of(slotLabel),
                        ItemStack.EMPTY.getTooltipImage(),
                        mouseX,
                        mouseY
                );
            }
            return;
        }
        DatabaseHitResult hitResult = this.findDatabaseSlot(mouseX, mouseY);
        if (hitResult != null && hitResult.panelIndex() < this.currentPanels().size()) {
            DatabasePanelView panel = this.currentPanels().get(hitResult.panelIndex());
            if (hitResult.slotIndex() < panel.entries().size()) {
                VisibleDatabaseEntry entry = panel.entries().get(hitResult.slotIndex());
                List<Component> tooltip = new ArrayList<>(this.getTooltipFromContainerItem(entry.stack()));
                tooltip.add(Component.translatable("screen.infiniteinventory.tooltip.amount", CompactNumberFormatter.format(entry.amount())).withStyle(ChatFormatting.GRAY));
                tooltip.add(this.tabLabel(this.findTab(entry.tabId())).copy().withStyle(ChatFormatting.BLUE));
                tooltip.add(Component.literal(entry.registryName()).withStyle(ChatFormatting.DARK_GRAY));
                guiGraphics.renderTooltip(this.font, tooltip, entry.stack().getTooltipImage(), mouseX, mouseY);
                return;
            }
        }
        DatabaseTab hoveredTab = this.findHoveredTab(mouseX, mouseY);
        if (hoveredTab != null) {
            guiGraphics.renderTooltip(this.font, List.of(this.tabLabel(hoveredTab)), ItemStack.EMPTY.getTooltipImage(), mouseX, mouseY);
        }
    }

    private void renderToolbarOverlays(GuiGraphics guiGraphics) {
        if (this.layout == null) {
            return;
        }
        PersonalDatabaseLayout.Rect searchRect = this.layout.searchFieldRect();
        int iconX = searchRect.x() + 6;
        int iconY = searchRect.y() + (searchRect.height() - SEARCH_ICON_SIZE) / 2;
        VanillaWidgetRenderer.renderSearchGlyph(guiGraphics, iconX, iconY, 0xFF6D6D6D);

        PersonalDatabaseLayout.Rect sortRect = this.layout.sortButtonRect();
        VanillaWidgetRenderer.renderDropdownIndicator(
                guiGraphics,
                sortRect.right() - 10,
                sortRect.y() + sortRect.height() / 2,
                0xFF3F3F3F
        );

        PersonalDatabaseLayout.Rect pageRect = this.layout.pageLabelRect();
        VanillaWidgetRenderer.renderDropdownIndicator(
                guiGraphics,
                pageRect.right() - 10,
                pageRect.y() + pageRect.height() / 2,
                0xFF3F3F3F
        );

        if (this.advancedSearchButton != null && this.layout.advancedSearchButtonRect().width() > 0) {
            PersonalDatabaseLayout.Rect advancedRect = this.layout.advancedSearchButtonRect();
            VanillaWidgetRenderer.renderDropdownIndicator(
                    guiGraphics,
                    advancedRect.right() - 10,
                    advancedRect.y() + advancedRect.height() / 2,
                    0xFF3F3F3F
            );
        }
        if (this.enhancementButton != null && this.layout.enhancementButtonRect().width() > 0) {
            PersonalDatabaseLayout.Rect enhancementRect = this.layout.enhancementButtonRect();
            VanillaWidgetRenderer.renderDropdownIndicator(
                    guiGraphics,
                    enhancementRect.right() - 10,
                    enhancementRect.y() + enhancementRect.height() / 2,
                    0xFF3F3F3F
            );
        }
        if (this.viewSelectorButton != null && this.layout.viewSelectorButtonRect().width() > 0) {
            PersonalDatabaseLayout.Rect viewRect = this.layout.viewSelectorButtonRect();
            VanillaWidgetRenderer.renderDropdownIndicator(
                    guiGraphics,
                    viewRect.right() - 10,
                    viewRect.y() + viewRect.height() / 2,
                    0xFF3F3F3F
            );
        }
    }

    private void renderDatabaseScaffold(GuiGraphics guiGraphics) {
        if (this.layout == null) {
            return;
        }
        VanillaWidgetRenderer.renderPanel(guiGraphics, this.layout.databasePanelRect());
        for (int panelIndex = 0; panelIndex < this.currentPanels().size(); panelIndex++) {
            PersonalDatabaseLayout.DatabaseViewportLayout viewportLayout = this.layout.databaseViewportLayout(panelIndex);
            VanillaWidgetRenderer.renderPanel(guiGraphics, viewportLayout.panelRect());
        }
        guiGraphics.fill(
                this.layout.databaseFooterRect().x(),
                this.layout.databaseFooterRect().y() - 6,
                this.layout.databaseFooterRect().right(),
                this.layout.databaseFooterRect().y() - 5,
                0x66FFFFFF
        );
        guiGraphics.drawString(
                this.font,
                Component.translatable(this.menu.viewState().query().scope().sectionTranslationKey()),
                this.layout.databasePanelRect().x() + PersonalDatabaseLayout.GRID_PADDING,
                this.layout.databasePanelRect().y() - 12,
                0x404040,
                true
        );
    }

    private List<DatabasePanelView> currentPanels() {
        return this.menu.viewState().panels();
    }

    private List<DatabaseTab> currentTabs() {
        return this.menu.viewState().tabsForScope(this.menu.viewState().query().scope());
    }

    private List<DatabaseTab> currentConcreteTabs() {
        return this.currentTabs().stream().filter(DatabaseTab::isConcreteTab).toList();
    }

    private DatabaseTab findTab(String tabId) {
        for (DatabaseTab tab : this.currentTabs()) {
            if (tab.id().equals(tabId)) {
                return tab;
            }
        }
        return DatabaseTabs.isAllTabId(tabId) ? DatabaseTabs.allTab() : DatabaseTabs.defaultConcreteTab();
    }

    private Component tabLabel(@Nullable DatabaseTab tab) {
        if (tab == null) {
            return Component.translatable(DatabaseTabs.DEFAULT_TAB_TRANSLATION_KEY);
        }
        if (tab.usesTranslationKey() || (!tab.translationKey().isBlank() && tab.customName().isBlank())) {
            return Component.translatable(tab.translationKey());
        }
        if (!tab.customName().isBlank()) {
            return Component.literal(tab.customName());
        }
        if (!tab.translationKey().isBlank()) {
            return Component.translatable(tab.translationKey());
        }
        return Component.literal(tab.id());
    }

    private String tabEditableName(DatabaseTab tab) {
        if (tab == null) {
            return "";
        }
        return tab.customName().isBlank() ? this.tabLabel(tab).getString() : tab.customName();
    }

    private ItemStack tabIcon(@Nullable DatabaseTab tab) {
        if (tab == null) {
            return new ItemStack(Items.CHEST);
        }
        return this.resolveIconStack(tab.iconItemId(), tab.isAllTab());
    }

    private ItemStack resolveIconStack(String itemId, boolean allTab) {
        String fallbackItemId = allTab ? DatabaseTabs.DEFAULT_ALL_ICON_ITEM_ID : DatabaseTabs.DEFAULT_CONCRETE_ICON_ITEM_ID;
        ResourceLocation resolvedItemId = this.parseResourceLocation(itemId);
        if (resolvedItemId != null && BuiltInRegistries.ITEM.containsKey(resolvedItemId)) {
            Item item = BuiltInRegistries.ITEM.get(resolvedItemId);
            if (item != Items.AIR) {
                return new ItemStack(item);
            }
        }
        ResourceLocation fallbackId = this.parseResourceLocation(fallbackItemId);
        if (fallbackId != null && BuiltInRegistries.ITEM.containsKey(fallbackId)) {
            return new ItemStack(BuiltInRegistries.ITEM.get(fallbackId));
        }
        return new ItemStack(allTab ? Items.COMPASS : Items.CHEST);
    }

    @Nullable
    private ResourceLocation parseResourceLocation(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ResourceLocation.parse(value);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private void ensureManagementWidgets() {
        if (this.managementNameBox == null) {
            this.managementNameBox = new EditBox(this.font, 0, 0, 10, 12, Component.translatable("screen.infiniteinventory.management.name"));
            this.managementNameBox.setBordered(false);
            this.managementNameBox.setMaxLength(DatabaseTabs.MAX_TAB_NAME_LENGTH);
            this.managementNameBox.setTextColor(0x303030);
            this.managementNameBox.setTextColorUneditable(0x606060);
            this.managementNameBox.visible = false;
            this.addRenderableWidget(this.managementNameBox);
        }
        if (this.iconSearchBox == null) {
            this.iconSearchBox = new EditBox(this.font, 0, 0, 10, 12, Component.translatable("screen.infiniteinventory.icon_picker.search"));
            this.iconSearchBox.setBordered(false);
            this.iconSearchBox.setMaxLength(64);
            this.iconSearchBox.setTextColor(0x303030);
            this.iconSearchBox.setTextColorUneditable(0x606060);
            this.iconSearchBox.visible = false;
            this.addRenderableWidget(this.iconSearchBox);
        }
    }

    private void syncManagementWidgets() {
        if (this.managementNameBox == null || this.iconSearchBox == null) {
            return;
        }
        DatabaseTab selectedTab = this.findTab(this.managementSelectedTabId);
        String selectedTabId = selectedTab.id();
        boolean selectedTabPresent = this.currentTabs().stream().anyMatch(tab -> tab.id().equals(selectedTabId));
        if (!selectedTabPresent) {
            DatabaseTab fallbackTab = this.findTab(this.menu.viewState().query().focusedTabId());
            if (!fallbackTab.isConcreteTab() && !this.currentConcreteTabs().isEmpty()) {
                fallbackTab = this.currentConcreteTabs().getFirst();
            }
            this.loadManagementDrafts(fallbackTab);
            selectedTab = fallbackTab;
        }

        PersonalDatabaseLayout.Rect managementFieldRect = this.managementNameFieldRect();
        this.managementNameBox.setX(managementFieldRect.x() + 4);
        this.managementNameBox.setY(managementFieldRect.y() + 4);
        this.managementNameBox.setWidth(Math.max(1, managementFieldRect.width() - 8));
        this.managementNameBox.setHeight(12);
        this.managementNameBox.visible = this.tabManagementExpanded;
        this.managementNameBox.active = selectedTab.canRename();
        if (!this.tabManagementExpanded) {
            this.managementNameBox.setFocused(false);
        }

        PersonalDatabaseLayout.Rect iconSearchRect = this.iconPickerSearchFieldRect();
        this.iconSearchBox.setX(iconSearchRect.x() + 4);
        this.iconSearchBox.setY(iconSearchRect.y() + 4);
        this.iconSearchBox.setWidth(Math.max(1, iconSearchRect.width() - 8));
        this.iconSearchBox.setHeight(12);
        this.iconSearchBox.visible = this.iconPickerExpanded;
        this.iconSearchBox.active = this.iconPickerExpanded;
        if (!this.iconPickerExpanded) {
            this.iconSearchBox.setFocused(false);
        }
    }

    private void loadManagementDrafts(DatabaseTab tab) {
        DatabaseTab resolvedTab = tab == null ? DatabaseTabs.defaultConcreteTab() : tab;
        this.managementSelectedTabId = resolvedTab.id();
        this.pendingIconItemId = resolvedTab.iconItemId();
        if (this.managementNameBox != null) {
            this.managementNameBox.setValue(this.tabEditableName(resolvedTab));
        }
    }

    private List<DatabaseTab> visibleTopTabs() {
        List<DatabaseTab> tabs = this.currentTabs();
        if (this.layout == null || tabs.size() <= 1) {
            return tabs;
        }
        int gapWithoutMore = this.inlineTabGap(tabs.size(), false);
        int maxVisibleWithoutMore = Math.max(1, (this.layout.tabBarRect().width() + gapWithoutMore) / (INLINE_TAB_MIN_WIDTH + gapWithoutMore));
        if (tabs.size() <= maxVisibleWithoutMore) {
            return tabs;
        }

        int gap = this.inlineTabGap(tabs.size(), true);
        int availableWidth = Math.max(1, this.layout.tabBarRect().width() - INLINE_TAB_MORE_WIDTH - gap);
        int maxVisibleWithMore = Math.max(1, (availableWidth + gap) / (INLINE_TAB_MIN_WIDTH_WITH_MORE + gap));
        int visibleCount = Math.max(1, maxVisibleWithMore);

        LinkedHashSet<String> visibleTabIds = new LinkedHashSet<>();
        for (DatabaseTab tab : tabs) {
            if (visibleTabIds.size() >= visibleCount) {
                break;
            }
            visibleTabIds.add(tab.id());
        }

        String focusedTabId = this.menu.viewState().query().focusedTabId();
        if (!visibleTabIds.contains(focusedTabId)) {
            List<String> orderedTabIds = new ArrayList<>(visibleTabIds);
            if (!orderedTabIds.isEmpty()) {
                orderedTabIds.set(orderedTabIds.size() - 1, focusedTabId);
                visibleTabIds.clear();
                visibleTabIds.addAll(orderedTabIds);
            }
        }

        List<DatabaseTab> visibleTabs = new ArrayList<>(visibleTabIds.size());
        for (DatabaseTab tab : tabs) {
            if (visibleTabIds.contains(tab.id())) {
                visibleTabs.add(tab);
            }
        }
        return List.copyOf(visibleTabs);
    }

    private List<DatabaseTab> hiddenTopTabs() {
        LinkedHashSet<String> visibleTabIds = new LinkedHashSet<>();
        for (DatabaseTab tab : this.visibleTopTabs()) {
            visibleTabIds.add(tab.id());
        }
        List<DatabaseTab> hiddenTabs = new ArrayList<>();
        for (DatabaseTab tab : this.currentTabs()) {
            if (!visibleTabIds.contains(tab.id())) {
                hiddenTabs.add(tab);
            }
        }
        return List.copyOf(hiddenTabs);
    }

    private int inlineTabGap(int visibleTabCount, boolean hasMore) {
        return visibleTabCount + (hasMore ? 1 : 0) >= 5 ? INLINE_TAB_TIGHT_GAP : PersonalDatabaseLayout.TAB_GAP;
    }

    private PersonalDatabaseLayout.Rect topTabRect(int index, int visibleTabCount, boolean hasMore) {
        if (this.layout == null || visibleTabCount <= 0) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        PersonalDatabaseLayout.Rect barRect = this.layout.tabBarRect();
        int gap = this.inlineTabGap(visibleTabCount, hasMore);
        int availableWidth = Math.max(1, barRect.width() - (hasMore ? INLINE_TAB_MORE_WIDTH + gap : 0));
        int totalGap = Math.max(0, visibleTabCount - 1) * gap;
        int tabWidth = Math.max(1, (availableWidth - totalGap) / visibleTabCount);
        int x = barRect.x() + index * (tabWidth + gap);
        int right = index == visibleTabCount - 1 ? barRect.x() + availableWidth : x + tabWidth;
        return new PersonalDatabaseLayout.Rect(x, barRect.y(), Math.max(1, right - x), barRect.height());
    }

    private PersonalDatabaseLayout.Rect moreTabsButtonRect() {
        if (this.layout == null || this.hiddenTopTabs().isEmpty()) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        PersonalDatabaseLayout.Rect barRect = this.layout.tabBarRect();
        int gap = this.inlineTabGap(this.visibleTopTabs().size(), true);
        int availableWidth = Math.max(1, barRect.width() - INLINE_TAB_MORE_WIDTH - gap);
        int x = barRect.x() + availableWidth + gap;
        return new PersonalDatabaseLayout.Rect(x, barRect.y(), Math.max(1, barRect.right() - x), barRect.height());
    }

    @Nullable
    private DatabaseTab findHoveredTab(double mouseX, double mouseY) {
        List<DatabaseTab> visibleTabs = this.visibleTopTabs();
        for (int index = 0; index < visibleTabs.size(); index++) {
            if (this.topTabRect(index, visibleTabs.size(), !this.hiddenTopTabs().isEmpty()).contains(mouseX, mouseY)) {
                return visibleTabs.get(index);
            }
        }
        return null;
    }

    private int findDatabasePanel(double mouseX, double mouseY) {
        if (this.layout == null) {
            return -1;
        }
        for (int panelIndex = 0; panelIndex < this.currentPanels().size(); panelIndex++) {
            if (this.layout.databaseViewportLayout(panelIndex).panelRect().contains(mouseX, mouseY)) {
                return panelIndex;
            }
        }
        return -1;
    }

    private void renderViewSelector(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.Rect panelRect = this.viewSelectorRect();
        if (panelRect.width() <= 0 || panelRect.height() <= 0) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 252.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        guiGraphics.drawString(this.font, Component.translatable("screen.infiniteinventory.visible_tabs_title"), panelRect.x() + 8, panelRect.y() + 8, OVERLAY_TEXT_COLOR, true);
        guiGraphics.fill(panelRect.x() + 8, panelRect.y() + 20, panelRect.right() - 8, panelRect.y() + 21, 0x70A89E8C);

        DatabaseQuery query = this.menu.viewState().query();
        List<DatabaseTab> tabs = this.currentTabs();
        for (int index = 0; index < tabs.size(); index++) {
            DatabaseTab tab = tabs.get(index);
            PersonalDatabaseLayout.Rect rowRect = this.selectorRowRect(panelRect, index, TAB_SELECTOR_ROW_HEIGHT);
            boolean visible = query.visibleTabIds().contains(tab.id());
            boolean hovered = rowRect.contains(mouseX, mouseY);
            boolean enabled = visible || query.visibleTabIds().size() < DatabaseTabs.MAX_VISIBLE_TAB_COUNT;
            VanillaWidgetRenderer.renderOverlayRow(guiGraphics, rowRect, hovered, visible);
            guiGraphics.renderItem(this.tabIcon(tab), rowRect.x() + 3, rowRect.y() + 2);
            guiGraphics.drawString(
                    this.font,
                    this.truncateToWidth(this.tabLabel(tab).getString(), Math.max(0, rowRect.width() - 44)),
                    rowRect.x() + 24,
                    rowRect.y() + 6,
                    query.focusedTabId().equals(tab.id()) ? OVERLAY_ACCENT_TEXT_COLOR : OVERLAY_TEXT_COLOR,
                    true
            );
            this.drawCenteredShadow(
                    guiGraphics,
                    Component.literal(visible ? "ON" : "OFF"),
                    rowRect.right() - 30,
                    rowRect.right() - 4,
                    rowRect.y() + 6,
                    enabled ? OVERLAY_ACCENT_TEXT_COLOR : OVERLAY_MUTED_TEXT_COLOR
            );
        }
        guiGraphics.pose().popPose();
    }

    private boolean handleViewSelectorClick(double mouseX, double mouseY) {
        if (!this.viewSelectorExpanded) {
            return false;
        }
        if (this.layout != null && this.layout.viewSelectorButtonRect().contains(mouseX, mouseY)) {
            this.viewSelectorExpanded = false;
            return true;
        }
        PersonalDatabaseLayout.Rect panelRect = this.viewSelectorRect();
        if (!panelRect.contains(mouseX, mouseY)) {
            this.viewSelectorExpanded = false;
            return false;
        }
        DatabaseQuery query = this.menu.viewState().query();
        List<DatabaseTab> tabs = this.currentTabs();
        for (int index = 0; index < tabs.size(); index++) {
            DatabaseTab tab = tabs.get(index);
            PersonalDatabaseLayout.Rect rowRect = this.selectorRowRect(panelRect, index, TAB_SELECTOR_ROW_HEIGHT);
            if (!rowRect.contains(mouseX, mouseY)) {
                continue;
            }
            if (query.visibleTabIds().contains(tab.id())) {
                if (query.visibleTabIds().size() <= 1) {
                    return true;
                }
                List<String> nextVisibleTabIds = query.visibleTabIds().stream()
                        .filter(tabId -> !tabId.equals(tab.id()))
                        .toList();
                this.sendQuery(query.withVisibleTabIds(nextVisibleTabIds));
                return true;
            }
            if (query.visibleTabIds().size() >= DatabaseTabs.MAX_VISIBLE_TAB_COUNT) {
                return true;
            }
            LinkedHashSet<String> nextVisibleTabIds = new LinkedHashSet<>(query.visibleTabIds());
            nextVisibleTabIds.add(tab.id());
            this.sendQuery(query.withVisibleTabIds(new ArrayList<>(nextVisibleTabIds)).withFocusedTabId(tab.id()));
            return true;
        }
        return true;
    }

    private void renderMoreTabsDropdown(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.Rect panelRect = this.moreTabsDropdownRect();
        if (panelRect.width() <= 0 || panelRect.height() <= 0) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 253.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        List<DatabaseTab> tabs = this.hiddenTopTabs();
        for (int index = 0; index < tabs.size(); index++) {
            DatabaseTab tab = tabs.get(index);
            PersonalDatabaseLayout.Rect rowRect = new PersonalDatabaseLayout.Rect(panelRect.x() + 2, panelRect.y() + 2 + index * TAB_SELECTOR_ROW_HEIGHT, panelRect.width() - 4, TAB_SELECTOR_ROW_HEIGHT - 1);
            boolean hovered = rowRect.contains(mouseX, mouseY);
            boolean selected = this.menu.viewState().query().focusedTabId().equals(tab.id());
            VanillaWidgetRenderer.renderOverlayRow(guiGraphics, rowRect, hovered, selected);
            guiGraphics.renderItem(this.tabIcon(tab), rowRect.x() + 3, rowRect.y() + 2);
            guiGraphics.drawString(this.font, this.truncateToWidth(this.tabLabel(tab).getString(), Math.max(0, rowRect.width() - 28)), rowRect.x() + 24, rowRect.y() + 6, selected ? OVERLAY_ACCENT_TEXT_COLOR : OVERLAY_TEXT_COLOR, true);
        }
        guiGraphics.pose().popPose();
    }

    private boolean handleMoreTabsClick(double mouseX, double mouseY) {
        if (!this.moreTabsExpanded) {
            return false;
        }
        if (this.moreTabsButtonRect().contains(mouseX, mouseY)) {
            this.moreTabsExpanded = false;
            return true;
        }
        PersonalDatabaseLayout.Rect panelRect = this.moreTabsDropdownRect();
        if (!panelRect.contains(mouseX, mouseY)) {
            this.moreTabsExpanded = false;
            return false;
        }
        List<DatabaseTab> tabs = this.hiddenTopTabs();
        for (int index = 0; index < tabs.size(); index++) {
            PersonalDatabaseLayout.Rect rowRect = new PersonalDatabaseLayout.Rect(panelRect.x() + 2, panelRect.y() + 2 + index * TAB_SELECTOR_ROW_HEIGHT, panelRect.width() - 4, TAB_SELECTOR_ROW_HEIGHT - 1);
            if (!rowRect.contains(mouseX, mouseY)) {
                continue;
            }
            DatabaseTab tab = tabs.get(index);
            this.moreTabsExpanded = false;
            this.sendQuery(this.menu.viewState().query().withSingleVisibleTab(tab.id()).withFocusedTabId(tab.id()));
            return true;
        }
        return true;
    }

    private void openTargetSelector(TargetSelectorMode mode, int panelIndex, int slotIndex, String sourceTabId) {
        this.closeContextMenu();
        this.sortDropdownExpanded = false;
        this.pagePickerExpanded = false;
        this.moreTabsExpanded = false;
        this.viewSelectorExpanded = false;
        this.targetSelectorMode = mode == null ? TargetSelectorMode.NONE : mode;
        this.pendingTargetPanelIndex = panelIndex;
        this.pendingQuickDepositSlotIndex = slotIndex;
        this.pendingTargetSourceTabId = sourceTabId == null ? "" : sourceTabId;
        this.targetSelectorExpanded = true;
    }

    private void closeTargetSelector() {
        this.targetSelectorExpanded = false;
        this.targetSelectorMode = TargetSelectorMode.NONE;
        this.pendingTargetPanelIndex = -1;
        this.pendingQuickDepositSlotIndex = -1;
        this.pendingTargetSourceTabId = "";
        this.pendingTargetStoresSingle = false;
    }

    private void renderTargetSelector(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.Rect panelRect = this.targetSelectorRect();
        if (panelRect.width() <= 0 || panelRect.height() <= 0) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 254.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        guiGraphics.drawString(this.font, this.targetSelectorTitle(), panelRect.x() + 8, panelRect.y() + 8, OVERLAY_TEXT_COLOR, true);
        guiGraphics.fill(panelRect.x() + 8, panelRect.y() + 20, panelRect.right() - 8, panelRect.y() + 21, 0x70A89E8C);

        List<DatabaseTab> candidateTabs = this.targetSelectorTabs();
        if (candidateTabs.isEmpty()) {
            this.drawCenteredShadow(guiGraphics, Component.translatable("screen.infiniteinventory.target_selector.none"), panelRect.x() + 8, panelRect.right() - 8, panelRect.y() + 36, OVERLAY_MUTED_TEXT_COLOR);
            guiGraphics.pose().popPose();
            return;
        }

        String selectedTargetTabId = this.targetSelectorMode == TargetSelectorMode.AUTO_STORE_TARGET ? this.menu.viewState().autoStoreTargetTabId() : "";
        for (int index = 0; index < candidateTabs.size(); index++) {
            DatabaseTab tab = candidateTabs.get(index);
            PersonalDatabaseLayout.Rect rowRect = this.selectorRowRect(panelRect, index, TARGET_SELECTOR_ROW_HEIGHT);
            boolean hovered = rowRect.contains(mouseX, mouseY);
            boolean selected = selectedTargetTabId.equals(tab.id());
            VanillaWidgetRenderer.renderOverlayRow(guiGraphics, rowRect, hovered, selected);
            guiGraphics.renderItem(this.tabIcon(tab), rowRect.x() + 3, rowRect.y() + 2);
            guiGraphics.drawString(this.font, this.truncateToWidth(this.tabLabel(tab).getString(), Math.max(0, rowRect.width() - 28)), rowRect.x() + 24, rowRect.y() + 6, selected ? OVERLAY_ACCENT_TEXT_COLOR : OVERLAY_TEXT_COLOR, true);
        }
        guiGraphics.pose().popPose();
    }

    private boolean handleTargetSelectorClick(double mouseX, double mouseY) {
        if (!this.targetSelectorExpanded) {
            return false;
        }
        PersonalDatabaseLayout.Rect panelRect = this.targetSelectorRect();
        if (!panelRect.contains(mouseX, mouseY)) {
            this.closeTargetSelector();
            return true;
        }
        List<DatabaseTab> candidateTabs = this.targetSelectorTabs();
        for (int index = 0; index < candidateTabs.size(); index++) {
            PersonalDatabaseLayout.Rect rowRect = this.selectorRowRect(panelRect, index, TARGET_SELECTOR_ROW_HEIGHT);
            if (!rowRect.contains(mouseX, mouseY)) {
                continue;
            }
            this.applyTargetSelection(candidateTabs.get(index).id());
            return true;
        }
        return true;
    }

    private void applyTargetSelection(String targetTabId) {
        switch (this.targetSelectorMode) {
            case DEPOSIT_ALL -> PacketDistributor.sendToServer(new DepositAllPayload(this.menu.containerId, this.menu.viewState().sessionId(), targetTabId));
            case CARRIED_STORE -> this.sendDatabaseClick(
                    Math.max(0, this.pendingTargetPanelIndex),
                    0,
                    this.pendingTargetStoresSingle ? DatabaseClickAction.STORE_SINGLE : DatabaseClickAction.STORE_STACK,
                    targetTabId
            );
            case QUICK_DEPOSIT -> PacketDistributor.sendToServer(new DatabaseQuickDepositPayload(
                    this.menu.containerId,
                    this.menu.viewState().sessionId(),
                    this.pendingQuickDepositSlotIndex,
                    targetTabId
            ));
            case TRANSFER_TAB -> this.sendTabMutation(DatabaseTabMutationAction.TRANSFER, this.pendingTargetSourceTabId, targetTabId, "", "");
            case DELETE_TAB -> this.sendTabMutation(DatabaseTabMutationAction.DELETE, this.pendingTargetSourceTabId, targetTabId, "", "");
            case AUTO_STORE_TARGET -> PacketDistributor.sendToServer(new DatabaseEnhancementPayload(
                    this.menu.containerId,
                    this.menu.viewState().sessionId(),
                    this.menu.viewState().enhancementConfig(),
                    targetTabId
            ));
            case NONE -> {
            }
        }
        this.closeTargetSelector();
    }

    private List<DatabaseTab> targetSelectorTabs() {
        return switch (this.targetSelectorMode) {
            case DEPOSIT_ALL, CARRIED_STORE, QUICK_DEPOSIT -> {
                LinkedHashSet<String> visibleConcreteTabIds = new LinkedHashSet<>();
                for (DatabasePanelView panel : this.currentPanels()) {
                    if (panel.tab().isConcreteTab()) {
                        visibleConcreteTabIds.add(panel.tab().id());
                    }
                }
                if (visibleConcreteTabIds.isEmpty()) {
                    for (DatabaseTab tab : this.currentConcreteTabs()) {
                        visibleConcreteTabIds.add(tab.id());
                    }
                }
                List<DatabaseTab> candidateTabs = new ArrayList<>();
                for (DatabaseTab tab : this.currentTabs()) {
                    if (visibleConcreteTabIds.contains(tab.id())) {
                        candidateTabs.add(tab);
                    }
                }
                yield List.copyOf(candidateTabs);
            }
            case TRANSFER_TAB, DELETE_TAB -> this.currentConcreteTabs().stream()
                    .filter(tab -> !tab.id().equals(this.pendingTargetSourceTabId))
                    .toList();
            case AUTO_STORE_TARGET -> this.menu.viewState().personalTabs().stream()
                    .filter(DatabaseTab::isConcreteTab)
                    .toList();
            case NONE -> List.of();
        };
    }

    private Component targetSelectorTitle() {
        return switch (this.targetSelectorMode) {
            case DEPOSIT_ALL -> Component.translatable("screen.infiniteinventory.target_selector.deposit_all");
            case CARRIED_STORE -> Component.translatable("screen.infiniteinventory.target_selector.store");
            case QUICK_DEPOSIT -> Component.translatable("screen.infiniteinventory.target_selector.quick_deposit");
            case TRANSFER_TAB -> Component.translatable("screen.infiniteinventory.target_selector.transfer");
            case DELETE_TAB -> Component.translatable("screen.infiniteinventory.target_selector.delete");
            case AUTO_STORE_TARGET -> Component.translatable("screen.infiniteinventory.target_selector.auto_store");
            case NONE -> Component.translatable("screen.infiniteinventory.target_selector.title");
        };
    }

    private void renderTabManagementPanel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.Rect panelRect = this.tabManagementPanelRect();
        if (panelRect.width() <= 0 || panelRect.height() <= 0) {
            return;
        }
        this.ensureManagementWidgets();
        DatabaseTab selectedTab = this.findTab(this.managementSelectedTabId);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 255.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        guiGraphics.drawString(this.font, Component.translatable("screen.infiniteinventory.management.title"), panelRect.x() + MANAGEMENT_PANEL_PADDING, panelRect.y() + MANAGEMENT_PANEL_PADDING, OVERLAY_TEXT_COLOR, true);
        guiGraphics.fill(panelRect.x() + MANAGEMENT_PANEL_PADDING, panelRect.y() + MANAGEMENT_PANEL_PADDING + OVERLAY_SECTION_TITLE_HEIGHT, panelRect.right() - MANAGEMENT_PANEL_PADDING, panelRect.y() + MANAGEMENT_PANEL_PADDING + OVERLAY_SECTION_TITLE_HEIGHT + 1, 0x70A89E8C);

        List<DatabaseTab> tabs = this.currentTabs();
        for (int index = 0; index < tabs.size(); index++) {
            PersonalDatabaseLayout.Rect rowRect = this.managementListRowRect(index);
            if (rowRect.bottom() > panelRect.bottom() - MANAGEMENT_PANEL_PADDING) {
                break;
            }
            DatabaseTab tab = tabs.get(index);
            boolean hovered = rowRect.contains(mouseX, mouseY);
            boolean selected = tab.id().equals(selectedTab.id());
            VanillaWidgetRenderer.renderOverlayRow(guiGraphics, rowRect, hovered, selected);
            guiGraphics.renderItem(this.tabIcon(tab), rowRect.x() + 3, rowRect.y() + 2);
            guiGraphics.drawString(this.font, this.truncateToWidth(this.tabLabel(tab).getString(), Math.max(0, rowRect.width() - 28)), rowRect.x() + 24, rowRect.y() + 6, selected ? OVERLAY_ACCENT_TEXT_COLOR : OVERLAY_TEXT_COLOR, true);
        }

        PersonalDatabaseLayout.Rect nameFieldRect = this.managementNameFieldRect();
        guiGraphics.drawString(this.font, Component.translatable("screen.infiniteinventory.management.name"), nameFieldRect.x(), nameFieldRect.y() - 12, OVERLAY_TEXT_COLOR, true);
        VanillaWidgetRenderer.renderTextField(guiGraphics, nameFieldRect, this.managementNameBox != null && this.managementNameBox.isFocused());
        if (this.managementNameBox != null) {
            this.managementNameBox.render(guiGraphics, mouseX, mouseY, 0.0F);
        }

        PersonalDatabaseLayout.Rect iconFieldRect = this.managementIconFieldRect();
        guiGraphics.drawString(this.font, Component.translatable("screen.infiniteinventory.management.icon"), iconFieldRect.x(), iconFieldRect.y() - 12, OVERLAY_TEXT_COLOR, true);
        VanillaWidgetRenderer.renderOverlayRow(guiGraphics, iconFieldRect, iconFieldRect.contains(mouseX, mouseY), false);
        guiGraphics.renderItem(this.resolveIconStack(this.pendingIconItemId, selectedTab.isAllTab()), iconFieldRect.x() + 2, iconFieldRect.y() + 2);
        guiGraphics.drawString(this.font, this.truncateToWidth(this.pendingIconItemId, Math.max(0, iconFieldRect.width() - 26)), iconFieldRect.x() + 24, iconFieldRect.y() + 6, OVERLAY_TEXT_COLOR, true);

        this.renderManagementActionButton(
                guiGraphics,
                this.managementActionButtonRect(0, 0),
                mouseX,
                mouseY,
                Component.translatable("screen.infiniteinventory.management.add"),
                true
        );
        this.renderManagementActionButton(
                guiGraphics,
                this.managementActionButtonRect(0, 1),
                mouseX,
                mouseY,
                Component.translatable("screen.infiniteinventory.management.rename"),
                selectedTab.canRename()
        );
        this.renderManagementActionButton(
                guiGraphics,
                this.managementActionButtonRect(1, 0),
                mouseX,
                mouseY,
                Component.translatable("screen.infiniteinventory.management.pick_icon"),
                true
        );
        this.renderManagementActionButton(
                guiGraphics,
                this.managementActionButtonRect(1, 1),
                mouseX,
                mouseY,
                Component.translatable("screen.infiniteinventory.management.apply_icon"),
                selectedTab.isConcreteTab()
        );
        this.renderManagementActionButton(
                guiGraphics,
                this.managementActionButtonRect(2, 0),
                mouseX,
                mouseY,
                Component.translatable("screen.infiniteinventory.management.move_left"),
                this.canMoveManagementTab(selectedTab, -1)
        );
        this.renderManagementActionButton(
                guiGraphics,
                this.managementActionButtonRect(2, 1),
                mouseX,
                mouseY,
                Component.translatable("screen.infiniteinventory.management.move_right"),
                this.canMoveManagementTab(selectedTab, 1)
        );
        this.renderManagementActionButton(
                guiGraphics,
                this.managementActionButtonRect(3, 0),
                mouseX,
                mouseY,
                Component.translatable("screen.infiniteinventory.management.transfer"),
                this.currentConcreteTabs().size() > 1 && selectedTab.isConcreteTab()
        );
        this.renderManagementActionButton(
                guiGraphics,
                this.managementActionButtonRect(3, 1),
                mouseX,
                mouseY,
                Component.translatable("screen.infiniteinventory.management.delete"),
                this.currentConcreteTabs().size() > 1 && selectedTab.canDelete()
        );
        guiGraphics.pose().popPose();
    }

    private void renderManagementActionButton(
            GuiGraphics guiGraphics,
            PersonalDatabaseLayout.Rect rect,
            int mouseX,
            int mouseY,
            Component label,
            boolean enabled
    ) {
        VanillaWidgetRenderer.renderOverlayChip(guiGraphics, rect, rect.contains(mouseX, mouseY), false, enabled);
        this.drawCenteredShadow(
                guiGraphics,
                label,
                rect.x() + 2,
                rect.right() - 2,
                rect.y() + 6,
                enabled ? OVERLAY_TEXT_COLOR : OVERLAY_MUTED_TEXT_COLOR
        );
    }

    private boolean handleTabManagementClick(double mouseX, double mouseY) {
        if (!this.tabManagementExpanded) {
            return false;
        }
        this.ensureManagementWidgets();
        PersonalDatabaseLayout.Rect panelRect = this.tabManagementPanelRect();
        if (!panelRect.contains(mouseX, mouseY)) {
            this.tabManagementExpanded = false;
            this.iconPickerExpanded = false;
            return true;
        }

        if (this.managementNameBox != null && this.managementNameFieldRect().contains(mouseX, mouseY)) {
            this.managementNameBox.mouseClicked(mouseX, mouseY, 0);
            return true;
        }

        List<DatabaseTab> tabs = this.currentTabs();
        for (int index = 0; index < tabs.size(); index++) {
            PersonalDatabaseLayout.Rect rowRect = this.managementListRowRect(index);
            if (rowRect.bottom() > panelRect.bottom() - MANAGEMENT_PANEL_PADDING) {
                break;
            }
            if (rowRect.contains(mouseX, mouseY)) {
                this.loadManagementDrafts(tabs.get(index));
                return true;
            }
        }

        DatabaseTab selectedTab = this.findTab(this.managementSelectedTabId);
        if (this.managementActionButtonRect(0, 0).contains(mouseX, mouseY)) {
            this.sendTabMutation(DatabaseTabMutationAction.ADD, "", "", this.managementDraftName(), this.pendingIconItemId);
            return true;
        }
        if (this.managementActionButtonRect(0, 1).contains(mouseX, mouseY) && selectedTab.canRename()) {
            this.sendTabMutation(DatabaseTabMutationAction.RENAME, selectedTab.id(), "", this.managementDraftName(), "");
            return true;
        }
        if (this.managementActionButtonRect(1, 0).contains(mouseX, mouseY)) {
            this.iconPickerExpanded = true;
            if (this.iconSearchBox != null) {
                this.iconSearchBox.setValue("");
                this.iconSearchBox.setFocused(true);
            }
            return true;
        }
        if (this.managementActionButtonRect(1, 1).contains(mouseX, mouseY) && selectedTab.isConcreteTab()) {
            this.sendTabMutation(DatabaseTabMutationAction.CHANGE_ICON, selectedTab.id(), "", "", this.pendingIconItemId);
            return true;
        }
        if (this.managementActionButtonRect(2, 0).contains(mouseX, mouseY) && this.canMoveManagementTab(selectedTab, -1)) {
            this.sendTabMutation(DatabaseTabMutationAction.MOVE_LEFT, selectedTab.id(), "", "", "");
            return true;
        }
        if (this.managementActionButtonRect(2, 1).contains(mouseX, mouseY) && this.canMoveManagementTab(selectedTab, 1)) {
            this.sendTabMutation(DatabaseTabMutationAction.MOVE_RIGHT, selectedTab.id(), "", "", "");
            return true;
        }
        if (this.managementActionButtonRect(3, 0).contains(mouseX, mouseY) && this.currentConcreteTabs().size() > 1 && selectedTab.isConcreteTab()) {
            this.openTargetSelector(TargetSelectorMode.TRANSFER_TAB, -1, -1, selectedTab.id());
            return true;
        }
        if (this.managementActionButtonRect(3, 1).contains(mouseX, mouseY) && this.currentConcreteTabs().size() > 1 && selectedTab.canDelete()) {
            this.openTargetSelector(TargetSelectorMode.DELETE_TAB, -1, -1, selectedTab.id());
            return true;
        }
        return true;
    }

    private boolean canMoveManagementTab(DatabaseTab selectedTab, int direction) {
        if (selectedTab == null || !selectedTab.isConcreteTab()) {
            return false;
        }
        List<DatabaseTab> concreteTabs = this.currentConcreteTabs();
        int selectedIndex = -1;
        for (int index = 0; index < concreteTabs.size(); index++) {
            if (concreteTabs.get(index).id().equals(selectedTab.id())) {
                selectedIndex = index;
                break;
            }
        }
        if (selectedIndex < 0) {
            return false;
        }
        int nextIndex = selectedIndex + direction;
        return nextIndex >= 0 && nextIndex < concreteTabs.size();
    }

    private void sendTabMutation(DatabaseTabMutationAction action, String tabId, String targetTabId, String name, String iconItemId) {
        PacketDistributor.sendToServer(new DatabaseTabMutationPayload(
                this.menu.containerId,
                this.menu.viewState().sessionId(),
                this.menu.viewState().query().scope(),
                action,
                tabId == null ? "" : tabId,
                targetTabId == null ? "" : targetTabId,
                name == null ? "" : name,
                iconItemId == null ? "" : iconItemId
        ));
    }

    private String managementDraftName() {
        return this.managementNameBox == null ? "" : this.managementNameBox.getValue().trim();
    }

    private void renderIconPicker(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PersonalDatabaseLayout.Rect panelRect = this.iconPickerRect();
        if (panelRect.width() <= 0 || panelRect.height() <= 0) {
            return;
        }
        this.ensureManagementWidgets();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 256.0F);
        VanillaWidgetRenderer.renderOverlayPanel(guiGraphics, panelRect);
        guiGraphics.drawString(this.font, Component.translatable("screen.infiniteinventory.icon_picker.title"), panelRect.x() + MANAGEMENT_PANEL_PADDING, panelRect.y() + MANAGEMENT_PANEL_PADDING, OVERLAY_TEXT_COLOR, true);
        guiGraphics.fill(panelRect.x() + MANAGEMENT_PANEL_PADDING, panelRect.y() + MANAGEMENT_PANEL_PADDING + OVERLAY_SECTION_TITLE_HEIGHT, panelRect.right() - MANAGEMENT_PANEL_PADDING, panelRect.y() + MANAGEMENT_PANEL_PADDING + OVERLAY_SECTION_TITLE_HEIGHT + 1, 0x70A89E8C);
        PersonalDatabaseLayout.Rect searchFieldRect = this.iconPickerSearchFieldRect();
        VanillaWidgetRenderer.renderTextField(guiGraphics, searchFieldRect, this.iconSearchBox != null && this.iconSearchBox.isFocused());
        if (this.iconSearchBox != null) {
            this.iconSearchBox.render(guiGraphics, mouseX, mouseY, 0.0F);
        }

        List<IconChoice> choices = this.matchingIconChoices();
        IconChoice hoveredChoice = null;
        for (int index = 0; index < choices.size(); index++) {
            IconChoice choice = choices.get(index);
            PersonalDatabaseLayout.Rect cellRect = this.iconPickerCellRect(index);
            boolean hovered = cellRect.contains(mouseX, mouseY);
            boolean selected = this.pendingIconItemId.equals(choice.itemId());
            VanillaWidgetRenderer.renderOverlayChip(guiGraphics, cellRect, hovered, selected, true);
            guiGraphics.renderItem(choice.previewStack(), cellRect.x() + (cellRect.width() - 16) / 2, cellRect.y() + 6);
            if (hovered) {
                hoveredChoice = choice;
            }
        }
        if (choices.isEmpty()) {
            this.drawCenteredShadow(guiGraphics, Component.translatable("screen.infiniteinventory.icon_picker.empty"), panelRect.x() + MANAGEMENT_PANEL_PADDING, panelRect.right() - MANAGEMENT_PANEL_PADDING, searchFieldRect.bottom() + 16, OVERLAY_MUTED_TEXT_COLOR);
        }
        guiGraphics.drawString(
                this.font,
                this.truncateToWidth(Component.translatable("screen.infiniteinventory.icon_picker.selected", this.pendingIconItemId).getString(), panelRect.width() - MANAGEMENT_PANEL_PADDING * 2),
                panelRect.x() + MANAGEMENT_PANEL_PADDING,
                panelRect.bottom() - MANAGEMENT_PANEL_PADDING - 12,
                OVERLAY_TEXT_COLOR,
                true
        );
        if (hoveredChoice != null) {
            guiGraphics.renderTooltip(this.font, List.of(Component.literal(hoveredChoice.itemId()), hoveredChoice.previewStack().getHoverName().copy().withStyle(ChatFormatting.GRAY)), hoveredChoice.previewStack().getTooltipImage(), mouseX, mouseY);
        }
        guiGraphics.pose().popPose();
    }

    private boolean handleIconPickerClick(double mouseX, double mouseY) {
        if (!this.iconPickerExpanded) {
            return false;
        }
        if (this.iconSearchBox != null && this.iconPickerSearchFieldRect().contains(mouseX, mouseY)) {
            this.iconSearchBox.mouseClicked(mouseX, mouseY, 0);
            return true;
        }
        List<IconChoice> choices = this.matchingIconChoices();
        for (int index = 0; index < choices.size(); index++) {
            PersonalDatabaseLayout.Rect cellRect = this.iconPickerCellRect(index);
            if (!cellRect.contains(mouseX, mouseY)) {
                continue;
            }
            this.pendingIconItemId = choices.get(index).itemId();
            this.iconPickerExpanded = false;
            if (this.iconSearchBox != null) {
                this.iconSearchBox.setFocused(false);
            }
            return true;
        }
        if (!this.iconPickerRect().contains(mouseX, mouseY)) {
            this.iconPickerExpanded = false;
            if (this.iconSearchBox != null) {
                this.iconSearchBox.setFocused(false);
            }
            return true;
        }
        return true;
    }

    private List<IconChoice> matchingIconChoices() {
        String keyword = this.iconSearchBox == null ? "" : this.iconSearchBox.getValue().trim().toLowerCase(Locale.ROOT);
        List<IconChoice> matchingChoices = new ArrayList<>(ICON_PICKER_MAX_RESULTS);
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (itemId == null) {
                continue;
            }
            ItemStack previewStack = new ItemStack(item);
            String searchableText = (itemId + " " + previewStack.getHoverName().getString()).toLowerCase(Locale.ROOT);
            if (!keyword.isBlank() && !searchableText.contains(keyword)) {
                continue;
            }
            matchingChoices.add(new IconChoice(itemId.toString(), previewStack, searchableText));
            if (matchingChoices.size() >= ICON_PICKER_MAX_RESULTS) {
                break;
            }
        }
        return List.copyOf(matchingChoices);
    }

    private boolean handleQuickDepositClick(double mouseX, double mouseY, int button) {
        if (button != 0 || !hasShiftDown() || this.hoveredSlot == null || !this.menu.getCarried().isEmpty()) {
            return false;
        }
        int slotIndex = this.menu.slots.indexOf(this.hoveredSlot);
        if (slotIndex < 0 || !this.hoveredSlot.hasItem() || !this.isQuickDepositSlot(slotIndex, this.hoveredSlot)) {
            return false;
        }
        String directTargetTabId = this.resolveSingleStoreTargetTabId();
        if (directTargetTabId != null) {
            PacketDistributor.sendToServer(new DatabaseQuickDepositPayload(this.menu.containerId, this.menu.viewState().sessionId(), slotIndex, directTargetTabId));
            return true;
        }
        this.openTargetSelector(TargetSelectorMode.QUICK_DEPOSIT, -1, slotIndex, "");
        return true;
    }

    private boolean isQuickDepositSlot(int slotIndex, Slot slot) {
        return this.menu.isAccessorySlotIndex(slotIndex)
                || (slot.container instanceof Inventory && slot.getContainerSlot() >= 0 && slot.getContainerSlot() < 36);
    }

    @Nullable
    private String resolveSingleStoreTargetTabId() {
        DatabaseQuery query = this.menu.viewState().query();
        if (query.visibleTabIds().size() != 1) {
            return null;
        }
        String onlyVisibleTabId = query.visibleTabIds().getFirst();
        return DatabaseTabs.isAllTabId(onlyVisibleTabId) ? null : onlyVisibleTabId;
    }

    private boolean handleEnhancementPanelClick(double mouseX, double mouseY) {
        PersonalDatabaseLayout.Rect autoStoreRowRect = this.enhancementAutoStoreRowRect();
        if (autoStoreRowRect.contains(mouseX, mouseY)) {
            this.openTargetSelector(TargetSelectorMode.AUTO_STORE_TARGET, -1, -1, "");
            return true;
        }
        return false;
    }

    private PersonalDatabaseLayout.Rect viewSelectorRect() {
        if (this.layout == null) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        int height = MANAGEMENT_PANEL_PADDING * 2 + OVERLAY_SECTION_TITLE_HEIGHT + 8 + this.currentTabs().size() * TAB_SELECTOR_ROW_HEIGHT;
        return this.dropdownPanelRect(this.layout.viewSelectorButtonRect(), TAB_SELECTOR_WIDTH, height);
    }

    private PersonalDatabaseLayout.Rect moreTabsDropdownRect() {
        if (this.layout == null || this.hiddenTopTabs().isEmpty()) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        PersonalDatabaseLayout.Rect anchorRect = this.moreTabsButtonRect();
        int width = Math.max(MORE_TABS_WIDTH, anchorRect.width());
        int height = this.hiddenTopTabs().size() * TAB_SELECTOR_ROW_HEIGHT + 4;
        return this.dropdownPanelRect(anchorRect, width, height);
    }

    private PersonalDatabaseLayout.Rect targetSelectorRect() {
        if (this.layout == null) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        int rowCount = Math.max(1, this.targetSelectorTabs().size());
        int height = MANAGEMENT_PANEL_PADDING * 2 + OVERLAY_SECTION_TITLE_HEIGHT + 8 + rowCount * TARGET_SELECTOR_ROW_HEIGHT;
        return this.centeredOverlayRect(TARGET_SELECTOR_WIDTH, height);
    }

    private PersonalDatabaseLayout.Rect tabManagementPanelRect() {
        if (this.layout == null) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        int desiredHeight = Math.max(MANAGEMENT_PANEL_HEIGHT, 56 + this.currentTabs().size() * MANAGEMENT_ROW_HEIGHT);
        return this.centeredOverlayRect(MANAGEMENT_PANEL_WIDTH, desiredHeight);
    }

    private PersonalDatabaseLayout.Rect managementListRowRect(int index) {
        PersonalDatabaseLayout.Rect panelRect = this.tabManagementPanelRect();
        int rowY = panelRect.y() + MANAGEMENT_PANEL_PADDING + OVERLAY_SECTION_TITLE_HEIGHT + 8 + index * MANAGEMENT_ROW_HEIGHT;
        return new PersonalDatabaseLayout.Rect(
                panelRect.x() + MANAGEMENT_PANEL_PADDING,
                rowY,
                MANAGEMENT_LIST_WIDTH,
                MANAGEMENT_ROW_HEIGHT - 1
        );
    }

    private PersonalDatabaseLayout.Rect managementNameFieldRect() {
        PersonalDatabaseLayout.Rect panelRect = this.tabManagementPanelRect();
        int x = panelRect.x() + MANAGEMENT_PANEL_PADDING + MANAGEMENT_LIST_WIDTH + 14;
        return new PersonalDatabaseLayout.Rect(
                x,
                panelRect.y() + MANAGEMENT_PANEL_PADDING + OVERLAY_SECTION_TITLE_HEIGHT + 18,
                Math.max(1, panelRect.right() - MANAGEMENT_PANEL_PADDING - x),
                20
        );
    }

    private PersonalDatabaseLayout.Rect managementIconFieldRect() {
        PersonalDatabaseLayout.Rect nameFieldRect = this.managementNameFieldRect();
        return new PersonalDatabaseLayout.Rect(nameFieldRect.x(), nameFieldRect.bottom() + 22, nameFieldRect.width(), 20);
    }

    private PersonalDatabaseLayout.Rect managementActionButtonRect(int row, int column) {
        PersonalDatabaseLayout.Rect iconFieldRect = this.managementIconFieldRect();
        int x = iconFieldRect.x() + column * (MANAGEMENT_BUTTON_WIDTH + 8);
        int y = iconFieldRect.bottom() + 18 + row * (MANAGEMENT_ROW_HEIGHT + 6);
        return new PersonalDatabaseLayout.Rect(x, y, MANAGEMENT_BUTTON_WIDTH, MANAGEMENT_ROW_HEIGHT);
    }

    private PersonalDatabaseLayout.Rect iconPickerRect() {
        return this.centeredOverlayRect(ICON_PICKER_WIDTH, ICON_PICKER_HEIGHT);
    }

    private PersonalDatabaseLayout.Rect iconPickerSearchFieldRect() {
        PersonalDatabaseLayout.Rect panelRect = this.iconPickerRect();
        return new PersonalDatabaseLayout.Rect(
                panelRect.x() + MANAGEMENT_PANEL_PADDING,
                panelRect.y() + MANAGEMENT_PANEL_PADDING + OVERLAY_SECTION_TITLE_HEIGHT + 10,
                panelRect.width() - MANAGEMENT_PANEL_PADDING * 2,
                20
        );
    }

    private PersonalDatabaseLayout.Rect iconPickerCellRect(int index) {
        PersonalDatabaseLayout.Rect searchFieldRect = this.iconPickerSearchFieldRect();
        int gridX = searchFieldRect.x() + 2;
        int gridY = searchFieldRect.bottom() + 12;
        int column = index % ICON_PICKER_COLUMNS;
        int row = index / ICON_PICKER_COLUMNS;
        return new PersonalDatabaseLayout.Rect(gridX + column * ICON_PICKER_CELL_SIZE, gridY + row * ICON_PICKER_CELL_SIZE, ICON_PICKER_CELL_SIZE - 4, ICON_PICKER_CELL_SIZE - 4);
    }

    private PersonalDatabaseLayout.Rect enhancementAutoStoreRowRect() {
        PersonalDatabaseLayout.Rect panelRect = this.enhancementPanelRect();
        int rowY = panelRect.y()
                + ENHANCEMENT_PANEL_PADDING
                + ENHANCEMENT_TITLE_HEIGHT
                + DatabaseEnhancementOption.orderedValues().size() * (ENHANCEMENT_ROW_HEIGHT + ENHANCEMENT_ROW_GAP);
        return new PersonalDatabaseLayout.Rect(
                panelRect.x() + ENHANCEMENT_PANEL_PADDING,
                rowY,
                panelRect.width() - ENHANCEMENT_PANEL_PADDING * 2,
                ENHANCEMENT_ROW_HEIGHT
        );
    }

    private PersonalDatabaseLayout.Rect selectorRowRect(PersonalDatabaseLayout.Rect panelRect, int rowIndex, int rowHeight) {
        return new PersonalDatabaseLayout.Rect(
                panelRect.x() + MANAGEMENT_PANEL_PADDING,
                panelRect.y() + MANAGEMENT_PANEL_PADDING + OVERLAY_SECTION_TITLE_HEIGHT + 8 + rowIndex * rowHeight,
                panelRect.width() - MANAGEMENT_PANEL_PADDING * 2,
                rowHeight - 1
        );
    }

    private PersonalDatabaseLayout.Rect dropdownPanelRect(PersonalDatabaseLayout.Rect anchorRect, int width, int height) {
        if (this.layout == null) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        int minX = this.layout.frameRect().x() + CONTEXT_MENU_MARGIN;
        int maxX = Math.max(minX, this.layout.frameRect().right() - width - CONTEXT_MENU_MARGIN);
        int x = Mth.clamp(anchorRect.right() - width, minX, maxX);
        int minY = anchorRect.bottom() + 2;
        int maxY = Math.max(minY, this.layout.frameRect().bottom() - height - CONTEXT_MENU_MARGIN);
        int y = Mth.clamp(minY, minY, maxY);
        return new PersonalDatabaseLayout.Rect(x, y, width, height);
    }

    private PersonalDatabaseLayout.Rect centeredOverlayRect(int width, int desiredHeight) {
        if (this.layout == null) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        int maxWidth = Math.max(1, this.layout.frameRect().width() - CONTEXT_MENU_MARGIN * 2);
        int maxHeight = Math.max(1, this.layout.frameRect().height() - CONTEXT_MENU_MARGIN * 2);
        int resolvedWidth = Math.min(width, maxWidth);
        int resolvedHeight = Math.min(desiredHeight, maxHeight);
        int x = this.layout.frameRect().x() + Math.max(0, (this.layout.frameRect().width() - resolvedWidth) / 2);
        int y = this.layout.frameRect().y() + Math.max(0, (this.layout.frameRect().height() - resolvedHeight) / 2);
        return new PersonalDatabaseLayout.Rect(x, y, resolvedWidth, resolvedHeight);
    }

    private String truncateToWidth(String text, int maxWidth) {
        if (maxWidth <= 0 || this.font.width(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int suffixWidth = this.font.width(suffix);
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (this.font.width(builder.toString() + character) + suffixWidth > maxWidth) {
                break;
            }
            builder.append(character);
        }
        return builder.isEmpty() ? "" : builder.append(suffix).toString();
    }

    private PersonalDatabaseLayout.Rect advancedSearchPanelRect() {
        if (this.layout == null) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        PersonalDatabaseLayout.Rect anchorRect = this.layout.advancedSearchButtonRect();
        int width = ADVANCED_SEARCH_PANEL_WIDTH;
        int height = ADVANCED_SEARCH_PANEL_PADDING * 2
                + ADVANCED_SEARCH_TITLE_HEIGHT
                + DatabaseSearchField.values().length * ADVANCED_SEARCH_ROW_HEIGHT
                + Math.max(0, DatabaseSearchField.values().length - 1) * ADVANCED_SEARCH_ROW_GAP;
        int minX = this.layout.frameRect().x() + CONTEXT_MENU_MARGIN;
        int maxX = Math.max(minX, this.layout.frameRect().right() - width - CONTEXT_MENU_MARGIN);
        int x = Mth.clamp(anchorRect.right() - width, minX, maxX);
        int minY = anchorRect.bottom() + 4;
        int maxY = Math.max(minY, this.layout.frameRect().bottom() - height - CONTEXT_MENU_MARGIN);
        int y = Mth.clamp(minY, minY, maxY);
        return new PersonalDatabaseLayout.Rect(x, y, width, height);
    }

    private PersonalDatabaseLayout.Rect advancedSearchRowRect(DatabaseSearchField field) {
        PersonalDatabaseLayout.Rect panelRect = this.advancedSearchPanelRect();
        int rowY = panelRect.y()
                + ADVANCED_SEARCH_PANEL_PADDING
                + ADVANCED_SEARCH_TITLE_HEIGHT
                + field.ordinal() * (ADVANCED_SEARCH_ROW_HEIGHT + ADVANCED_SEARCH_ROW_GAP);
        return new PersonalDatabaseLayout.Rect(
                panelRect.x() + ADVANCED_SEARCH_PANEL_PADDING,
                rowY,
                panelRect.width() - ADVANCED_SEARCH_PANEL_PADDING * 2,
                ADVANCED_SEARCH_ROW_HEIGHT
        );
    }

    private boolean isWithinAdvancedSearchPanel(double mouseX, double mouseY) {
        if (!this.advancedSearchExpanded || this.layout == null) {
            return false;
        }
        return this.layout.advancedSearchButtonRect().contains(mouseX, mouseY)
                || this.advancedSearchPanelRect().contains(mouseX, mouseY);
    }

    private PersonalDatabaseLayout.Rect enhancementPanelRect() {
        if (this.layout == null) {
            return PersonalDatabaseLayout.Rect.empty();
        }
        PersonalDatabaseLayout.Rect anchorRect = this.layout.enhancementButtonRect();
        int width = ENHANCEMENT_PANEL_WIDTH;
        int height = ENHANCEMENT_PANEL_PADDING * 2
                + ENHANCEMENT_TITLE_HEIGHT
                + DatabaseEnhancementOption.orderedValues().size() * ENHANCEMENT_ROW_HEIGHT
                + Math.max(0, DatabaseEnhancementOption.orderedValues().size()) * ENHANCEMENT_ROW_GAP
                + ENHANCEMENT_ROW_HEIGHT;
        int minX = this.layout.frameRect().x() + CONTEXT_MENU_MARGIN;
        int maxX = Math.max(minX, this.layout.frameRect().right() - width - CONTEXT_MENU_MARGIN);
        int x = Mth.clamp(anchorRect.right() - width, minX, maxX);
        int minY = anchorRect.bottom() + 4;
        int maxY = Math.max(minY, this.layout.frameRect().bottom() - height - CONTEXT_MENU_MARGIN);
        int y = Mth.clamp(minY, minY, maxY);
        return new PersonalDatabaseLayout.Rect(x, y, width, height);
    }

    private PersonalDatabaseLayout.Rect enhancementRowRect(DatabaseEnhancementOption option) {
        PersonalDatabaseLayout.Rect panelRect = this.enhancementPanelRect();
        int rowY = panelRect.y()
                + ENHANCEMENT_PANEL_PADDING
                + ENHANCEMENT_TITLE_HEIGHT
                + option.ordinal() * (ENHANCEMENT_ROW_HEIGHT + ENHANCEMENT_ROW_GAP);
        return new PersonalDatabaseLayout.Rect(
                panelRect.x() + ENHANCEMENT_PANEL_PADDING,
                rowY,
                panelRect.width() - ENHANCEMENT_PANEL_PADDING * 2,
                ENHANCEMENT_ROW_HEIGHT
        );
    }

    private boolean isWithinEnhancementPanel(double mouseX, double mouseY) {
        if (!this.enhancementPanelExpanded || this.layout == null) {
            return false;
        }
        return this.layout.enhancementButtonRect().contains(mouseX, mouseY)
                || this.enhancementPanelRect().contains(mouseX, mouseY);
    }

    private void validateContextMenu(DatabaseViewState viewState) {
        if (!this.contextMenuExpanded) {
            return;
        }
        if (this.contextMenuPanelIndex < 0 || this.contextMenuPanelIndex >= viewState.panels().size()) {
            this.closeContextMenu();
            return;
        }
        DatabasePanelView panel = viewState.panels().get(this.contextMenuPanelIndex);
        if (this.contextMenuSlotIndex < 0 || this.contextMenuSlotIndex >= panel.entries().size()) {
            this.closeContextMenu();
            return;
        }
        ItemStack currentStack = panel.entries().get(this.contextMenuSlotIndex).stack();
        if (!ItemStack.isSameItemSameComponents(this.contextMenuEntryStack, currentStack)) {
            this.closeContextMenu();
        }
    }

    private void openContextMenu(int panelIndex, int slotIndex) {
        if (this.layout == null) {
            return;
        }
        if (panelIndex < 0 || panelIndex >= this.currentPanels().size()) {
            this.closeContextMenu();
            return;
        }
        List<VisibleDatabaseEntry> entries = this.currentPanels().get(panelIndex).entries();
        if (slotIndex < 0 || slotIndex >= entries.size()) {
            this.closeContextMenu();
            return;
        }
        VisibleDatabaseEntry entry = entries.get(slotIndex);
        PersonalDatabaseLayout.Rect slotRect = this.layout.visibleDatabaseSlotBounds(panelIndex, slotIndex);
        int menuWidth = this.contextMenuWidth();
        int menuHeight = CONTEXT_MENU_ACTIONS.length * CONTEXT_MENU_ROW_HEIGHT;
        PersonalDatabaseLayout.Rect frameRect = this.layout.frameRect();
        int minX = frameRect.x() + CONTEXT_MENU_MARGIN;
        int maxX = Math.max(minX, frameRect.right() - menuWidth - CONTEXT_MENU_MARGIN);
        int preferredX = slotRect.right() + 2;
        if (preferredX > maxX) {
            preferredX = slotRect.x() - menuWidth - 2;
        }
        this.contextMenuX = Mth.clamp(preferredX, minX, maxX);

        int minY = frameRect.y() + CONTEXT_MENU_MARGIN;
        int maxY = Math.max(minY, frameRect.bottom() - menuHeight - CONTEXT_MENU_MARGIN);
        this.contextMenuY = Mth.clamp(slotRect.y(), minY, maxY);
        this.contextMenuPanelIndex = panelIndex;
        this.contextMenuSlotIndex = slotIndex;
        this.contextMenuEntryStack = entry.stack().copyWithCount(1);
        this.contextMenuExpanded = true;
    }

    private void closeContextMenu() {
        this.contextMenuExpanded = false;
        this.contextMenuPanelIndex = -1;
        this.contextMenuSlotIndex = -1;
        this.contextMenuEntryStack = ItemStack.EMPTY;
    }

    private Component contextMenuLabel(DatabaseClickAction action) {
        return switch (action) {
            case TAKE_SINGLE -> Component.translatable("screen.infiniteinventory.context.take_single");
            case TAKE_STACK -> Component.translatable("screen.infiniteinventory.context.take_stack");
            case TAKE_HALF_STACK_TO_INVENTORY -> Component.translatable("screen.infiniteinventory.context.take_half_stack_to_inventory");
            case TAKE_HALF_ENTRY_TO_INVENTORY -> Component.translatable("screen.infiniteinventory.context.take_half_entry_to_inventory");
            case TAKE_ALL -> Component.translatable("screen.infiniteinventory.context.take_all_to_inventory");
            default -> Component.empty();
        };
    }

    private boolean isWithinContextMenu(double mouseX, double mouseY) {
        if (!this.contextMenuExpanded) {
            return false;
        }
        int menuWidth = this.contextMenuWidth();
        return mouseX >= this.contextMenuX
                && mouseX < this.contextMenuX + menuWidth
                && mouseY >= this.contextMenuY
                && mouseY < this.contextMenuY + CONTEXT_MENU_ACTIONS.length * CONTEXT_MENU_ROW_HEIGHT;
    }

    private boolean isAdvancedToggleClickable(DatabaseSearchField field, DatabaseSearchConfig searchConfig) {
        DatabaseSearchWeight weight = searchConfig.weightFor(field);
        return !field.isTextField() || weight == DatabaseSearchWeight.OFF || this.enabledTextFieldCount(searchConfig) > 1;
    }

    private boolean isWithinAccessoriesPanel(double mouseX, double mouseY) {
        return this.layout != null
                && this.accessoriesExpanded
                && this.layout.accessoriesPanelRect().contains(mouseX, mouseY);
    }

    @Nullable
    private PersonalDatabaseLayout.AccessorySlotLayout findHoveredAccessorySlot(double mouseX, double mouseY) {
        if (this.layout == null || !this.accessoriesExpanded) {
            return null;
        }
        for (PersonalDatabaseLayout.AccessorySlotLayout slotLayout : this.layout.accessorySlotLayouts()) {
            if (slotLayout.visible() && slotLayout.slotRect().contains(mouseX, mouseY)) {
                return slotLayout;
            }
        }
        return null;
    }

    @Nullable
    private PersonalDatabaseLayout.AccessorySlotLayout resolveAccessorySlotLayout(@Nullable Slot slot) {
        if (slot == null || this.layout == null) {
            return null;
        }
        int menuSlotIndex = this.menu.slots.indexOf(slot);
        if (!this.menu.isAccessorySlotIndex(menuSlotIndex)) {
            return null;
        }
        for (PersonalDatabaseLayout.AccessorySlotLayout slotLayout : this.layout.accessorySlotLayouts()) {
            if (slotLayout.slotIndex() == menuSlotIndex) {
                return slotLayout;
            }
        }
        return null;
    }

    @Nullable
    private DatabaseHitResult findDatabaseSlot(double mouseX, double mouseY) {
        if (this.layout == null) {
            return null;
        }
        for (int panelIndex = 0; panelIndex < this.currentPanels().size(); panelIndex++) {
            int visibleSlotCount = this.layout.visibleDatabaseSlotCount(panelIndex);
            for (int slotIndex = 0; slotIndex < visibleSlotCount; slotIndex++) {
                PersonalDatabaseLayout.Rect slotRect = this.layout.visibleDatabaseSlotBounds(panelIndex, slotIndex);
                if (slotRect.contains(mouseX, mouseY)) {
                    return new DatabaseHitResult(panelIndex, slotIndex);
                }
            }
        }
        return null;
    }

    @Nullable
    private PersonalDatabaseLayout.Rect sortDropdownRect() {
        if (this.layout == null || this.sortButton == null) {
            return null;
        }
        int width = Math.max(SORT_DROPDOWN_WIDTH, this.layout.sortButtonRect().width());
        int height = DatabaseSortOption.orderedValues().size() * DROPDOWN_ROW_HEIGHT;
        int minX = this.layout.frameRect().x() + CONTEXT_MENU_MARGIN;
        int maxX = Math.max(minX, this.layout.frameRect().right() - width - CONTEXT_MENU_MARGIN);
        int x = Mth.clamp(this.layout.sortButtonRect().x(), minX, maxX);
        int minY = this.layout.sortButtonRect().bottom() + 2;
        int maxY = Math.max(minY, this.layout.frameRect().bottom() - height - CONTEXT_MENU_MARGIN);
        int y = Mth.clamp(minY, minY, maxY);
        return new PersonalDatabaseLayout.Rect(x, y, width, height);
    }

    private List<DatabasePagePickerModel.PageOption> pagePickerOptions() {
        DatabaseViewState viewState = this.menu.viewState();
        return DatabasePagePickerModel.build(viewState.totalPages(), viewState.query().pageIndex());
    }

    private Component pagePickerLabel(DatabasePagePickerModel.PageOption option) {
        return switch (option.shortcutType()) {
            case FIRST -> Component.translatable("screen.infiniteinventory.page_picker.first", 1);
            case LAST -> Component.translatable("screen.infiniteinventory.page_picker.last", this.menu.viewState().totalPages());
            case PAGE -> Component.literal(Integer.toString(option.pageIndex() + 1));
        };
    }

    @Nullable
    private PersonalDatabaseLayout.Rect pagePickerRect() {
        if (this.layout == null || !this.pagePickerExpanded) {
            return null;
        }
        List<DatabasePagePickerModel.PageOption> options = this.pagePickerOptions();
        int width = this.pagePickerWidth(options);
        int height = options.size() * PAGE_PICKER_ROW_HEIGHT;
        int minX = this.layout.frameRect().x() + CONTEXT_MENU_MARGIN;
        int maxX = Math.max(minX, this.layout.frameRect().right() - width - CONTEXT_MENU_MARGIN);
        int x = Mth.clamp(this.layout.pageLabelRect().centerX() - width / 2, minX, maxX);
        int minY = this.layout.pageLabelRect().bottom() + 2;
        int maxY = Math.max(minY, this.layout.frameRect().bottom() - height - CONTEXT_MENU_MARGIN);
        int y = Mth.clamp(minY, minY, maxY);
        return new PersonalDatabaseLayout.Rect(x, y, width, height);
    }

    private int pagePickerWidth(List<DatabasePagePickerModel.PageOption> options) {
        int width = PAGE_PICKER_MIN_WIDTH;
        for (DatabasePagePickerModel.PageOption option : options) {
            width = Math.max(width, this.font.width(this.pagePickerLabel(option)) + 16);
        }
        return width;
    }

    private int contextMenuWidth() {
        int width = CONTEXT_MENU_MIN_WIDTH;
        for (DatabaseClickAction action : CONTEXT_MENU_ACTIONS) {
            width = Math.max(width, this.font.width(this.contextMenuLabel(action)) + 16);
        }
        return width;
    }

    private void drawCenteredShadow(GuiGraphics guiGraphics, Component text, int left, int right, int y, int color) {
        int availableWidth = Math.max(0, right - left);
        int textWidth = this.font.width(text);
        int x = left + Math.max(0, (availableWidth - textWidth) / 2);
        guiGraphics.drawString(this.font, text, x, y, color, true);
    }
}
