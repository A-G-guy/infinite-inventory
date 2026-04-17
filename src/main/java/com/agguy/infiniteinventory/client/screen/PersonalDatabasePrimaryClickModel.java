package com.agguy.infiniteinventory.client.screen;

final class PersonalDatabasePrimaryClickModel {
    private PersonalDatabasePrimaryClickModel() {
    }

    static Action resolve(boolean shiftDown, boolean controlDown) {
        if (shiftDown) {
            return Action.TAKE_STACK_TO_INVENTORY;
        }
        return controlDown ? Action.START_ADDITIVE_SELECTION : Action.START_REPLACE_SELECTION;
    }

    enum Action {
        TAKE_STACK_TO_INVENTORY,
        START_ADDITIVE_SELECTION,
        START_REPLACE_SELECTION
    }
}
