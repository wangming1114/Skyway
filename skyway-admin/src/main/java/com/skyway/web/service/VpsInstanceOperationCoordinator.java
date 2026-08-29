package com.skyway.web.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Component;

/** Serializes mutating node operations for the same VPS inside one Skyway process. */
@Component
public class VpsInstanceOperationCoordinator {

    private final ConcurrentMap<Long, ReentrantLock> instanceLocks = new ConcurrentHashMap<>();

    public LockHandle lock(Long instanceId) {
        if (instanceId == null) {
            throw new IllegalArgumentException("实例ID不能为空");
        }
        ReentrantLock lock = instanceLocks.computeIfAbsent(instanceId, ignored -> new ReentrantLock(true));
        lock.lock();
        return new LockHandle(lock);
    }

    public static final class LockHandle implements AutoCloseable {
        private final ReentrantLock lock;
        private boolean closed;

        private LockHandle(ReentrantLock lock) {
            this.lock = lock;
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                lock.unlock();
            }
        }
    }
}
