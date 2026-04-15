package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.compat.PlayerInventoryPaneProvider;
import com.agguy.infiniteinventory.compat.VanillaPlayerInventoryPaneProvider;
import com.agguy.infiniteinventory.database.DatabaseCategory;
import com.agguy.infiniteinventory.database.DatabaseEnhancementOption;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseSearchField;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import com.agguy.infiniteinventory.menu.PersonalDatabaseMenu;
import com.agguy.infiniteinventory.network.DatabaseClickAction;
import java.util.ArrayList;
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

public final class PersonalDatabaseScreen extends AbstractContainerScreen<PersonalDatabaseMenu> {
    static final int OVERLAY_TEXT_COLOR = 0x3D342B;
    static final int OVERLAY_MUTED_TEXT_COLOR = 0x6B6257;
    static final int OVERLAY_ACCENT_TEXT_COLOR = 0x5A4523;
    static final int DROPDOWN_ROW_HEIGHT = 20;
    static final int SORT_DROPDOWN_WIDTH = 168;
    static final int CONTEXT_MENU_MIN_WIDTH = 112;
    static final int CONTEXT_MENU_ROW_HEIGHT = 20;
    static final int CONTEXT_MENU_MARGIN = 4;
    static final int PAGE_PICKER_MIN_WIDTH = 88;
    static final int PAGE_PICKER_ROW_HEIGHT = 20;
    static final int SEARCH_ICON_SIZE = 9;
    static final int SEARCH_TEXT_LEFT_PADDING = 18;
    static final int ADVANCED_SEARCH_PANEL_WIDTH = 236;
    static final int ADVANCED_SEARCH_PANEL_PADDING = 6;
    static final int ADVANCED_SEARCH_TITLE_HEIGHT = 12;
    static final int ADVANCED_SEARCH_ROW_HEIGHT = 20;
    static final int ADVANCED_SEARCH_ROW_GAP = 2;
    static final int ADVANCED_SEARCH_TOGGLE_WIDTH = 24;
    static final int ADVANCED_SEARCH_WEIGHT_WIDTH = 40;
    static final int ENHANCEMENT_PANEL_WIDTH = 236;
    static final int ENHANCEMENT_PANEL_PADDING = 6;
    static final int ENHANCEMENT_TITLE_HEIGHT = 12;
    static final int ENHANCEMENT_ROW_HEIGHT = 20;
    static final int ENHANCEMENT_ROW_GAP = 2;
    static final int ENHANCEMENT_TOGGLE_WIDTH = 24;
    static final int TAB_SELECTOR_WIDTH = 220;
    static final int TAB_SELECTOR_ROW_HEIGHT = 20;
    static final int MORE_TABS_WIDTH = 180;
    static final int TARGET_SELECTOR_WIDTH = 180;
    static final int TARGET_SELECTOR_ROW_HEIGHT = 20;
    static final int MANAGEMENT_PANEL_WIDTH = 320;
    static final int MANAGEMENT_PANEL_HEIGHT = 260;
    static final int MANAGEMENT_ROW_HEIGHT = 20;
    static final int MANAGEMENT_LIST_WIDTH = 124;
    static final int MANAGEMENT_BUTTON_WIDTH = 76;
    static final int ICON_PICKER_WIDTH = 900;
    static final int ICON_PICKER_HEIGHT = 620;
    static final int MANAGEMENT_PANEL_PADDING = 8;
    static final int OVERLAY_SECTION_TITLE_HEIGHT = 12;
    static final int INLINE_TAB_MIN_WIDTH = 88;
    static final int INLINE_TAB_MIN_WIDTH_WITH_MORE = 72;
    static final int INLINE_TAB_TIGHT_GAP = 2;
    static final int INLINE_TAB_MORE_WIDTH = 56;
    static final int ICON_PICKER_COLUMNS = 6;
    static final int ICON_PICKER_ROWS = 4;
    static final int ICON_PICKER_CELL_SIZE = 40;
    static final int ICON_PICKER_MAX_RESULTS = ICON_PICKER_COLUMNS * ICON_PICKER_ROWS;
    static final int TAB_ICON_SIZE = 16;
    static final int TAB_ICON_LEFT_PADDING = 4;
    static final int TAB_TEXT_GAP = 3;
    static final DatabaseClickAction[] CONTEXT_MENU_ACTIONS = {
            DatabaseClickAction.TAKE_SINGLE,
            DatabaseClickAction.TAKE_HALF_STACK_TO_INVENTORY,
            DatabaseClickAction.TAKE_STACK,
            DatabaseClickAction.TAKE_HALF_ENTRY_TO_INVENTORY,
            DatabaseClickAction.TAKE_ALL
    };

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
    final Map<DatabaseSearchField, Button> advancedSearchToggleButtons = new EnumMap<>(DatabaseSearchField.class);
    final Map<DatabaseSearchField, Button> advancedSearchWeightButtons = new EnumMap<>(DatabaseSearchField.class);
    final Map<DatabaseEnhancementOption, Button> enhancementToggleButtons = new EnumMap<>(DatabaseEnhancementOption.class);
    boolean syncingSearchBox;
    boolean sortDropdownExpanded;
    boolean pagePickerExpanded;
    int activeSortPanelIndex = -1;
    int activePagePickerPanelIndex = -1;
    boolean advancedSearchExpanded;
    boolean enhancementPanelExpanded;
    boolean viewSelectorExpanded;
    boolean moreTabsExpanded;
    boolean targetSelectorExpanded;
    boolean tabManagementExpanded;
    boolean iconPickerExpanded;
    boolean accessoriesExpanded;
    boolean contextMenuExpanded;
    boolean suppressVanillaTooltipRender;
    int accessoryScrollRow;
    int contextMenuPanelIndex = -1;
    int contextMenuSlotIndex = -1;
    int contextMenuX;
    int contextMenuY;
    ItemStack contextMenuEntryStack = ItemStack.EMPTY;
    String pendingTargetSourceTabId = "";
    int pendingTargetPanelIndex = -1;
    int pendingQuickDepositSlotIndex = -1;
    String managementSelectedTabId = com.agguy.infiniteinventory.database.DatabaseTabs.DEFAULT_TAB_ID;
    String pendingIconItemId = com.agguy.infiniteinventory.database.DatabaseTabs.DEFAULT_CONCRETE_ICON_ITEM_ID;
    com.agguy.infiniteinventory.database.DatabaseScope managementSnapshotScope = com.agguy.infiniteinventory.database.DatabaseScope.PERSONAL;
    String managementSnapshotTabId = "";
    String managementSnapshotName = "";
    String managementSnapshotIconItemId = "";
    DatabaseCategory iconPickerCategory = DatabaseCategory.ALL;
    int iconPickerPageIndex;
    String iconPickerOriginalItemId = com.agguy.infiniteinventory.database.DatabaseTabs.DEFAULT_CONCRETE_ICON_ITEM_ID;
    String lastUiSignature = "";
    boolean pendingTargetStoresSingle;
    TargetSelectorMode targetSelectorMode = TargetSelectorMode.NONE;
    int targetSelectorScrollIndex;

    enum TargetSelectorMode {
        NONE,
        DEPOSIT_ALL,
        CARRIED_STORE,
        QUICK_DEPOSIT,
        TRANSFER_TAB,
        DELETE_TAB,
        AUTO_STORE_TARGET
    }

    record DatabaseHitResult(int panelIndex, int slotIndex) {
    }

    record IconChoice(String itemId, ItemStack previewStack, String searchableText, DatabaseCategory category) {
    }

    public PersonalDatabaseScreen(PersonalDatabaseMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.databaseMenu = menu;
        this.databaseScreenTitle = title;
        this.inventoryLabelY = Integer.MAX_VALUE;
        this.titleLabelY = Integer.MAX_VALUE;
    }

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
        this.targetSelectorExpanded = false;
        this.tabManagementExpanded = false;
        this.iconPickerExpanded = false;
        this.targetSelectorScrollIndex = 0;
        PersonalDatabaseScreenContextHelper.closeContextMenu(this);
        PersonalDatabaseScreenWidgetHelper.buildWidgets(this);
        PersonalDatabaseScreenWidgetHelper.syncWidgetsFromState(this);
        PersonalDatabaseScreenLayoutHelper.ensureLayoutQuerySynced(this);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        PersonalDatabaseScreenLayoutHelper.refreshUiStructureIfNeeded(this);
        PersonalDatabaseScreenWidgetHelper.syncWidgetsFromState(this);
        PersonalDatabaseScreenLayoutHelper.ensureLayoutQuerySynced(this);
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
        PersonalDatabaseScreenRenderHelper.renderScreenTooltips(this, guiGraphics, mouseX, mouseY);
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
        PersonalDatabaseScreenRenderHelper.renderBg(this, guiGraphics, partialTick, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        PersonalDatabaseScreenRenderHelper.renderSlot(this, guiGraphics, slot);
        super.renderSlot(guiGraphics, slot);
    }

    @Override
    protected void renderSlotHighlight(GuiGraphics guiGraphics, Slot slot, int mouseX, int mouseY, float partialTick) {
        if (PersonalDatabaseScreenRenderHelper.shouldSkipSlotHighlight(this, slot)) {
            return;
        }
        super.renderSlotHighlight(guiGraphics, slot, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return PersonalDatabaseScreenInteractionHelper.mouseClicked(this, mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return PersonalDatabaseScreenInteractionHelper.mouseScrolled(this, mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return PersonalDatabaseScreenInteractionHelper.keyPressed(this, keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return PersonalDatabaseScreenInteractionHelper.charTyped(this, codePoint, modifiers);
    }

    Font screenFont() {
        return this.font;
    }

    @Nullable
    Minecraft minecraftClient() {
        return this.minecraft;
    }

    @Nullable
    Slot hoveredSlotRef() {
        return this.hoveredSlot;
    }

    int screenWidthValue() {
        return this.width;
    }

    int screenHeightValue() {
        return this.height;
    }

    Button addScreenButton(Button button) {
        return super.addRenderableWidget(button);
    }

    EditBox addScreenEditBox(EditBox editBox) {
        return super.addRenderableWidget(editBox);
    }

    void clearScreenWidgets() {
        super.clearWidgets();
    }

    boolean invokeSuperMouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    boolean invokeSuperMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    boolean invokeSuperKeyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    boolean invokeSuperCharTyped(char codePoint, int modifiers) {
        return super.charTyped(codePoint, modifiers);
    }

    void invokeSuperRenderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    java.util.List<Component> containerTooltip(ItemStack stack) {
        return this.getTooltipFromContainerItem(stack);
    }

    void focusScreen(@Nullable GuiEventListener listener) {
        this.setFocused(listener);
    }
}
