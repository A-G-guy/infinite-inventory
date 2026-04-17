package com.agguy.infiniteinventory.client.screen;

final class PersonalDatabasePrimaryClickModel {
    private PersonalDatabasePrimaryClickModel() {
    }

    static Action resolve(boolean shiftDown, boolean controlDown, boolean clickedEntrySelected) {
        if (shiftDown) {
            return Action.TAKE_STACK_TO_INVENTORY;
        }
        if (controlDown) {
            return Action.CTRL_SELECTION;
        }
        return clickedEntrySelected ? Action.TAKE_SINGLE : Action.REPLACE_SELECTION;
    }

    enum Action {
        TAKE_STACK_TO_INVENTORY,
        CTRL_SELECTION,
        TAKE_SINGLE,
        REPLACE_SELECTION
    }
}
