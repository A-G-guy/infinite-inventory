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
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.InventoryMenu;
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

    public void syncViewToClient() {
        if (!(this.owner instanceof ServerPlayer serverPlayer)) {
            return;
        }
        this.currentPage = PersonalDatabaseService.INSTANCE.buildPage(serverPlayer, this.query, PersonalDatabaseLayout.DATABASE_SLOT_COUNT);
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
        if (!(this.owner instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (action == DatabaseClickAction.QUICK_MOVE) {
            this.quickMoveFromDatabase(pageSlotIndex);
            return;
        }
        if (!this.getCarried().isEmpty()) {
            this.storeCarriedStack(action == DatabaseClickAction.SECONDARY);
            return;
        }
        DatabasePageEntry pageEntry = this.getPageEntry(pageSlotIndex);
        if (pageEntry == null) {
            return;
        }
        int requestedAmount = action == DatabaseClickAction.SECONDARY ? 1 : pageEntry.key().maxStackSize();
        if (this.withdrawToCarried(pageEntry.key(), requestedAmount)) {
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
        if (slotIndex == 0) {
            if (!this.moveItemStackTo(rawStack, 9, 46, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(rawStack, copy);
        } else if (slotIndex >= 1 && slotIndex < 5) {
            if (!this.moveItemStackTo(rawStack, 9, 46, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex >= 5 && slotIndex < 9) {
            if (!this.moveItemStackTo(rawStack, 9, 46, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex == 45) {
            if (!this.moveItemStackTo(rawStack, 9, 45, false)) {
                return ItemStack.EMPTY;
            }
        } else if (equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            int armorSlotIndex = 8 - equipmentSlot.getIndex();
            if (!this.slots.get(armorSlotIndex).hasItem() && !this.moveItemStackTo(rawStack, armorSlotIndex, armorSlotIndex + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (equipmentSlot == EquipmentSlot.OFFHAND && !this.slots.get(45).hasItem()) {
            if (!this.moveItemStackTo(rawStack, 45, 46, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex >= 9 && slotIndex < 36) {
            if (!this.moveItemStackTo(rawStack, 36, 45, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex >= 36 && slotIndex < 45) {
            if (!this.moveItemStackTo(rawStack, 9, 36, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(rawStack, 9, 45, false)) {
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
        return 0;
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
        int panelY = PersonalDatabaseLayout.PLAYER_PANEL_Y;
        this.addSlot(new ResultSlot(owner, this.craftSlots, this.resultSlots, 0, 154, panelY + 28));
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 2; column++) {
                this.addSlot(new Slot(this.craftSlots, column + row * 2, 98 + column * 18, panelY + 18 + row * 18));
            }
        }
        for (int index = 0; index < ARMOR_ORDER.length; index++) {
            EquipmentSlot equipmentSlot = ARMOR_ORDER[index];
            int inventoryIndex = 39 - index;
            int x = 8;
            int y = panelY + 8 + index * 18;
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
                this.addSlot(new Slot(playerInventory, slotIndex, 8 + column * 18, panelY + 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, panelY + 142));
        }
        this.addSlot(new OffhandDisplaySlot(playerInventory, owner, 40, 77, panelY + 62));
    }

    private boolean shouldDepositQuickMovedSlot(int slotIndex) {
        return slotIndex >= 9 && slotIndex < 45;
    }

    private void storeCarriedStack(boolean singleItem) {
        ItemStack carried = this.getCarried();
        if (!PersonalDatabaseService.INSTANCE.canStore(carried)) {
            return;
        }
        ItemStack storedStack = singleItem ? carried.split(1) : carried.copyAndClear();
        if (storedStack.isEmpty()) {
            return;
        }
        PersonalDatabaseService.INSTANCE.getDatabase(this.owner).store(storedStack);
        this.setCarried(carried);
        this.broadcastChanges();
        this.syncViewToClient();
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

    private void quickMoveFromDatabase(int pageSlotIndex) {
        DatabasePageEntry pageEntry = this.getPageEntry(pageSlotIndex);
        if (pageEntry == null) {
            return;
        }
        if (PersonalDatabaseService.INSTANCE.extractToInventory(this.owner, pageEntry.key()) > 0) {
            this.broadcastChanges();
            this.syncViewToClient();
        }
    }

    @Nullable
    private DatabasePageEntry getPageEntry(int pageSlotIndex) {
        if (pageSlotIndex < 0 || pageSlotIndex >= PersonalDatabaseLayout.DATABASE_SLOT_COUNT || this.currentPage == null) {
            return null;
        }
        return this.currentPage.entryAt(pageSlotIndex);
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
