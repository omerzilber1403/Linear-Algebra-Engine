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
 * Unit tests for SharedVector locking discipline.
 * These tests verify that OUR usage of locks is correct:
 * - Operations never hang (no deadlocks)
 * - Locks are always released even when exceptions are thrown
 */
public class SharedVectorLockDisciplineTest {

    private static final long OPERATION_TIMEOUT_MS = 1000;
    private static final long LOCK_CHECK_TIMEOUT_MS = 500;

    /**
     * Helper method to run a task with a timeout and verify it completes successfully.
     * 
     * @param task The task to run
     * @throws Exception if the task fails or times out
     */
    private void assertCompletesWithinTimeout(Runnable task) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> future = executor.submit(task);
            future.get(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } finally {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    /**
     * Helper method to verify that a vector can be write-locked (i.e., no leftover locks).
     * This is used to verify that locks are properly released after exceptions.
     * 
     * @param vector The vector to test
     */
    private void assertVectorCanBeWriteLocked(SharedVector vector) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> lockTask = executor.submit(() -> {
                vector.writeLock();
                try {
                    // Successfully acquired write lock
                } finally {
                    vector.writeUnlock();
                }
            });
            
            // Should complete without timeout
            assertDoesNotThrow(() -> {
                lockTask.get(LOCK_CHECK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            }, "Vector should be lockable after exception (no leftover locks)");
            
        } finally {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    // ==================== ADD OPERATION TESTS ====================

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("add() with length mismatch releases locks on both vectors")
    void testAddLengthMismatchReleasesLocks() throws Exception {
        // Arrange: Create vectors with different lengths
        SharedVector v1 = new SharedVector(new double[]{1.0, 2.0, 3.0}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{4.0, 5.0}, VectorOrientation.ROW_MAJOR);
        
        // Act: Try to add vectors (should throw IllegalArgumentException)
        assertThrows(IllegalArgumentException.class, () -> {
            v1.add(v2);
        }, "add() should throw IllegalArgumentException for length mismatch");
        
        // Assert: Both vectors should be lockable (locks were properly released)
        assertVectorCanBeWriteLocked(v1);
        assertVectorCanBeWriteLocked(v2);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("add() with orientation mismatch releases locks on both vectors")
    void testAddOrientationMismatchReleasesLocks() throws Exception {
        // Arrange: Create vectors with different orientations
        SharedVector v1 = new SharedVector(new double[]{1.0, 2.0, 3.0}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{4.0, 5.0, 6.0}, VectorOrientation.COLUMN_MAJOR);
        
        // Act: Try to add vectors (should throw IllegalArgumentException)
        assertThrows(IllegalArgumentException.class, () -> {
            v1.add(v2);
        }, "add() should throw IllegalArgumentException for orientation mismatch");
        
        // Assert: Both vectors should be lockable (locks were properly released)
        assertVectorCanBeWriteLocked(v1);
        assertVectorCanBeWriteLocked(v2);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Concurrent add operations with lock ordering (no deadlock)")
    void testAddConcurrentNoDeadlock() throws Exception {
        // Arrange: Create two vectors
        SharedVector v1 = new SharedVector(new double[]{1.0, 2.0, 3.0}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{4.0, 5.0, 6.0}, VectorOrientation.ROW_MAJOR);
        
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        try {
            // Act: Run v1.add(v2) and v2.add(v1) concurrently
            Future<?> task1 = executor.submit(() -> {
                v1.add(v2);
            });
            
            Future<?> task2 = executor.submit(() -> {
                v2.add(v1);
            });
            
            // Assert: Both operations should complete without deadlock
            assertDoesNotThrow(() -> {
                task1.get(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                task2.get(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            }, "Concurrent add operations should complete without deadlock");
            
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS));
        }
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("add() completes successfully within timeout")
    void testAddSuccessCompletes() throws Exception {
        // Arrange: Create compatible vectors
        SharedVector v1 = new SharedVector(new double[]{1.0, 2.0, 3.0}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{4.0, 5.0, 6.0}, VectorOrientation.ROW_MAJOR);
        
        // Act & Assert: Operation should complete without hanging
        assertCompletesWithinTimeout(() -> {
            v1.add(v2);
        });
        
        // Verify result
        assertEquals(5.0, v1.get(0), 1e-9);
        assertEquals(7.0, v1.get(1), 1e-9);
        assertEquals(9.0, v1.get(2), 1e-9);
    }

    // ==================== DOT OPERATION TESTS ====================

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("dot() with same orientation releases locks on both vectors")
    void testDotSameOrientationReleasesLocks() throws Exception {
        // Arrange: Create vectors with same orientation
        SharedVector v1 = new SharedVector(new double[]{1.0, 2.0, 3.0}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{4.0, 5.0, 6.0}, VectorOrientation.ROW_MAJOR);
        
        // Act: Try to compute dot product (should throw IllegalArgumentException)
        assertThrows(IllegalArgumentException.class, () -> {
            v1.dot(v2);
        }, "dot() should throw IllegalArgumentException for same orientation");
        
        // Assert: Both vectors should be lockable (locks were properly released)
        assertVectorCanBeWriteLocked(v1);
        assertVectorCanBeWriteLocked(v2);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("dot() with length mismatch releases locks on both vectors")
    void testDotLengthMismatchReleasesLocks() throws Exception {
        // Arrange: Create vectors with different lengths and orientations
        SharedVector v1 = new SharedVector(new double[]{1.0, 2.0, 3.0}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{4.0, 5.0}, VectorOrientation.COLUMN_MAJOR);
        
        // Act: Try to compute dot product (should throw IllegalArgumentException)
        assertThrows(IllegalArgumentException.class, () -> {
            v1.dot(v2);
        }, "dot() should throw IllegalArgumentException for length mismatch");
        
        // Assert: Both vectors should be lockable (locks were properly released)
        assertVectorCanBeWriteLocked(v1);
        assertVectorCanBeWriteLocked(v2);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("dot() completes successfully within timeout")
    void testDotSuccessCompletes() throws Exception {
        // Arrange: Create compatible vectors
        SharedVector v1 = new SharedVector(new double[]{1.0, 2.0, 3.0}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{4.0, 5.0, 6.0}, VectorOrientation.COLUMN_MAJOR);
        
        // Act & Assert: Operation should complete without hanging
        final double[] result = new double[1];
        assertCompletesWithinTimeout(() -> {
            result[0] = v1.dot(v2);
        });
        
        // Verify result: 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
        assertEquals(32.0, result[0], 1e-9);
    }

    // ==================== VECMATMUL OPERATION TESTS ====================

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("vecMatMul() with wrong vector orientation releases lock")
    void testVecMatMulWrongVectorOrientationReleasesLock() throws Exception {
        // Arrange: Create column vector (should be row vector)
        SharedVector vector = new SharedVector(new double[]{1.0, 2.0, 3.0}, VectorOrientation.COLUMN_MAJOR);
        SharedMatrix matrix = new SharedMatrix(new double[][]{
            {1.0, 2.0, 3.0},
            {4.0, 5.0, 6.0}
        });
        
        // Act: Try vecMatMul (should throw IllegalArgumentException)
        assertThrows(IllegalArgumentException.class, () -> {
            vector.vecMatMul(matrix);
        }, "vecMatMul() should throw IllegalArgumentException for non-row vector");
        
        // Assert: Vector should be lockable (lock was properly released)
        assertVectorCanBeWriteLocked(vector);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("vecMatMul() with wrong matrix orientation releases lock")
    void testVecMatMulWrongMatrixOrientationReleasesLock() throws Exception {
        // Arrange: Create row vector but row-major matrix
        SharedVector vector = new SharedVector(new double[]{1.0, 2.0, 3.0}, VectorOrientation.ROW_MAJOR);
        
        // Create row-major matrix (constructor creates row-major by default)
        // Then manually transpose the vectors to make it inconsistent
        SharedMatrix matrix = new SharedMatrix(new double[][]{
            {1.0, 4.0},
            {2.0, 5.0},
            {3.0, 6.0}
        });
        // Transpose all vectors to make it ROW_MAJOR orientation
        for (int i = 0; i < matrix.length(); i++) {
            matrix.get(i).transpose();
        }
        
        // Act: Try vecMatMul (should throw IllegalArgumentException)
        assertThrows(IllegalArgumentException.class, () -> {
            vector.vecMatMul(matrix);
        }, "vecMatMul() should throw IllegalArgumentException for non-column-major matrix");
        
        // Assert: Vector should be lockable (lock was properly released)
        assertVectorCanBeWriteLocked(vector);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("vecMatMul() with dimension mismatch releases lock")
    void testVecMatMulDimensionMismatchReleasesLock() throws Exception {
        // Arrange: Create vector and matrix with incompatible dimensions
        SharedVector vector = new SharedVector(new double[]{1.0, 2.0}, VectorOrientation.ROW_MAJOR);
        SharedMatrix matrix = new SharedMatrix(new double[][]{
            {1.0, 2.0, 3.0},
            {4.0, 5.0, 6.0}
        });
        
        // Act: Try vecMatMul (should throw IllegalArgumentException)
        assertThrows(IllegalArgumentException.class, () -> {
            vector.vecMatMul(matrix);
        }, "vecMatMul() should throw IllegalArgumentException for dimension mismatch");
        
        // Assert: Vector should be lockable (lock was properly released)
        assertVectorCanBeWriteLocked(vector);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("vecMatMul() with empty matrix releases lock")
    void testVecMatMulEmptyMatrixReleasesLock() throws Exception {
        // Arrange: Create vector and empty matrix
        SharedVector vector = new SharedVector(new double[]{1.0, 2.0, 3.0}, VectorOrientation.ROW_MAJOR);
        SharedMatrix matrix = new SharedMatrix();
        
        // Act: Try vecMatMul (should throw IllegalArgumentException)
        assertThrows(IllegalArgumentException.class, () -> {
            vector.vecMatMul(matrix);
        }, "vecMatMul() should throw IllegalArgumentException for empty matrix");
        
        // Assert: Vector should be lockable (lock was properly released)
        assertVectorCanBeWriteLocked(vector);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("vecMatMul() with null matrix releases lock")
    void testVecMatMulNullMatrixReleasesLock() throws Exception {
        // Arrange: Create a valid vector
        SharedVector vector = new SharedVector(new double[]{1.0, 2.0, 3.0}, VectorOrientation.ROW_MAJOR);
        
        // Act: Try vecMatMul with null (should throw NullPointerException)
        assertThrows(NullPointerException.class, () -> {
            vector.vecMatMul(null);
        }, "vecMatMul() should throw NullPointerException for null matrix");
        
        // Assert: Vector should be lockable (lock was properly released)
        assertVectorCanBeWriteLocked(vector);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("vecMatMul() completes successfully within timeout")
    void testVecMatMulSuccessCompletes() throws Exception {
        // Arrange: Create compatible vector and matrix
        // Vector: [1, 2, 3] (row)
        // Matrix: [[1, 4],
        //          [2, 5],
        //          [3, 6]] (column-major)
        // Result: [1*1 + 2*2 + 3*3, 1*4 + 2*5 + 3*6] = [14, 32]
        SharedVector vector = new SharedVector(new double[]{1.0, 2.0, 3.0}, VectorOrientation.ROW_MAJOR);
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(new double[][]{
            {1.0, 4.0},
            {2.0, 5.0},
            {3.0, 6.0}
        });
        
        // Act & Assert: Operation should complete without hanging
        assertCompletesWithinTimeout(() -> {
            vector.vecMatMul(matrix);
        });
        
        // Verify result
        assertEquals(2, vector.length());
        assertEquals(14.0, vector.get(0), 1e-9);
        assertEquals(32.0, vector.get(1), 1e-9);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Concurrent vecMatMul operations complete without deadlock")
    void testVecMatMulConcurrentNoDeadlock() throws Exception {
        // Arrange: Create multiple vectors and matrices
        SharedVector v1 = new SharedVector(new double[]{1.0, 2.0}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{3.0, 4.0}, VectorOrientation.ROW_MAJOR);
        SharedMatrix m1 = new SharedMatrix();
        m1.loadColumnMajor(new double[][]{{1.0, 2.0}, {3.0, 4.0}});
        SharedMatrix m2 = new SharedMatrix();
        m2.loadColumnMajor(new double[][]{{5.0, 6.0}, {7.0, 8.0}});
        
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        try {
            // Act: Run multiple vecMatMul operations concurrently
            Future<?> task1 = executor.submit(() -> {
                v1.vecMatMul(m1);
            });
            
            Future<?> task2 = executor.submit(() -> {
                v2.vecMatMul(m2);
            });
            
            // Assert: Both operations should complete without deadlock
            assertDoesNotThrow(() -> {
                task1.get(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                task2.get(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            }, "Concurrent vecMatMul operations should complete without deadlock");
            
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS));
        }
    }
}
