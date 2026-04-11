package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackEntry;
import com.agguy.infiniteinventory.database.StoredStackKey;
import java.lang.reflect.Field;
import java.util.Map;
import sun.misc.Unsafe;

final class DatabaseTestReflectionHelper {
    private DatabaseTestReflectionHelper() {
    }

    @SuppressWarnings("unchecked")
    static void forceEntry(StoredItemDatabase database, StoredStackKey key, StoredStackEntry entry) throws ReflectiveOperationException {
        Field entriesField = StoredItemDatabase.class.getDeclaredField("entries");
        entriesField.setAccessible(true);
        Map<StoredStackKey, StoredStackEntry> entries = (Map<StoredStackKey, StoredStackEntry>) entriesField.get(database);
        entries.put(key, entry);
    }

    static void forceNextSequence(StoredItemDatabase database, long nextSequence) throws ReflectiveOperationException {
        Field nextSequenceField = StoredItemDatabase.class.getDeclaredField("nextSequence");
        nextSequenceField.setAccessible(true);
        nextSequenceField.setLong(database, nextSequence);
    }

    static long readNextSequence(StoredItemDatabase database) throws ReflectiveOperationException {
        Field nextSequenceField = StoredItemDatabase.class.getDeclaredField("nextSequence");
        nextSequenceField.setAccessible(true);
        return nextSequenceField.getLong(database);
    }

    static StoredStackKey fakeKey(String registryName) throws ReflectiveOperationException {
        StoredStackKey key = (StoredStackKey) unsafe().allocateInstance(StoredStackKey.class);
        String[] nameParts = registryName.split(":", 2);
        setField(key, "displayStack", null);
        setField(key, "hashCode", registryName.hashCode());
        setField(key, "registryName", registryName);
        setField(key, "registryNamespace", nameParts.length > 1 ? nameParts[0] : "test");
        setField(key, "registryPath", nameParts.length > 1 ? nameParts[1] : registryName);
        return key;
    }

    private static Unsafe unsafe() throws ReflectiveOperationException {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (Unsafe) unsafeField.get(null);
    }

    private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
