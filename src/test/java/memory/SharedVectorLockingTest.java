package memory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SharedVector locking behavior.
 * These tests verify the correct interaction between read locks and write locks.
 * All tests are deterministic and use timeouts to avoid hanging.
 */
public class SharedVectorLockingTest {

    /**
     * Test that a read lock blocks a write lock.
     * 
     * Expected behavior:
     * 1. Main thread acquires readLock
     * 2. Background thread attempts to acquire writeLock (should block)
     * 3. Verify writeLock does NOT complete while readLock is held
     * 4. Main thread releases readLock
     * 5. Verify writeLock completes after readLock is released
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("readLock blocks writeLock until readLock is released")
    void testReadLockBlocksWriteLock() throws Exception {
        // Arrange: Create a SharedVector
        SharedVector vector = new SharedVector(new double[]{1.0, 2.0, 3.0}, VectorOrientation.ROW_MAJOR);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        try {
            // Act: Main thread acquires read lock
            vector.readLock();
            
            // Submit task that tries to acquire write lock
            Future<?> writeLockTask = executor.submit(() -> {
                vector.writeLock();
                try {
                    // Successfully acquired write lock
                } finally {
                    vector.writeUnlock();
                }
            });
            
            // Assert: Write lock should be blocked (timeout expected)
            assertThrows(TimeoutException.class, () -> {
                writeLockTask.get(200, TimeUnit.MILLISECONDS);
            }, "writeLock should be blocked while readLock is held");
            
            // Act: Release read lock
            vector.readUnlock();
            
            // Assert: Write lock should now complete
            assertDoesNotThrow(() -> {
                writeLockTask.get(1000, TimeUnit.MILLISECONDS);
            }, "writeLock should complete after readLock is released");
            
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
        }
    }

    /**
     * Test that a write lock blocks a read lock.
     * 
     * Expected behavior:
     * 1. Main thread acquires writeLock
     * 2. Background thread attempts to acquire readLock (should block)
     * 3. Verify readLock does NOT complete while writeLock is held
     * 4. Main thread releases writeLock
     * 5. Verify readLock completes after writeLock is released
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("writeLock blocks readLock until writeLock is released")
    void testWriteLockBlocksReadLock() throws Exception {
        // Arrange: Create a SharedVector
        SharedVector vector = new SharedVector(new double[]{4.0, 5.0, 6.0}, VectorOrientation.COLUMN_MAJOR);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        try {
            // Act: Main thread acquires write lock
            vector.writeLock();
            
            // Submit task that tries to acquire read lock
            Future<?> readLockTask = executor.submit(() -> {
                vector.readLock();
                try {
                    // Successfully acquired read lock
                } finally {
                    vector.readUnlock();
                }
            });
            
            // Assert: Read lock should be blocked (timeout expected)
            assertThrows(TimeoutException.class, () -> {
                readLockTask.get(200, TimeUnit.MILLISECONDS);
            }, "readLock should be blocked while writeLock is held");
            
            // Act: Release write lock
            vector.writeUnlock();
            
            // Assert: Read lock should now complete
            assertDoesNotThrow(() -> {
                readLockTask.get(1000, TimeUnit.MILLISECONDS);
            }, "readLock should complete after writeLock is released");
            
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
        }
    }

    /**
     * Test that multiple read locks do NOT block each other.
     * 
     * Expected behavior:
     * 1. Main thread acquires readLock
     * 2. Background thread attempts to acquire readLock (should NOT block)
     * 3. Verify second readLock completes immediately
     * 4. Both locks are released
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Multiple readLocks do NOT block each other")
    void testMultipleReadLocksDoNotBlock() throws Exception {
        // Arrange: Create a SharedVector
        SharedVector vector = new SharedVector(new double[]{7.0, 8.0, 9.0}, VectorOrientation.ROW_MAJOR);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        try {
            // Act: Main thread acquires read lock
            vector.readLock();
            
            // Submit task that tries to acquire another read lock
            Future<?> secondReadLockTask = executor.submit(() -> {
                vector.readLock();
                try {
                    // Successfully acquired read lock
                } finally {
                    vector.readUnlock();
                }
            });
            
            // Assert: Second read lock should complete immediately (no blocking)
            assertDoesNotThrow(() -> {
                secondReadLockTask.get(500, TimeUnit.MILLISECONDS);
            }, "Second readLock should NOT be blocked by first readLock");
            
            // Cleanup: Release first read lock
            vector.readUnlock();
            
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
        }
    }

    /**
     * Test that multiple threads can acquire read locks simultaneously.
     * 
     * This test launches multiple threads that all acquire read locks at the same time.
     * All threads should succeed without blocking each other.
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Multiple threads can hold read locks simultaneously")
    void testMultipleSimultaneousReadLocks() throws Exception {
        // Arrange: Create a SharedVector
        SharedVector vector = new SharedVector(new double[]{10.0, 11.0, 12.0}, VectorOrientation.COLUMN_MAJOR);
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        try {
            // Act: Submit 3 tasks that all acquire read locks
            Future<?> task1 = executor.submit(() -> {
                vector.readLock();
                try {
                    // Hold the lock briefly
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    vector.readUnlock();
                }
            });
            
            Future<?> task2 = executor.submit(() -> {
                vector.readLock();
                try {
                    // Hold the lock briefly
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    vector.readUnlock();
                }
            });
            
            Future<?> task3 = executor.submit(() -> {
                vector.readLock();
                try {
                    // Hold the lock briefly
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    vector.readUnlock();
                }
            });
            
            // Assert: All tasks should complete without timeout
            assertDoesNotThrow(() -> {
                task1.get(1000, TimeUnit.MILLISECONDS);
                task2.get(1000, TimeUnit.MILLISECONDS);
                task3.get(1000, TimeUnit.MILLISECONDS);
            }, "All read locks should be acquired simultaneously without blocking");
            
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS));
        }
    }

    /**
     * Test that write lock provides exclusive access.
     * 
     * This test verifies that when a write lock is held, no other thread
     * can acquire either a read lock or a write lock.
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("writeLock provides exclusive access (blocks both read and write)")
    void testWriteLockExclusiveAccess() throws Exception {
        // Arrange: Create a SharedVector
        SharedVector vector = new SharedVector(new double[]{13.0, 14.0}, VectorOrientation.ROW_MAJOR);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        try {
            // Act: Main thread acquires write lock
            vector.writeLock();
            
            // Submit task that tries to acquire read lock
            Future<?> readTask = executor.submit(() -> {
                vector.readLock();
                vector.readUnlock();
            });
            
            // Submit task that tries to acquire write lock
            Future<?> writeTask = executor.submit(() -> {
                vector.writeLock();
                vector.writeUnlock();
            });
            
            // Assert: Both tasks should be blocked
            assertThrows(TimeoutException.class, () -> {
                readTask.get(200, TimeUnit.MILLISECONDS);
            }, "readLock should be blocked by writeLock");
            
            assertThrows(TimeoutException.class, () -> {
                writeTask.get(200, TimeUnit.MILLISECONDS);
            }, "writeLock should be blocked by another writeLock");
            
            // Act: Release write lock
            vector.writeUnlock();
            
            // Assert: Both tasks should now complete
            assertDoesNotThrow(() -> {
                readTask.get(1000, TimeUnit.MILLISECONDS);
                writeTask.get(1000, TimeUnit.MILLISECONDS);
            }, "Both locks should be acquired after writeLock is released");
            
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS));
        }
    }
}
