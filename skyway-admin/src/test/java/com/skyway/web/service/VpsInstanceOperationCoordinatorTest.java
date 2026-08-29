package com.skyway.web.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class VpsInstanceOperationCoordinatorTest {

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @AfterEach
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void sameVpsOperationsAreSerialized() throws Exception {
        VpsInstanceOperationCoordinator coordinator = new VpsInstanceOperationCoordinator();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch acquired = new CountDownLatch(1);

        try (VpsInstanceOperationCoordinator.LockHandle ignored = coordinator.lock(1L)) {
            executor.submit(() -> {
                started.countDown();
                try (VpsInstanceOperationCoordinator.LockHandle second = coordinator.lock(1L)) {
                    acquired.countDown();
                }
            });
            assertTrue(started.await(1, TimeUnit.SECONDS));
            assertFalse(acquired.await(150, TimeUnit.MILLISECONDS));
        }

        assertTrue(acquired.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void differentVpsOperationsDoNotBlockEachOther() throws Exception {
        VpsInstanceOperationCoordinator coordinator = new VpsInstanceOperationCoordinator();
        CountDownLatch acquired = new CountDownLatch(1);

        try (VpsInstanceOperationCoordinator.LockHandle ignored = coordinator.lock(1L)) {
            executor.submit(() -> {
                try (VpsInstanceOperationCoordinator.LockHandle second = coordinator.lock(2L)) {
                    acquired.countDown();
                }
            });
            assertTrue(acquired.await(1, TimeUnit.SECONDS));
        }
    }
}
