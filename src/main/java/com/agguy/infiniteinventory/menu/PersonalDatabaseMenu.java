package com.agguy.infiniteinventory.menu;

import com.agguy.infiniteinventory.compat.AccessoriesCompat;
import com.agguy.infiniteinventory.compat.AccessorySlotGroup;
import com.agguy.infiniteinventory.database.DatabasePage;
import com.agguy.infiniteinventory.database.DatabasePageEntry;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseScope;
import com.agguy.infiniteinventory.database.DatabaseViewPreferencesAttachment;
import com.agguy.infiniteinventory.database.DatabaseViewState;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.network.DatabaseClickAction;
import com.agguy.infiniteinventory.network.DatabaseSnapshotPayload;
import com.agguy.infiniteinventory.registry.ModMenus;
import com.agguy.infiniteinventory.service.PersonalDatabaseService;
import com.mojang.datafixers.util.Pair;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public final class PersonalDatabaseMenu extends RecipeBookMenu<CraftingInput, CraftingRecipe> {
    private static final EquipmentSlot[] ARMOR_ORDER = {
            EquipmentSlot.FEET,
            EquipmentSlot.LEGS,
            EquipmentSlot.CHEST,
            EquipmentSlot.HEAD
    };
    private static final int TOP_SECTION_RESULT_X = 154;
    private static final int TOP_SECTION_RESULT_Y = 28;
    private static final int TOP_SECTION_CRAFT_X = 98;
    private static final int TOP_SECTION_CRAFT_Y = 18;
    private static final int TOP_SECTION_ARMOR_X = 8;
    private static final int TOP_SECTION_ARMOR_Y = 8;
    private static final int TOP_SECTION_OFFHAND_X = 77;
    private static final int TOP_SECTION_OFFHAND_Y = 62;
    private static final int BOTTOM_SECTION_INVENTORY_X = 8;
    private static final int BOTTOM_SECTION_INVENTORY_Y = 1;
    private static final int BOTTOM_SECTION_HOTBAR_Y = 59;
    private static final Field SLOT_X_FIELD = findSlotField("x");
    private static final Field SLOT_Y_FIELD = findSlotField("y");

    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 2, 2);
    private final ResultContainer resultSlots = new ResultContainer();
    private final Player owner;
    private final int resultSlotIndex;
    private final MenuSlotRange craftingSlotRange;
    private final MenuSlotRange armorSlotRange;
    private final MenuSlotRange mainInventorySlotRange;
    private final MenuSlotRange hotbarSlotRange;
    private final int offhandSlotIndex;
    private final List<AccessorySlotGroup> accessorySlotGroups;
    private final MenuSlotRange accessorySlotRange;
    private DatabaseScope activeScope = DatabaseScope.defaultScope();
    private DatabaseQuery personalQuery = DatabaseQuery.defaultQuery(DatabaseScope.PERSONAL);
    private DatabaseQuery publicQuery = DatabaseQuery.defaultQuery(DatabaseScope.PUBLIC);
    private DatabaseViewState viewState;
    @Nullable
    private DatabasePage currentPage;

    public PersonalDatabaseMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, playerInventory.player);
    }

    public PersonalDatabaseMenu(int containerId, Inventory playerInventory, Player owner) {
        super(ModMenus.PERSONAL_DATABASE_MENU.get(), containerId);
        this.owner = owner;
        this.viewState = DatabaseViewState.empty(containerId, this.currentQuery());
        this.resultSlotIndex = this.addTrackedSlot(new ResultSlot(owner, this.craftSlots, this.resultSlots, 0, TOP_SECTION_RESULT_X, TOP_SECTION_RESULT_Y));
        this.craftingSlotRange = this.addCraftingSlots();
        this.armorSlotRange = this.addArmorSlots(playerInventory, owner);
        this.mainInventorySlotRange = this.addMainInventorySlots(playerInventory);
        this.hotbarSlotRange = this.addHotbarSlots(playerInventory);
        this.offhandSlotIndex = this.addTrackedSlot(new OffhandDisplaySlot(playerInventory, owner, 40, TOP_SECTION_OFFHAND_X, TOP_SECTION_OFFHAND_Y));
        this.accessorySlotGroups = List.copyOf(AccessoriesCompat.appendAccessorySlots(owner, this::addTrackedSlot));
        this.accessorySlotRange = MenuSlotRange.fromGroups(this.accessorySlotGroups);
    }

    public DatabaseViewState viewState() {
        return this.viewState;
    }

    public DatabaseScope activeScope() {
        return this.activeScope;
    }

    public List<AccessorySlotGroup> accessorySlotGroups() {
        return this.accessorySlotGroups;
    }

    public void initializeFromPreferences(DatabaseViewPreferencesAttachment preferences) {
        if (preferences == null) {
            return;
        }
        this.personalQuery = DatabaseQuery.normalizeForScope(DatabaseScope.PERSONAL, preferences.queryFor(DatabaseScope.PERSONAL));
        this.publicQuery = DatabaseQuery.normalizeForScope(DatabaseScope.PUBLIC, preferences.queryFor(DatabaseScope.PUBLIC));
        this.activeScope = DatabaseScope.normalize(preferences.lastScope());
        this.viewState = DatabaseViewState.empty(this.containerId, this.currentQuery());
    }

    public void applyViewState(DatabaseViewState newState) {
        this.viewState = newState;
        this.activeScope = newState.query().scope();
        this.personalQuery = newState.personalQuery();
        this.publicQuery = newState.publicQuery();
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
                this.moveSlot(slotIndex, equipmentPanel.x() + TOP_SECTION_CRAFT_X + column * PersonalDatabaseLayout.SLOT_SIZE, equipmentPanel.y() + TOP_SECTION_CRAFT_Y + row * PersonalDatabaseLayout.SLOT_SIZE);
            }
        }
        for (int index = 0; index < ARMOR_ORDER.length; index++) {
            this.moveSlot(this.armorSlotRange.firstIndex() + index, equipmentPanel.x() + TOP_SECTION_ARMOR_X, equipmentPanel.y() + TOP_SECTION_ARMOR_Y + index * PersonalDatabaseLayout.SLOT_SIZE);
        }
        this.moveSlot(this.offhandSlotIndex, equipmentPanel.x() + TOP_SECTION_OFFHAND_X, equipmentPanel.y() + TOP_SECTION_OFFHAND_Y);
        this.moveAccessorySlots(layout);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int slotIndex = this.mainInventorySlotRange.firstIndex() + column + row * 9;
                this.moveSlot(slotIndex, bottomInventory.x() + BOTTOM_SECTION_INVENTORY_X + column * PersonalDatabaseLayout.SLOT_SIZE, bottomInventory.y() + BOTTOM_SECTION_INVENTORY_Y + row * PersonalDatabaseLayout.SLOT_SIZE);
            }
        }
        for (int column = 0; column < 9; column++) {
            int slotIndex = this.hotbarSlotRange.firstIndex() + column;
            this.moveSlot(slotIndex, bottomInventory.x() + BOTTOM_SECTION_INVENTORY_X + column * PersonalDatabaseLayout.SLOT_SIZE, bottomInventory.y() + BOTTOM_SECTION_HOTBAR_Y);
        }
    }

    public void syncViewToClient() {
        if (!(this.owner instanceof ServerPlayer serverPlayer)) {
            return;
        }
        this.currentPage = PersonalDatabaseService.INSTANCE.buildPage(serverPlayer, this.currentQuery());
        this.setActiveQuery(this.currentPage.query());
        this.persistPreferences(serverPlayer);
        this.viewState = this.currentPage.toViewState(this.containerId, this.personalQuery, this.publicQuery);
        PacketDistributor.sendToPlayer(serverPlayer, new DatabaseSnapshotPayload(this.viewState));
    }

    public void updateQuery(DatabaseQuery newQuery) {
        this.setActiveQuery(newQuery == null ? this.currentQuery() : newQuery);
        if (this.owner instanceof ServerPlayer serverPlayer) {
            this.persistPreferences(serverPlayer);
        }
        this.syncViewToClient();
    }

    public void depositAllFromMainInventory() {
        if (this.owner instanceof ServerPlayer serverPlayer
                && PersonalDatabaseService.INSTANCE.depositMainInventory(serverPlayer, this.activeScope) > 0) {
            this.broadcastChanges();
            this.syncAfterDatabaseMutation(serverPlayer);
        }
    }

    public void handleDatabaseClick(int pageSlotIndex, DatabaseClickAction action) {
        if (!(this.owner instanceof ServerPlayer serverPlayer)) {
            return;
        }
        boolean changed = false;
        boolean refreshSharedView = false;
        if (action.isStoreAction()) {
            if (!this.getCarried().isEmpty()) {
                changed = this.storeCarriedStack(serverPlayer, action.storesSingleItem());
            }
        } else {
            if (!this.getCarried().isEmpty()) {
                return;
            }
            DatabasePageEntry pageEntry = this.getPageEntry(pageSlotIndex);
            if (pageEntry == null) {
                refreshSharedView = this.activeScope == DatabaseScope.PUBLIC;
            } else {
                if (action.extractsToInventory()) {
                    long requestedAmount = action.extractsEntireEntry()
                            ? Long.MAX_VALUE
                            : action.resolveRequestedAmount(pageEntry.key().maxStackSize());
                    changed = PersonalDatabaseService.INSTANCE.extractToInventory(serverPlayer, this.activeScope, pageEntry.key(), requestedAmount) > 0L;
                } else {
                    changed = this.withdrawToCarried(serverPlayer, pageEntry.key(), action.resolveRequestedAmount(pageEntry.key().maxStackSize()));
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
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack rawStack = slot.getItem();
        ItemStack copy = rawStack.copy();

        if (this.shouldDepositQuickMovedSlot(slotIndex)
                && player instanceof ServerPlayer serverPlayer
                && PersonalDatabaseService.INSTANCE.depositSlot(serverPlayer, this.activeScope, slot)) {
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

    private MenuSlotRange addCraftingSlots() {
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

    private MenuSlotRange addArmorSlots(Inventory playerInventory, Player owner) {
        int start = this.slots.size();
        for (int index = 0; index < ARMOR_ORDER.length; index++) {
            EquipmentSlot equipmentSlot = ARMOR_ORDER[index];
            int inventoryIndex = 39 - index;
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

    private MenuSlotRange addMainInventorySlots(Inventory playerInventory) {
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

    private MenuSlotRange addHotbarSlots(Inventory playerInventory) {
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

    private int addTrackedSlot(Slot slot) {
        int index = this.slots.size();
        this.addSlot(slot);
        return index;
    }

    private void moveAccessorySlots(PersonalDatabaseLayout layout) {
        List<PersonalDatabaseLayout.AccessoryGroupLayout> groupLayouts = layout.accessoryGroupLayouts();
        int groupCount = Math.min(this.accessorySlotGroups.size(), groupLayouts.size());
        for (int groupIndex = 0; groupIndex < groupCount; groupIndex++) {
            AccessorySlotGroup group = this.accessorySlotGroups.get(groupIndex);
            PersonalDatabaseLayout.AccessoryGroupLayout groupLayout = groupLayouts.get(groupIndex);
            for (int slotOffset = 0; slotOffset < group.slotCount(); slotOffset++) {
                PersonalDatabaseLayout.Rect slotRect = groupLayout.slotBounds(slotOffset);
                this.moveSlot(group.firstSlotIndex() + slotOffset, slotRect.x(), slotRect.y());
            }
        }
    }

    private boolean shouldDepositQuickMovedSlot(int slotIndex) {
        return this.mainInventorySlotRange.contains(slotIndex)
                || this.hotbarSlotRange.contains(slotIndex)
                || this.accessorySlotRange.contains(slotIndex);
    }

    private boolean moveToPlayerStorage(ItemStack stack, boolean reverse) {
        return this.moveItemStackTo(stack, this.mainInventorySlotRange.firstIndex(), this.offhandSlotIndex + 1, reverse);
    }

    private boolean tryMoveToAccessorySlots(ItemStack stack) {
        if (this.accessorySlotRange.isEmpty()) {
            return false;
        }
        return this.moveItemStackTo(stack, this.accessorySlotRange.firstIndex(), this.accessorySlotRange.lastIndexExclusive(), false);
    }

    private boolean storeCarriedStack(ServerPlayer player, boolean singleItem) {
        ItemStack carried = this.getCarried();
        if (!PersonalDatabaseService.INSTANCE.canStore(carried)) {
            return false;
        }
        ItemStack storedStack = singleItem ? carried.split(1) : carried.copyAndClear();
        if (storedStack.isEmpty()) {
            return false;
        }
        if (!PersonalDatabaseService.INSTANCE.storeStack(player, this.activeScope, storedStack)) {
            this.setCarried(singleItem ? carried.copyWithCount(carried.getCount() + storedStack.getCount()) : storedStack);
            return false;
        }
        this.setCarried(carried);
        return true;
    }

    private boolean withdrawToCarried(ServerPlayer player, StoredStackKey key, int requestedAmount) {
        ItemStack carried = this.getCarried();
        if (!carried.isEmpty() && !ItemStack.isSameItemSameComponents(carried, key.displayStack())) {
            return false;
        }
        int room = carried.isEmpty() ? key.maxStackSize() : carried.getMaxStackSize() - carried.getCount();
        if (room <= 0) {
            return false;
        }
        ItemStack extracted = PersonalDatabaseService.INSTANCE.extractToCarried(player, this.activeScope, key, Math.min(room, requestedAmount));
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
    private DatabasePageEntry getPageEntry(int pageSlotIndex) {
        if (pageSlotIndex < 0 || this.currentPage == null) {
            return null;
        }
        return this.currentPage.entryAt(pageSlotIndex);
    }

    private void moveSlot(int slotIndex, int x, int y) {
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

    private DatabaseQuery currentQuery() {
        return this.activeScope == DatabaseScope.PUBLIC ? this.publicQuery : this.personalQuery;
    }

    private void setActiveQuery(DatabaseQuery query) {
        DatabaseQuery normalizedQuery = query == null
                ? DatabaseQuery.defaultQuery(this.activeScope)
                : query;
        this.activeScope = normalizedQuery.scope();
        if (this.activeScope == DatabaseScope.PUBLIC) {
            this.publicQuery = DatabaseQuery.normalizeForScope(DatabaseScope.PUBLIC, normalizedQuery);
        } else {
            this.personalQuery = DatabaseQuery.normalizeForScope(DatabaseScope.PERSONAL, normalizedQuery);
        }
    }

    private void persistPreferences(ServerPlayer player) {
        DatabaseViewPreferencesAttachment preferences = PersonalDatabaseService.INSTANCE.getViewPreferences(player);
        preferences.setQuery(DatabaseScope.PERSONAL, this.personalQuery);
        preferences.setQuery(DatabaseScope.PUBLIC, this.publicQuery);
        preferences.setLastScope(this.activeScope);
    }

    private void syncAfterDatabaseMutation(ServerPlayer player) {
        if (this.activeScope == DatabaseScope.PUBLIC) {
            PersonalDatabaseService.INSTANCE.syncPublicViewers(player.server);
            return;
        }
        this.syncViewToClient();
    }

    private static final class CraftingMenuAccess extends CraftingMenu {
        private CraftingMenuAccess(int containerId, Inventory playerInventory) {
            super(containerId, playerInventory);
        }

        private static void updateResult(
                AbstractContainerMenu menu,
                net.minecraft.world.level.Level level,
                Player player,
                CraftingContainer craftingSlots,
                ResultContainer resultSlots,
                @Nullable RecipeHolder<CraftingRecipe> recipe
        ) {
            slotChangedCraftingGrid(menu, level, player, craftingSlots, resultSlots, recipe);
        }
    }

    private record MenuSlotRange(int firstIndex, int slotCount) {
        private static MenuSlotRange of(int firstIndex, int slotCount) {
            return new MenuSlotRange(firstIndex, Math.max(0, slotCount));
        }

        private static MenuSlotRange empty() {
            return new MenuSlotRange(0, 0);
        }

        private static MenuSlotRange fromGroups(List<AccessorySlotGroup> groups) {
            if (groups == null || groups.isEmpty()) {
                return empty();
            }
            AccessorySlotGroup firstGroup = groups.getFirst();
            AccessorySlotGroup lastGroup = groups.getLast();
            return of(firstGroup.firstSlotIndex(), lastGroup.lastSlotIndexExclusive() - firstGroup.firstSlotIndex());
        }

        private int lastIndexExclusive() {
            return this.firstIndex + this.slotCount;
        }

        private boolean contains(int slotIndex) {
            return slotIndex >= this.firstIndex && slotIndex < this.lastIndexExclusive();
        }

        private boolean isEmpty() {
            return this.slotCount <= 0;
        }
    }

    private static final class EquipmentDisplaySlot extends Slot {
        private final LivingEntity owner;
        private final EquipmentSlot slotType;
        @Nullable
        private final ResourceLocation emptyIcon;

        private EquipmentDisplaySlot(Container container, LivingEntity owner, EquipmentSlot slotType, int slotIndex, int x, int y, @Nullable ResourceLocation emptyIcon) {
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

    private static final class OffhandDisplaySlot extends Slot {
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
