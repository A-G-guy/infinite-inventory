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
 */
public final class PersonalDatabaseMenu extends PersonalDatabaseMenuSupport {

    /**
     * 以默认会话创建菜单。
     *
     * @param containerId    容器网络标识
     * @param playerInventory 玩家背包
     */
    public PersonalDatabaseMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, playerInventory.player, 0L);
    }

    /**
     * 以指定会话 ID 创建菜单。
     *
     * @param containerId    容器网络标识
     * @param playerInventory 玩家背包
     * @param sessionId      会话标识，用于客户端状态校验
     */
    public PersonalDatabaseMenu(int containerId, Inventory playerInventory, long sessionId) {
        this(containerId, playerInventory, playerInventory.player, sessionId);
    }

    /**
     * 以打开状态恢复菜单，用于从持久化偏好重建玩家上次界面状态。
     *
     * @param containerId    容器网络标识
     * @param playerInventory 玩家背包
     * @param openState      打开状态快照，若为 null 则回退到默认会话
     */
    public PersonalDatabaseMenu(int containerId, Inventory playerInventory, PersonalDatabaseOpenState openState) {
        this(containerId, playerInventory, playerInventory.player, openState == null ? 0L : openState.sessionId());
        this.applyOpenState(openState);
    }

    /**
     * 以指定所有者创建菜单，使用自增会话 ID。
     *
     * @param containerId    容器网络标识
     * @param playerInventory 玩家背包
     * @param owner          菜单所有者玩家
     */
    public PersonalDatabaseMenu(int containerId, Inventory playerInventory, Player owner) {
        this(containerId, playerInventory, owner, NEXT_SESSION_ID.getAndIncrement());
    }

    /**
     * 完整构造函数。
     *
     * @param containerId    容器网络标识
     * @param playerInventory 玩家背包
     * @param owner          菜单所有者玩家
     * @param sessionId      会话标识
     */
    public PersonalDatabaseMenu(int containerId, Inventory playerInventory, Player owner, long sessionId) {
        super(containerId, playerInventory, owner, sessionId);
    }

    /**
     * 返回当前完整视图状态，供客户端同步与 UI 渲染使用。
     *
     * @return 当前视图状态
     */
    public DatabaseViewState viewState() {
        return this.viewState;
    }

    /**
     * 返回当前激活的数据库作用域（个人 / 公共）。
     *
     * @return 当前作用域
     */
    public DatabaseScope activeScope() {
        return this.activeScope;
    }

    /**
     * 返回当前会话 ID，用于区分不同打开实例并防止过期状态覆盖。
     *
     * @return 会话标识
     */
    public long sessionId() {
        return this.sessionId;
    }

    /**
     * 返回当前查看者语言设置，用于本地化文本渲染。
     *
     * @return 查看者语言
     */
    public ViewerLanguage viewerLanguage() {
        return this.currentViewerLanguage();
    }

    /**
     * 返回已注册的饰品槽位分组列表，供客户端布局使用。
     *
     * @return 饰品槽位分组列表
     */
    public List<com.agguy.infiniteinventory.compat.AccessorySlotGroup> accessorySlotGroups() {
        return this.accessorySlotGroups;
    }

    /**
     * 返回当前增强配置，控制自动存储等高级行为。
     *
     * @return 增强配置
     */
    public DatabaseEnhancementConfig enhancementConfig() {
        return this.enhancementConfig;
    }

    /**
     * 返回当前自动存储目标，决定快捷存入的默认目的地。
     *
     * @return 自动存储目标
     */
    public DatabaseAutoStoreTarget autoStoreTarget() {
        return this.autoStoreTarget;
    }

    /**
     * 从玩家持久化偏好初始化菜单状态，实现“记住上次界面设置”的体验。
     *
     * @param preferences 视图偏好附件，若为 null 则不做任何操作
     */
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

    /**
     * 应用服务端推送的新视图状态，覆盖本地查询与配置。
     *
     * <p>业务约束：会同步更新 personalQuery、publicQuery、enhancementConfig 等派生字段，
     * 保证菜单内部状态一致性。
     *
     * @param newState 服务端下发的最新视图状态
     */
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

    /**
     * 获取指定作用域下的查询对象。
     *
     * @param scope 目标作用域
     * @return 对应作用域的查询对象；若 scope 为 null 则归一化后按个人域处理
     */
    public DatabaseQuery queryForScope(DatabaseScope scope) {
        return DatabaseScope.normalize(scope) == DatabaseScope.PUBLIC ? this.publicQuery : this.personalQuery;
    }

    /**
     * 判断指定槽位索引是否属于饰品槽位范围。
     *
     * @param slotIndex 槽位索引
     * @return true 当且仅当索引落在已注册的饰品槽位区间内
     */
    public boolean isAccessorySlotIndex(int slotIndex) {
        return this.accessorySlotRange.contains(slotIndex);
    }

    /**
     * 根据布局描述重新定位所有玩家槽位（合成、护甲、副手、饰品、背包、快捷栏）。
     *
     * <p>设计决策：将槽位坐标计算外化到 {@link PersonalDatabaseLayout}，
     * 使菜单类不直接关心屏幕像素布局，便于响应不同分辨率与 UI 缩放。
     *
     * @param layout 屏幕布局描述，若为 null 则忽略
     */
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

    /**
     * 将当前视图状态同步到客户端，通常在服务端数据变更后调用。
     */
    public void syncViewToClient() {
        PersonalDatabaseMenuSyncHelper.syncViewToClient(this);
    }

    /**
     * 批量更新目标物品的备注。
     *
     * <p>业务约束：空备注会清除已有备注；仅当实际发生变更时才触发网络广播与视图同步，
     * 避免无意义的数据包发送。
     *
     * @param scope       目标作用域
     * @param targetStacks 待更新的物品堆叠列表
     * @param note        新备注内容，null 视为空字符串
     */
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

    /**
     * 批量对目标物品执行星标操作。
     *
     * <p>业务约束：仅服务端玩家可操作；支持切换、全标、全取消三种动作。
     * 若没有任何物品状态发生变化，则跳过同步以减少网络负载。
     *
     * @param scope        目标作用域
     * @param targetStacks 待操作的物品堆叠列表
     * @param action       星标动作类型
     */
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
        if (changed) {
            this.broadcastChanges();
            this.syncAfterScopeMutation(serverPlayer, scope);
        }
    }

    /**
     * 更新当前查询状态，并触发服务端到客户端的视图同步。
     *
     * @param newQuery 新的查询对象
     */
    public void updateQuery(DatabaseQuery newQuery) {
        PersonalDatabaseMenuSyncHelper.updateQuery(this, newQuery);
    }

    /**
     * 更新增强配置与自动存储目标，通常由客户端设置面板触发。
     *
     * @param newConfig          新的增强配置
     * @param newAutoStoreTarget 新的自动存储目标
     */
    public void updateEnhancementConfig(DatabaseEnhancementConfig newConfig, DatabaseAutoStoreTarget newAutoStoreTarget) {
        PersonalDatabaseMenuSyncHelper.updateEnhancementConfig(this, newConfig, newAutoStoreTarget);
    }

    /**
     * 更新查看者语言，若语言实际发生变化则同步到客户端以刷新本地化文本。
     *
     * @param viewerLanguage 新的语言设置
     */
    public void updateViewerLanguage(ViewerLanguage viewerLanguage) {
        if (this.setViewerLanguage(viewerLanguage)) {
            this.syncViewToClient();
        }
    }

    /**
     * 将玩家主背包所有物品存入数据库指定标签页。
     *
     * <p>业务约束：若目标标签页未指定，则按当前自动存储目标解析；
     * 仅当实际存入数量大于 0 时才广播变更。
     *
     * @param targetScope 目标作用域，可为 null（按默认规则解析）
     * @param targetTabId 目标标签页标识
     */
    public void depositAllFromMainInventory(@Nullable DatabaseScope targetScope, String targetTabId) {
        DatabaseScopedTabRef targetTab = this.resolveStoreTarget(-1, targetScope, targetTabId);
        if (this.owner instanceof ServerPlayer serverPlayer
                && PersonalDatabaseService.INSTANCE.depositMainInventory(serverPlayer, targetTab.scope(), targetTab.tabId()) > 0L) {
            this.broadcastChanges();
            this.syncAfterScopeMutation(serverPlayer, targetTab.scope());
        }
    }

    /**
     * 将指定背包槽位的物品存入数据库。
     *
     * <p>业务约束：会校验槽位索引有效性与是否允许快捷存入；
     * 若槽位为空或服务端玩家不合法则直接返回。
     *
     * @param slotIndex   背包槽位索引
     * @param targetScope 目标作用域，可为 null
     * @param targetTabId 目标标签页标识
     */
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

    /**
     * 处理玩家对数据库面板的点击操作，包括存入与提取两种主路径。
     *
     * <p>业务约束：
     * <ul>
     *   <li>存入路径要求玩家手持物品，按动作决定单件或整组存入
     *   <li>提取路径要求玩家手持为空，支持提取到背包、提取到世界、提取到光标三种模式
     *   <li>公共域操作失败时需要刷新所有公共域查看者，防止状态不一致
     * </ul>
     *
     * @param panelIndex    面板索引
     * @param pageSlotIndex 页内槽位索引
     * @param action        点击动作描述
     * @param targetScope   存入时的目标作用域，可为 null
     * @param targetTabId   存入时的目标标签页，可为 null
     */
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

    /**
     * 处理多选批处理操作（使用当前作用域作为目标）。
     *
     * @param action          选择动作
     * @param selectionEntries 选中的数据库条目
     * @param targetScope     目标作用域，可为 null（按默认规则解析）
     * @param targetTabId     目标标签页标识，可为 null
     */
    public void handleSelectionAction(
            DatabaseSelectionAction action,
            List<DatabaseSelectionEntry> selectionEntries,
            @Nullable DatabaseScope targetScope,
            @Nullable String targetTabId
    ) {
        this.handleSelectionAction(action, selectionEntries, targetScope, targetTabId, 0L);
    }

    /**
     * 处理多选批处理操作（目标作用域由调用方显式指定或默认）。
     *
     * @param action          选择动作
     * @param selectionEntries 选中的数据库条目
     * @param targetTabId     目标标签页标识，可为 null
     */
    public void handleSelectionAction(
            DatabaseSelectionAction action,
            List<DatabaseSelectionEntry> selectionEntries,
            @Nullable String targetTabId
    ) {
        this.handleSelectionAction(action, selectionEntries, null, targetTabId, 0L);
    }

    /**
     * 处理多选批处理操作的完整重载，支持跨作用域转移与按数量提取。
     *
     * <p>业务约束：
     * <ul>
     *   <li>空选择或无效动作直接返回，避免无意义的服务端计算
     *   <li>按作用域分组后批量调用服务层，减少数据库往返
     *   <li>跨作用域转移时同时追踪 personalChanged 与 publicChanged，以精准同步受影响的查看者
     * </ul>
     *
     * @param action          选择动作
     * @param selectionEntries 选中的数据库条目
     * @param targetScope     目标作用域，可为 null
     * @param targetTabId     目标标签页标识，可为 null
     * @param requestedAmount 请求提取数量，0 表示按动作默认值处理
     */
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

    /**
     * 快捷移动物品（Shift+点击）的核心逻辑。
     *
     * <p>业务约束与优先级：
     * <ol>
     *   <li>若槽位允许快捷存入且已配置自动存储目标，优先存入数据库
     *   <li>合成结果槽：尝试移入玩家存储区
     *   <li>装备/合成/副手/饰品槽：尝试移入玩家主背包或快捷栏
     *   <li>主背包/快捷栏：尝试装备到对应护甲/饰品槽，或在背包与快捷栏之间互换
     *   <li>数据库槽位：尝试移入玩家存储区
     * </ol>
     *
     * @param player    执行操作的玩家
     * @param slotIndex 被点击的槽位索引
     * @return 若发生移动则返回原始堆叠副本，否则返回空堆叠
     */
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
        if (slotIndex == this.resultSlotIndex) {
            moved = this.handleCraftingSlotMove(player, slot, rawStack, copy);
        } else if (this.craftingSlotRange.contains(slotIndex)
                || this.armorSlotRange.contains(slotIndex)
                || slotIndex == this.offhandSlotIndex
                || this.accessorySlotRange.contains(slotIndex)) {
            moved = this.handleArmorSlotMove(rawStack);
        } else if (this.mainInventorySlotRange.contains(slotIndex) || this.hotbarSlotRange.contains(slotIndex)) {
            moved = this.handleInventorySlotMove(slotIndex, rawStack, player);
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

    /**
     * 处理合成结果槽的快捷移动：将产物移入玩家存储区并触发合成回调。
     *
     * @param player   执行操作的玩家
     * @param slot     被点击的槽位
     * @param rawStack 槽位中的原始堆叠（会被修改）
     * @param copy     原始堆叠的副本，用于合成回调比对
     * @return 若发生移动则返回 true
     */
    private boolean handleCraftingSlotMove(Player player, net.minecraft.world.inventory.Slot slot, ItemStack rawStack, ItemStack copy) {
        boolean moved = this.moveToPlayerStorage(rawStack, true);
        if (moved) {
            slot.onQuickCraft(rawStack, copy);
        }
        return moved;
    }

    /**
     * 处理装备/合成/副手/饰品槽的快捷移动：将物品移入玩家主存储区。
     *
     * @param rawStack 槽位中的原始堆叠（会被修改）
     * @return 若发生移动则返回 true
     */
    private boolean handleArmorSlotMove(ItemStack rawStack) {
        return this.moveToPlayerStorage(rawStack, false);
    }

    /**
     * 处理主背包/快捷栏槽位的快捷移动：优先尝试饰品槽，再尝试装备槽，最后在背包与快捷栏之间互换。
     *
     * @param slotIndex 被点击的槽位索引
     * @param rawStack  槽位中的原始堆叠（会被修改）
     * @param player    执行操作的玩家
     * @return 若发生移动则返回 true
     */
    private boolean handleInventorySlotMove(int slotIndex, ItemStack rawStack, Player player) {
        boolean moved = this.tryMoveToAccessorySlots(rawStack);
        EquipmentSlot equipmentSlot = player.getEquipmentSlotForItem(rawStack);
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
        return moved;
    }

    @Override
    public void fillCraftSlotsStackedContents(net.minecraft.world.entity.player.StackedContents stackedContents) { this.craftSlots.fillStackedContents(stackedContents); }
    @Override
    public void clearCraftingContent() { this.resultSlots.clearContent(); this.craftSlots.clearContent(); }
    @Override
    public boolean recipeMatches(RecipeHolder<CraftingRecipe> recipe) { return recipe.value().matches(this.craftSlots.asCraftInput(), this.owner.level()); }
    @Override
    public int getResultSlotIndex() { return this.resultSlotIndex; }
    @Override
    public int getGridWidth() { return 2; }
    @Override
    public int getGridHeight() { return 2; }
    @Override
    public int getSize() { return 5; }
    @Override
    public RecipeBookType getRecipeBookType() { return RecipeBookType.CRAFTING; }

    @Override
    public boolean shouldMoveToInventory(int slotIndex) {
        return slotIndex != this.getResultSlotIndex();
    }
}
