package com.agguy.infiniteinventory.event.tests;

import com.agguy.infiniteinventory.event.ModGameEvents;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModGameEventsTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ModGameEvents> constructor = ModGameEvents.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()), "工具类构造器应为 private");
    }

    @Test
    void onLivingDropsShouldNotModifyDropsForNonPlayerEntities() {
        Collection<ItemEntity> drops = new ArrayList<>();
        ItemEntity diamondEntity = mock(ItemEntity.class);
        when(diamondEntity.getItem()).thenReturn(new ItemStack(Items.DIAMOND));
        drops.add(diamondEntity);

        net.minecraft.world.entity.LivingEntity nonPlayerLiving = mock(net.minecraft.world.entity.LivingEntity.class);
        LivingDropsEvent event = mock(LivingDropsEvent.class);
        when(event.getEntity()).thenReturn(nonPlayerLiving);
        when(event.getDrops()).thenReturn(drops);

        ModGameEvents.onLivingDrops(event);

        assertEquals(1, drops.size());
    }
}
