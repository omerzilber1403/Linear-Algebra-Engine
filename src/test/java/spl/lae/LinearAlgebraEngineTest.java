package spl.lae;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import parser.ComputationNode;
import parser.ComputationNodeType;
import parser.InputParser;

import java.io.File;
import java.io.FileReader;
import java.text.ParseException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for LinearAlgebraEngine.
 * Tests our TODO logic and integration with TiredExecutor.
 */
public class LinearAlgebraEngineTest {

    private static final double DELTA = 1e-9;
    private LinearAlgebraEngine lae;



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

    /**
     * Helper to run test from JSON file with expected result and thread count.
     */
    private void runTestFromJson(String jsonFile, double[][] expected, int threadCount) throws Exception {
        lae = new LinearAlgebraEngine(threadCount);
        InputParser parser = new InputParser();
        ComputationNode root = parser.parse(getResourcePath(jsonFile));
        
        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
            lae.run(root);
        }, "Test should complete without deadlock");
        
        double[][] result = root.getMatrix();
        assertMatrixEquals(expected, result, jsonFile);
    }

    /**
     * Helper to load expected result from JSON file.
     */
    private double[][] loadExpectedFromJson(String jsonFile) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File(getResourcePath(jsonFile)));
        if (root.has("expected")) {
            return mapper.treeToValue(root.get("expected"), double[][].class);
        }
        throw new IllegalArgumentException("No 'expected' field in " + jsonFile);
    }

    /**
     * Helper to load thread count from JSON file.
     */
    private int loadThreadCountFromJson(String jsonFile) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File(getResourcePath(jsonFile)));
        if (root.has("threadCount")) {
            return root.get("threadCount").asInt();
        }
        return 2; // Default
    }

    /**
     * Helper to load test name from JSON file.
     */
    private String loadTestNameFromJson(String jsonFile) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File(getResourcePath(jsonFile)));
        if (root.has("testName")) {
            return root.get("testName").asText();
        }
        return jsonFile;
    }

    // ==================== DATA PROVIDERS FOR PARAMETERIZED TESTS ====================

    /**
     * Provides test data for basic operations (ADD, MULTIPLY, NEGATE, TRANSPOSE).
     */
    static Stream<Arguments> basicOperationTestData() {
        return Stream.of(
            Arguments.of("add_test.json"),
            Arguments.of("multiply_test.json"),
            Arguments.of("negate_test.json"),
            Arguments.of("transpose_test.json")
        );
    }

    /**
     * Provides test data for complex computation tree cases.
     */
    static Stream<Arguments> complexCaseTestData() {
        return Stream.of(
            Arguments.of("complex_case1.json"),
            Arguments.of("complex_case2.json"),
            Arguments.of("complex_case3.json"),
            Arguments.of("complex_case4.json"),
            Arguments.of("complex_case5.json"),
            Arguments.of("complex_case6.json")
        );
    }

    /**
     * Provides test data for dimension mismatch error cases.
     */
    static Stream<Arguments> dimensionMismatchTestData() {
        return Stream.of(
            Arguments.of(ComputationNodeType.ADD, 
                new double[][]{{1, 2}, {3, 4}}, 
                new double[][]{{1, 2, 3}},
                "can't perform addition"),
            Arguments.of(ComputationNodeType.MULTIPLY, 
                new double[][]{{1, 2, 3}}, 
                new double[][]{{1, 2}, {3, 4}},
                "can't perform multiplication")
        );
    }

    // ==================== PARAMETERIZED TESTS ====================

    @ParameterizedTest
    @MethodSource("basicOperationTestData")
    @Timeout(value = 3, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("Basic operations produce correct results from JSON")
    void testBasicOperationsFromJson(String jsonFile) throws Exception {
        double[][] expected = loadExpectedFromJson(jsonFile);
        int threadCount = loadThreadCountFromJson(jsonFile);
        String testName = loadTestNameFromJson(jsonFile);
        
        runTestFromJson(jsonFile, expected, threadCount);
    }

    @ParameterizedTest
    @MethodSource("complexCaseTestData")
    @Timeout(value = 3, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("Complex computation trees produce correct results from JSON")
    void testComplexCasesFromJson(String jsonFile) throws Exception {
        double[][] expected = loadExpectedFromJson(jsonFile);
        int threadCount = loadThreadCountFromJson(jsonFile);
        String testName = loadTestNameFromJson(jsonFile);
        
        runTestFromJson(jsonFile, expected, threadCount);
    }

    @ParameterizedTest
    @MethodSource("dimensionMismatchTestData")
    @Timeout(value = 3, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("Operations with mismatched dimensions throw IllegalArgumentException")
    void testDimensionMismatch(ComputationNodeType operationType, 
                               double[][] matrix1, 
                               double[][] matrix2, 
                               String expectedMessage) {
        lae = new LinearAlgebraEngine(2);
        
        ComputationNode node1 = new ComputationNode(matrix1);
        ComputationNode node2 = new ComputationNode(matrix2);
        ComputationNode root = new ComputationNode(operationType, Arrays.asList(node1, node2));
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> lae.run(root),
            "Should throw IllegalArgumentException for mismatched dimensions"
        );
        
        assertTrue(exception.getMessage().contains(expectedMessage),
                "Exception message should contain: " + expectedMessage);
    }

    // ==================== REMAINING INDIVIDUAL TESTS ====================

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
        assertTrue(report.contains("Worker Report"), "Report should contain header");
        assertTrue(report.contains("fatigue="), "Report should contain 'fatigue='");
        assertTrue(report.contains("used="), "Report should contain 'used='");
        assertTrue(report.contains("idle="), "Report should contain 'idle='");
        assertTrue(report.contains("Fairness:"), "Report should contain 'Fairness:'");
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
                // Extract the used time from line (format: "Worker X | fatigue=... | used=0.0662 ms | ...")
                String pattern = "used=([0-9.]+) ms";
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
                java.util.regex.Matcher m = p.matcher(line);
                if (m.find()) {
                    double usedTime = Double.parseDouble(m.group(1));
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
        double fatigueBefore = extractFatigueFromReport(reportBefore, 0);
        
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
        double fatigueAfter = extractFatigueFromReport(reportAfter, 0);
        
        // Assert: Fatigue should increase
        assertTrue(fatigueAfter > fatigueBefore,
                "Fatigue should increase after work. Before: " + fatigueBefore + ", After: " + fatigueAfter +
                "\nReport Before:\n" + reportBefore + "\nReport After:\n" + reportAfter);
    }

    @Test
    @Timeout(value = 5, unit = java.util.concurrent.TimeUnit.SECONDS)
    @DisplayName("UTIL4: Busy state transitions correctly (idle -> busy -> idle)")
    void testUtilization_BusyStateTransitions() throws Exception {
        // Arrange
        lae = new LinearAlgebraEngine(1);
        
        // Initial report - fatigue should be zero or very low
        String initialReport = lae.getWorkerReport();
        double fatigueInitial = extractFatigueFromReport(initialReport, 0);
        
        assertTrue(fatigueInitial >= 0,
                "Worker fatigue should be non-negative initially\nReport:\n" + initialReport);
        
        // Run operation
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
        
        // Final report - fatigue should have increased
        String finalReport = lae.getWorkerReport();
        double fatigueAfter = extractFatigueFromReport(finalReport, 0);
        
        assertTrue(fatigueAfter > fatigueInitial,
                "Worker fatigue should increase after work. Initial: " + fatigueInitial + ", After: " + fatigueAfter +
                "\nReport:\n" + finalReport);
        
        // Verify some work was done
        double usedTime = extractUsedTime(finalReport, 0);
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
        List<Double> fatigueValues = new ArrayList<>();
        
        for (int i = 0; i < 4; i++) {
            double fatigue = extractFatigueFromReport(report, i);
            fatigueValues.add(fatigue);
        }
        
        assertEquals(4, fatigueValues.size(), "Should have 4 workers in report");
        
        // Calculate max/min ratio to check balance
        double maxFatigue = fatigueValues.stream().max(Double::compare).orElse(0.0);
        double minFatigue = fatigueValues.stream().min(Double::compare).orElse(0.0);
        
        // All workers should have done some work
        assertTrue(minFatigue > 0,
                "All workers should have positive fatigue. Fatigue values: " + fatigueValues +
                "\nReport:\n" + report);
        
        // Max fatigue shouldn't be more than 10x min fatigue (reasonable distribution)
        // Note: With random fatigueFactor, perfect balance isn't expected
        double ratio = maxFatigue / minFatigue;
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
            if (line.contains("Worker") && line.contains("|")) {
                workerCount++;
                
                // Each worker line should contain all required fields in new format: Worker X | fatigue=... | used=... ms | idle=... ms
                assertTrue(line.contains("fatigue="), "Line should contain fatigue= : " + line);
                assertTrue(line.contains("used="), "Line should contain used= : " + line);
                assertTrue(line.contains("idle="), "Line should contain idle= : " + line);
                assertTrue(line.contains(" ms"), "Line should contain ms unit: " + line);
                
                // Verify format: fatigue is a double, used and idle are in ms
                assertTrue(line.matches(".*fatigue=[0-9.]+.*"),
                        "fatigue field should be in format 'fatigue=XXX.XXX': " + line);
                assertTrue(line.matches(".*used=[0-9.]+ ms.*"),
                        "used field should be in format 'used=XXX.XXX ms': " + line);
                assertTrue(line.matches(".*idle=[0-9.]+ ms.*"),
                        "idle field should be in format 'idle=XXX.XXX ms': " + line);
            }
        }
        
        assertEquals(3, workerCount,
                "Report should contain exactly 3 worker entries\nReport:\n" + report);
    }

    // ==================== HELPER METHODS FOR UTILIZATION TESTS ====================

    /**
     * Extract fatigue value for a specific worker from report.
     * Format: "Worker 0 | fatigue=87257.33 | used=0.0598 ms | idle=0.1307 ms"
     * @param report The worker report string
     * @param workerIndex The worker index (0-based)
     * @return The fatigue value
     */
    private double extractFatigueFromReport(String report, int workerIndex) {
        String pattern = "Worker " + workerIndex + " \\| fatigue=([0-9.]+) \\|";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(report);
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        return 0.0;
    }

    /**
     * Extract used time for a specific worker from report.
     * Format: "Worker 0 | fatigue=87257.33 | used=0.0598 ms | idle=0.1307 ms"
     * @param report The worker report string or line
     * @param workerIndex The worker index (0-based)
     * @return The used time in milliseconds
     */
    private double extractUsedTime(String report, int workerIndex) {
        String pattern = "Worker " + workerIndex + " \\| fatigue=[0-9.]+ \\| used=([0-9.]+) ms";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(report);
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        return 0.0;
    }
}
