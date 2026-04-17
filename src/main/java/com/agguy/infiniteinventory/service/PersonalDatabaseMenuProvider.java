package com.agguy.infiniteinventory.service;

import com.agguy.infiniteinventory.database.DatabaseViewPreferencesAttachment;
import com.agguy.infiniteinventory.menu.PersonalDatabaseMenu;
import com.agguy.infiniteinventory.menu.PersonalDatabaseOpenState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.extensions.IMenuProviderExtension;

final class PersonalDatabaseMenuProvider implements MenuProvider, IMenuProviderExtension {
    private final ServerPlayer player;
    private final DatabaseViewPreferencesAttachment preferences;

    PersonalDatabaseMenuProvider(ServerPlayer player, DatabaseViewPreferencesAttachment preferences) {
        this.player = player;
        this.preferences = preferences;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.infiniteinventory.database.title");
    }

    @Override
    public PersonalDatabaseMenu createMenu(int containerId, Inventory playerInventory, Player ignoredPlayer) {
        PersonalDatabaseMenu menu = new PersonalDatabaseMenu(containerId, playerInventory, this.player);
        menu.initializeFromPreferences(this.preferences);
        return menu;
    }

    @Override
    public void writeClientSideData(net.minecraft.world.inventory.AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        if (menu instanceof PersonalDatabaseMenu databaseMenu) {
            PersonalDatabaseOpenState.write(buffer, new PersonalDatabaseOpenState(
                    databaseMenu.sessionId(),
                    databaseMenu.viewState().query(),
                    databaseMenu.enhancementConfig(),
                    databaseMenu.autoStoreTarget()
            ));
        } else {
            PersonalDatabaseOpenState.write(buffer, PersonalDatabaseOpenState.defaultState());
        }
    }
}
