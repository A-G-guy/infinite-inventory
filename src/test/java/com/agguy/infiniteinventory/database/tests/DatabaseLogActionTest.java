package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.DatabaseLogAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link DatabaseLogAction} 枚举的序列化与反序列化行为测试。
 */
class DatabaseLogActionTest {

    @Test
    void shouldReturnDepositWhenNormalizedNull() {
        assertEquals(DatabaseLogAction.DEPOSIT, DatabaseLogAction.normalize(null));
    }

    @Test
    void shouldReturnSelfWhenNormalizedNonNull() {
        assertEquals(DatabaseLogAction.EXTRACT, DatabaseLogAction.normalize(DatabaseLogAction.EXTRACT));
        assertEquals(DatabaseLogAction.TRANSFER, DatabaseLogAction.normalize(DatabaseLogAction.TRANSFER));
        assertEquals(DatabaseLogAction.DELETE, DatabaseLogAction.normalize(DatabaseLogAction.DELETE));
    }

    @Test
    void shouldDeserializeValidNames() {
        assertEquals(DatabaseLogAction.DEPOSIT, DatabaseLogAction.read("DEPOSIT"));
        assertEquals(DatabaseLogAction.EXTRACT, DatabaseLogAction.read("EXTRACT"));
        assertEquals(DatabaseLogAction.TRANSFER, DatabaseLogAction.read("TRANSFER"));
        assertEquals(DatabaseLogAction.DELETE, DatabaseLogAction.read("DELETE"));
    }

    @Test
    void shouldFallbackToDepositWhenReadNull() {
        assertEquals(DatabaseLogAction.DEPOSIT, DatabaseLogAction.read(null));
    }

    @Test
    void shouldFallbackToDepositWhenReadBlank() {
        assertEquals(DatabaseLogAction.DEPOSIT, DatabaseLogAction.read(""));
        assertEquals(DatabaseLogAction.DEPOSIT, DatabaseLogAction.read("   "));
    }

    @Test
    void shouldFallbackToDepositWhenReadInvalidName() {
        assertEquals(DatabaseLogAction.DEPOSIT, DatabaseLogAction.read("UNKNOWN"));
        assertEquals(DatabaseLogAction.DEPOSIT, DatabaseLogAction.read("deposit"));
        assertEquals(DatabaseLogAction.DEPOSIT, DatabaseLogAction.read("Deposit"));
    }

    @Test
    void shouldPreserveTranslationKey() {
        assertEquals("screen.infiniteinventory.log.action.deposit", DatabaseLogAction.DEPOSIT.translationKey());
        assertEquals("screen.infiniteinventory.log.action.extract", DatabaseLogAction.EXTRACT.translationKey());
        assertEquals("screen.infiniteinventory.log.action.transfer", DatabaseLogAction.TRANSFER.translationKey());
        assertEquals("screen.infiniteinventory.log.action.delete", DatabaseLogAction.DELETE.translationKey());
    }

    @Test
    void serializeDeserializeShouldBeSymmetric() {
        for (DatabaseLogAction action : DatabaseLogAction.values()) {
            String serialized = action.name();
            DatabaseLogAction deserialized = DatabaseLogAction.read(serialized);
            assertEquals(action, deserialized, "序列化/反序列化应对称: " + action);
        }
    }
}
