package com.agguy.infiniteinventory.menu;

import com.agguy.infiniteinventory.compat.AccessoriesCompat;
import com.agguy.infiniteinventory.compat.AccessorySlotGroup;
import com.agguy.infiniteinventory.compat.CuriosCompat;
import com.agguy.infiniteinventory.database.DatabaseAutoStoreTarget;
import com.agguy.infiniteinventory.database.DatabaseEnhancementConfig;
import com.agguy.infiniteinventory.database.DatabasePage;
import com.agguy.infiniteinventory.database.DatabasePageEntry;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScopedTabRef;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseViewPreferencesAttachment;
import com.agguy.infiniteinventory.database.DatabaseViewState;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.localization.ViewerLanguage;
import com.agguy.infiniteinventory.registry.ModMenus;
import com.agguy.infiniteinventory.service.PersonalDatabaseService;
import com.agguy.infiniteinventory.service.PersonalDatabaseServiceDepositHelper;
import com.mojang.datafixers.util.Pair;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.jetbrains.annotations.Nullable;

abstract class PersonalDatabaseMenuSupport extends RecipeBookMenu<CraftingInput, CraftingRecipe> {
    protected static final AtomicLong NEXT_SESSION_ID = new AtomicLong(1L);
    protected static final EquipmentSlot[] ARMOR_ORDER = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    protected static final int TOP_SECTION_RESULT_X = 154;
    protected static final int TOP_SECTION_RESULT_Y = 28;
    protected static final int TOP_SECTION_CRAFT_X = 98;
    protected static final int TOP_SECTION_CRAFT_Y = 18;
    protected static final int TOP_SECTION_ARMOR_X = 8;
    protected static final int TOP_SECTION_ARMOR_Y = 8;
    protected static final int TOP_SECTION_OFFHAND_X = 77;
    protected static final int TOP_SECTION_OFFHAND_Y = 62;
    protected static final int BOTTOM_SECTION_INVENTORY_X = 8;
    protected static final int BOTTOM_SECTION_INVENTORY_Y = 1;
    protected static final int BOTTOM_SECTION_HOTBAR_Y = 59;
    protected static final Field SLOT_X_FIELD = findSlotField("x");
    protected static final Field SLOT_Y_FIELD = findSlotField("y");

    protected final CraftingContainer craftSlots = new TransientCraftingContainer(this, 2, 2);
    protected final ResultContainer resultSlots = new ResultContainer();
    protected final Player owner;
    protected final int resultSlotIndex;
    protected final MenuSlotRange craftingSlotRange;
    protected final MenuSlotRange armorSlotRange;
    protected final MenuSlotRange mainInventorySlotRange;
    protected final MenuSlotRange hotbarSlotRange;
    protected final MenuSlotRange playerStorageSlotRange;
    protected final int offhandSlotIndex;
    protected final List<AccessorySlotGroup> accessorySlotGroups;
    protected final MenuSlotRange accessorySlotRange;
    protected long sessionId;
    protected DatabaseQuery query = DatabaseQuery.defaultQuery();
    protected DatabaseScope activeScope = DatabaseScope.defaultScope();
    protected DatabaseQuery personalQuery = DatabaseQuery.defaultQuery(DatabaseScope.PERSONAL);
    protected DatabaseQuery publicQuery = DatabaseQuery.defaultQuery(DatabaseScope.PUBLIC);
    protected DatabaseEnhancementConfig enhancementConfig = DatabaseEnhancementConfig.defaultConfig();
    protected DatabaseAutoStoreTarget autoStoreTarget = DatabaseAutoStoreTarget.defaultTarget();
    protected ViewerLanguage viewerLanguage = ViewerLanguage.defaultLanguage();
    protected DatabaseViewState viewState;
    protected List<DatabasePage> currentPages = List.of();

    protected PersonalDatabaseMenuSupport(int containerId, Inventory playerInventory, Player owner, long sessionId) {
        super(ModMenus.PERSONAL_DATABASE_MENU.get(), containerId);
        this.owner = owner;
        this.sessionId = Math.max(0L, sessionId);
        this.viewState = DatabaseViewState.empty(containerId, this.sessionId, this.query);
        this.resultSlotIndex = this.addTrackedSlot(new ResultSlot(owner, this.craftSlots, this.resultSlots, 0, TOP_SECTION_RESULT_X, TOP_SECTION_RESULT_Y));
        this.craftingSlotRange = this.addCraftingSlots();
        this.armorSlotRange = this.addArmorSlots(playerInventory, owner);
        this.mainInventorySlotRange = this.addMainInventorySlots(playerInventory);
        this.hotbarSlotRange = this.addHotbarSlots(playerInventory);
        this.playerStorageSlotRange = MenuSlotRange.span(this.mainInventorySlotRange, this.hotbarSlotRange);
        this.offhandSlotIndex = this.addTrackedSlot(new OffhandDisplaySlot(playerInventory, owner, 40, TOP_SECTION_OFFHAND_X, TOP_SECTION_OFFHAND_Y));
        List<AccessorySlotGroup> allGroups = new ArrayList<>();
        allGroups.addAll(AccessoriesCompat.appendAccessorySlots(owner, this::addTrackedSlot));
        allGroups.addAll(CuriosCompat.appendAccessorySlots(owner, this::addTrackedSlot));
        this.accessorySlotGroups = List.copyOf(allGroups);
        this.accessorySlotRange = MenuSlotRange.fromGroups(this.accessorySlotGroups);
    }

    protected MenuSlotRange addCraftingSlots() {
        int start = this.slots.size();
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 2; column++) {
                this.addTrackedSlot(new Slot(
                        this.craftSlots,
                        column + row * 2,
                        TOP_SECTION_CRAFT_X + column * PersonalDatabaseLayout.SLOT_SIZE,
                        TOP_SECTION_CRAFT_Y + row * PersonalDatabaseLayout.SLOT_SIZE
                ));
            }
        }
        return MenuSlotRange.of(start, this.slots.size() - start);
    }

    protected MenuSlotRange addArmorSlots(Inventory playerInventory, Player owner) {
        int start = this.slots.size();
        for (int index = 0; index < ARMOR_ORDER.length; index++) {
            EquipmentSlot equipmentSlot = ARMOR_ORDER[index];
            int inventoryIndex = armorInventoryIndex(equipmentSlot);
            int x = TOP_SECTION_ARMOR_X;
            int y = TOP_SECTION_ARMOR_Y + index * PersonalDatabaseLayout.SLOT_SIZE;
            ResourceLocation icon = switch (equipmentSlot) {
                case HEAD -> InventoryMenu.EMPTY_ARMOR_SLOT_HELMET;
                case CHEST -> InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE;
                case LEGS -> InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS;
                case FEET -> InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS;
                default -> null;
            };
            this.addTrackedSlot(new EquipmentDisplaySlot(playerInventory, owner, equipmentSlot, inventoryIndex, x, y, icon));
        }
        return MenuSlotRange.of(start, this.slots.size() - start);
    }

    protected static int armorSlotOffset(EquipmentSlot equipmentSlot) {
        return switch (equipmentSlot) {
            case HEAD -> 0;
            case CHEST -> 1;
            case LEGS -> 2;
            case FEET -> 3;
            default -> -1;
        };
    }

    protected static int armorInventoryIndex(EquipmentSlot equipmentSlot) {
        return switch (equipmentSlot) {
            case HEAD -> 39;
            case CHEST -> 38;
            case LEGS -> 37;
            case FEET -> 36;
            default -> -1;
        };
    }

    protected MenuSlotRange addMainInventorySlots(Inventory playerInventory) {
        int start = this.slots.size();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int slotIndex = column + (row + 1) * 9;
                this.addTrackedSlot(new Slot(
                        playerInventory,
                        slotIndex,
                        BOTTOM_SECTION_INVENTORY_X + column * PersonalDatabaseLayout.SLOT_SIZE,
                        BOTTOM_SECTION_INVENTORY_Y + row * PersonalDatabaseLayout.SLOT_SIZE
                ));
            }
        }
        return MenuSlotRange.of(start, this.slots.size() - start);
    }

    protected MenuSlotRange addHotbarSlots(Inventory playerInventory) {
        int start = this.slots.size();
        for (int column = 0; column < 9; column++) {
            this.addTrackedSlot(new Slot(
                    playerInventory,
                    column,
                    BOTTOM_SECTION_INVENTORY_X + column * PersonalDatabaseLayout.SLOT_SIZE,
                    BOTTOM_SECTION_HOTBAR_Y
            ));
        }
        return MenuSlotRange.of(start, this.slots.size() - start);
    }

    protected int addTrackedSlot(Slot slot) {
        int index = this.slots.size();
        this.addSlot(slot);
        return index;
    }

    protected void moveAccessorySlots(PersonalDatabaseLayout layout) {
        for (PersonalDatabaseLayout.AccessorySlotLayout slotLayout : layout.accessorySlotLayouts()) {
            this.moveSlot(slotLayout.slotIndex(), slotLayout.slotRect().x(), slotLayout.slotRect().y());
        }
    }

    protected boolean shouldDepositQuickMovedSlot(int slotIndex) {
        return this.mainInventorySlotRange.contains(slotIndex)
                || this.hotbarSlotRange.contains(slotIndex)
                || this.accessorySlotRange.contains(slotIndex);
    }

    protected boolean moveToPlayerStorage(ItemStack stack, boolean reverse) {
        if (this.playerStorageSlotRange.isEmpty()) {
            return false;
        }
        return this.moveItemStackTo(
                stack,
                this.playerStorageSlotRange.firstIndex(),
                this.playerStorageSlotRange.lastIndexExclusive(),
                reverse
        );
    }

    protected boolean tryMoveToAccessorySlots(ItemStack stack) {
        if (this.accessorySlotRange.isEmpty()) {
            return false;
        }
        return this.moveItemStackTo(
                stack,
                this.accessorySlotRange.firstIndex(),
                this.accessorySlotRange.lastIndexExclusive(),
                false
        );
    }

    protected boolean invokeMoveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverse) {
        return this.moveItemStackTo(stack, startIndex, endIndex, reverse);
    }

    protected boolean storeCarriedStack(ServerPlayer player, DatabaseScope targetScope, boolean singleItem, String targetTabId) {
        ItemStack carried = this.getCarried();
        if (!PersonalDatabaseService.INSTANCE.canStore(carried)) {
            return false;
        }
        PersonalDatabaseServiceDepositHelper.DepositConflict conflict = PersonalDatabaseService.INSTANCE.checkDepositConflict(
                player, targetScope, targetTabId, carried
        );
        if (conflict != null) {
            if (this instanceof PersonalDatabaseMenu menu) {
                menu.sendDepositConflict(conflict.scope(), conflict.targetTabId(), conflict.existingTabId(), conflict.stack(), -2);
            }
            return false;
        }
        ItemStack storedStack = singleItem ? carried.split(1) : carried.copyAndClear();
        if (storedStack.isEmpty()) {
            return false;
        }
        if (!PersonalDatabaseService.INSTANCE.storeStack(player, targetScope, targetTabId, storedStack)) {
            this.setCarried(singleItem ? carried.copyWithCount(carried.getCount() + storedStack.getCount()) : storedStack);
            return false;
        }
        this.setCarried(carried);
        return true;
    }

    protected boolean withdrawToCarried(ServerPlayer player, DatabaseScope sourceScope, StoredStackKey key, int requestedAmount) {
        ItemStack carried = this.getCarried();
        if (!carried.isEmpty() && !ItemStack.isSameItemSameComponents(carried, key.displayStack())) {
            return false;
        }
        int room = carried.isEmpty() ? key.maxStackSize() : carried.getMaxStackSize() - carried.getCount();
        if (room <= 0) {
            return false;
        }
        ItemStack extracted = PersonalDatabaseService.INSTANCE.extractToCarried(
                player,
                sourceScope,
                key,
                Math.min(room, requestedAmount)
        );
        if (extracted.isEmpty()) {
            return false;
        }
        if (carried.isEmpty()) {
            this.setCarried(extracted);
        } else {
            carried.grow(extracted.getCount());
            this.setCarried(carried);
        }
        return true;
    }

    @Nullable
    protected DatabasePageEntry getPageEntry(int panelIndex, int pageSlotIndex) {
        if (panelIndex < 0 || panelIndex >= this.currentPages.size() || pageSlotIndex < 0) {
            return null;
        }
        return this.currentPages.get(panelIndex).entryAt(pageSlotIndex);
    }

    protected void moveSlot(int slotIndex, int x, int y) {
        Slot slot = this.slots.get(slotIndex);
        try {
            SLOT_X_FIELD.setInt(slot, x);
            SLOT_Y_FIELD.setInt(slot, y);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Failed to reposition slot " + slotIndex, exception);
        }
    }

    private static Field findSlotField(String fieldName) {
        try {
            Field field = Slot.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    protected DatabaseQuery currentQuery() {
        return this.query;
    }

    protected ViewerLanguage currentViewerLanguage() {
        return this.viewerLanguage;
    }

    protected boolean setViewerLanguage(ViewerLanguage viewerLanguage) {
        ViewerLanguage normalizedLanguage = viewerLanguage == null ? ViewerLanguage.defaultLanguage() : viewerLanguage;
        if (this.viewerLanguage == normalizedLanguage) {
            return false;
        }
        this.viewerLanguage = normalizedLanguage;
        return true;
    }

    protected void applyOpenState(PersonalDatabaseOpenState openState) {
        PersonalDatabaseOpenState normalizedState = openState == null ? PersonalDatabaseOpenState.defaultState() : openState;
        this.query = normalizedState.query();
        this.personalQuery = DatabaseQuery.normalizeForScope(DatabaseScope.PERSONAL, this.query);
        this.publicQuery = DatabaseQuery.normalizeForScope(DatabaseScope.PUBLIC, this.query);
        this.activeScope = this.query.scope();
        this.viewState = new DatabaseViewState(
                this.containerId,
                normalizedState.sessionId(),
                this.query,
                normalizedState.enhancementConfig(),
                normalizedState.autoStoreTarget(),
                List.of(com.agguy.infiniteinventory.database.DatabaseTabs.allTab(), com.agguy.infiniteinventory.database.DatabaseTabs.defaultConcreteTab()),
                List.of(com.agguy.infiniteinventory.database.DatabaseTabs.allTab(), com.agguy.infiniteinventory.database.DatabaseTabs.defaultConcreteTab()),
                List.of()
        );
        this.enhancementConfig = normalizedState.enhancementConfig();
        this.autoStoreTarget = normalizedState.autoStoreTarget();
    }

    protected void setActiveQuery(DatabaseQuery query) {
        this.query = query == null ? DatabaseQuery.defaultQuery() : query;
        this.activeScope = this.query.scope();
        this.personalQuery = DatabaseQuery.normalizeForScope(DatabaseScope.PERSONAL, this.query);
        this.publicQuery = DatabaseQuery.normalizeForScope(DatabaseScope.PUBLIC, this.query);
    }

    protected void persistPreferences(ServerPlayer player) {
        DatabaseViewPreferencesAttachment preferences = PersonalDatabaseService.INSTANCE.getViewPreferences(player);
        preferences.setQuery(this.query);
        preferences.setEnhancementConfig(this.enhancementConfig);
        preferences.setAutoStoreTarget(this.autoStoreTarget);
    }

    protected void syncAfterDatabaseMutation(ServerPlayer player) {
        if (this.activeScope == DatabaseScope.PUBLIC) {
            PersonalDatabaseService.INSTANCE.syncPublicViewers(player.server);
            return;
        }
        this.syncViewToClient();
    }

    @Nullable
    protected DatabaseScopedTabRef resolveSingleStoreTarget() {
        DatabaseQuery query = this.currentQuery();
        if (query.visibleTabs().size() != 1) {
            return null;
        }
        DatabaseScopedTabRef onlyVisibleTab = query.visibleTabs().getFirst();
        if (onlyVisibleTab.isAllTab()) {
            return null;
        }
        if (!(this.owner instanceof ServerPlayer serverPlayer)) {
            return null;
        }
        return DatabaseScopedTabRef.concreteTab(
                onlyVisibleTab.scope(),
                PersonalDatabaseService.INSTANCE.resolveConcreteTargetTabId(serverPlayer, onlyVisibleTab.scope(), onlyVisibleTab.tabId())
        );
    }

    protected DatabaseScopedTabRef resolveStoreTarget(
            int panelIndex,
            @Nullable DatabaseScope explicitTargetScope,
            @Nullable String explicitTargetTabId
    ) {
        if (this.owner instanceof ServerPlayer serverPlayer && explicitTargetTabId != null && !explicitTargetTabId.isBlank()) {
            DatabaseScope targetScope = DatabaseScope.normalize(explicitTargetScope);
            return DatabaseScopedTabRef.concreteTab(
                    targetScope,
                    PersonalDatabaseService.INSTANCE.resolveConcreteTargetTabId(serverPlayer, targetScope, explicitTargetTabId)
            );
        }
        if (panelIndex >= 0 && panelIndex < this.currentPages.size()) {
            DatabasePage page = this.currentPages.get(panelIndex);
            if (!page.tab().isAllTab()) {
                return page.scopedTab();
            }
        }
        DatabaseScopedTabRef singleStoreTarget = this.resolveSingleStoreTarget();
        if (singleStoreTarget != null) {
            return singleStoreTarget;
        }
        if (this.owner instanceof ServerPlayer serverPlayer) {
            DatabaseScopedTabRef focusedTab = this.currentQuery().focusedTab();
            return DatabaseScopedTabRef.concreteTab(
                    focusedTab.scope(),
                    PersonalDatabaseService.INSTANCE.resolveConcreteTargetTabId(serverPlayer, focusedTab.scope(), focusedTab.tabId())
            );
        }
        return DatabaseScopedTabRef.concreteTab(DatabaseScope.defaultScope(), com.agguy.infiniteinventory.database.DatabaseTabs.DEFAULT_TAB_ID);
    }

    protected abstract void syncViewToClient();

    protected static final class EquipmentDisplaySlot extends Slot {
        private final LivingEntity owner;
        private final EquipmentSlot slotType;
        @Nullable
        private final ResourceLocation emptyIcon;

        private EquipmentDisplaySlot(
                Container container,
                LivingEntity owner,
                EquipmentSlot slotType,
                int slotIndex,
                int x,
                int y,
                @Nullable ResourceLocation emptyIcon
        ) {
            super(container, slotIndex, x, y);
            this.owner = owner;
            this.slotType = slotType;
            this.emptyIcon = emptyIcon;
        }

        @Override
        public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
            this.owner.onEquipItem(this.slotType, oldStack, newStack);
            super.setByPlayer(newStack, oldStack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.canEquip(this.slotType, this.owner);
        }

        @Override
        public boolean mayPickup(Player player) {
            return super.mayPickup(player);
        }

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            if (this.emptyIcon == null) {
                return super.getNoItemIcon();
            }
            return Pair.of(InventoryMenu.BLOCK_ATLAS, this.emptyIcon);
        }
    }

    protected static final class OffhandDisplaySlot extends Slot {
        private final Player owner;

        private OffhandDisplaySlot(Container container, Player owner, int slotIndex, int x, int y) {
            super(container, slotIndex, x, y);
            this.owner = owner;
        }

        @Override
        public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
            this.owner.onEquipItem(EquipmentSlot.OFFHAND, oldStack, newStack);
            super.setByPlayer(newStack, oldStack);
        }

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
        }
    }
}
