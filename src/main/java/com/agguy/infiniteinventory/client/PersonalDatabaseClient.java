package com.agguy.infiniteinventory.client;

import com.agguy.infiniteinventory.database.DatabaseViewState;
import com.agguy.infiniteinventory.menu.PersonalDatabaseMenu;
import net.minecraft.client.Minecraft;

public final class PersonalDatabaseClient {
    private PersonalDatabaseClient() {
    }

    public static void applySnapshot(DatabaseViewState viewState) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (minecraft.player.containerMenu instanceof PersonalDatabaseMenu menu
                && menu.containerId == viewState.containerId()
                && menu.sessionId() == viewState.sessionId()) {
            menu.applyViewState(viewState);
        }
    }
}
