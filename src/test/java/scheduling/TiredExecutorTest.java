package scheduling;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit and integration tests for TiredExecutor class.
 * Tests focus on our implemented logic: constructor, submit, submitAll, shutdown, and getWorkerReport.
 */
public class TiredExecutorTest {

    private static final long TEST_TIMEOUT_MS = 2000;

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @DisplayName("TE1: Constructor rejects non-positive thread counts")
    void testConstructorRejectsNonPositiveThreads() {
        // Test zero threads
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> {
            new TiredExecutor(0);
        }, "TiredExecutor(0) should throw IllegalArgumentException");

        assertTrue(ex1.getMessage().toLowerCase().contains("non positive"),
                "Exception message should contain 'Non positive', got: " + ex1.getMessage());

        // Test negative threads
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> {
            new TiredExecutor(-1);
        }, "TiredExecutor(-1) should throw IllegalArgumentException");

        assertTrue(ex2.getMessage().toLowerCase().contains("non positive"),
                "Exception message should contain 'Non positive', got: " + ex2.getMessage());
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @DisplayName("TE2: submit(null) throws IllegalArgumentException")
    void testSubmitNullThrowsException() throws InterruptedException {
        // Arrange
        TiredExecutor executor = new TiredExecutor(2);

        try {
            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
                executor.submit(null);
            }, "submit(null) should throw IllegalArgumentException");

            assertTrue(ex.getMessage().toLowerCase().contains("task is null") || 
                       ex.getMessage().toLowerCase().contains("null"),
                    "Exception message should contain 'task is null', got: " + ex.getMessage());

        } finally {
            // Cleanup
            executor.shutdown();
        }
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @DisplayName("TE3: submit executes task exactly once")
    void testSubmitExecutesTaskOnce() throws InterruptedException {
        // Arrange
        TiredExecutor executor = new TiredExecutor(2);
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        Runnable task = () -> {
            counter.incrementAndGet();
            latch.countDown();
        };

        try {
            // Act
            executor.submit(task);

            // Assert: Wait for task completion with timeout
            boolean completed = latch.await(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            assertTrue(completed, "Task should complete within timeout");
            assertEquals(1, counter.get(), "Task should be executed exactly once");

        } finally {
            // Cleanup
            executor.shutdown();
        }
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("TE4: submit many tasks - all run exactly once, no deadlock")
    void testSubmitManyTasksNoDuplicatesNoDeadlock() throws InterruptedException {
        // Arrange
        int numThreads = 3;
        int numTasks = 100;
        TiredExecutor executor = new TiredExecutor(numThreads);
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(numTasks);

        try {
            // Act: Submit many tasks
            for (int i = 0; i < numTasks; i++) {
                executor.submit(() -> {
                    counter.incrementAndGet();
                    latch.countDown();
                });
            }

            // Assert: All tasks complete
            boolean completed = latch.await(3000, TimeUnit.MILLISECONDS);
            assertTrue(completed, "All tasks should complete within timeout");
            assertEquals(numTasks, counter.get(), "All tasks should execute exactly once");

        } finally {
            // Cleanup
            executor.shutdown();
        }
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("TE5: submitAll blocks until all tasks complete")
    void testSubmitAllBlocksUntilCompletion() throws Exception {
        // Arrange
        int numTasks = 10;
        TiredExecutor executor = new TiredExecutor(3);
        CountDownLatch startLatch = new CountDownLatch(1); // Controls when tasks start
        CountDownLatch doneLatch = new CountDownLatch(numTasks); // Tracks task completion

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < numTasks; i++) {
            tasks.add(() -> {
                try {
                    startLatch.await(); // Wait for signal to start
                    doneLatch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        ExecutorService testExecutor = Executors.newSingleThreadExecutor();

        try {
            // Act: Run submitAll in a separate thread
            Future<?> submitAllFuture = testExecutor.submit(() -> {
                executor.submitAll(tasks);
            });

            // Assert: submitAll should NOT complete before we release startLatch
            assertThrows(TimeoutException.class, () -> {
                submitAllFuture.get(500, TimeUnit.MILLISECONDS);
            }, "submitAll should block until tasks complete");

            // Release tasks to start
            startLatch.countDown();

            // Wait for all tasks to finish
            boolean tasksCompleted = doneLatch.await(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            assertTrue(tasksCompleted, "All tasks should complete after start signal");

            // Now submitAll should complete
            assertDoesNotThrow(() -> {
                submitAllFuture.get(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            }, "submitAll should complete after all tasks finish");

        } finally {
            // Cleanup
            testExecutor.shutdown();
            testExecutor.awaitTermination(1, TimeUnit.SECONDS);
            executor.shutdown();
        }
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @DisplayName("TE6: submitAll(null) throws IllegalArgumentException")
    void testSubmitAllNullThrowsException() throws InterruptedException {
        // Arrange
        TiredExecutor executor = new TiredExecutor(2);

        try {
            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
                executor.submitAll(null);
            }, "submitAll(null) should throw IllegalArgumentException");

            assertTrue(ex.getMessage().toLowerCase().contains("tasks is null") || 
                       ex.getMessage().toLowerCase().contains("null"),
                    "Exception message should contain 'tasks is null', got: " + ex.getMessage());

        } finally {
            // Cleanup
            executor.shutdown();
        }
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("TE7: Exception in task does NOT deadlock submitAll")
    void testExceptionInTaskDoesNotDeadlockSubmitAll() throws InterruptedException {
        // Arrange
        TiredExecutor executor = new TiredExecutor(3);
        AtomicInteger successCounter = new AtomicInteger(0);
        int numTasks = 10;

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < numTasks; i++) {
            final int taskId = i;
            tasks.add(() -> {
                if (taskId == 5) {
                    // One task throws an exception
                    throw new RuntimeException("Deliberate exception in task " + taskId);
                }
                successCounter.incrementAndGet();
            });
        }

        try {
            // Act: submitAll should complete even if a task throws
            // The key test is that it doesn't hang forever
            assertDoesNotThrow(() -> {
                executor.submitAll(tasks);
            }, "submitAll should complete even when tasks throw exceptions");

            // Assert: Other tasks should have completed
            assertEquals(numTasks - 1, successCounter.get(), 
                    "All non-throwing tasks should complete");

        } finally {
            // Cleanup
            executor.shutdown();
        }
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    @DisplayName("TE8: shutdown completes without deadlock and joins workers")
    void testShutdownCompletesAndJoinsWorkers() throws InterruptedException {
        // Arrange
        TiredExecutor executor = new TiredExecutor(3);
        CountDownLatch latch = new CountDownLatch(5);

        // Submit a few tasks
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                latch.countDown();
            });
        }

        // Wait for tasks to complete
        boolean tasksCompleted = latch.await(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(tasksCompleted, "Tasks should complete before shutdown");

        // Act & Assert: shutdown should complete within timeout
        long startTime = System.currentTimeMillis();
        assertDoesNotThrow(() -> {
            executor.shutdown();
        }, "shutdown should complete without throwing exceptions");
        long duration = System.currentTimeMillis() - startTime;

        assertTrue(duration < 2000, "shutdown should complete within reasonable time");

        // getWorkerReport should still work after shutdown
        String report = executor.getWorkerReport();
        assertNotNull(report, "getWorkerReport should return non-null after shutdown");
        assertFalse(report.isEmpty(), "getWorkerReport should return non-empty string");
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @DisplayName("TE9: getWorkerReport returns valid report with expected fields")
    void testGetWorkerReportBasicSanity() throws InterruptedException {
        // Arrange
        TiredExecutor executor = new TiredExecutor(3);

        try {
            // Act: Get report before tasks
            String reportBefore = executor.getWorkerReport();

            // Assert: Report should contain expected fields
            assertNotNull(reportBefore, "Report should not be null");
            assertTrue(reportBefore.contains("Worker"), "Report should contain 'Worker'");
            assertTrue(reportBefore.contains("busy="), "Report should contain 'busy='");
            assertTrue(reportBefore.contains("used="), "Report should contain 'used='");
            assertTrue(reportBefore.contains("idle="), "Report should contain 'idle='");
            assertTrue(reportBefore.contains("fatigue="), "Report should contain 'fatigue='");

            // Submit some tasks
            CountDownLatch latch = new CountDownLatch(5);
            for (int i = 0; i < 5; i++) {
                executor.submit(() -> {
                    // Do some work
                    long start = System.nanoTime();
                    while (System.nanoTime() - start < 1_000_000) {
                        // Busy loop for ~1ms
                    }
                    latch.countDown();
                });
            }

            // Wait for tasks to complete
            latch.await(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            // Get report after tasks
            String reportAfter = executor.getWorkerReport();

            // Assert: Report should still contain expected fields
            assertNotNull(reportAfter, "Report should not be null after tasks");
            assertTrue(reportAfter.contains("Worker"), "Report should contain 'Worker'");
            assertTrue(reportAfter.contains("busy="), "Report should contain 'busy='");
            assertTrue(reportAfter.contains("used="), "Report should contain 'used='");
            assertTrue(reportAfter.contains("idle="), "Report should contain 'idle='");
            assertTrue(reportAfter.contains("fatigue="), "Report should contain 'fatigue='");

        } finally {
            // Cleanup
            executor.shutdown();
        }
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    @DisplayName("submitAll with empty list completes immediately")
    void testSubmitAllWithEmptyList() throws InterruptedException {
        // Arrange
        TiredExecutor executor = new TiredExecutor(2);
        List<Runnable> emptyTasks = new ArrayList<>();

        try {
            // Act & Assert: Should complete immediately without blocking
            long startTime = System.currentTimeMillis();
            assertDoesNotThrow(() -> {
                executor.submitAll(emptyTasks);
            }, "submitAll with empty list should not throw");
            long duration = System.currentTimeMillis() - startTime;

            assertTrue(duration < 500, "submitAll with empty list should complete quickly");

        } finally {
            // Cleanup
            executor.shutdown();
        }
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Concurrent submit and submitAll do not interfere")
    void testConcurrentSubmitAndSubmitAll() throws Exception {
        // Arrange
        TiredExecutor executor = new TiredExecutor(4);
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch submitAllLatch = new CountDownLatch(10);
        CountDownLatch submitLatch = new CountDownLatch(5);

        List<Runnable> submitAllTasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            submitAllTasks.add(() -> {
                counter.incrementAndGet();
                submitAllLatch.countDown();
            });
        }

        ExecutorService testExecutor = Executors.newSingleThreadExecutor();

        try {
            // Act: Run submitAll in background
            Future<?> submitAllFuture = testExecutor.submit(() -> {
                executor.submitAll(submitAllTasks);
            });

            // Submit additional tasks concurrently
            for (int i = 0; i < 5; i++) {
                executor.submit(() -> {
                    counter.incrementAndGet();
                    submitLatch.countDown();
                });
            }

            // Assert: All tasks should complete
            boolean submitAllCompleted = submitAllLatch.await(3000, TimeUnit.MILLISECONDS);
            boolean submitCompleted = submitLatch.await(3000, TimeUnit.MILLISECONDS);

            assertTrue(submitAllCompleted, "submitAll tasks should complete");
            assertTrue(submitCompleted, "submit tasks should complete");

            submitAllFuture.get(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            assertEquals(15, counter.get(), "All 15 tasks should execute");

        } finally {
            // Cleanup
            testExecutor.shutdown();
            testExecutor.awaitTermination(1, TimeUnit.SECONDS);
            executor.shutdown();
        }
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    @DisplayName("Multiple submitAll calls complete correctly")
    void testMultipleSubmitAllCalls() throws InterruptedException {
        // Arrange
        TiredExecutor executor = new TiredExecutor(3);
        AtomicInteger counter = new AtomicInteger(0);

        try {
            // First submitAll
            List<Runnable> tasks1 = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                tasks1.add(() -> counter.incrementAndGet());
            }
            executor.submitAll(tasks1);
            assertEquals(5, counter.get(), "First submitAll should complete all tasks");

            // Second submitAll
            List<Runnable> tasks2 = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                tasks2.add(() -> counter.incrementAndGet());
            }
            executor.submitAll(tasks2);
            assertEquals(12, counter.get(), "Second submitAll should complete all tasks");

            // Third submitAll
            List<Runnable> tasks3 = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                tasks3.add(() -> counter.incrementAndGet());
            }
            executor.submitAll(tasks3);
            assertEquals(15, counter.get(), "Third submitAll should complete all tasks");

        } finally {
            // Cleanup
            executor.shutdown();
        }
    }
}
