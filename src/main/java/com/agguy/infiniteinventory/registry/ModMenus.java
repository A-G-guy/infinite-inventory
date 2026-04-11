package com.agguy.infiniteinventory.registry;

import com.agguy.infiniteinventory.InfiniteInventory;
import com.agguy.infiniteinventory.menu.PersonalDatabaseMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> REGISTER = DeferredRegister.create(Registries.MENU, InfiniteInventory.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<PersonalDatabaseMenu>> PERSONAL_DATABASE_MENU = REGISTER.register(
            "personal_database",
            () -> IMenuTypeExtension.create((containerId, playerInventory, extraData) ->
                    new PersonalDatabaseMenu(containerId, playerInventory, extraData == null ? 0L : extraData.readVarLong()))
    );

    private ModMenus() {
    }
}
