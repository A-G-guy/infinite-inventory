package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackEntry;
import com.agguy.infiniteinventory.database.StoredStackKey;
import com.agguy.infiniteinventory.database.UnresolvedStoredEntry;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import sun.misc.Unsafe;

public final class DatabaseTestReflectionHelper {
    private DatabaseTestReflectionHelper() {
    }

    @SuppressWarnings("unchecked")
    public static void forceEntry(StoredItemDatabase database, StoredStackKey key, StoredStackEntry entry) throws ReflectiveOperationException {
        Field entriesField = StoredItemDatabase.class.getDeclaredField("entries");
        entriesField.setAccessible(true);
        Map<StoredStackKey, StoredStackEntry> entries = (Map<StoredStackKey, StoredStackEntry>) entriesField.get(database);
        entries.put(key, entry);
    }

    public static void forceNextSequence(StoredItemDatabase database, long nextSequence) throws ReflectiveOperationException {
        Field nextSequenceField = StoredItemDatabase.class.getDeclaredField("nextSequence");
        nextSequenceField.setAccessible(true);
        AtomicLong atomicLong = (AtomicLong) nextSequenceField.get(database);
        atomicLong.set(nextSequence);
    }

    @SuppressWarnings("unchecked")
    public static void forceUnresolvedEntry(StoredItemDatabase database, UnresolvedStoredEntry unresolvedEntry) throws ReflectiveOperationException {
        Field unresolvedEntriesField = StoredItemDatabase.class.getDeclaredField("unresolvedEntries");
        unresolvedEntriesField.setAccessible(true);
        List<UnresolvedStoredEntry> unresolvedEntries = (List<UnresolvedStoredEntry>) unresolvedEntriesField.get(database);
        unresolvedEntries.add(unresolvedEntry);
    }

    public static long readNextSequence(StoredItemDatabase database) throws ReflectiveOperationException {
        Field nextSequenceField = StoredItemDatabase.class.getDeclaredField("nextSequence");
        nextSequenceField.setAccessible(true);
        AtomicLong atomicLong = (AtomicLong) nextSequenceField.get(database);
        return atomicLong.get();
    }

    public static long readRevision(StoredItemDatabase database) throws ReflectiveOperationException {
        Field revisionField = StoredItemDatabase.class.getDeclaredField("revision");
        revisionField.setAccessible(true);
        AtomicLong atomicLong = (AtomicLong) revisionField.get(database);
        return atomicLong.get();
    }

    public static boolean invokeRecategorizeResolvedEntriesIfNeeded(StoredItemDatabase database, int storedClassifierVersion) throws ReflectiveOperationException {
        var method = StoredItemDatabase.class.getDeclaredMethod("recategorizeResolvedEntriesIfNeeded", int.class);
        method.setAccessible(true);
        return (boolean) method.invoke(database, storedClassifierVersion);
    }

    public static StoredStackKey fakeKey(String registryName) throws ReflectiveOperationException {
        StoredStackKey key = (StoredStackKey) unsafe().allocateInstance(StoredStackKey.class);
        String[] nameParts = registryName.split(":", 2);
        setField(key, "displayStack", null);
        setField(key, "hashCode", registryName.hashCode());
        setField(key, "registryName", registryName);
        setField(key, "registryNamespace", nameParts.length > 1 ? nameParts[0] : "test");
        setField(key, "registryPath", nameParts.length > 1 ? nameParts[1] : registryName);
        return key;
    }

    public static StoredStackKey fakeKey(String registryName, ItemStack displayStack) throws ReflectiveOperationException {
        StoredStackKey key = fakeKey(registryName);
        ItemStack normalizedStack = displayStack.copyWithCount(1);
        setField(key, "displayStack", normalizedStack);
        setField(key, "hashCode", ItemStack.hashItemAndComponents(normalizedStack));
        return key;
    }

    public static CompoundTag invalidStackTag(String itemId) {
        CompoundTag stackTag = new CompoundTag();
        stackTag.putString("id", itemId);
        stackTag.putInt("count", 1);
        return stackTag;
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
