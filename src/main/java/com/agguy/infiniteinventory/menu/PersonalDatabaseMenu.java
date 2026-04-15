package com.agguy.infiniteinventory.menu;

import com.agguy.infiniteinventory.database.DatabaseEnhancementConfig;
import com.agguy.infiniteinventory.database.DatabasePage;
import com.agguy.infiniteinventory.database.DatabasePageEntry;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseViewPreferencesAttachment;
import com.agguy.infiniteinventory.database.DatabaseViewState;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.network.DatabaseClickAction;
import com.agguy.infiniteinventory.network.DatabaseSnapshotPayload;
import com.agguy.infiniteinventory.service.PersonalDatabaseService;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public final class PersonalDatabaseMenu extends PersonalDatabaseMenuSupport {
    public PersonalDatabaseMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, playerInventory.player, 0L);
    }

    public PersonalDatabaseMenu(int containerId, Inventory playerInventory, long sessionId) {
        this(containerId, playerInventory, playerInventory.player, sessionId);
    }

    public PersonalDatabaseMenu(int containerId, Inventory playerInventory, PersonalDatabaseOpenState openState) {
        this(containerId, playerInventory, playerInventory.player, openState == null ? 0L : openState.sessionId());
        this.applyOpenState(openState);
    }

    public PersonalDatabaseMenu(int containerId, Inventory playerInventory, Player owner) {
        this(containerId, playerInventory, owner, NEXT_SESSION_ID.getAndIncrement());
    }

    public PersonalDatabaseMenu(int containerId, Inventory playerInventory, Player owner, long sessionId) {
        super(containerId, playerInventory, owner, sessionId);
    }

    public DatabaseViewState viewState() {
        return this.viewState;
    }

    public DatabaseScope activeScope() {
        return this.activeScope;
    }

    public long sessionId() {
        return this.sessionId;
    }

    public List<com.agguy.infiniteinventory.compat.AccessorySlotGroup> accessorySlotGroups() {
        return this.accessorySlotGroups;
    }

    public DatabaseEnhancementConfig enhancementConfig() {
        return this.enhancementConfig;
    }

    public String autoStoreTargetTabId() {
        return this.autoStoreTargetTabId;
    }

    public void initializeFromPreferences(DatabaseViewPreferencesAttachment preferences) {
        if (preferences == null) {
            return;
        }
        this.applyOpenState(new PersonalDatabaseOpenState(
                this.sessionId,
                preferences.lastScope(),
                preferences.queryFor(DatabaseScope.PERSONAL),
                preferences.queryFor(DatabaseScope.PUBLIC),
                preferences.enhancementConfig(),
                preferences.autoStoreTargetTabId()
        ));
    }

    public void applyViewState(DatabaseViewState newState) {
        this.sessionId = newState.sessionId();
        this.viewState = newState;
        this.activeScope = newState.query().scope();
        this.personalQuery = newState.personalQuery();
        this.publicQuery = newState.publicQuery();
        this.enhancementConfig = newState.enhancementConfig();
        this.autoStoreTargetTabId = newState.autoStoreTargetTabId();
    }

    public DatabaseQuery queryForScope(DatabaseScope scope) {
        return DatabaseScope.normalize(scope) == DatabaseScope.PUBLIC ? this.publicQuery : this.personalQuery;
    }

    public boolean isAccessorySlotIndex(int slotIndex) {
        return this.accessorySlotRange.contains(slotIndex);
    }

    public void applySlotLayout(PersonalDatabaseLayout layout) {
        if (layout == null) {
            return;
        }
        PersonalDatabaseLayout.Rect equipmentPanel = layout.equipmentPanelRect();
        PersonalDatabaseLayout.Rect bottomInventory = layout.bottomInventoryRect();
        this.moveSlot(this.resultSlotIndex, equipmentPanel.x() + TOP_SECTION_RESULT_X, equipmentPanel.y() + TOP_SECTION_RESULT_Y);
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 2; column++) {
                int slotIndex = this.craftingSlotRange.firstIndex() + column + row * 2;
                this.moveSlot(
                        slotIndex,
                        equipmentPanel.x() + TOP_SECTION_CRAFT_X + column * PersonalDatabaseLayout.SLOT_SIZE,
                        equipmentPanel.y() + TOP_SECTION_CRAFT_Y + row * PersonalDatabaseLayout.SLOT_SIZE
                );
            }
        }
        for (int index = 0; index < ARMOR_ORDER.length; index++) {
            this.moveSlot(
                    this.armorSlotRange.firstIndex() + index,
                    equipmentPanel.x() + TOP_SECTION_ARMOR_X,
                    equipmentPanel.y() + TOP_SECTION_ARMOR_Y + index * PersonalDatabaseLayout.SLOT_SIZE
            );
        }
        this.moveSlot(this.offhandSlotIndex, equipmentPanel.x() + TOP_SECTION_OFFHAND_X, equipmentPanel.y() + TOP_SECTION_OFFHAND_Y);
        this.moveAccessorySlots(layout);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int slotIndex = this.mainInventorySlotRange.firstIndex() + column + row * 9;
                this.moveSlot(
                        slotIndex,
                        bottomInventory.x() + BOTTOM_SECTION_INVENTORY_X + column * PersonalDatabaseLayout.SLOT_SIZE,
                        bottomInventory.y() + BOTTOM_SECTION_INVENTORY_Y + row * PersonalDatabaseLayout.SLOT_SIZE
                );
            }
        }
        for (int column = 0; column < 9; column++) {
            int slotIndex = this.hotbarSlotRange.firstIndex() + column;
            this.moveSlot(
                    slotIndex,
                    bottomInventory.x() + BOTTOM_SECTION_INVENTORY_X + column * PersonalDatabaseLayout.SLOT_SIZE,
                    bottomInventory.y() + BOTTOM_SECTION_HOTBAR_Y
            );
        }
    }

    public void syncViewToClient() {
        if (!(this.owner instanceof ServerPlayer serverPlayer)) {
            return;
        }
        this.personalQuery = PersonalDatabaseService.INSTANCE.sanitizeQuery(serverPlayer, this.personalQuery);
        this.publicQuery = PersonalDatabaseService.INSTANCE.sanitizeQuery(serverPlayer, this.publicQuery);
        DatabaseQuery activeQuery = this.currentQuery();
        java.util.ArrayList<DatabasePage> rebuiltPages = new java.util.ArrayList<>(activeQuery.visibleTabIds().size());
        DatabaseQuery adjustedQuery = activeQuery;
        for (String visibleTabId : activeQuery.visibleTabIds()) {
            DatabasePage page = PersonalDatabaseService.INSTANCE.buildPage(serverPlayer, adjustedQuery, visibleTabId);
            rebuiltPages.add(page);
            adjustedQuery = adjustedQuery
                    .withPageIndex(page.tab().id(), page.pageIndex())
                    .withPageSize(page.tab().id(), page.pageSize());
        }
        this.currentPages = List.copyOf(rebuiltPages);
        this.setActiveQuery(adjustedQuery);
        this.autoStoreTargetTabId = PersonalDatabaseService.INSTANCE.resolveAutoStoreTargetTabId(serverPlayer);
        this.persistPreferences(serverPlayer);
        this.viewState = new DatabaseViewState(
                this.containerId,
                this.sessionId,
                this.currentQuery(),
                this.personalQuery,
                this.publicQuery,
                this.enhancementConfig,
                this.autoStoreTargetTabId,
                PersonalDatabaseService.INSTANCE.tabsForScope(serverPlayer, DatabaseScope.PERSONAL),
                PersonalDatabaseService.INSTANCE.tabsForScope(serverPlayer, DatabaseScope.PUBLIC),
                this.currentPages.stream().map(DatabasePage::toPanelView).toList()
        );
        PacketDistributor.sendToPlayer(serverPlayer, new DatabaseSnapshotPayload(this.viewState));
    }

    public void updateQuery(DatabaseQuery newQuery) {
        DatabaseScope previousScope = this.activeScope;
        this.setActiveQuery(newQuery == null ? this.currentQuery() : newQuery);
        if (this.owner instanceof ServerPlayer serverPlayer) {
            this.persistPreferences(serverPlayer);
        }
        this.syncViewToClient();
        if (this.owner instanceof ServerPlayer serverPlayer && previousScope != this.activeScope) {
            PersonalDatabaseService.INSTANCE.notifyViewerAboutUnresolvedEntries(serverPlayer, this.activeScope);
        }
    }

    public void updateEnhancementConfig(DatabaseEnhancementConfig newConfig, String newAutoStoreTargetTabId) {
        this.enhancementConfig = newConfig == null ? DatabaseEnhancementConfig.defaultConfig() : newConfig;
        this.autoStoreTargetTabId = com.agguy.infiniteinventory.database.DatabaseTabs.normalizeConcreteTarget(newAutoStoreTargetTabId);
        if (this.owner instanceof ServerPlayer serverPlayer) {
            this.persistPreferences(serverPlayer);
        }
        this.syncViewToClient();
    }

    public void depositAllFromMainInventory(String targetTabId) {
        if (this.owner instanceof ServerPlayer serverPlayer
                && PersonalDatabaseService.INSTANCE.depositMainInventory(serverPlayer, this.activeScope, targetTabId) > 0L) {
            this.broadcastChanges();
            this.syncAfterDatabaseMutation(serverPlayer);
        }
    }

    public void depositInventorySlot(int slotIndex, String targetTabId) {
        if (!(this.owner instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (slotIndex < 0 || slotIndex >= this.slots.size() || !this.shouldDepositQuickMovedSlot(slotIndex)) {
            return;
        }
        net.minecraft.world.inventory.Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) {
            return;
        }
        if (PersonalDatabaseService.INSTANCE.depositSlot(serverPlayer, this.activeScope, targetTabId, slot)) {
            this.broadcastChanges();
            this.syncAfterDatabaseMutation(serverPlayer);
        }
    }

    public void handleDatabaseClick(int panelIndex, int pageSlotIndex, DatabaseClickAction action, @Nullable String targetTabId) {
        if (!(this.owner instanceof ServerPlayer serverPlayer)) {
            return;
        }
        boolean changed = false;
        boolean refreshSharedView = false;
        if (action.isStoreAction()) {
            if (!this.getCarried().isEmpty()) {
                changed = this.storeCarriedStack(serverPlayer, action.storesSingleItem(), this.resolveStoreTargetTab(panelIndex, targetTabId));
            }
        } else {
            if (!this.getCarried().isEmpty()) {
                return;
            }
            DatabasePageEntry pageEntry = this.getPageEntry(panelIndex, pageSlotIndex);
            if (pageEntry == null) {
                refreshSharedView = this.activeScope == DatabaseScope.PUBLIC;
            } else {
                long requestedAmount = action.resolveRequestedAmount(pageEntry.view().amount(), pageEntry.key().maxStackSize());
                if (action.extractsToInventory()) {
                    changed = PersonalDatabaseService.INSTANCE.extractToInventory(serverPlayer, this.activeScope, pageEntry.key(), requestedAmount) > 0L;
                } else {
                    changed = this.withdrawToCarried(
                            serverPlayer,
                            pageEntry.key(),
                            (int) Math.min(Integer.MAX_VALUE, requestedAmount)
                    );
                }
                if (!changed && this.activeScope == DatabaseScope.PUBLIC) {
                    refreshSharedView = true;
                }
            }
        }
        if (changed) {
            this.broadcastChanges();
            this.syncAfterDatabaseMutation(serverPlayer);
        } else if (refreshSharedView) {
            PersonalDatabaseService.INSTANCE.syncPublicViewers(serverPlayer.server);
        }
    }

    @Override
    public void slotsChanged(Container container) {
        CraftingMenuAccess.updateResult(this, this.owner.level(), this.owner, this.craftSlots, this.resultSlots, null);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.resultSlots.clearContent();
        if (!player.level().isClientSide) {
            this.clearContainer(player, this.craftSlots);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= this.slots.size()) {
            return ItemStack.EMPTY;
        }
        net.minecraft.world.inventory.Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack rawStack = slot.getItem();
        ItemStack copy = rawStack.copy();

        String quickMoveTargetTabId = this.resolveSingleStoreTargetTab();
        if (this.shouldDepositQuickMovedSlot(slotIndex)
                && player instanceof ServerPlayer serverPlayer
                && quickMoveTargetTabId != null
                && PersonalDatabaseService.INSTANCE.depositSlot(serverPlayer, this.activeScope, quickMoveTargetTabId, slot)) {
            this.broadcastChanges();
            this.syncAfterDatabaseMutation(serverPlayer);
            return copy;
        }

        boolean moved = false;
        EquipmentSlot equipmentSlot = player.getEquipmentSlotForItem(copy);
        if (slotIndex == this.resultSlotIndex) {
            moved = this.moveToPlayerStorage(rawStack, true);
            if (moved) {
                slot.onQuickCraft(rawStack, copy);
            }
        } else if (this.craftingSlotRange.contains(slotIndex)
                || this.armorSlotRange.contains(slotIndex)
                || slotIndex == this.offhandSlotIndex
                || this.accessorySlotRange.contains(slotIndex)) {
            moved = this.moveToPlayerStorage(rawStack, false);
        } else if (this.mainInventorySlotRange.contains(slotIndex) || this.hotbarSlotRange.contains(slotIndex)) {
            moved = this.tryMoveToAccessorySlots(rawStack);
            if (!rawStack.isEmpty() && equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                int armorSlotIndex = this.armorSlotRange.firstIndex() + (3 - equipmentSlot.getIndex());
                if (!this.slots.get(armorSlotIndex).hasItem()) {
                    moved = this.moveItemStackTo(rawStack, armorSlotIndex, armorSlotIndex + 1, false) || moved;
                }
            } else if (!rawStack.isEmpty() && equipmentSlot == EquipmentSlot.OFFHAND && !this.slots.get(this.offhandSlotIndex).hasItem()) {
                moved = this.moveItemStackTo(rawStack, this.offhandSlotIndex, this.offhandSlotIndex + 1, false) || moved;
            }
            if (!rawStack.isEmpty() && this.mainInventorySlotRange.contains(slotIndex)) {
                moved = this.moveItemStackTo(rawStack, this.hotbarSlotRange.firstIndex(), this.hotbarSlotRange.lastIndexExclusive(), false) || moved;
            } else if (!rawStack.isEmpty() && this.hotbarSlotRange.contains(slotIndex)) {
                moved = this.moveItemStackTo(rawStack, this.mainInventorySlotRange.firstIndex(), this.mainInventorySlotRange.lastIndexExclusive(), false) || moved;
            }
        } else {
            moved = this.moveToPlayerStorage(rawStack, false);
        }

        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (rawStack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY, copy);
        } else {
            slot.setChanged();
        }
        if (rawStack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, rawStack);
        return copy;
    }

    @Override
    public void fillCraftSlotsStackedContents(net.minecraft.world.entity.player.StackedContents stackedContents) {
        this.craftSlots.fillStackedContents(stackedContents);
    }

    @Override
    public void clearCraftingContent() {
        this.resultSlots.clearContent();
        this.craftSlots.clearContent();
    }

    @Override
    public boolean recipeMatches(RecipeHolder<CraftingRecipe> recipe) {
        return recipe.value().matches(this.craftSlots.asCraftInput(), this.owner.level());
    }

    @Override
    public int getResultSlotIndex() {
        return this.resultSlotIndex;
    }

    @Override
    public int getGridWidth() {
        return 2;
    }

    @Override
    public int getGridHeight() {
        return 2;
    }

    @Override
    public int getSize() {
        return 5;
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    @Override
    public boolean shouldMoveToInventory(int slotIndex) {
        return slotIndex != this.getResultSlotIndex();
    }
}
