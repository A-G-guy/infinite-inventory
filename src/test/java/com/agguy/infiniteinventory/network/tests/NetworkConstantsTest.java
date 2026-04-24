package com.agguy.infiniteinventory.network.tests;

import com.agguy.infiniteinventory.network.NetworkConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NetworkConstants 边界校验方法的单元测试。
 */
class NetworkConstantsTest {

    @Test
    void checkListSizeShouldAcceptValidCount() {
        assertDoesNotThrow(() -> NetworkConstants.checkListSize(0, 100, "testField"));
        assertDoesNotThrow(() -> NetworkConstants.checkListSize(50, 100, "testField"));
        assertDoesNotThrow(() -> NetworkConstants.checkListSize(100, 100, "testField"));
    }

    @Test
    void checkListSizeShouldThrowForNegativeCount() {
        assertThrows(IllegalStateException.class,
                () -> NetworkConstants.checkListSize(-1, 100, "testField"));
    }

    @Test
    void checkListSizeShouldThrowForCountExceedingMax() {
        assertThrows(IllegalStateException.class,
                () -> NetworkConstants.checkListSize(101, 100, "testField"));
    }

    @Test
    void checkListSizeExceptionMessageShouldContainFieldName() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> NetworkConstants.checkListSize(10001, NetworkConstants.MAX_SELECTION_ENTRY_COUNT, "selectedEntries"));
        assertTrue(exception.getMessage().contains("selectedEntries"));
    }

    @Test
    void allMaxConstantsShouldBePositive() {
        assertTrue(NetworkConstants.MAX_SELECTION_ENTRY_COUNT > 0);
        assertTrue(NetworkConstants.MAX_STACK_LIST_COUNT > 0);
        assertTrue(NetworkConstants.MAX_PANEL_COUNT > 0);
        assertTrue(NetworkConstants.MAX_TAB_COUNT > 0);
        assertTrue(NetworkConstants.MAX_LOG_ENTRY_COUNT > 0);
        assertTrue(NetworkConstants.MAX_JEI_ENTRY_COUNT > 0);
        assertTrue(NetworkConstants.MAX_QUERY_VISIBLE_TAB_COUNT > 0);
        assertTrue(NetworkConstants.MAX_QUERY_TAB_STATE_COUNT > 0);
        assertTrue(NetworkConstants.MAX_QUERY_HIDDEN_TOP_TAB_COUNT > 0);
    }

    @Test
    void checkListSizeShouldAcceptZeroCount() {
        assertDoesNotThrow(() -> NetworkConstants.checkListSize(0, 1, "field"));
    }
}
