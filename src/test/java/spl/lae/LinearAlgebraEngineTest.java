package spl.lae;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import parser.ComputationNode;
import parser.ComputationNodeType;
import parser.InputParser;

import java.io.File;
import java.text.ParseException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for LinearAlgebraEngine.
 * Tests our TODO logic and integration with TiredExecutor.
 */
public class LinearAlgebraEngineTest {

    private static final double DELTA = 1e-9;
    private LinearAlgebraEngine lae;

    @AfterEach
    void cleanup() throws InterruptedException {
        if (lae != null) {
            lae.shutdown();
        }
    }

    /**
     * Helper method to compare two matrices with tolerance.
     */
    private void assertMatrixEquals(double[][] expected, double[][] actual, String message) {
        assertNotNull(actual, message + " - actual matrix is null");
        assertEquals(expected.length, actual.length, message + " - row count mismatch");
        
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i].length, actual[i].length, 
                    message + " - column count mismatch at row " + i);
            for (int j = 0; j < expected[i].length; j++) {
                assertEquals(expected[i][j], actual[i][j], DELTA,
                        message + " - value mismatch at [" + i + "][" + j + "]");
            }
        }
    }

    /**
     * Helper to get resource file path.
     */
    private String getResourcePath(String filename) {
        return "src/test/resources/" + filename;
    }

    @Test
    @Timeout(value = 3, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("LAE1: ADD operation produces correct result")
    void testAddCorrectness() throws Exception {
        // Arrange
        lae = new LinearAlgebraEngine(2);
        InputParser parser = new InputParser();
        ComputationNode root = parser.parse(getResourcePath("add_test.json"));

        // Expected: [1+5, 2+6] = [6, 8]
        //           [3+7, 4+8]   [10, 12]
        double[][] expected = {
            {6.0, 8.0},
            {10.0, 12.0}
        };

        // Act
        lae.run(root);
        double[][] result = root.getMatrix();

        // Assert
        assertMatrixEquals(expected, result, "ADD operation result");
    }

    @Test
    @Timeout(value = 3, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("LAE2: MULTIPLY operation produces correct result")
    void testMultiplyCorrectness() throws Exception {
        // Arrange
        lae = new LinearAlgebraEngine(2);
        InputParser parser = new InputParser();
        ComputationNode root = parser.parse(getResourcePath("multiply_test.json"));

        // A (2x3) * B (3x2)
        // A = [[1, 2, 3],     B = [[1, 2],
        //      [4, 5, 6]]          [3, 4],
        //                          [5, 6]]
        // Result (2x2):
        // [1*1+2*3+3*5, 1*2+2*4+3*6] = [1+6+15, 2+8+18] = [22, 28]
        // [4*1+5*3+6*5, 4*2+5*4+6*6]   [4+15+30, 8+20+36] = [49, 64]
        double[][] expected = {
            {22.0, 28.0},
            {49.0, 64.0}
        };

        // Act
        lae.run(root);
        double[][] result = root.getMatrix();

        // Assert
        assertMatrixEquals(expected, result, "MULTIPLY operation result");
    }

    @Test
    @Timeout(value = 3, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("LAE3: NEGATE operation produces correct result")
    void testNegateCorrectness() throws Exception {
        // Arrange
        lae = new LinearAlgebraEngine(2);
        InputParser parser = new InputParser();
        ComputationNode root = parser.parse(getResourcePath("negate_test.json"));

        // Input:  [[1, -2],     Expected: [[-1, 2],
        //          [-3, 4]]                [3, -4]]
        double[][] expected = {
            {-1.0, 2.0},
            {3.0, -4.0}
        };

        // Act
        lae.run(root);
        double[][] result = root.getMatrix();

        // Assert
        assertMatrixEquals(expected, result, "NEGATE operation result");
    }

    @Test
    @Timeout(value = 3, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("LAE4: TRANSPOSE operation produces correct result")
    void testTransposeCorrectness() throws Exception {
        // Arrange
        lae = new LinearAlgebraEngine(2);
        InputParser parser = new InputParser();
        ComputationNode root = parser.parse(getResourcePath("transpose_test.json"));

        // Input (2x3):  [[1, 2, 3],
        //                [4, 5, 6]]
        // Expected (3x2): [[1, 4],
        //                  [2, 5],
        //                  [3, 6]]
        double[][] expected = {
            {1.0, 4.0},
            {2.0, 5.0},
            {3.0, 6.0}
        };

        // Act
        lae.run(root);
        double[][] result = root.getMatrix();

        // Assert
        assertMatrixEquals(expected, result, "TRANSPOSE operation result");
    }

    @Test
    @Timeout(value = 3, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("LAE5: ADD with mismatched dimensions throws IllegalArgumentException")
    void testAddMismatchedDimensions() {
        // Arrange
        lae = new LinearAlgebraEngine(2);
        
        // Create matrices with different dimensions
        ComputationNode matrix1 = new ComputationNode(new double[][]{
            {1.0, 2.0},
            {3.0, 4.0}
        });
        
        ComputationNode matrix2 = new ComputationNode(new double[][]{
            {5.0, 6.0, 7.0}  // Different column count
        });
        
        ComputationNode root = new ComputationNode(ComputationNodeType.ADD, 
                Arrays.asList(matrix1, matrix2));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            lae.run(root);
        }, "ADD with mismatched dimensions should throw IllegalArgumentException");
    }

    @Test
    @Timeout(value = 3, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("LAE6: MULTIPLY with mismatched dimensions throws IllegalArgumentException")
    void testMultiplyMismatchedDimensions() {
        // Arrange
        lae = new LinearAlgebraEngine(2);
        
        // Create matrices where A columns != B rows
        ComputationNode matrix1 = new ComputationNode(new double[][]{
            {1.0, 2.0, 3.0}  // 1x3 matrix
        });
        
        ComputationNode matrix2 = new ComputationNode(new double[][]{
            {4.0, 5.0}  // 1x2 matrix (can't multiply 1x3 * 1x2)
        });
        
        ComputationNode root = new ComputationNode(ComputationNodeType.MULTIPLY, 
                Arrays.asList(matrix1, matrix2));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            lae.run(root);
        }, "MULTIPLY with mismatched dimensions should throw IllegalArgumentException");
    }

    @Test
    @Timeout(value = 3, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("LAE7: No deadlock - operations complete within timeout")
    void testNoDeadlock() throws Exception {
        // Arrange
        lae = new LinearAlgebraEngine(3);
        InputParser parser = new InputParser();
        ComputationNode root = parser.parse(getResourcePath("multiply_test.json"));

        // Act & Assert: This should complete without hanging
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            lae.run(root);
        }, "LAE.run should complete without deadlock");

        assertNotNull(root.getMatrix(), "Result matrix should not be null");
    }

    @Test
    @Timeout(value = 3, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("LAE8: getWorkerReport returns valid report after computation")
    void testGetWorkerReportSanity() throws Exception {
        // Arrange
        lae = new LinearAlgebraEngine(3);
        InputParser parser = new InputParser();
        ComputationNode root = parser.parse(getResourcePath("add_test.json"));

        // Act
        lae.run(root);
        String report = lae.getWorkerReport();

        // Assert
        assertNotNull(report, "Worker report should not be null");
        assertFalse(report.isEmpty(), "Worker report should not be empty");
        assertTrue(report.contains("Worker"), "Report should contain 'Worker'");
        assertTrue(report.contains("busy="), "Report should contain 'busy='");
        assertTrue(report.contains("used="), "Report should contain 'used='");
        assertTrue(report.contains("idle="), "Report should contain 'idle='");
        assertTrue(report.contains("fatigue="), "Report should contain 'fatigue='");
    }

    @Test
    @Timeout(value = 3, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("LAE9: shutdown completes without hanging")
    void testShutdownDoesNotHang() throws Exception {
        // Arrange
        lae = new LinearAlgebraEngine(2);
        InputParser parser = new InputParser();
        ComputationNode root = parser.parse(getResourcePath("add_test.json"));
        lae.run(root);

        // Act & Assert: shutdown should complete within timeout
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            lae.shutdown();
            lae = null; // Prevent double shutdown in @AfterEach
        }, "shutdown should complete without hanging");
    }

    @Test
    @Timeout(value = 3, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("loadAndCompute with null node throws IllegalArgumentException")
    void testLoadAndComputeNullNode() {
        // Arrange
        lae = new LinearAlgebraEngine(2);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            lae.loadAndCompute(null);
        }, "loadAndCompute with null should throw IllegalArgumentException");
    }

    @Test
    @Timeout(value = 3, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("Complex computation tree executes correctly")
    void testComplexComputationTree() {
        // Arrange: (A + B) * C
        lae = new LinearAlgebraEngine(3);
        
        ComputationNode matrixA = new ComputationNode(new double[][]{
            {1.0, 2.0},
            {3.0, 4.0}
        });
        
        ComputationNode matrixB = new ComputationNode(new double[][]{
            {5.0, 6.0},
            {7.0, 8.0}
        });
        
        ComputationNode matrixC = new ComputationNode(new double[][]{
            {1.0, 0.0},
            {0.0, 1.0}
        });
        
        // A + B = [[6, 8], [10, 12]]
        ComputationNode addNode = new ComputationNode(ComputationNodeType.ADD,
                Arrays.asList(matrixA, matrixB));
        
        // (A + B) * C = [[6, 8], [10, 12]] * [[1, 0], [0, 1]]
        //             = [[6, 8], [10, 12]] (identity multiplication)
        ComputationNode root = new ComputationNode(ComputationNodeType.MULTIPLY,
                Arrays.asList(addNode, matrixC));
        
        double[][] expected = {
            {6.0, 8.0},
            {10.0, 12.0}
        };

        // Act
        lae.run(root);
        double[][] result = root.getMatrix();

        // Assert
        assertMatrixEquals(expected, result, "Complex computation result");
    }

    @Test
    @Timeout(value = 3, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("Multiple sequential operations complete correctly")
    void testMultipleSequentialOperations() throws Exception {
        // Arrange
        lae = new LinearAlgebraEngine(2);
        InputParser parser = new InputParser();

        // Test ADD
        ComputationNode addRoot = parser.parse(getResourcePath("add_test.json"));
        lae.run(addRoot);
        assertNotNull(addRoot.getMatrix(), "ADD result should not be null");

        // Test MULTIPLY
        ComputationNode multiplyRoot = parser.parse(getResourcePath("multiply_test.json"));
        lae.run(multiplyRoot);
        assertNotNull(multiplyRoot.getMatrix(), "MULTIPLY result should not be null");

        // Test NEGATE
        ComputationNode negateRoot = parser.parse(getResourcePath("negate_test.json"));
        lae.run(negateRoot);
        assertNotNull(negateRoot.getMatrix(), "NEGATE result should not be null");

        // All should complete without hanging
    }

    @Test
    @Timeout(value = 3, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("Single-threaded executor works correctly")
    void testSingleThreadedExecution() throws Exception {
        // Arrange: Test with only 1 thread
        lae = new LinearAlgebraEngine(1);
        InputParser parser = new InputParser();
        ComputationNode root = parser.parse(getResourcePath("multiply_test.json"));

        double[][] expected = {
            {22.0, 28.0},
            {49.0, 64.0}
        };

        // Act
        lae.run(root);
        double[][] result = root.getMatrix();

        // Assert
        assertMatrixEquals(expected, result, "Single-threaded execution result");
    }

    @Test
    @Timeout(value = 3, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("Many threads execute correctly")
    void testManyThreadsExecution() throws Exception {
        // Arrange: Test with many threads
        lae = new LinearAlgebraEngine(10);
        InputParser parser = new InputParser();
        ComputationNode root = parser.parse(getResourcePath("add_test.json"));

        double[][] expected = {
            {6.0, 8.0},
            {10.0, 12.0}
        };

        // Act
        lae.run(root);
        double[][] result = root.getMatrix();

        // Assert
        assertMatrixEquals(expected, result, "Many-threaded execution result");
    }

    // ==================== COMPLEX COMPUTATION TREE TEST CASES ====================

    @Test
    @Timeout(value = 3, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("CASE 1: TRANSPOSE( ADD(A, NEGATE(B)) )")
    void testComplexCase1_TransposeAddNegate() {
        // Arrange: TRANSPOSE( ADD(A, NEGATE(B)) )
        lae = new LinearAlgebraEngine(3);
        
        // A (2x3) = [[1,2,3],[4,5,6]]
        ComputationNode matrixA = new ComputationNode(new double[][]{
            {1.0, 2.0, 3.0},
            {4.0, 5.0, 6.0}
        });
        
        // B (2x3) = [[6,5,4],[3,2,1]]
        ComputationNode matrixB = new ComputationNode(new double[][]{
            {6.0, 5.0, 4.0},
            {3.0, 2.0, 1.0}
        });
        
        // NEGATE(B) = [[-6,-5,-4],[-3,-2,-1]]
        ComputationNode negateB = new ComputationNode(ComputationNodeType.NEGATE,
                Arrays.asList(matrixB));
        
        // ADD(A, NEGATE(B)) = [[-5,-3,-1],[1,3,5]]
        ComputationNode addNode = new ComputationNode(ComputationNodeType.ADD,
                Arrays.asList(matrixA, negateB));
        
        // TRANSPOSE = [[-5,1],[-3,3],[-1,5]]
        ComputationNode root = new ComputationNode(ComputationNodeType.TRANSPOSE,
                Arrays.asList(addNode));
        
        double[][] expected = {
            {-5.0, 1.0},
            {-3.0, 3.0},
            {-1.0, 5.0}
        };

        // Act
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            lae.run(root);
        }, "CASE 1 should complete without deadlock");
        
        double[][] result = root.getMatrix();

        // Assert
        assertMatrixEquals(expected, result, "CASE 1: TRANSPOSE(ADD(A, NEGATE(B)))");
    }

    @Test
    @Timeout(value = 3, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("CASE 2: ADD( MUL(A,B), MUL(C,D) )")
    void testComplexCase2_AddTwoMultiplications() {
        // Arrange: ADD( MUL(A,B), MUL(C,D) )
        lae = new LinearAlgebraEngine(4);
        
        // A=[[1,2],[3,4]]
        ComputationNode matrixA = new ComputationNode(new double[][]{
            {1.0, 2.0},
            {3.0, 4.0}
        });
        
        // B=[[5,6],[7,8]]
        ComputationNode matrixB = new ComputationNode(new double[][]{
            {5.0, 6.0},
            {7.0, 8.0}
        });
        
        // C=[[2,0],[1,2]]
        ComputationNode matrixC = new ComputationNode(new double[][]{
            {2.0, 0.0},
            {1.0, 2.0}
        });
        
        // D=[[3,1],[4,2]]
        ComputationNode matrixD = new ComputationNode(new double[][]{
            {3.0, 1.0},
            {4.0, 2.0}
        });
        
        // MUL(A,B) = [[19,22],[43,50]]
        ComputationNode mulAB = new ComputationNode(ComputationNodeType.MULTIPLY,
                Arrays.asList(matrixA, matrixB));
        
        // MUL(C,D) = [[6,2],[11,5]]
        ComputationNode mulCD = new ComputationNode(ComputationNodeType.MULTIPLY,
                Arrays.asList(matrixC, matrixD));
        
        // ADD = [[25,24],[54,55]]
        ComputationNode root = new ComputationNode(ComputationNodeType.ADD,
                Arrays.asList(mulAB, mulCD));
        
        double[][] expected = {
            {25.0, 24.0},
            {54.0, 55.0}
        };

        // Act
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            lae.run(root);
        }, "CASE 2 should complete without deadlock");
        
        double[][] result = root.getMatrix();

        // Assert
        assertMatrixEquals(expected, result, "CASE 2: ADD(MUL(A,B), MUL(C,D))");
    }

    @Test
    @Timeout(value = 3, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("CASE 3: NEGATE( MUL( TRANSPOSE(A), B ) )")
    void testComplexCase3_NegateMultiplyTranspose() {
        // Arrange: NEGATE( MUL( TRANSPOSE(A), B ) )
        lae = new LinearAlgebraEngine(3);
        
        // A (2x3) = [[1,2,3],[4,5,6]]
        ComputationNode matrixA = new ComputationNode(new double[][]{
            {1.0, 2.0, 3.0},
            {4.0, 5.0, 6.0}
        });
        
        // B (2x2) = [[2,1],[0,3]]
        ComputationNode matrixB = new ComputationNode(new double[][]{
            {2.0, 1.0},
            {0.0, 3.0}
        });
        
        // TRANSPOSE(A) = [[1,4],[2,5],[3,6]]
        ComputationNode transposeA = new ComputationNode(ComputationNodeType.TRANSPOSE,
                Arrays.asList(matrixA));
        
        // MUL(TRANSPOSE(A), B) = [[2,13],[4,17],[6,19]]
        ComputationNode mulNode = new ComputationNode(ComputationNodeType.MULTIPLY,
                Arrays.asList(transposeA, matrixB));
        
        // NEGATE = [[-2,-13],[-4,-17],[-6,-21]]
        ComputationNode root = new ComputationNode(ComputationNodeType.NEGATE,
                Arrays.asList(mulNode));
        
        double[][] expected = {
            {-2.0, -13.0},
            {-4.0, -17.0},
            {-6.0, -21.0}
        };

        // Act
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            lae.run(root);
        }, "CASE 3 should complete without deadlock");
        
        double[][] result = root.getMatrix();

        // Assert
        assertMatrixEquals(expected, result, "CASE 3: NEGATE(MUL(TRANSPOSE(A), B))");
    }

    @Test
    @Timeout(value = 3, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("CASE 4: MUL( ADD(A,B), TRANSPOSE(C) )")
    void testComplexCase4_MultiplyAddTranspose() {
        // Arrange: MUL( ADD(A,B), TRANSPOSE(C) )
        lae = new LinearAlgebraEngine(3);
        
        // A (2x2)=[[1,2],[3,4]]
        ComputationNode matrixA = new ComputationNode(new double[][]{
            {1.0, 2.0},
            {3.0, 4.0}
        });
        
        // B (2x2)=[[5,6],[7,8]]
        ComputationNode matrixB = new ComputationNode(new double[][]{
            {5.0, 6.0},
            {7.0, 8.0}
        });
        
        // C (3x2)=[[1,0],[0,1],[1,1]]
        ComputationNode matrixC = new ComputationNode(new double[][]{
            {1.0, 0.0},
            {0.0, 1.0},
            {1.0, 1.0}
        });
        
        // ADD(A,B) = [[6,8],[10,12]]
        ComputationNode addNode = new ComputationNode(ComputationNodeType.ADD,
                Arrays.asList(matrixA, matrixB));
        
        // TRANSPOSE(C) = [[1,0,1],[0,1,1]]
        ComputationNode transposeC = new ComputationNode(ComputationNodeType.TRANSPOSE,
                Arrays.asList(matrixC));
        
        // MUL = [[6,8,14],[10,12,22]]
        ComputationNode root = new ComputationNode(ComputationNodeType.MULTIPLY,
                Arrays.asList(addNode, transposeC));
        
        double[][] expected = {
            {6.0, 8.0, 14.0},
            {10.0, 12.0, 22.0}
        };

        // Act
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            lae.run(root);
        }, "CASE 4 should complete without deadlock");
        
        double[][] result = root.getMatrix();

        // Assert
        assertMatrixEquals(expected, result, "CASE 4: MUL(ADD(A,B), TRANSPOSE(C))");
    }

    @Test
    @Timeout(value = 3, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("CASE 5: TRANSPOSE( ADD( MUL( NEGATE(A), B ), C ) )")
    void testComplexCase5_TransposeAddMultiplyNegate() {
        // Arrange: TRANSPOSE( ADD( MUL( NEGATE(A), B ), C ) )
        lae = new LinearAlgebraEngine(4);
        
        // A (2x2)=[[1,-1],[2,0]]
        ComputationNode matrixA = new ComputationNode(new double[][]{
            {1.0, -1.0},
            {2.0, 0.0}
        });
        
        // B (2x2)=[[3,2],[1,4]]
        ComputationNode matrixB = new ComputationNode(new double[][]{
            {3.0, 2.0},
            {1.0, 4.0}
        });
        
        // C (2x2)=[[5,5],[1,1]]
        ComputationNode matrixC = new ComputationNode(new double[][]{
            {5.0, 5.0},
            {1.0, 1.0}
        });
        
        // NEGATE(A) = [[-1,1],[-2,0]]
        ComputationNode negateA = new ComputationNode(ComputationNodeType.NEGATE,
                Arrays.asList(matrixA));
        
        // MUL(NEGATE(A), B) = [[-2,2],[-6,-4]]
        ComputationNode mulNode = new ComputationNode(ComputationNodeType.MULTIPLY,
                Arrays.asList(negateA, matrixB));
        
        // ADD(MUL, C) = [[3,7],[-5,-3]]
        ComputationNode addNode = new ComputationNode(ComputationNodeType.ADD,
                Arrays.asList(mulNode, matrixC));
        
        // TRANSPOSE = [[3,-5],[7,-3]]
        ComputationNode root = new ComputationNode(ComputationNodeType.TRANSPOSE,
                Arrays.asList(addNode));
        
        double[][] expected = {
            {3.0, -5.0},
            {7.0, -3.0}
        };

        // Act
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            lae.run(root);
        }, "CASE 5 should complete without deadlock");
        
        double[][] result = root.getMatrix();

        // Assert
        assertMatrixEquals(expected, result, "CASE 5: TRANSPOSE(ADD(MUL(NEGATE(A), B), C))");
    }

    @Test
    @Timeout(value = 3, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("CASE 6: ADD( MUL(A, ADD(B,C)), NEGATE(TRANSPOSE(D)) )")
    void testComplexCase6_AddMultiplyAddNegateTranspose() {
        // Arrange: ADD( MUL(A, ADD(B,C)), NEGATE(TRANSPOSE(D)) )
        lae = new LinearAlgebraEngine(4);
        
        // A (2x2)=[[2,0],[1,2]]
        ComputationNode matrixA = new ComputationNode(new double[][]{
            {2.0, 0.0},
            {1.0, 2.0}
        });
        
        // B (2x2)=[[1,2],[3,4]]
        ComputationNode matrixB = new ComputationNode(new double[][]{
            {1.0, 2.0},
            {3.0, 4.0}
        });
        
        // C (2x2)=[[0,1],[1,0]]
        ComputationNode matrixC = new ComputationNode(new double[][]{
            {0.0, 1.0},
            {1.0, 0.0}
        });
        
        // D (2x2)=[[1,2],[3,4]]
        ComputationNode matrixD = new ComputationNode(new double[][]{
            {1.0, 2.0},
            {3.0, 4.0}
        });
        
        // ADD(B,C) = [[1,3],[4,4]]
        ComputationNode addBC = new ComputationNode(ComputationNodeType.ADD,
                Arrays.asList(matrixB, matrixC));
        
        // MUL(A, ADD(B,C)) = [[2,6],[9,11]]
        ComputationNode mulNode = new ComputationNode(ComputationNodeType.MULTIPLY,
                Arrays.asList(matrixA, addBC));
        
        // TRANSPOSE(D) = [[1,3],[2,4]]
        ComputationNode transposeD = new ComputationNode(ComputationNodeType.TRANSPOSE,
                Arrays.asList(matrixD));
        
        // NEGATE(TRANSPOSE(D)) = [[-1,-3],[-2,-4]]
        ComputationNode negateTranspose = new ComputationNode(ComputationNodeType.NEGATE,
                Arrays.asList(transposeD));
        
        // ADD(MUL, NEGATE) = [[1,3],[7,7]]
        ComputationNode root = new ComputationNode(ComputationNodeType.ADD,
                Arrays.asList(mulNode, negateTranspose));
        
        double[][] expected = {
            {1.0, 3.0},
            {7.0, 7.0}
        };

        // Act
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            lae.run(root);
        }, "CASE 6 should complete without deadlock");
        
        double[][] result = root.getMatrix();

        // Assert
        assertMatrixEquals(expected, result, "CASE 6: ADD(MUL(A, ADD(B,C)), NEGATE(TRANSPOSE(D)))");
    }

    // ==================== UTILIZATION & EFFICIENCY TESTS ====================

    @Test
    @Timeout(value = 5, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("UTIL1: All workers participate in computation (load distribution)")
    void testUtilization_AllWorkersParticipate() throws Exception {
        // Arrange: Create executor with 4 threads and many tasks
        lae = new LinearAlgebraEngine(4);
        
        // Create a large computation tree that generates many tasks
        // Using a chain of additions: ((A + B) + C) + D) to create multiple tasks
        ComputationNode matrixA = new ComputationNode(new double[][]{
            {1.0, 2.0, 3.0, 4.0, 5.0},
            {6.0, 7.0, 8.0, 9.0, 10.0},
            {11.0, 12.0, 13.0, 14.0, 15.0},
            {16.0, 17.0, 18.0, 19.0, 20.0},
            {21.0, 22.0, 23.0, 24.0, 25.0}
        });
        
        // Create multiple copies for additions
        ComputationNode matrixB = new ComputationNode(new double[][]{
            {1.0, 1.0, 1.0, 1.0, 1.0},
            {1.0, 1.0, 1.0, 1.0, 1.0},
            {1.0, 1.0, 1.0, 1.0, 1.0},
            {1.0, 1.0, 1.0, 1.0, 1.0},
            {1.0, 1.0, 1.0, 1.0, 1.0}
        });
        
        ComputationNode matrixC = new ComputationNode(new double[][]{
            {2.0, 2.0, 2.0, 2.0, 2.0},
            {2.0, 2.0, 2.0, 2.0, 2.0},
            {2.0, 2.0, 2.0, 2.0, 2.0},
            {2.0, 2.0, 2.0, 2.0, 2.0},
            {2.0, 2.0, 2.0, 2.0, 2.0}
        });
        
        ComputationNode matrixD = new ComputationNode(new double[][]{
            {3.0, 3.0, 3.0, 3.0, 3.0},
            {3.0, 3.0, 3.0, 3.0, 3.0},
            {3.0, 3.0, 3.0, 3.0, 3.0},
            {3.0, 3.0, 3.0, 3.0, 3.0},
            {3.0, 3.0, 3.0, 3.0, 3.0}
        });
        
        // Build tree: ((A + B) + C) + D
        ComputationNode add1 = new ComputationNode(ComputationNodeType.ADD,
                Arrays.asList(matrixA, matrixB));
        ComputationNode add2 = new ComputationNode(ComputationNodeType.ADD,
                Arrays.asList(add1, matrixC));
        ComputationNode root = new ComputationNode(ComputationNodeType.ADD,
                Arrays.asList(add2, matrixD));

        // Act
        lae.run(root);
        String report = lae.getWorkerReport();

        // Assert: Parse report to verify all workers did some work
        String[] lines = report.split("\n");
        int workersWithWork = 0;
        
        for (String line : lines) {
            if (line.contains("Worker") && line.contains("used=")) {
                // Extract the used time (format: "used=123, idle=...")
                int usedIndex = line.indexOf("used=");
                int commaIndex = line.indexOf(",", usedIndex);
                if (usedIndex != -1 && commaIndex != -1) {
                    String usedStr = line.substring(usedIndex + 5, commaIndex);
                    long usedTime = Long.parseLong(usedStr.trim());
                    
                    if (usedTime > 0) {
                        workersWithWork++;
                    }
                }
            }
        }
        
        // At least 3 out of 4 workers should have done some work
        assertTrue(workersWithWork >= 3,
                "Expected at least 3 workers to participate, but only " + workersWithWork + " did work.\nReport:\n" + report);
    }

    @Test
    @Timeout(value = 5, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("UTIL2: Fatigue increases with work done")
    void testUtilization_FatigueIncreasesWithWork() throws Exception {
        // Arrange: Single thread to ensure predictable fatigue
        lae = new LinearAlgebraEngine(1);
        
        // Get initial report
        String reportBefore = lae.getWorkerReport();
        long fatigueBefore = extractFatigueFromReport(reportBefore, 0);
        
        // Act: Run a computation
        ComputationNode matrix = new ComputationNode(new double[][]{
            {1.0, 2.0, 3.0},
            {4.0, 5.0, 6.0},
            {7.0, 8.0, 9.0}
        });
        ComputationNode root = new ComputationNode(ComputationNodeType.NEGATE,
                Arrays.asList(matrix));
        lae.run(root);
        
        // Get report after work
        String reportAfter = lae.getWorkerReport();
        long fatigueAfter = extractFatigueFromReport(reportAfter, 0);
        
        // Assert: Fatigue should increase
        assertTrue(fatigueAfter > fatigueBefore,
                "Fatigue should increase after work. Before: " + fatigueBefore + ", After: " + fatigueAfter +
                "\nReport Before:\n" + reportBefore + "\nReport After:\n" + reportAfter);
    }

    @Test
    @Timeout(value = 5, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("UTIL3: Used time accumulates across multiple operations")
    void testUtilization_UsedTimeAccumulates() throws Exception {
        // Arrange
        lae = new LinearAlgebraEngine(2);
        
        // Run first operation
        ComputationNode matrix1 = new ComputationNode(new double[][]{
            {1.0, 2.0},
            {3.0, 4.0}
        });
        ComputationNode root1 = new ComputationNode(ComputationNodeType.NEGATE,
                Arrays.asList(matrix1));
        lae.run(root1);
        
        String reportAfterFirst = lae.getWorkerReport();
        long totalUsedAfterFirst = extractTotalUsedTime(reportAfterFirst);
        
        // Run second operation
        ComputationNode matrix2 = new ComputationNode(new double[][]{
            {5.0, 6.0},
            {7.0, 8.0}
        });
        ComputationNode root2 = new ComputationNode(ComputationNodeType.TRANSPOSE,
                Arrays.asList(matrix2));
        lae.run(root2);
        
        String reportAfterSecond = lae.getWorkerReport();
        long totalUsedAfterSecond = extractTotalUsedTime(reportAfterSecond);
        
        // Assert: Total used time should increase
        assertTrue(totalUsedAfterSecond > totalUsedAfterFirst,
                "Total used time should accumulate. After first: " + totalUsedAfterFirst +
                "ns, After second: " + totalUsedAfterSecond + "ns" +
                "\nReport after first:\n" + reportAfterFirst +
                "\nReport after second:\n" + reportAfterSecond);
    }

    @Test
    @Timeout(value = 5, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("UTIL4: Busy state transitions correctly (idle -> busy -> idle)")
    void testUtilization_BusyStateTransitions() throws Exception {
        // Arrange
        lae = new LinearAlgebraEngine(1);
        
        // Initial report - worker should be idle
        String initialReport = lae.getWorkerReport();
        assertTrue(initialReport.contains("busy=false"),
                "Worker should initially be idle\nReport:\n" + initialReport);
        
        // Run operation - during execution, at least one measurement should show busy
        // (though we can't guarantee we catch it in this state, we verify the operation completes)
        ComputationNode matrix = new ComputationNode(new double[][]{
            {1.0, 2.0, 3.0, 4.0, 5.0},
            {6.0, 7.0, 8.0, 9.0, 10.0},
            {11.0, 12.0, 13.0, 14.0, 15.0},
            {16.0, 17.0, 18.0, 19.0, 20.0},
            {21.0, 22.0, 23.0, 24.0, 25.0}
        });
        ComputationNode root = new ComputationNode(ComputationNodeType.NEGATE,
                Arrays.asList(matrix));
        lae.run(root);
        
        // Final report - worker should be idle again after completion
        String finalReport = lae.getWorkerReport();
        assertTrue(finalReport.contains("busy=false"),
                "Worker should be idle after operation completes\nReport:\n" + finalReport);
        
        // Verify some work was done
        long usedTime = extractTotalUsedTime(finalReport);
        assertTrue(usedTime > 0,
                "Worker should have recorded used time > 0\nReport:\n" + finalReport);
    }

    @Test
    @Timeout(value = 5, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("UTIL5: Multiple threads show balanced load distribution")
    void testUtilization_BalancedLoadDistribution() throws Exception {
        // Arrange: Create executor with 4 threads and a computation with many parallel tasks
        lae = new LinearAlgebraEngine(4);
        
        // Create a large matrix that will be split into many row tasks
        double[][] largeMatrix = new double[20][20];
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                largeMatrix[i][j] = i * 20 + j + 1;
            }
        }
        
        ComputationNode matrix = new ComputationNode(largeMatrix);
        ComputationNode root = new ComputationNode(ComputationNodeType.NEGATE,
                Arrays.asList(matrix));

        // Act
        lae.run(root);
        String report = lae.getWorkerReport();

        // Assert: Extract fatigue values for all workers
        List<Long> fatigueValues = new ArrayList<>();
        String[] lines = report.split("\n");
        
        for (String line : lines) {
            if (line.contains("Worker") && line.contains("fatigue=")) {
                int fatigueIndex = line.indexOf("fatigue=");
                int newlineIndex = line.indexOf("\n", fatigueIndex);
                if (newlineIndex == -1) newlineIndex = line.length();
                if (fatigueIndex != -1) {
                    String fatigueStr = line.substring(fatigueIndex + 8, newlineIndex).trim();
                    double fatigue = Double.parseDouble(fatigueStr);
                    fatigueValues.add((long) fatigue);
                }
            }
        }
        
        assertEquals(4, fatigueValues.size(), "Should have 4 workers in report");
        
        // Calculate max/min ratio to check balance
        long maxFatigue = fatigueValues.stream().max(Long::compare).orElse(0L);
        long minFatigue = fatigueValues.stream().min(Long::compare).orElse(0L);
        
        // All workers should have done some work
        assertTrue(minFatigue > 0,
                "All workers should have positive fatigue. Fatigue values: " + fatigueValues +
                "\nReport:\n" + report);
        
        // Max fatigue shouldn't be more than 10x min fatigue (reasonable distribution)
        // Note: With random fatigueFactor, perfect balance isn't expected
        double ratio = (double) maxFatigue / minFatigue;
        assertTrue(ratio < 10.0,
                "Fatigue distribution should be reasonably balanced (ratio < 10). " +
                "Max: " + maxFatigue + ", Min: " + minFatigue + ", Ratio: " + ratio +
                "\nFatigue values: " + fatigueValues +
                "\nReport:\n" + report);
    }

    @Test
    @Timeout(value = 5, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("UTIL6: Report format is consistent and parseable")
    void testUtilization_ReportFormatConsistency() throws Exception {
        // Arrange
        lae = new LinearAlgebraEngine(3);
        
        // Run some operations
        ComputationNode matrix = new ComputationNode(new double[][]{
            {1.0, 2.0},
            {3.0, 4.0}
        });
        ComputationNode root = new ComputationNode(ComputationNodeType.NEGATE,
                Arrays.asList(matrix));
        lae.run(root);

        // Act
        String report = lae.getWorkerReport();

        // Assert: Verify report structure
        assertNotNull(report, "Report should not be null");
        assertFalse(report.isEmpty(), "Report should not be empty");
        
        String[] lines = report.split("\n");
        int workerCount = 0;
        
        for (String line : lines) {
            if (line.contains("Worker")) {
                workerCount++;
                
                // Each worker line should contain all required fields
                assertTrue(line.contains("busy="), "Line should contain busy= : " + line);
                assertTrue(line.contains("used="), "Line should contain used= : " + line);
                assertTrue(line.contains("idle="), "Line should contain idle= : " + line);
                assertTrue(line.contains("fatigue="), "Line should contain fatigue= : " + line);
                
                // Verify format: busy should be true/false
                assertTrue(line.contains("busy=true") || line.contains("busy=false"),
                        "busy field should be boolean: " + line);
                
                // Verify format: used, idle are integers, fatigue is a double
                assertTrue(line.matches(".*used=\\d+,.*"),
                        "used field should be in format 'used=XXX,': " + line);
                assertTrue(line.matches(".*idle=\\d+,.*"),
                        "idle field should be in format 'idle=XXX,': " + line);
                assertTrue(line.matches(".*fatigue=[0-9.]+.*"),
                        "fatigue field should be in format 'fatigue=XXX.XXX': " + line);
            }
        }
        
        assertEquals(3, workerCount,
                "Report should contain exactly 3 worker entries\nReport:\n" + report);
    }

    // ==================== HELPER METHODS FOR UTILIZATION TESTS ====================

    /**
     * Extract fatigue value for a specific worker from report.
     * @param report The worker report string
     * @param workerIndex The worker index (0-based)
     * @return The fatigue value in nanoseconds
     */
    private long extractFatigueFromReport(String report, int workerIndex) {
        String[] lines = report.split("\n");
        int currentWorker = -1;
        
        for (String line : lines) {
            if (line.contains("Worker")) {
                currentWorker++;
                if (currentWorker == workerIndex && line.contains("fatigue=")) {
                    int fatigueIndex = line.indexOf("fatigue=");
                    if (fatigueIndex != -1) {
                        String fatigueStr = line.substring(fatigueIndex + 8).trim();
                        double fatigue = Double.parseDouble(fatigueStr);
                        return (long) fatigue;
                    }
                }
            }
        }
        return 0L;
    }

    /**
     * Extract total used time across all workers from report.
     * @param report The worker report string
     * @return Total used time in nanoseconds
     */
    private long extractTotalUsedTime(String report) {
        long totalUsed = 0L;
        String[] lines = report.split("\n");
        
        for (String line : lines) {
            if (line.contains("Worker") && line.contains("used=")) {
                int usedIndex = line.indexOf("used=");
                int commaIndex = line.indexOf(",", usedIndex);
                if (usedIndex != -1 && commaIndex != -1) {
                    String usedStr = line.substring(usedIndex + 5, commaIndex).trim();
                    totalUsed += Long.parseLong(usedStr);
                }
            }
        }
        
        return totalUsed;
    }
}
