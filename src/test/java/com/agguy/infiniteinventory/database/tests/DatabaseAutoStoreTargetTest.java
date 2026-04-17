package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseAutoStoreTarget;
import com.agguy.infiniteinventory.database.DatabaseScope;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseAutoStoreTargetTest {
    @Test
    void shouldRoundTripThroughTagAndBuffer() {
        DatabaseAutoStoreTarget target = new DatabaseAutoStoreTarget(DatabaseScope.PUBLIC, "public_food");
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        DatabaseAutoStoreTarget.write(buffer, target);

        assertEquals(target, DatabaseAutoStoreTarget.fromTag(target.toTag()));
        assertEquals(target, DatabaseAutoStoreTarget.read(buffer));
    }
}
