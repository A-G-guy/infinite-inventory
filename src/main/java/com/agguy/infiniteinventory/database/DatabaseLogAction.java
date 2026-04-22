package com.agguy.infiniteinventory.database;

public enum DatabaseLogAction {
    DEPOSIT("screen.infiniteinventory.log.action.deposit"),
    EXTRACT("screen.infiniteinventory.log.action.extract"),
    TRANSFER("screen.infiniteinventory.log.action.transfer"),
    DELETE("screen.infiniteinventory.log.action.delete");

    private final String translationKey;

    DatabaseLogAction(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public static DatabaseLogAction normalize(DatabaseLogAction action) {
        return action == null ? DEPOSIT : action;
    }

    public static DatabaseLogAction read(String serializedAction) {
        if (serializedAction == null || serializedAction.isBlank()) {
            return DEPOSIT;
        }
        try {
            return DatabaseLogAction.valueOf(serializedAction);
        } catch (IllegalArgumentException exception) {
            return DEPOSIT;
        }
    }
}
