package com.agguy.infiniteinventory.database.tests;

import com.agguy.infiniteinventory.database.StoredItemDatabase;
import com.agguy.infiniteinventory.database.StoredStackKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 公共数据库并发存取测试。
 *
 * <p>模拟多玩家同时存取同一数据库，验证同步机制下的数据一致性。</p>
 */
class PublicDatabaseConcurrencyTest {

    @Test
    void concurrentStoreShouldNotLoseItems() throws Exception {
        StoredItemDatabase database = new StoredItemDatabase();
        int threadCount = 8;
        int storesPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int thread = 0; thread < threadCount; thread++) {
            tasks.add(() -> {
                for (int i = 0; i < storesPerThread; i++) {
                    database.store(new ItemStack(Items.STONE, 1));
                }
                return null;
            });
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> future : futures) {
            future.get();
        }
        executor.shutdown();

        StoredStackKey stoneKey = StoredStackKey.of(new ItemStack(Items.STONE));
        assertEquals((long) threadCount * storesPerThread, database.getAmount(stoneKey));
    }

    @Test
    void concurrentStoreAndExtractShouldPreserveTotalAmount() throws Exception {
        StoredItemDatabase database = new StoredItemDatabase();
        // 预先存入足够的物品
        database.store(new ItemStack(Items.DIAMOND, 1000));

        int threadCount = 8;
        int operationsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        StoredStackKey diamondKey = StoredStackKey.of(new ItemStack(Items.DIAMOND));

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int thread = 0; thread < threadCount; thread++) {
            final boolean isStoreThread = thread % 2 == 0;
            tasks.add(() -> {
                for (int i = 0; i < operationsPerThread; i++) {
                    if (isStoreThread) {
                        database.store(new ItemStack(Items.DIAMOND, 1));
                    } else {
                        database.extract(diamondKey, 1);
                    }
                }
                return null;
            });
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> future : futures) {
            future.get();
        }
        executor.shutdown();

        long finalAmount = database.getAmount(diamondKey);
        // 4 个存线程各存 50，4 个取线程各取 50，净变化为 0，最终应为 1000
        assertEquals(1000L, finalAmount);
    }

    @Test
    void concurrentToggleStarShouldNotThrow() throws Exception {
        StoredItemDatabase database = new StoredItemDatabase();
        database.store(new ItemStack(Items.IRON_SWORD, 1));
        StoredStackKey key = StoredStackKey.of(new ItemStack(Items.IRON_SWORD));

        int threadCount = 8;
        int togglesPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int thread = 0; thread < threadCount; thread++) {
            tasks.add(() -> {
                for (int i = 0; i < togglesPerThread; i++) {
                    database.toggleStar(key);
                }
                return null;
            });
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> future : futures) {
            future.get();
        }
        executor.shutdown();

        // 最终收藏状态取决于总切换次数的奇偶性，但不应抛出异常
        assertTrue(database.revision() > 0);
    }

    @Test
    void concurrentMixedOperationsShouldBeConsistent() throws Exception {
        StoredItemDatabase database = new StoredItemDatabase();
        int threadCount = 4;
        int operationsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int thread = 0; thread < threadCount; thread++) {
            final int threadIndex = thread;
            tasks.add(() -> {
                for (int i = 0; i < operationsPerThread; i++) {
                    switch (threadIndex % 4) {
                        case 0 -> database.store(new ItemStack(Items.STONE, 2));
                        case 1 -> {
                            StoredStackKey key = StoredStackKey.of(new ItemStack(Items.STONE));
                            database.extract(key, 1);
                        }
                        case 2 -> database.setNote(
                                StoredStackKey.of(new ItemStack(Items.STONE)),
                                "note-" + threadIndex + "-" + i
                        );
                        case 3 -> database.toggleStar(StoredStackKey.of(new ItemStack(Items.STONE)));
                    }
                }
                return null;
            });
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> future : futures) {
            future.get();
        }
        executor.shutdown();

        // thread0: store(STONE, 2) × 50 = +100; thread1: extract × 50 = -50; thread2/3: 只改元数据
        // 净增加 = +100 - 50 = +50
        StoredStackKey stoneKey = StoredStackKey.of(new ItemStack(Items.STONE));
        assertEquals(50L, database.getAmount(stoneKey));
    }
}
