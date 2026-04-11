package com.agguy.infiniteinventory;

import com.agguy.infiniteinventory.client.screen.PersonalDatabaseScreen;
import com.agguy.infiniteinventory.registry.ModMenus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class InfiniteInventoryClient {
    private InfiniteInventoryClient() {
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.PERSONAL_DATABASE_MENU.get(), PersonalDatabaseScreen::new);
    }
}
