package com.agguy.infiniteinventory.menu;

import com.agguy.infiniteinventory.database.DatabaseEnhancementConfig;
import com.agguy.infiniteinventory.database.DatabaseAutoStoreTarget;
import com.agguy.infiniteinventory.database.DatabasePageEntry;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseViewPreferencesAttachment;
import com.agguy.infiniteinventory.database.DatabaseViewState;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.localization.ViewerLanguage;
import com.agguy.infiniteinventory.network.DatabaseClickAction;
import com.agguy.infiniteinventory.network.DatabaseSelectionAction;
import com.agguy.infiniteinventory.network.DepositConflictAction;
import com.agguy.infiniteinventory.service.PersonalDatabaseService;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

/**
 * 个人数据库服务器端容器菜单，管理玩家与数据库交互的完整生命周期。
 *
 * <p>职责边界：
 * <ul>
 *   <li>维护 UI 状态同步（查询、增强配置、视图状态）
 *   <li>处理物品存取、快捷移动、合成网格交互
 *   <li>协调多选批处理、备注与星标等元数据操作
 * </ul>
 *
 * <p>设计决策：继承 {@link PersonalDatabaseMenuSupport} 以复用槽位注册与基础工具方法，
 * 本类仅保留业务编排逻辑，避免与网络同步细节耦合。
 * 为控制类规模，将合成网格、存取操作与多选操作分别委托给对应的辅助类。
 */
public final class PersonalDatabaseMenu extends PersonalDatabaseMenuSupport {

    private final PersonalDatabaseMenuCraftingHelper craftingHelper;
    private final PersonalDatabaseMenuDepositHelper depositHelper;
    private final PersonalDatabaseMenuSelectionHelper selectionHelper;
    private final PersonalDatabaseMenuConflictHelper conflictHelper;
    private final PersonalDatabaseMenuQuickMoveHelper quickMoveHelper;

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
        this.craftingHelper = new PersonalDatabaseMenuCraftingHelper(this);
        this.depositHelper = new PersonalDatabaseMenuDepositHelper(this);
        this.selectionHelper = new PersonalDatabaseMenuSelectionHelper(this);
        this.conflictHelper = new PersonalDatabaseMenuConflictHelper(this);
        this.quickMoveHelper = new PersonalDatabaseMenuQuickMoveHelper(this);
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

    long lastSentPersonalRevision = -1L, lastSentPublicRevision = -1L;
    DatabaseQuery lastSentQuery;

    public void applyViewState(DatabaseViewState newState) {
        this.sessionId = newState.sessionId();
        this.viewState = newState;
        this.query = newState.query();
        this.activeScope = this.query.scope();
        this.personalQuery = newState.personalQuery();
        this.publicQuery = newState.publicQuery();
        this.enhancementConfig = newState.enhancementConfig();
        this.autoStoreTarget = newState.autoStoreTarget();
        PersonalDatabaseMenuSyncHelper.trackPublicViewerState(this);
    }

    @Override protected void applyOpenState(PersonalDatabaseOpenState openState) {
        super.applyOpenState(openState);
        PersonalDatabaseMenuSyncHelper.trackPublicViewerState(this);
    }

    @Override protected void setActiveQuery(DatabaseQuery query) {
        super.setActiveQuery(query);
        PersonalDatabaseMenuSyncHelper.trackPublicViewerState(this);
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

    public void handleNoteUpdate(DatabaseScope scope, List<ItemStack> targetStacks, String note) {
        if (!(this.owner instanceof ServerPlayer serverPlayer)) return;
        boolean changed = false;
        String trimmedNote = note == null ? "" : note.trim();
        for (ItemStack stack : targetStacks) {
            if (stack.isEmpty()) continue;
            StoredStackKey key = StoredStackKey.of(stack);
            String currentNote = PersonalDatabaseService.INSTANCE.noteFor(serverPlayer, scope, key);
            if (trimmedNote.isEmpty()) {
                if (!currentNote.isEmpty()) { PersonalDatabaseService.INSTANCE.setNote(serverPlayer, scope, key, ""); changed = true; }
            } else if (!currentNote.equals(trimmedNote)) {
                PersonalDatabaseService.INSTANCE.setNote(serverPlayer, scope, key, trimmedNote); changed = true;
            }
        }
        if (changed) { this.broadcastChanges(); this.syncAfterScopeMutation(serverPlayer, scope); }
    }

    public void handleStarAction(DatabaseScope scope, List<ItemStack> targetStacks, com.agguy.infiniteinventory.network.DatabaseStarPayload.StarAction action) {
        if (!(this.owner instanceof ServerPlayer serverPlayer)) return;
        boolean changed = false;
        for (ItemStack stack : targetStacks) {
            if (stack.isEmpty()) continue;
            StoredStackKey key = StoredStackKey.of(stack);
            boolean itemChanged = switch (action) {
                case TOGGLE -> PersonalDatabaseService.INSTANCE.toggleStar(serverPlayer, scope, key);
                case STAR_ALL -> PersonalDatabaseService.INSTANCE.setStarred(serverPlayer, scope, key, true);
                case UNSTAR_ALL -> PersonalDatabaseService.INSTANCE.setStarred(serverPlayer, scope, key, false);
            };
            if (itemChanged) changed = true;
        }
        if (changed) { this.broadcastChanges(); this.syncAfterScopeMutation(serverPlayer, scope); }
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
        this.depositHelper.depositAllFromMainInventory(targetScope, targetTabId);
    }

    public void depositExistingByTab(@Nullable DatabaseScope targetScope) {
        this.depositHelper.depositExistingByTab(targetScope);
    }

    public void depositInventorySlot(int slotIndex, @Nullable DatabaseScope targetScope, String targetTabId) {
        this.depositHelper.depositInventorySlot(slotIndex, targetScope, targetTabId);
    }

    void sendDepositConflict(DatabaseScope scope, String targetTabId, String existingTabId, ItemStack stack, int slotIndex) {
        this.conflictHelper.sendDepositConflict(scope, targetTabId, existingTabId, stack, slotIndex);
    }

    public void resolveDepositConflict(
            DatabaseScope scope,
            String targetTabId,
            String existingTabId,
            DepositConflictAction action,
            int slotIndex,
            ItemStack originalStack
    ) {
        this.conflictHelper.resolveDepositConflict(scope, targetTabId, existingTabId, action, slotIndex, originalStack);
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
        if (changed) { this.broadcastChanges(); this.syncAfterScopeMutation(serverPlayer, changedScope); }
        else if (refreshSharedView) PersonalDatabaseService.INSTANCE.syncPublicViewers(serverPlayer.server);
    }

    public void handleSelectionAction(
            DatabaseSelectionAction action,
            List<com.agguy.infiniteinventory.database.DatabaseSelectionEntry> selectionEntries,
            @Nullable DatabaseScope targetScope,
            @Nullable String targetTabId
    ) {
        this.selectionHelper.handleSelectionAction(action, selectionEntries, targetScope, targetTabId);
    }

    public void handleSelectionAction(
            DatabaseSelectionAction action,
            List<com.agguy.infiniteinventory.database.DatabaseSelectionEntry> selectionEntries,
            @Nullable String targetTabId
    ) {
        this.selectionHelper.handleSelectionAction(action, selectionEntries, targetTabId);
    }

    public void handleSelectionAction(
            DatabaseSelectionAction action,
            List<com.agguy.infiniteinventory.database.DatabaseSelectionEntry> selectionEntries,
            @Nullable DatabaseScope targetScope,
            @Nullable String targetTabId,
            long requestedAmount
    ) {
        this.selectionHelper.handleSelectionAction(action, selectionEntries, targetScope, targetTabId, requestedAmount);
    }

    void syncAfterScopeMutation(ServerPlayer player, @Nullable DatabaseScope scope) {
        PersonalDatabaseMenuSyncHelper.syncAfterScopeMutation(this, player, scope);
    }

    void syncAfterScopedMutations(ServerPlayer player, boolean personalChanged, boolean publicChanged) {
        PersonalDatabaseMenuSyncHelper.syncAfterScopedMutations(this, player, personalChanged, publicChanged);
    }

    @Override
    public void slotsChanged(Container container) {
        PersonalDatabaseCraftingMenuAccess.updateResult(this, this.owner.level(), this.owner, this.craftSlots, this.resultSlots, null);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.resultSlots.clearContent();
        if (!player.level().isClientSide) this.clearContainer(player, this.craftSlots);
        if (player instanceof ServerPlayer serverPlayer) PersonalDatabaseService.INSTANCE.unregisterPublicViewer(serverPlayer);
    }

    @Override
    public boolean stillValid(Player player) {
        if (player == null || !player.isAlive()) {
            return false;
        }
        return player.containerMenu == this;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return this.quickMoveHelper.quickMoveStack(player, slotIndex);
    }

    @Override
    public void fillCraftSlotsStackedContents(net.minecraft.world.entity.player.StackedContents stackedContents) {
        this.craftingHelper.fillCraftSlotsStackedContents(stackedContents);
    }

    @Override
    public void clearCraftingContent() {
        this.craftingHelper.clearCraftingContent();
    }

    @Override
    public boolean recipeMatches(RecipeHolder<CraftingRecipe> recipe) {
        return this.craftingHelper.recipeMatches(recipe);
    }

    @Override
    public int getResultSlotIndex() {
        return this.craftingHelper.getResultSlotIndex();
    }

    @Override
    public int getGridWidth() {
        return this.craftingHelper.getGridWidth();
    }

    @Override
    public int getGridHeight() {
        return this.craftingHelper.getGridHeight();
    }

    @Override
    public int getSize() {
        return this.craftingHelper.getSize();
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return this.craftingHelper.getRecipeBookType();
    }

    @Override
    public boolean shouldMoveToInventory(int slotIndex) {
        return this.craftingHelper.shouldMoveToInventory(slotIndex);
    }
}
