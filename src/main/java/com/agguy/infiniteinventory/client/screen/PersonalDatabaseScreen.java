package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.compat.PlayerInventoryPaneProvider;
import com.agguy.infiniteinventory.compat.VanillaPlayerInventoryPaneProvider;
import com.agguy.infiniteinventory.database.DatabaseCategory;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseSearchConfig;
import com.agguy.infiniteinventory.database.DatabaseSearchField;
import com.agguy.infiniteinventory.database.DatabaseSearchWeight;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseSortOption;
import com.agguy.infiniteinventory.database.DatabaseViewState;
import com.agguy.infiniteinventory.database.VisibleDatabaseEntry;
import com.agguy.infiniteinventory.menu.PersonalDatabaseLayout;
import com.agguy.infiniteinventory.menu.PersonalDatabaseMenu;
import com.agguy.infiniteinventory.network.DatabaseClickAction;
import com.agguy.infiniteinventory.network.DatabaseClickPayload;
import com.agguy.infiniteinventory.network.DatabaseQueryPayload;
import com.agguy.infiniteinventory.network.DepositAllPayload;
import com.agguy.infiniteinventory.util.CompactNumberFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
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
    private static final int CONTEXT_MENU_WIDTH = 112;
    private static final int CONTEXT_MENU_ROW_HEIGHT = 20;
    private static final int CONTEXT_MENU_MARGIN = 4;
    private static final int SEARCH_ICON_SIZE = 9;
    private static final int SEARCH_TEXT_LEFT_PADDING = 18;
    private static final int ADVANCED_SEARCH_PANEL_WIDTH = 236;
    private static final int ADVANCED_SEARCH_PANEL_PADDING = 6;
    private static final int ADVANCED_SEARCH_TITLE_HEIGHT = 12;
    private static final int ADVANCED_SEARCH_ROW_HEIGHT = 20;
    private static final int ADVANCED_SEARCH_ROW_GAP = 2;
    private static final int ADVANCED_SEARCH_TOGGLE_WIDTH = 24;
    private static final int ADVANCED_SEARCH_WEIGHT_WIDTH = 40;
    private static final int TAB_ICON_SIZE = 16;
    private static final int TAB_ICON_LEFT_PADDING = 4;
    private static final int TAB_TEXT_GAP = 3;
    private static final DatabaseClickAction[] CONTEXT_MENU_ACTIONS = {
            DatabaseClickAction.TAKE_SINGLE,
            DatabaseClickAction.TAKE_STACK,
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
    private Button personalScopeButton;
    private Button publicScopeButton;
    private Button accessoriesToggleButton;
    private final Map<DatabaseSearchField, Button> advancedSearchToggleButtons = new EnumMap<>(DatabaseSearchField.class);
    private final Map<DatabaseSearchField, Button> advancedSearchWeightButtons = new EnumMap<>(DatabaseSearchField.class);
    private boolean syncingSearchBox;
    private boolean sortDropdownExpanded;
    private boolean advancedSearchExpanded;
    private boolean accessoriesExpanded;
    private boolean contextMenuExpanded;
    private int accessoryScrollRow;
    private int contextMenuSlotIndex = -1;
    private int contextMenuX;
    private int contextMenuY;
    private ItemStack contextMenuEntryStack = ItemStack.EMPTY;

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
        this.advancedSearchExpanded = false;
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
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderToolbarOverlays(guiGraphics);
        if (this.advancedSearchExpanded) {
            this.renderAdvancedSearchPanel(guiGraphics, mouseX, mouseY);
        }
        this.renderSearchHint(guiGraphics);
        if (this.sortDropdownExpanded) {
            this.renderSortDropdown(guiGraphics, mouseX, mouseY);
        }
        if (this.contextMenuExpanded) {
            this.renderContextMenu(guiGraphics, mouseX, mouseY);
        }
        this.renderCustomTooltips(guiGraphics, mouseX, mouseY);
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.advancedSearchExpanded && !this.isWithinAdvancedSearchPanel(mouseX, mouseY)) {
            this.advancedSearchExpanded = false;
        }
        if (this.advancedSearchExpanded && this.isWithinAdvancedSearchPanel(mouseX, mouseY)) {
            super.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        if (this.contextMenuExpanded && this.handleContextMenuClick(mouseX, mouseY)) {
            return true;
        }
        if (this.sortDropdownExpanded && this.handleSortDropdownClick(mouseX, mouseY)) {
            return true;
        }
        if (this.accessoriesExpanded && this.isWithinAccessoriesPanel(mouseX, mouseY)) {
            super.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        if (this.handleCategoryClick(mouseX, mouseY)) {
            return true;
        }
        if (this.handleDatabaseClick(mouseX, mouseY, button)) {
            return true;
        }
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (!handled) {
            this.sortDropdownExpanded = false;
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
        if (this.searchBox != null && this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBox != null && this.searchBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void rebuildLayout() {
        this.layout = PersonalDatabaseLayout.create(
                this.width,
                this.height,
                this.inventoryPaneProvider.equipmentPanelWidth(),
                this.inventoryPaneProvider.equipmentPanelHeight(),
                this.inventoryPaneProvider.bottomInventoryWidth(),
                this.inventoryPaneProvider.bottomInventoryHeight(),
                this.menu.accessorySlotGroups(),
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
                    this.advancedSearchExpanded = !this.advancedSearchExpanded;
                })
                .bounds(advancedSearchRect.x(), advancedSearchRect.y(), advancedSearchRect.width(), advancedSearchRect.height())
                .build());

        this.buildAdvancedSearchButtons();

        PersonalDatabaseLayout.Rect sortRect = this.layout.sortButtonRect();
        this.sortButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
                    this.closeContextMenu();
                    this.advancedSearchExpanded = false;
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
                    PacketDistributor.sendToServer(new DepositAllPayload(this.menu.containerId));
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

    private void sendSearchConfig(DatabaseQuery currentQuery, DatabaseSearchConfig newSearchConfig) {
        if (currentQuery.searchConfig().equals(newSearchConfig)) {
            return;
        }
        this.sendQuery(currentQuery.withSearchConfig(newSearchConfig));
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
        if (!this.menu.getCarried().isEmpty()) {
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
            if (currentQuery.pageSize() == this.pendingLayoutQuery.pageSize()) {
                this.pendingLayoutQuery = null;
            } else {
                return;
            }
        }
        int targetPageSize = this.layout.databaseSlotCount();
        if (currentQuery.pageSize() == targetPageSize) {
            return;
        }
        int firstVisibleEntryIndex = currentQuery.pageIndex() * Math.max(1, currentQuery.pageSize());
        DatabaseQuery adjustedQuery = currentQuery.withPageSize(targetPageSize).withPageIndex(firstVisibleEntryIndex / targetPageSize);
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
        PacketDistributor.sendToServer(new DatabaseQueryPayload(this.menu.containerId, query));
    }

    private void sendDatabaseClick(int slotIndex, DatabaseClickAction action) {
        PacketDistributor.sendToServer(new DatabaseClickPayload(this.menu.containerId, slotIndex, action));
    }

    private void prepareForServerQuery() {
        this.closeContextMenu();
        this.sortDropdownExpanded = false;
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
        DatabaseCategory selectedCategory = this.menu.viewState().query().category();
        DatabaseCategory[] categories = DatabaseCategory.values();
        for (int index = 0; index < categories.length; index++) {
            DatabaseCategory category = categories[index];
            PersonalDatabaseLayout.Rect tabRect = this.layout.tabBounds(index, categories.length);
            boolean selected = category == selectedCategory;
            boolean hovered = tabRect.contains(mouseX, mouseY);
            VanillaWidgetRenderer.renderTab(guiGraphics, tabRect, selected, hovered);
            guiGraphics.renderItem(this.categoryIcon(category), tabRect.x() + TAB_ICON_LEFT_PADDING, tabRect.y() + 4);

            String shortLabel = Component.translatable(this.categoryShortTranslationKey(category)).getString();
            int labelX = tabRect.x() + TAB_ICON_LEFT_PADDING + TAB_ICON_SIZE + TAB_TEXT_GAP;
            int labelWidth = Math.max(0, tabRect.right() - 4 - labelX);
            int color = selected ? 0x404040 : 0xFFFFFF;
            guiGraphics.drawString(
                    this.font,
                    this.truncateToWidth(shortLabel, labelWidth),
                    labelX,
                    tabRect.y() + 8,
                    color,
                    true
            );
        }
    }

    private void renderDatabaseSlots(GuiGraphics guiGraphics) {
        if (this.layout == null) {
            return;
        }
        for (int slotIndex = 0; slotIndex < this.layout.databaseSlotCount(); slotIndex++) {
            PersonalDatabaseLayout.Rect slotRect = this.layout.databaseSlotBounds(slotIndex);
            if (this.isCoveredByAccessoriesPanel(slotRect)) {
                continue;
            }
            VanillaWidgetRenderer.renderSlot(guiGraphics, slotRect.x(), slotRect.y());
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
        if (this.layout.accessoryTotalRows() > 0) {
            Component pageLabel = Component.translatable(
                    "screen.infiniteinventory.accessories_scroll",
                    this.layout.accessoryScrollRow() + 1,
                    Math.max(1, this.layout.accessoryTotalRows())
            );
            int indicatorRight = panelRect.right() - PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING;
            int indicatorLeft = Math.max(panelRect.x(), indicatorRight - 56);
            this.drawCenteredShadow(
                    guiGraphics,
                    pageLabel,
                    indicatorLeft,
                    indicatorRight,
                    panelRect.y() + PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING,
                    OVERLAY_MUTED_TEXT_COLOR
            );
        }
        guiGraphics.fill(
                panelRect.x() + PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING,
                panelRect.y() + PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING + PersonalDatabaseLayout.ACCESSORY_DRAWER_TITLE_HEIGHT + 1,
                panelRect.right() - PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING,
                panelRect.y() + PersonalDatabaseLayout.ACCESSORY_DRAWER_PADDING + PersonalDatabaseLayout.ACCESSORY_DRAWER_TITLE_HEIGHT + 2,
                0x70A89E8C
        );
        PersonalDatabaseLayout.AccessorySlotLayout hoveredSlot = this.findHoveredAccessorySlot(mouseX, mouseY);
        if (hoveredSlot != null) {
            PersonalDatabaseLayout.Rect slotRect = hoveredSlot.slotRect();
            guiGraphics.fill(slotRect.x() + 1, slotRect.y() + 1, slotRect.right() - 1, slotRect.bottom() - 1, 0x35000000);
        }
        guiGraphics.pose().popPose();
    }

    private void renderDatabaseEntries(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.layout == null) {
            return;
        }
        List<VisibleDatabaseEntry> entries = this.menu.viewState().entries();
        for (int slotIndex = 0; slotIndex < this.layout.databaseSlotCount(); slotIndex++) {
            PersonalDatabaseLayout.Rect slotRect = this.layout.databaseSlotBounds(slotIndex);
            if (this.isCoveredByAccessoriesPanel(slotRect)) {
                continue;
            }
            if (slotRect.contains(mouseX, mouseY)) {
                VanillaWidgetRenderer.renderSlotHighlight(guiGraphics, slotRect);
            }
            if (slotIndex < entries.size()) {
                VisibleDatabaseEntry entry = entries.get(slotIndex);
                ItemStack stack = entry.stack();
                int itemX = slotRect.x() + (PersonalDatabaseLayout.DATABASE_SLOT_SIZE - 16) / 2;
                int itemY = slotRect.y() + (PersonalDatabaseLayout.DATABASE_SLOT_SIZE - 16) / 2;
                guiGraphics.renderItem(stack, itemX, itemY);
                guiGraphics.renderItemDecorations(this.font, stack, itemX, itemY, CompactNumberFormatter.format(entry.amount()));
            }
        }
    }

    private void renderEmptyState(GuiGraphics guiGraphics) {
        if (this.layout == null || !this.menu.viewState().entries().isEmpty()) {
            return;
        }
        DatabaseQuery query = this.menu.viewState().query();
        Component message = Component.translatable(query.searchText().isEmpty()
                ? query.scope().emptyTranslationKey()
                : "screen.infiniteinventory.no_results");
        PersonalDatabaseLayout.Rect gridRect = this.layout.databaseGridRect();
        int y = gridRect.y() + Math.max(0, gridRect.height() / 2 - 4);
        this.drawCenteredShadow(guiGraphics, message, gridRect.x(), gridRect.right(), y, 0x7A7A7A);
    }

    private void renderFrameText(GuiGraphics guiGraphics) {
        if (this.layout == null) {
            return;
        }
        DatabaseViewState viewState = this.menu.viewState();
        guiGraphics.drawString(this.font, this.title, this.layout.titleRect().x(), this.layout.titleRect().y() + 6, 0x404040, true);

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

    private void renderContextMenu(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!this.contextMenuExpanded) {
            return;
        }
        PersonalDatabaseLayout.Rect menuRect = new PersonalDatabaseLayout.Rect(
                this.contextMenuX,
                this.contextMenuY,
                CONTEXT_MENU_WIDTH,
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

    private boolean handleCategoryClick(double mouseX, double mouseY) {
        if (this.layout == null) {
            return false;
        }
        DatabaseCategory[] categories = DatabaseCategory.values();
        for (int index = 0; index < categories.length; index++) {
            DatabaseCategory category = categories[index];
            PersonalDatabaseLayout.Rect tabRect = this.layout.tabBounds(index, categories.length);
            if (!tabRect.contains(mouseX, mouseY)) {
                continue;
            }
            this.closeContextMenu();
            this.sortDropdownExpanded = false;
            if (category != this.menu.viewState().query().category()) {
                this.sendQuery(this.menu.viewState().query().withCategory(category));
            }
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

    private boolean handleContextMenuClick(double mouseX, double mouseY) {
        if (!this.contextMenuExpanded) {
            return false;
        }
        for (int index = 0; index < CONTEXT_MENU_ACTIONS.length; index++) {
            int rowY = this.contextMenuY + index * CONTEXT_MENU_ROW_HEIGHT;
            if (mouseX < this.contextMenuX || mouseX >= this.contextMenuX + CONTEXT_MENU_WIDTH || mouseY < rowY || mouseY >= rowY + CONTEXT_MENU_ROW_HEIGHT) {
                continue;
            }
            this.sendDatabaseClick(this.contextMenuSlotIndex, CONTEXT_MENU_ACTIONS[index]);
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
        int slotIndex = this.findDatabaseSlot(mouseX, mouseY);
        boolean carryingStack = !this.menu.getCarried().isEmpty();
        if (slotIndex < 0 || (slotIndex >= this.menu.viewState().entries().size() && !carryingStack)) {
            return false;
        }
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
            this.sendDatabaseClick(slotIndex, action);
            return true;
        }
        if (button == 0) {
            this.closeContextMenu();
            this.sortDropdownExpanded = false;
            DatabaseClickAction action = hasShiftDown()
                    ? DatabaseClickAction.TAKE_STACK_TO_INVENTORY
                    : DatabaseClickAction.TAKE_SINGLE;
            this.sendDatabaseClick(slotIndex, action);
            return true;
        }
        if (button == 1) {
            this.sortDropdownExpanded = false;
            this.openContextMenu(slotIndex);
            return true;
        }
        return false;
    }

    private void renderCustomTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.contextMenuExpanded || this.sortDropdownExpanded || this.advancedSearchExpanded) {
            return;
        }
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
        int slotIndex = this.findDatabaseSlot(mouseX, mouseY);
        if (slotIndex >= 0 && slotIndex < this.menu.viewState().entries().size()) {
            VisibleDatabaseEntry entry = this.menu.viewState().entries().get(slotIndex);
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(entry.stack().getHoverName());
            tooltip.add(Component.translatable("screen.infiniteinventory.tooltip.amount", CompactNumberFormatter.format(entry.amount())).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable(entry.category().translationKey()).withStyle(ChatFormatting.BLUE));
            tooltip.add(Component.literal(entry.registryName()).withStyle(ChatFormatting.DARK_GRAY));
            guiGraphics.renderTooltip(this.font, tooltip, ItemStack.EMPTY.getTooltipImage(), mouseX, mouseY);
            return;
        }
        DatabaseCategory hoveredCategory = this.findHoveredCategory(mouseX, mouseY);
        if (hoveredCategory != null) {
            guiGraphics.renderTooltip(
                    this.font,
                    List.of(Component.translatable(hoveredCategory.translationKey())),
                    ItemStack.EMPTY.getTooltipImage(),
                    mouseX,
                    mouseY
            );
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

        if (this.advancedSearchButton != null && this.layout.advancedSearchButtonRect().width() > 0) {
            PersonalDatabaseLayout.Rect advancedRect = this.layout.advancedSearchButtonRect();
            VanillaWidgetRenderer.renderDropdownIndicator(
                    guiGraphics,
                    advancedRect.right() - 10,
                    advancedRect.y() + advancedRect.height() / 2,
                    0xFF3F3F3F
            );
        }
    }

    private void renderDatabaseScaffold(GuiGraphics guiGraphics) {
        if (this.layout == null) {
            return;
        }
        VanillaWidgetRenderer.renderPanel(guiGraphics, this.layout.databasePanelRect());
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

    @Nullable
    private DatabaseCategory findHoveredCategory(double mouseX, double mouseY) {
        if (this.layout == null) {
            return null;
        }
        DatabaseCategory[] categories = DatabaseCategory.values();
        for (int index = 0; index < categories.length; index++) {
            if (this.layout.tabBounds(index, categories.length).contains(mouseX, mouseY)) {
                return categories[index];
            }
        }
        return null;
    }

    private String categoryShortTranslationKey(DatabaseCategory category) {
        return switch (category) {
            case ALL -> "screen.infiniteinventory.category_short.all";
            case BLOCKS -> "screen.infiniteinventory.category_short.blocks";
            case TOOLS_WEAPONS -> "screen.infiniteinventory.category_short.tools_weapons";
            case EQUIPMENT -> "screen.infiniteinventory.category_short.equipment";
            case CONSUMABLES -> "screen.infiniteinventory.category_short.consumables";
            case MATERIALS -> "screen.infiniteinventory.category_short.materials";
            case OTHER -> "screen.infiniteinventory.category_short.other";
        };
    }

    private ItemStack categoryIcon(DatabaseCategory category) {
        return switch (category) {
            case ALL -> new ItemStack(Items.CHEST);
            case BLOCKS -> new ItemStack(Items.GRASS_BLOCK);
            case TOOLS_WEAPONS -> new ItemStack(Items.IRON_SWORD);
            case EQUIPMENT -> new ItemStack(Items.IRON_CHESTPLATE);
            case CONSUMABLES -> new ItemStack(Items.GOLDEN_CARROT);
            case MATERIALS -> new ItemStack(Items.IRON_INGOT);
            case OTHER -> new ItemStack(Items.COMPASS);
        };
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

    private void validateContextMenu(DatabaseViewState viewState) {
        if (!this.contextMenuExpanded) {
            return;
        }
        if (this.contextMenuSlotIndex < 0 || this.contextMenuSlotIndex >= viewState.entries().size()) {
            this.closeContextMenu();
            return;
        }
        ItemStack currentStack = viewState.entries().get(this.contextMenuSlotIndex).stack();
        if (!ItemStack.isSameItemSameComponents(this.contextMenuEntryStack, currentStack)) {
            this.closeContextMenu();
        }
    }

    private void openContextMenu(int slotIndex) {
        if (this.layout == null) {
            return;
        }
        List<VisibleDatabaseEntry> entries = this.menu.viewState().entries();
        if (slotIndex < 0 || slotIndex >= entries.size()) {
            this.closeContextMenu();
            return;
        }
        VisibleDatabaseEntry entry = entries.get(slotIndex);
        PersonalDatabaseLayout.Rect slotRect = this.layout.databaseSlotBounds(slotIndex);
        int menuHeight = CONTEXT_MENU_ACTIONS.length * CONTEXT_MENU_ROW_HEIGHT;
        PersonalDatabaseLayout.Rect frameRect = this.layout.frameRect();
        int minX = frameRect.x() + CONTEXT_MENU_MARGIN;
        int maxX = Math.max(minX, frameRect.right() - CONTEXT_MENU_WIDTH - CONTEXT_MENU_MARGIN);
        int preferredX = slotRect.right() + 2;
        if (preferredX > maxX) {
            preferredX = slotRect.x() - CONTEXT_MENU_WIDTH - 2;
        }
        this.contextMenuX = Mth.clamp(preferredX, minX, maxX);

        int minY = frameRect.y() + CONTEXT_MENU_MARGIN;
        int maxY = Math.max(minY, frameRect.bottom() - menuHeight - CONTEXT_MENU_MARGIN);
        this.contextMenuY = Mth.clamp(slotRect.y(), minY, maxY);
        this.contextMenuSlotIndex = slotIndex;
        this.contextMenuEntryStack = entry.stack().copyWithCount(1);
        this.contextMenuExpanded = true;
    }

    private void closeContextMenu() {
        this.contextMenuExpanded = false;
        this.contextMenuSlotIndex = -1;
        this.contextMenuEntryStack = ItemStack.EMPTY;
    }

    private Component contextMenuLabel(DatabaseClickAction action) {
        return switch (action) {
            case TAKE_SINGLE -> Component.translatable("screen.infiniteinventory.context.take_single");
            case TAKE_STACK -> Component.translatable("screen.infiniteinventory.context.take_stack");
            case TAKE_ALL -> Component.translatable("screen.infiniteinventory.context.take_all");
            default -> Component.empty();
        };
    }

    private boolean isWithinContextMenu(double mouseX, double mouseY) {
        if (!this.contextMenuExpanded) {
            return false;
        }
        return mouseX >= this.contextMenuX
                && mouseX < this.contextMenuX + CONTEXT_MENU_WIDTH
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

    private boolean isCoveredByAccessoriesPanel(PersonalDatabaseLayout.Rect rect) {
        return this.accessoriesExpanded
                && this.layout != null
                && this.layout.accessoriesPanelRect().height() > 0
                && this.layout.accessoriesPanelRect().intersects(rect);
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

    private int findDatabaseSlot(double mouseX, double mouseY) {
        if (this.layout == null) {
            return -1;
        }
        for (int slotIndex = 0; slotIndex < this.layout.databaseSlotCount(); slotIndex++) {
            PersonalDatabaseLayout.Rect slotRect = this.layout.databaseSlotBounds(slotIndex);
            if (this.isCoveredByAccessoriesPanel(slotRect)) {
                continue;
            }
            if (slotRect.contains(mouseX, mouseY)) {
                return slotIndex;
            }
        }
        return -1;
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

    private void drawCenteredShadow(GuiGraphics guiGraphics, Component text, int left, int right, int y, int color) {
        int availableWidth = Math.max(0, right - left);
        int textWidth = this.font.width(text);
        int x = left + Math.max(0, (availableWidth - textWidth) / 2);
        guiGraphics.drawString(this.font, text, x, y, color, true);
    }
}
