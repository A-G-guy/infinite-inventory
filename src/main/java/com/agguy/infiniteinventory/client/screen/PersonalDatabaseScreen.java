package com.agguy.infiniteinventory.client.screen;

import com.agguy.infiniteinventory.compat.PlayerInventoryPaneProvider;
import com.agguy.infiniteinventory.compat.VanillaPlayerInventoryPaneProvider;
import com.agguy.infiniteinventory.database.DatabaseCategory;
import com.agguy.infiniteinventory.database.DatabaseQuery;
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
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PersonalDatabaseScreen extends AbstractContainerScreen<PersonalDatabaseMenu> {
    private static final ResourceLocation DATABASE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int HEADER_HEIGHT = 94;
    private static final int TAB_HEIGHT = 20;
    private static final int TAB_GAP = 4;
    private static final int DROPDOWN_ROW_HEIGHT = 18;

    private final PlayerInventoryPaneProvider inventoryPaneProvider = new VanillaPlayerInventoryPaneProvider();
    private EditBox searchBox;
    private Button depositButton;
    private Button previousPageButton;
    private Button nextPageButton;
    private Button sortButton;
    private boolean syncingSearchBox;
    private boolean sortDropdownExpanded;

    public PersonalDatabaseScreen(PersonalDatabaseMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = PersonalDatabaseLayout.SCREEN_WIDTH;
        this.imageHeight = PersonalDatabaseLayout.SCREEN_HEIGHT;
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        int searchX = this.leftPos + PersonalDatabaseLayout.DATABASE_PANEL_X + 8;
        int searchY = this.topPos + 50;
        this.searchBox = new EditBox(this.font, searchX, searchY, 92, 16, Component.translatable("screen.infiniteinventory.search"));
        this.searchBox.setMaxLength(DatabaseQuery.MAX_SEARCH_LENGTH);
        this.searchBox.setBordered(true);
        this.searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(this.searchBox);

        this.sortButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> this.sortDropdownExpanded = !this.sortDropdownExpanded)
                .bounds(searchX + 96, searchY, 64, 16)
                .build());
        this.depositButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.infiniteinventory.deposit_all"), button -> {
                    this.sortDropdownExpanded = false;
                    PacketDistributor.sendToServer(new DepositAllPayload(this.menu.containerId));
                })
                .bounds(searchX, searchY + 22, 78, 20)
                .build());
        this.previousPageButton = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> this.changePage(-1))
                .bounds(searchX + 84, searchY + 22, 20, 20)
                .build());
        this.nextPageButton = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> this.changePage(1))
                .bounds(searchX + 108, searchY + 22, 20, 20)
                .build());
        this.syncWidgetsFromState();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.syncWidgetsFromState();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderCustomTooltips(guiGraphics, mouseX, mouseY);
        if (this.sortDropdownExpanded) {
            this.renderSortDropdown(guiGraphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        guiGraphics.fill(left, top, left + this.imageWidth, top + HEADER_HEIGHT, 0xC0101010);
        guiGraphics.fill(left, top + HEADER_HEIGHT - 1, left + this.imageWidth, top + HEADER_HEIGHT, 0xFF8B8B8B);
        guiGraphics.fill(left, top, left + this.imageWidth, top + 1, 0xFFFFFFFF);
        guiGraphics.fill(left, top, left + 1, top + HEADER_HEIGHT, 0xFFFFFFFF);
        guiGraphics.fill(left + this.imageWidth - 1, top, left + this.imageWidth, top + HEADER_HEIGHT, 0xFF373737);

        this.inventoryPaneProvider.render(
                guiGraphics,
                this.minecraft.player,
                left + PersonalDatabaseLayout.PLAYER_PANEL_X,
                top + PersonalDatabaseLayout.PLAYER_PANEL_Y,
                mouseX,
                mouseY
        );
        guiGraphics.blit(
                DATABASE_TEXTURE,
                left + PersonalDatabaseLayout.DATABASE_PANEL_X,
                top + PersonalDatabaseLayout.DATABASE_PANEL_Y,
                0,
                0,
                PersonalDatabaseLayout.DATABASE_PANEL_WIDTH,
                PersonalDatabaseLayout.DATABASE_PANEL_HEIGHT
        );

        this.renderTabs(guiGraphics, mouseX, mouseY);
        this.renderDatabaseEntries(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        DatabaseViewState viewState = this.menu.viewState();
        int titleColor = 0x404040;
        guiGraphics.drawString(this.font, this.title, 8, 8, titleColor, false);
        guiGraphics.drawString(this.font, Component.translatable("container.inventory"), 8, PersonalDatabaseLayout.PLAYER_PANEL_Y - 12, titleColor, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.infiniteinventory.database.section"), PersonalDatabaseLayout.DATABASE_PANEL_X + 8, PersonalDatabaseLayout.DATABASE_PANEL_Y - 12, titleColor, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.infiniteinventory.search"), PersonalDatabaseLayout.DATABASE_PANEL_X + 8, 38, titleColor, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.infiniteinventory.total_entries", viewState.totalEntries()), PersonalDatabaseLayout.DATABASE_PANEL_X + 8, 76, titleColor, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.infiniteinventory.total_items", CompactNumberFormatter.format(viewState.totalItems())), PersonalDatabaseLayout.DATABASE_PANEL_X + 8, 88, titleColor, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.infiniteinventory.page", viewState.query().pageIndex() + 1, viewState.totalPages()), PersonalDatabaseLayout.DATABASE_PANEL_X + 112, 76, titleColor, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.sortDropdownExpanded && this.handleSortDropdownClick(mouseX, mouseY)) {
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
        }
        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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

    private void syncWidgetsFromState() {
        DatabaseViewState viewState = this.menu.viewState();
        DatabaseQuery query = viewState.query();
        if (this.searchBox != null && !this.searchBox.isFocused() && !this.searchBox.getValue().equals(query.searchText())) {
            this.syncingSearchBox = true;
            this.searchBox.setValue(query.searchText());
            this.syncingSearchBox = false;
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
        this.sortDropdownExpanded = false;
        PacketDistributor.sendToServer(new DatabaseQueryPayload(this.menu.containerId, query));
    }

    private void renderTabs(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        DatabaseCategory selectedCategory = this.menu.viewState().query().category();
        int tabWidth = (this.imageWidth - 16 - TAB_GAP * (DatabaseCategory.values().length - 1)) / DatabaseCategory.values().length;
        int tabY = this.topPos + 18;
        for (int index = 0; index < DatabaseCategory.values().length; index++) {
            DatabaseCategory category = DatabaseCategory.values()[index];
            int x = this.leftPos + 8 + index * (tabWidth + TAB_GAP);
            boolean selected = category == selectedCategory;
            boolean hovered = this.isWithinAbsolute(x, tabY, tabWidth, TAB_HEIGHT, mouseX, mouseY);
            int fillColor = selected ? 0xFFC6C6C6 : hovered ? 0xFF8B8B8B : 0xFF555555;
            int borderColor = selected ? 0xFFFFFFFF : 0xFF2B2B2B;
            guiGraphics.fill(x, tabY, x + tabWidth, tabY + TAB_HEIGHT, fillColor);
            guiGraphics.fill(x, tabY, x + tabWidth, tabY + 1, borderColor);
            guiGraphics.fill(x, tabY, x + 1, tabY + TAB_HEIGHT, borderColor);
            guiGraphics.fill(x + tabWidth - 1, tabY, x + tabWidth, tabY + TAB_HEIGHT, 0xFF2B2B2B);
            guiGraphics.fill(x, tabY + TAB_HEIGHT - 1, x + tabWidth, tabY + TAB_HEIGHT, 0xFF2B2B2B);
            guiGraphics.drawCenteredString(this.font, Component.translatable(category.translationKey()), x + tabWidth / 2, tabY + 6, selected ? 0x202020 : 0xF0F0F0);
        }
    }

    private void renderDatabaseEntries(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<VisibleDatabaseEntry> entries = this.menu.viewState().entries();
        for (int slotIndex = 0; slotIndex < PersonalDatabaseLayout.DATABASE_SLOT_COUNT; slotIndex++) {
            int slotX = this.leftPos + PersonalDatabaseLayout.databaseSlotX(slotIndex);
            int slotY = this.topPos + PersonalDatabaseLayout.databaseSlotY(slotIndex);
            if (slotIndex < entries.size()) {
                VisibleDatabaseEntry entry = entries.get(slotIndex);
                ItemStack stack = entry.stack();
                guiGraphics.renderItem(stack, slotX + 1, slotY + 1);
                guiGraphics.renderItemDecorations(this.font, stack, slotX + 1, slotY + 1, CompactNumberFormatter.format(entry.amount()));
            }
            if (this.isWithinAbsolute(slotX, slotY, 18, 18, mouseX, mouseY)) {
                guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0x66FFFFFF);
            }
        }
    }

    private void renderSortDropdown(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.sortButton == null) {
            return;
        }
        int x = this.sortButton.getX();
        int y = this.sortButton.getY() + this.sortButton.getHeight();
        int width = 84;
        int height = DatabaseSortOption.values().length * DROPDOWN_ROW_HEIGHT;
        guiGraphics.fill(x, y, x + width, y + height, 0xFF2F2F2F);
        guiGraphics.fill(x, y, x + width, y + 1, 0xFFFFFFFF);
        guiGraphics.fill(x, y, x + 1, y + height, 0xFFFFFFFF);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFF1B1B1B);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFF1B1B1B);
        DatabaseSortOption currentSort = this.menu.viewState().query().sortOption();
        for (int index = 0; index < DatabaseSortOption.values().length; index++) {
            DatabaseSortOption option = DatabaseSortOption.values()[index];
            int rowY = y + index * DROPDOWN_ROW_HEIGHT;
            boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + DROPDOWN_ROW_HEIGHT;
            if (hovered) {
                guiGraphics.fill(x + 1, rowY + 1, x + width - 1, rowY + DROPDOWN_ROW_HEIGHT - 1, 0x6666A3D2);
            }
            Component label = currentSort == option
                    ? Component.translatable(option.translationKey()).withStyle(ChatFormatting.GOLD)
                    : Component.translatable(option.translationKey());
            guiGraphics.drawString(this.font, label, x + 4, rowY + 5, 0xF0F0F0, false);
        }
    }

    private boolean handleCategoryClick(double mouseX, double mouseY) {
        int tabWidth = (this.imageWidth - 16 - TAB_GAP * (DatabaseCategory.values().length - 1)) / DatabaseCategory.values().length;
        int tabY = this.topPos + 18;
        for (int index = 0; index < DatabaseCategory.values().length; index++) {
            DatabaseCategory category = DatabaseCategory.values()[index];
            int x = this.leftPos + 8 + index * (tabWidth + TAB_GAP);
            if (!this.isWithinAbsolute(x, tabY, tabWidth, TAB_HEIGHT, mouseX, mouseY)) {
                continue;
            }
            if (category != this.menu.viewState().query().category()) {
                this.sendQuery(this.menu.viewState().query().withCategory(category));
            }
            return true;
        }
        return false;
    }

    private boolean handleSortDropdownClick(double mouseX, double mouseY) {
        if (this.sortButton == null) {
            return false;
        }
        int x = this.sortButton.getX();
        int y = this.sortButton.getY() + this.sortButton.getHeight();
        int width = 84;
        for (int index = 0; index < DatabaseSortOption.values().length; index++) {
            int rowY = y + index * DROPDOWN_ROW_HEIGHT;
            if (!this.isWithinAbsolute(x, rowY, width, DROPDOWN_ROW_HEIGHT, mouseX, mouseY)) {
                continue;
            }
            this.sendQuery(this.menu.viewState().query().withSortOption(DatabaseSortOption.values()[index]));
            return true;
        }
        if (!this.isWithinAbsolute(this.sortButton.getX(), this.sortButton.getY(), this.sortButton.getWidth(), this.sortButton.getHeight(), mouseX, mouseY)) {
            this.sortDropdownExpanded = false;
        }
        return false;
    }

    private boolean handleDatabaseClick(double mouseX, double mouseY, int button) {
        int slotIndex = this.findDatabaseSlot(mouseX, mouseY);
        if (slotIndex < 0 || slotIndex >= this.menu.viewState().entries().size()) {
            return false;
        }
        DatabaseClickAction action;
        if (hasShiftDown()) {
            action = DatabaseClickAction.QUICK_MOVE;
        } else if (button == 1) {
            action = DatabaseClickAction.SECONDARY;
        } else if (button == 0) {
            action = DatabaseClickAction.PRIMARY;
        } else {
            return false;
        }
        PacketDistributor.sendToServer(new DatabaseClickPayload(this.menu.containerId, slotIndex, action));
        return true;
    }

    private void renderCustomTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int slotIndex = this.findDatabaseSlot(mouseX, mouseY);
        if (slotIndex < 0 || slotIndex >= this.menu.viewState().entries().size()) {
            return;
        }
        VisibleDatabaseEntry entry = this.menu.viewState().entries().get(slotIndex);
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(entry.stack().getHoverName());
        tooltip.add(Component.translatable("screen.infiniteinventory.tooltip.amount", CompactNumberFormatter.format(entry.amount())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(entry.category().translationKey()).withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.literal(entry.registryName()).withStyle(ChatFormatting.DARK_GRAY));
        guiGraphics.renderTooltip(this.font, tooltip, ItemStack.EMPTY.getTooltipImage(), mouseX, mouseY);
    }

    private int findDatabaseSlot(double mouseX, double mouseY) {
        for (int slotIndex = 0; slotIndex < PersonalDatabaseLayout.DATABASE_SLOT_COUNT; slotIndex++) {
            int slotX = this.leftPos + PersonalDatabaseLayout.databaseSlotX(slotIndex);
            int slotY = this.topPos + PersonalDatabaseLayout.databaseSlotY(slotIndex);
            if (this.isWithinAbsolute(slotX, slotY, 18, 18, mouseX, mouseY)) {
                return slotIndex;
            }
        }
        return -1;
    }

    private boolean isWithin(int x, int y, int width, int height, double mouseX, double mouseY) {
        return this.isWithinAbsolute(this.leftPos + x, this.topPos + y, width, height, mouseX, mouseY);
    }

    private boolean isWithinAbsolute(int x, int y, int width, int height, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
