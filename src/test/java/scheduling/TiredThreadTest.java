package scheduling;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TiredThread class.
 * Tests focus on our implemented logic: newTask, shutdown, run, and compareTo.
 */
public class TiredThreadTest {

    private static final long TEST_TIMEOUT_MS = 2000;

    /**
     * Helper: busy-wait for a bounded duration to consume CPU time.
     * Uses System.nanoTime() to avoid Thread.sleep.
     */
    private void busyWork(long durationMs) {
        long start = System.nanoTime();
        long targetNanos = durationMs * 1_000_000;
        while (System.nanoTime() - start < targetNanos) {
            // Busy loop
        }
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @DisplayName("TT1: newTask(null) throws IllegalArgumentException with correct message")
    void testNewTaskNullThrowsException() {
        // Arrange
        TiredThread worker = new TiredThread(0, 1.0);
        worker.start();

        try {
            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
                worker.newTask(null);
            }, "newTask(null) should throw IllegalArgumentException");

            assertTrue(ex.getMessage().contains("No task to execute"),
                    "Exception message should contain 'No task to execute', got: " + ex.getMessage());

        } finally {
            // Cleanup
            worker.shutdown();
            try {
                worker.join(TEST_TIMEOUT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @DisplayName("TT2: run executes a submitted task exactly once")
    void testRunExecutesTaskOnce() throws InterruptedException {
        // Arrange
        TiredThread worker = new TiredThread(0, 1.0);
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        Runnable task = () -> {
            counter.incrementAndGet();
            latch.countDown();
        };

        worker.start();

        try {
            // Act
            worker.newTask(task);

            // Assert: Wait for task completion with timeout
            boolean completed = latch.await(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            assertTrue(completed, "Task should complete within timeout");
            assertEquals(1, counter.get(), "Task should be executed exactly once");

        } finally {
            // Cleanup
            worker.shutdown();
            worker.join(TEST_TIMEOUT_MS);
        }
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @DisplayName("TT3: shutdown stops the worker and is idempotent")
    void testShutdownIsIdempotent() throws InterruptedException {
        // Arrange
        TiredThread worker = new TiredThread(0, 1.0);
        worker.start();

        // Act: Call shutdown twice
        worker.shutdown();
        worker.shutdown(); // Should not throw or cause issues

        // Assert: Worker should stop
        worker.join(TEST_TIMEOUT_MS);
        assertFalse(worker.isAlive(), "Worker thread should not be alive after shutdown and join");
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @DisplayName("TT4: newTask after shutdown is rejected with IllegalStateException")
    void testNewTaskAfterShutdownThrowsException() throws InterruptedException {
        // Arrange
        TiredThread worker = new TiredThread(0, 1.0);
        worker.start();

        // Act: Shutdown the worker
        worker.shutdown();
        worker.join(TEST_TIMEOUT_MS);

        // Assert: newTask should throw IllegalStateException
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            worker.newTask(() -> {});
        }, "newTask after shutdown should throw IllegalStateException");

        String message = ex.getMessage().toLowerCase();
        assertTrue(message.contains("shutting down") || message.contains("stopped") || message.contains("shutdown"),
                "Exception message should indicate worker is shutting down or stopped, got: " + ex.getMessage());
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    @DisplayName("TT5: Handoff capacity behavior - third task while busy throws IllegalStateException")
    void testHandoffCapacityBehavior() throws InterruptedException {
        // Arrange
        TiredThread worker = new TiredThread(0, 1.0);
        CountDownLatch task1Started = new CountDownLatch(1);
        CountDownLatch task1CanFinish = new CountDownLatch(1);
        CountDownLatch task2Started = new CountDownLatch(1);

        // Task 1: blocks until released
        Runnable task1 = () -> {
            task1Started.countDown();
            try {
                task1CanFinish.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        // Task 2: just signals it started
        Runnable task2 = () -> {
            task2Started.countDown();
        };

        // Task 3: should never run
        Runnable task3 = () -> {
            fail("Task 3 should never be executed");
        };

        worker.start();

        try {
            // Act: Submit first task and wait for it to start
            worker.newTask(task1);
            boolean started = task1Started.await(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            assertTrue(started, "Task 1 should start within timeout");

            // Submit second task while first is running - should succeed (goes into handoff queue)
            assertDoesNotThrow(() -> {
                worker.newTask(task2);
            }, "Second task should be accepted while first is running");

            // Give a tiny moment for task2 to be taken from handoff
            // (We can't use Thread.sleep for sync, but a small yield is acceptable for setup)
            Thread.yield();

            // Immediately submit third task - should fail (handoff is full or worker busy)
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
                worker.newTask(task3);
            }, "Third task should throw IllegalStateException when handoff is full");

            assertTrue(ex.getMessage().toLowerCase().contains("already assigned") || 
                       ex.getMessage().toLowerCase().contains("not ready"),
                    "Exception message should indicate worker already assigned, got: " + ex.getMessage());

            // Release task1 to complete
            task1CanFinish.countDown();

            // Wait for task2 to complete
            boolean task2Completed = task2Started.await(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            assertTrue(task2Completed, "Task 2 should complete after task 1 finishes");

        } finally {
            // Cleanup
            task1CanFinish.countDown(); // In case test failed before release
            worker.shutdown();
            worker.join(TEST_TIMEOUT_MS);
        }
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    @DisplayName("TT6: compareTo is consistent with getFatigue ordering")
    void testCompareToConsistentWithFatigue() throws InterruptedException {
        // Arrange: Create two workers with different fatigue factors
        TiredThread worker1 = new TiredThread(0, 0.8);  // Lower fatigue factor
        TiredThread worker2 = new TiredThread(1, 1.2);  // Higher fatigue factor

        CountDownLatch task1Done = new CountDownLatch(1);
        CountDownLatch task2Done = new CountDownLatch(1);

        // Give both workers some work to do (to accumulate timeUsed)
        Runnable task1 = () -> {
            busyWork(5); // 5ms of busy work
            task1Done.countDown();
        };

        Runnable task2 = () -> {
            busyWork(5); // 5ms of busy work
            task2Done.countDown();
        };

        worker1.start();
        worker2.start();

        try {
            // Act: Execute tasks on both workers
            worker1.newTask(task1);
            worker2.newTask(task2);

            // Wait for both tasks to complete
            boolean task1Completed = task1Done.await(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            boolean task2Completed = task2Done.await(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            assertTrue(task1Completed, "Task 1 should complete within timeout");
            assertTrue(task2Completed, "Task 2 should complete within timeout");

            // Give workers a moment to update their statistics
            Thread.yield();

            // Assert: compareTo should be consistent with getFatigue
            double fatigue1 = worker1.getFatigue();
            double fatigue2 = worker2.getFatigue();

            // Both workers should have some time used
            assertTrue(worker1.getTimeUsed() > 0, "Worker 1 should have used some time");
            assertTrue(worker2.getTimeUsed() > 0, "Worker 2 should have used some time");

            int compareResult = worker1.compareTo(worker2);
            int fatigueComparison = Double.compare(fatigue1, fatigue2);

            // compareTo should have the same sign as fatigue comparison
            if (fatigueComparison < 0) {
                assertTrue(compareResult < 0,
                        "worker1.compareTo(worker2) should be negative when worker1.getFatigue() < worker2.getFatigue()");
            } else if (fatigueComparison > 0) {
                assertTrue(compareResult > 0,
                        "worker1.compareTo(worker2) should be positive when worker1.getFatigue() > worker2.getFatigue()");
            } else {
                assertEquals(0, compareResult,
                        "worker1.compareTo(worker2) should be 0 when fatigues are equal");
            }

            // Test antisymmetry: sign(w1.compareTo(w2)) == -sign(w2.compareTo(w1))
            int reverseCompare = worker2.compareTo(worker1);
            assertEquals(Integer.signum(compareResult), -Integer.signum(reverseCompare),
                    "compareTo should be antisymmetric");

        } finally {
            // Cleanup
            worker1.shutdown();
            worker2.shutdown();
            worker1.join(TEST_TIMEOUT_MS);
            worker2.join(TEST_TIMEOUT_MS);
        }
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @DisplayName("Multiple tasks execute sequentially in submission order")
    void testMultipleTasksExecuteSequentially() throws InterruptedException {
        // Arrange
        TiredThread worker = new TiredThread(0, 1.0);
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch task1Done = new CountDownLatch(1);
        CountDownLatch task2Done = new CountDownLatch(1);
        CountDownLatch task3Done = new CountDownLatch(1);

        worker.start();

        try {
            // Act: Submit three tasks
            worker.newTask(() -> {
                counter.incrementAndGet();
                task1Done.countDown();
            });

            task1Done.await(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            worker.newTask(() -> {
                counter.incrementAndGet();
                task2Done.countDown();
            });

            task2Done.await(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            worker.newTask(() -> {
                counter.incrementAndGet();
                task3Done.countDown();
            });

            // Assert: All tasks complete
            boolean completed = task3Done.await(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            assertTrue(completed, "All tasks should complete within timeout");
            assertEquals(3, counter.get(), "All three tasks should execute exactly once");

        } finally {
            // Cleanup
            worker.shutdown();
            worker.join(TEST_TIMEOUT_MS);
        }
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @DisplayName("Worker remains functional after executing multiple tasks")
    void testWorkerRemainsIdleAfterTaskCompletion() throws InterruptedException {
        // Arrange
        TiredThread worker = new TiredThread(0, 1.0);
        CountDownLatch latch = new CountDownLatch(1);

        worker.start();

        try {
            // Act: Execute a task
            worker.newTask(() -> {
                latch.countDown();
            });

            latch.await(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            // Assert: Worker should not be busy after task completes
            // Give it a moment to transition back to idle
            Thread.yield();
            
            // We can verify by submitting another task successfully
            CountDownLatch latch2 = new CountDownLatch(1);
            assertDoesNotThrow(() -> {
                worker.newTask(() -> {
                    latch2.countDown();
                });
            }, "Worker should accept new tasks after completing previous ones");

            boolean completed = latch2.await(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            assertTrue(completed, "Second task should complete");

        } finally {
            // Cleanup
            worker.shutdown();
            worker.join(TEST_TIMEOUT_MS);
        }
    }
}
