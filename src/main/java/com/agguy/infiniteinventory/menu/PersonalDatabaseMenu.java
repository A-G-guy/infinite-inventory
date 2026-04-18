package com.agguy.infiniteinventory.menu;

import com.agguy.infiniteinventory.database.DatabaseEnhancementConfig;
import com.agguy.infiniteinventory.database.DatabaseAutoStoreTarget;
import com.agguy.infiniteinventory.database.DatabasePageEntry;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseSelectionEntry;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseViewPreferencesAttachment;
import com.agguy.infiniteinventory.database.DatabaseViewState;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.localization.ViewerLanguage;
import com.agguy.infiniteinventory.network.DatabaseClickAction;
import com.agguy.infiniteinventory.network.DatabaseSelectionAction;
import com.agguy.infiniteinventory.network.DatabaseSnapshotPayload;
import com.agguy.infiniteinventory.service.PersonalDatabaseService;
import com.agguy.infiniteinventory.service.PersonalDatabaseTransferHelper;
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

    public ViewerLanguage viewerLanguage() {
        return this.currentViewerLanguage();
    }

    public List<com.agguy.infiniteinventory.compat.AccessorySlotGroup> accessorySlotGroups() {
        return this.accessorySlotGroups;
    }

    public DatabaseEnhancementConfig enhancementConfig() {
        return this.enhancementConfig;
    }

    public DatabaseAutoStoreTarget autoStoreTarget() {
        return this.autoStoreTarget;
    }

    public void initializeFromPreferences(DatabaseViewPreferencesAttachment preferences) {
        if (preferences == null) {
            return;
        }
        this.applyOpenState(new PersonalDatabaseOpenState(
                this.sessionId,
                preferences.query(),
                preferences.enhancementConfig(),
                preferences.autoStoreTarget()
        ));
    }

    public void applyViewState(DatabaseViewState newState) {
        this.sessionId = newState.sessionId();
        this.viewState = newState;
        this.query = newState.query();
        this.activeScope = this.query.scope();
        this.personalQuery = newState.personalQuery();
        this.publicQuery = newState.publicQuery();
        this.enhancementConfig = newState.enhancementConfig();
        this.autoStoreTarget = newState.autoStoreTarget();
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
        PersonalDatabaseMenuSyncHelper.syncViewToClient(this);
    }

    public void updateQuery(DatabaseQuery newQuery) {
        PersonalDatabaseMenuSyncHelper.updateQuery(this, newQuery);
    }

    public void updateEnhancementConfig(DatabaseEnhancementConfig newConfig, DatabaseAutoStoreTarget newAutoStoreTarget) {
        PersonalDatabaseMenuSyncHelper.updateEnhancementConfig(this, newConfig, newAutoStoreTarget);
    }

    public void updateViewerLanguage(ViewerLanguage viewerLanguage) {
        if (this.setViewerLanguage(viewerLanguage)) {
            this.syncViewToClient();
        }
    }

    public void depositAllFromMainInventory(@Nullable DatabaseScope targetScope, String targetTabId) {
        DatabaseScopedTabRef targetTab = this.resolveStoreTarget(-1, targetScope, targetTabId);
        if (this.owner instanceof ServerPlayer serverPlayer
                && PersonalDatabaseService.INSTANCE.depositMainInventory(serverPlayer, targetTab.scope(), targetTab.tabId()) > 0L) {
            this.broadcastChanges();
            this.syncAfterScopeMutation(serverPlayer, targetTab.scope());
        }
    }

    public void depositInventorySlot(int slotIndex, @Nullable DatabaseScope targetScope, String targetTabId) {
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
        DatabaseScopedTabRef targetTab = this.resolveStoreTarget(-1, targetScope, targetTabId);
        if (PersonalDatabaseService.INSTANCE.depositSlot(serverPlayer, targetTab.scope(), targetTab.tabId(), slot)) {
            this.broadcastChanges();
            this.syncAfterScopeMutation(serverPlayer, targetTab.scope());
        }
    }

    public void handleDatabaseClick(
            int panelIndex,
            int pageSlotIndex,
            DatabaseClickAction action,
            @Nullable DatabaseScope targetScope,
            @Nullable String targetTabId
    ) {
        if (!(this.owner instanceof ServerPlayer serverPlayer)) {
            return;
        }
        boolean changed = false;
        boolean refreshSharedView = false;
        DatabaseScope changedScope = null;
        if (action.isStoreAction()) {
            if (!this.getCarried().isEmpty()) {
                DatabaseScopedTabRef resolvedTarget = this.resolveStoreTarget(panelIndex, targetScope, targetTabId);
                changed = this.storeCarriedStack(serverPlayer, resolvedTarget.scope(), action.storesSingleItem(), resolvedTarget.tabId());
                changedScope = resolvedTarget.scope();
            }
        } else {
            if (!this.getCarried().isEmpty()) {
                return;
            }
            DatabasePageEntry pageEntry = this.getPageEntry(panelIndex, pageSlotIndex);
            if (pageEntry == null) {
                if (panelIndex >= 0 && panelIndex < this.currentPages.size()) {
                    refreshSharedView = this.currentPages.get(panelIndex).scopedTab().scope() == DatabaseScope.PUBLIC;
                }
            } else {
                DatabaseScope sourceScope = pageEntry.view().scope();
                long requestedAmount = action.resolveRequestedAmount(pageEntry.view().amount(), pageEntry.key().maxStackSize());
                if (action.dropsToWorld()) {
                    changed = PersonalDatabaseService.INSTANCE.extractToWorld(serverPlayer, sourceScope, pageEntry.key(), requestedAmount) > 0L;
                } else if (action.extractsToInventory()) {
                    changed = PersonalDatabaseService.INSTANCE.extractToInventory(serverPlayer, sourceScope, pageEntry.key(), requestedAmount) > 0L;
                } else {
                    changed = this.withdrawToCarried(
                            serverPlayer,
                            sourceScope,
                            pageEntry.key(),
                            (int) Math.min(Integer.MAX_VALUE, requestedAmount)
                    );
                }
                changedScope = sourceScope;
                if (!changed && sourceScope == DatabaseScope.PUBLIC) {
                    refreshSharedView = true;
                }
            }
        }
        if (changed) {
            this.broadcastChanges();
            this.syncAfterScopeMutation(serverPlayer, changedScope);
        } else if (refreshSharedView) {
            PersonalDatabaseService.INSTANCE.syncPublicViewers(serverPlayer.server);
        }
    }

    public void handleSelectionAction(
            DatabaseSelectionAction action,
            List<DatabaseSelectionEntry> selectionEntries,
            @Nullable DatabaseScope targetScope,
            @Nullable String targetTabId
    ) {
        this.handleSelectionAction(action, selectionEntries, targetScope, targetTabId, 0L);
    }

    public void handleSelectionAction(
            DatabaseSelectionAction action,
            List<DatabaseSelectionEntry> selectionEntries,
            @Nullable String targetTabId
    ) {
        this.handleSelectionAction(action, selectionEntries, null, targetTabId, 0L);
    }

    public void handleSelectionAction(
            DatabaseSelectionAction action,
            List<DatabaseSelectionEntry> selectionEntries,
            @Nullable DatabaseScope targetScope,
            @Nullable String targetTabId,
            long requestedAmount
    ) {
        if (!(this.owner instanceof ServerPlayer serverPlayer) || action == null || selectionEntries == null || selectionEntries.isEmpty()) {
            return;
        }
        boolean changed = false;
        boolean personalChanged = false;
        boolean publicChanged = false;
        java.util.Map<DatabaseScope, java.util.List<DatabaseSelectionEntry>> entriesByScope = new java.util.LinkedHashMap<>();
        for (DatabaseSelectionEntry selectionEntry : selectionEntries) {
            if (selectionEntry == null || selectionEntry.isEmpty()) {
                continue;
            }
            entriesByScope.computeIfAbsent(selectionEntry.scope(), ignored -> new java.util.ArrayList<>()).add(selectionEntry);
        }
        if (action.requiresTargetTab()) {
            DatabaseScope normalizedTargetScope = DatabaseScope.normalize(targetScope);
            for (java.util.Map.Entry<DatabaseScope, java.util.List<DatabaseSelectionEntry>> entry : entriesByScope.entrySet()) {
                boolean scopeChanged = PersonalDatabaseService.INSTANCE.transferSelection(
                        serverPlayer,
                        entry.getKey(),
                        normalizedTargetScope,
                        entry.getValue(),
                        targetTabId
                );
                changed = changed || scopeChanged;
                if (scopeChanged) {
                    personalChanged = personalChanged || entry.getKey() == DatabaseScope.PERSONAL || normalizedTargetScope == DatabaseScope.PERSONAL;
                    publicChanged = publicChanged || entry.getKey() == DatabaseScope.PUBLIC || normalizedTargetScope == DatabaseScope.PUBLIC;
                }
            }
        } else {
            for (java.util.Map.Entry<DatabaseScope, java.util.List<DatabaseSelectionEntry>> entry : entriesByScope.entrySet()) {
                boolean scopeChanged = PersonalDatabaseService.INSTANCE.extractSelectionToInventory(
                        serverPlayer,
                        entry.getKey(),
                        entry.getValue(),
                        action,
                        requestedAmount
                ) > 0L;
                changed = changed || scopeChanged;
                if (scopeChanged) {
                    personalChanged = personalChanged || entry.getKey() == DatabaseScope.PERSONAL;
                    publicChanged = publicChanged || entry.getKey() == DatabaseScope.PUBLIC;
                }
            }
        }
        if (changed) {
            this.broadcastChanges();
            this.syncAfterScopedMutations(serverPlayer, personalChanged, publicChanged);
        } else if (entriesByScope.containsKey(DatabaseScope.PUBLIC)) {
            PersonalDatabaseService.INSTANCE.syncPublicViewers(serverPlayer.server);
        }
    }

    private void syncAfterScopeMutation(ServerPlayer player, @Nullable DatabaseScope scope) {
        PersonalDatabaseMenuSyncHelper.syncAfterScopeMutation(this, player, scope);
    }

    private void syncAfterScopedMutations(ServerPlayer player, boolean personalChanged, boolean publicChanged) {
        PersonalDatabaseMenuSyncHelper.syncAfterScopedMutations(this, player, personalChanged, publicChanged);
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

        DatabaseScopedTabRef quickMoveTargetTab = this.resolveSingleStoreTarget();
        if (this.shouldDepositQuickMovedSlot(slotIndex)
                && player instanceof ServerPlayer serverPlayer
                && quickMoveTargetTab != null
                && PersonalDatabaseService.INSTANCE.depositSlot(
                        serverPlayer,
                        quickMoveTargetTab.scope(),
                        quickMoveTargetTab.tabId(),
                        slot
                )) {
            this.broadcastChanges();
            this.syncAfterScopeMutation(serverPlayer, quickMoveTargetTab.scope());
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
                int armorSlotOffset = armorSlotOffset(equipmentSlot);
                int armorSlotIndex = this.armorSlotRange.firstIndex() + armorSlotOffset;
                if (armorSlotOffset >= 0 && !this.slots.get(armorSlotIndex).hasItem()) {
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
