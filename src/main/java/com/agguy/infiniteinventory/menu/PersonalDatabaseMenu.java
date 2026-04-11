package com.agguy.infiniteinventory.menu;

import com.agguy.infiniteinventory.database.DatabasePage;
import com.agguy.infiniteinventory.database.DatabasePageEntry;
import com.agguy.infiniteinventory.database.DatabaseQuery;
import com.agguy.infiniteinventory.database.DatabaseViewState;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.network.DatabaseClickAction;
import com.agguy.infiniteinventory.network.DatabaseSnapshotPayload;
import com.agguy.infiniteinventory.registry.ModMenus;
import com.agguy.infiniteinventory.service.PersonalDatabaseService;
import com.mojang.datafixers.util.Pair;
import java.lang.reflect.Field;
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
    private static final int RESULT_SLOT_MENU_INDEX = 0;
    private static final int FIRST_CRAFT_SLOT_MENU_INDEX = 1;
    private static final int FIRST_ARMOR_SLOT_MENU_INDEX = 5;
    private static final int FIRST_MAIN_INVENTORY_SLOT_MENU_INDEX = 9;
    private static final int FIRST_HOTBAR_SLOT_MENU_INDEX = 36;
    private static final int OFFHAND_SLOT_MENU_INDEX = 45;
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
    private DatabaseQuery query = DatabaseQuery.defaultQuery();
    private DatabaseViewState viewState;
    @Nullable
    private DatabasePage currentPage;

    public PersonalDatabaseMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, playerInventory.player);
    }

    public PersonalDatabaseMenu(int containerId, Inventory playerInventory, Player owner) {
        super(ModMenus.PERSONAL_DATABASE_MENU.get(), containerId);
        this.owner = owner;
        this.viewState = DatabaseViewState.empty(containerId);
        this.addVanillaInventorySlots(playerInventory, owner);
    }

    public DatabaseViewState viewState() {
        return this.viewState;
    }

    public void applyViewState(DatabaseViewState newState) {
        this.viewState = newState;
        this.query = newState.query();
    }

    public void applySlotLayout(PersonalDatabaseLayout layout) {
        if (layout == null || this.slots.size() <= OFFHAND_SLOT_MENU_INDEX) {
            return;
        }
        PersonalDatabaseLayout.Rect equipmentPanel = layout.equipmentPanelRect();
        PersonalDatabaseLayout.Rect bottomInventory = layout.bottomInventoryRect();
        this.moveSlot(RESULT_SLOT_MENU_INDEX, equipmentPanel.x() + TOP_SECTION_RESULT_X, equipmentPanel.y() + TOP_SECTION_RESULT_Y);
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 2; column++) {
                int slotIndex = FIRST_CRAFT_SLOT_MENU_INDEX + column + row * 2;
                this.moveSlot(slotIndex, equipmentPanel.x() + TOP_SECTION_CRAFT_X + column * PersonalDatabaseLayout.SLOT_SIZE, equipmentPanel.y() + TOP_SECTION_CRAFT_Y + row * PersonalDatabaseLayout.SLOT_SIZE);
            }
        }
        for (int index = 0; index < ARMOR_ORDER.length; index++) {
            this.moveSlot(FIRST_ARMOR_SLOT_MENU_INDEX + index, equipmentPanel.x() + TOP_SECTION_ARMOR_X, equipmentPanel.y() + TOP_SECTION_ARMOR_Y + index * PersonalDatabaseLayout.SLOT_SIZE);
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int slotIndex = FIRST_MAIN_INVENTORY_SLOT_MENU_INDEX + column + row * 9;
                this.moveSlot(slotIndex, bottomInventory.x() + BOTTOM_SECTION_INVENTORY_X + column * PersonalDatabaseLayout.SLOT_SIZE, bottomInventory.y() + BOTTOM_SECTION_INVENTORY_Y + row * PersonalDatabaseLayout.SLOT_SIZE);
            }
        }
        for (int column = 0; column < 9; column++) {
            this.moveSlot(FIRST_HOTBAR_SLOT_MENU_INDEX + column, bottomInventory.x() + BOTTOM_SECTION_INVENTORY_X + column * PersonalDatabaseLayout.SLOT_SIZE, bottomInventory.y() + BOTTOM_SECTION_HOTBAR_Y);
        }
        this.moveSlot(OFFHAND_SLOT_MENU_INDEX, equipmentPanel.x() + TOP_SECTION_OFFHAND_X, equipmentPanel.y() + TOP_SECTION_OFFHAND_Y);
    }

    public void syncViewToClient() {
        if (!(this.owner instanceof ServerPlayer serverPlayer)) {
            return;
        }
        this.currentPage = PersonalDatabaseService.INSTANCE.buildPage(serverPlayer, this.query);
        this.query = this.currentPage.query();
        this.viewState = this.currentPage.toViewState(this.containerId);
        PacketDistributor.sendToPlayer(serverPlayer, new DatabaseSnapshotPayload(this.viewState));
    }

    public void updateQuery(DatabaseQuery newQuery) {
        this.query = newQuery == null ? DatabaseQuery.defaultQuery() : newQuery;
        this.syncViewToClient();
    }

    public void depositAllFromMainInventory() {
        if (this.owner instanceof ServerPlayer serverPlayer && PersonalDatabaseService.INSTANCE.depositMainInventory(serverPlayer) > 0) {
            this.broadcastChanges();
            this.syncViewToClient();
        }
    }

    public void handleDatabaseClick(int pageSlotIndex, DatabaseClickAction action) {
        if (!(this.owner instanceof ServerPlayer)) {
            return;
        }
        boolean changed = false;
        if (action.isStoreAction()) {
            if (!this.getCarried().isEmpty()) {
                changed = this.storeCarriedStack(action.storesSingleItem());
            }
        } else {
            if (!this.getCarried().isEmpty()) {
                return;
            }
            DatabasePageEntry pageEntry = this.getPageEntry(pageSlotIndex);
            if (pageEntry == null) {
                return;
            }
            if (action.extractsToInventory()) {
                long requestedAmount = action.extractsEntireEntry()
                        ? Long.MAX_VALUE
                        : action.resolveRequestedAmount(pageEntry.key().maxStackSize());
                changed = PersonalDatabaseService.INSTANCE.extractToInventory(this.owner, pageEntry.key(), requestedAmount) > 0L;
            } else {
                changed = this.withdrawToCarried(pageEntry.key(), action.resolveRequestedAmount(pageEntry.key().maxStackSize()));
            }
        }
        if (changed) {
            this.broadcastChanges();
            this.syncViewToClient();
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

        if (this.shouldDepositQuickMovedSlot(slotIndex) && player instanceof ServerPlayer serverPlayer && PersonalDatabaseService.INSTANCE.depositSlot(serverPlayer, slot)) {
            this.broadcastChanges();
            this.syncViewToClient();
            return copy;
        }

        EquipmentSlot equipmentSlot = player.getEquipmentSlotForItem(copy);
        if (slotIndex == RESULT_SLOT_MENU_INDEX) {
            if (!this.moveItemStackTo(rawStack, FIRST_MAIN_INVENTORY_SLOT_MENU_INDEX, OFFHAND_SLOT_MENU_INDEX + 1, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(rawStack, copy);
        } else if (slotIndex >= FIRST_CRAFT_SLOT_MENU_INDEX && slotIndex < FIRST_ARMOR_SLOT_MENU_INDEX) {
            if (!this.moveItemStackTo(rawStack, FIRST_MAIN_INVENTORY_SLOT_MENU_INDEX, OFFHAND_SLOT_MENU_INDEX + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex >= FIRST_ARMOR_SLOT_MENU_INDEX && slotIndex < FIRST_MAIN_INVENTORY_SLOT_MENU_INDEX) {
            if (!this.moveItemStackTo(rawStack, FIRST_MAIN_INVENTORY_SLOT_MENU_INDEX, OFFHAND_SLOT_MENU_INDEX + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex == OFFHAND_SLOT_MENU_INDEX) {
            if (!this.moveItemStackTo(rawStack, FIRST_MAIN_INVENTORY_SLOT_MENU_INDEX, OFFHAND_SLOT_MENU_INDEX, false)) {
                return ItemStack.EMPTY;
            }
        } else if (equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            int armorSlotIndex = FIRST_ARMOR_SLOT_MENU_INDEX + (3 - equipmentSlot.getIndex());
            if (!this.slots.get(armorSlotIndex).hasItem() && !this.moveItemStackTo(rawStack, armorSlotIndex, armorSlotIndex + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (equipmentSlot == EquipmentSlot.OFFHAND && !this.slots.get(OFFHAND_SLOT_MENU_INDEX).hasItem()) {
            if (!this.moveItemStackTo(rawStack, OFFHAND_SLOT_MENU_INDEX, OFFHAND_SLOT_MENU_INDEX + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex >= FIRST_MAIN_INVENTORY_SLOT_MENU_INDEX && slotIndex < FIRST_HOTBAR_SLOT_MENU_INDEX) {
            if (!this.moveItemStackTo(rawStack, FIRST_HOTBAR_SLOT_MENU_INDEX, OFFHAND_SLOT_MENU_INDEX, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex >= FIRST_HOTBAR_SLOT_MENU_INDEX && slotIndex < OFFHAND_SLOT_MENU_INDEX) {
            if (!this.moveItemStackTo(rawStack, FIRST_MAIN_INVENTORY_SLOT_MENU_INDEX, FIRST_HOTBAR_SLOT_MENU_INDEX, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(rawStack, FIRST_MAIN_INVENTORY_SLOT_MENU_INDEX, OFFHAND_SLOT_MENU_INDEX, false)) {
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
        return RESULT_SLOT_MENU_INDEX;
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

    private void addVanillaInventorySlots(Inventory playerInventory, Player owner) {
        this.addSlot(new ResultSlot(owner, this.craftSlots, this.resultSlots, 0, TOP_SECTION_RESULT_X, TOP_SECTION_RESULT_Y));
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 2; column++) {
                this.addSlot(new Slot(this.craftSlots, column + row * 2, TOP_SECTION_CRAFT_X + column * PersonalDatabaseLayout.SLOT_SIZE, TOP_SECTION_CRAFT_Y + row * PersonalDatabaseLayout.SLOT_SIZE));
            }
        }
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
            this.addSlot(new EquipmentDisplaySlot(playerInventory, owner, equipmentSlot, inventoryIndex, x, y, icon));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int slotIndex = column + (row + 1) * 9;
                this.addSlot(new Slot(playerInventory, slotIndex, BOTTOM_SECTION_INVENTORY_X + column * PersonalDatabaseLayout.SLOT_SIZE, BOTTOM_SECTION_INVENTORY_Y + row * PersonalDatabaseLayout.SLOT_SIZE));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, BOTTOM_SECTION_INVENTORY_X + column * PersonalDatabaseLayout.SLOT_SIZE, BOTTOM_SECTION_HOTBAR_Y));
        }
        this.addSlot(new OffhandDisplaySlot(playerInventory, owner, 40, TOP_SECTION_OFFHAND_X, TOP_SECTION_OFFHAND_Y));
    }

    private boolean shouldDepositQuickMovedSlot(int slotIndex) {
        return slotIndex >= FIRST_MAIN_INVENTORY_SLOT_MENU_INDEX && slotIndex < OFFHAND_SLOT_MENU_INDEX;
    }

    private boolean storeCarriedStack(boolean singleItem) {
        ItemStack carried = this.getCarried();
        if (!PersonalDatabaseService.INSTANCE.canStore(carried)) {
            return false;
        }
        ItemStack storedStack = singleItem ? carried.split(1) : carried.copyAndClear();
        if (storedStack.isEmpty()) {
            return false;
        }
        PersonalDatabaseService.INSTANCE.getDatabase(this.owner).store(storedStack);
        this.setCarried(carried);
        return true;
    }

    private boolean withdrawToCarried(StoredStackKey key, int requestedAmount) {
        ItemStack carried = this.getCarried();
        if (!carried.isEmpty() && !ItemStack.isSameItemSameComponents(carried, key.displayStack())) {
            return false;
        }
        int room = carried.isEmpty() ? key.maxStackSize() : carried.getMaxStackSize() - carried.getCount();
        if (room <= 0) {
            return false;
        }
        ItemStack extracted = PersonalDatabaseService.INSTANCE.getDatabase(this.owner).extract(key, Math.min(room, requestedAmount));
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
