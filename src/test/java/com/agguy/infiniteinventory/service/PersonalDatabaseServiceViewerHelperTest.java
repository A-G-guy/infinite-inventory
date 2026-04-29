package com.agguy.infiniteinventory.service;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PersonalDatabaseServiceViewerHelper} 公共数据库观众列表安全测试。
 */
class PersonalDatabaseServiceViewerHelperTest {

    @Test
    void publicViewersShouldUseCopyOnWriteArraySet() throws ReflectiveOperationException {
        Field publicViewersField = PersonalDatabaseServiceViewerHelper.class.getDeclaredField("PUBLIC_VIEWERS");
        publicViewersField.setAccessible(true);
        Set<?> publicViewers = (Set<?>) publicViewersField.get(null);

        assertTrue(publicViewers instanceof CopyOnWriteArraySet, "PUBLIC_VIEWERS 必须是 CopyOnWriteArraySet 以防止迭代期间并发修改");
    }

    @Test
    void onPlayerLogoutShouldRemoveFromPublicViewers() throws ReflectiveOperationException {
        Field publicViewersField = PersonalDatabaseServiceViewerHelper.class.getDeclaredField("PUBLIC_VIEWERS");
        publicViewersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<ServerPlayer> publicViewers = (Set<ServerPlayer>) publicViewersField.get(null);

        // 使用 null 作为占位符测试集合操作语义（实际运行中仅存储 ServerPlayer 实例）
        publicViewers.add(null);
        assertEquals(1, publicViewers.size());

        PersonalDatabaseServiceViewerHelper.onPlayerLogout(null);
        assertEquals(0, publicViewers.size());
    }

    @Test
    void concurrentRegisterAndUnregisterShouldNotLoseElements() throws ReflectiveOperationException, InterruptedException {
        Field publicViewersField = PersonalDatabaseServiceViewerHelper.class.getDeclaredField("PUBLIC_VIEWERS");
        publicViewersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Object> publicViewers = (Set<Object>) publicViewersField.get(null);
        publicViewers.clear();

        int threadCount = 8;
        int operationsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int threadIndex = t;
            new Thread(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        Object marker = threadIndex + "-" + i;
                        publicViewers.add(marker);
                        // 迭代期间删除不应抛出 ConcurrentModificationException
                        if (publicViewers.contains(marker)) {
                            publicViewers.remove(marker);
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue(latch.await(30L, TimeUnit.SECONDS));
        assertEquals(threadCount * operationsPerThread, successCount.get());
        assertTrue(publicViewers.isEmpty(), "并发操作后集合应为空");
    }
}
