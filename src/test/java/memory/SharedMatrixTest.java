package memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for SharedMatrix.
 * Tests cover: constructors, loadRowMajor, loadColumnMajor, readRowMajor, and accessors.
 * Including normal cases, edge cases, and negative tests.
 */
@DisplayName("SharedMatrix Unit Tests")
public class SharedMatrixTest {

    private static final double DELTA = 1e-9;

    // ==================== Helper Methods ====================

    /**
     * Asserts that two 2D arrays are equal element-wise.
     */
    private void assertMatrixEquals(double[][] expected, double[][] actual) {
        assertEquals(expected.length, actual.length, "Matrix row count mismatch");
        for (int r = 0; r < expected.length; r++) {
            assertArrayEquals(expected[r], actual[r], DELTA, "Row " + r + " mismatch");
        }
    }

    /**
     * Creates a row-major SharedMatrix from the given data.
     */
    private SharedMatrix createRowMajorMatrix(double[][] data) {
        return new SharedMatrix(data);
    }

    /**
     * Creates a column-major SharedMatrix from the given data.
     */
    private SharedMatrix createColumnMajorMatrix(double[][] data) {
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(data);
        return matrix;
    }

    // ==================== Constructor Tests ====================

    @Test
    @DisplayName("Constructor() - Creates empty matrix")
    public void testConstructor_Empty() {
        // Act
        SharedMatrix matrix = new SharedMatrix();
        
        // Assert
        assertEquals(0, matrix.length());
        assertNull(matrix.getOrientation());
        assertArrayEquals(new double[0][0], matrix.readRowMajor());
    }

    @Test
    @DisplayName("Constructor(double[][]) - Normal case")
    public void testConstructor_NormalCase() {
        // Arrange
        double[][] input = {
            {1.0, 2.0, 3.0},
            {4.0, 5.0, 6.0}
        };
        
        // Act
        SharedMatrix matrix = new SharedMatrix(input);
        
        // Assert
        assertEquals(2, matrix.length());
        assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());
        assertMatrixEquals(input, matrix.readRowMajor());
    }

    @Test
    @DisplayName("Constructor(double[][]) - Defensive copy")
    public void testConstructor_DefensiveCopy() {
        // Arrange
        double[][] input = {
            {1.0, 2.0},
            {3.0, 4.0}
        };
        
        SharedMatrix matrix = new SharedMatrix(input);
        
        // Act: Modify input after construction
        input[0][0] = 999.0;
        input[1][1] = 888.0;
        
        // Assert: Matrix should not be affected
        double[][] result = matrix.readRowMajor();
        assertEquals(1.0, result[0][0], DELTA);
        assertEquals(4.0, result[1][1], DELTA);
    }

    @Test
    @DisplayName("Constructor(double[][]) - Null matrix throws exception")
    public void testConstructor_NullMatrix() {
        assertThrows(IllegalArgumentException.class, () -> {
            new SharedMatrix(null);
        });
    }

    @Test
    @DisplayName("Constructor(double[][]) - Null first row throws exception")
    public void testConstructor_NullFirstRow() {
        // Arrange
        double[][] input = {null, {1.0, 2.0}};
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new SharedMatrix(input)
        );
        assertTrue(exception.getMessage().contains("All rows must have the same length"));
    }

    @Test
    @DisplayName("Constructor(double[][]) - Null middle row throws exception")
    public void testConstructor_NullMiddleRow() {
        // Arrange
        double[][] input = {{1.0, 2.0}, null, {3.0, 4.0}};
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new SharedMatrix(input)
        );
        assertTrue(exception.getMessage().contains("All rows must have the same length"));
    }

    @Test
    @DisplayName("Constructor(double[][]) - Ragged array throws exception")
    public void testConstructor_RaggedArray() {
        // Arrange
        double[][] input = {{1.0, 2.0, 3.0}, {4.0, 5.0}};
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new SharedMatrix(input)
        );
        assertTrue(exception.getMessage().contains("All rows must have the same length"));
    }

    @Test
    @DisplayName("Constructor(double[][]) - Empty array creates empty matrix")
    public void testConstructor_EmptyArray() {
        // Arrange
        double[][] input = new double[0][0];
        
        // Act
        SharedMatrix matrix = new SharedMatrix(input);
        
        // Assert
        assertEquals(0, matrix.length());
        assertNull(matrix.getOrientation());
    }

    // ==================== loadRowMajor Tests ====================

    @Test
    @DisplayName("loadRowMajor() - Normal case")
    public void testLoadRowMajor_NormalCase() {
        // Arrange
        SharedMatrix matrix = new SharedMatrix();
        double[][] data = {
            {1.0, 2.0},
            {3.0, 4.0},
            {5.0, 6.0}
        };
        
        // Act
        matrix.loadRowMajor(data);
        
        // Assert
        assertEquals(3, matrix.length());
        assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());
        assertMatrixEquals(data, matrix.readRowMajor());
    }

    @Test
    @DisplayName("loadRowMajor() - Replaces existing data")
    public void testLoadRowMajor_ReplacesData() {
        // Arrange
        SharedMatrix matrix = new SharedMatrix(new double[][]{{1.0, 2.0}});
        double[][] newData = {{10.0, 20.0, 30.0}, {40.0, 50.0, 60.0}};
        
        // Act
        matrix.loadRowMajor(newData);
        
        // Assert
        assertEquals(2, matrix.length());
        assertMatrixEquals(newData, matrix.readRowMajor());
    }

    @Test
    @DisplayName("loadRowMajor() - Defensive copy")
    public void testLoadRowMajor_DefensiveCopy() {
        // Arrange
        SharedMatrix matrix = new SharedMatrix();
        double[][] data = {{1.0, 2.0}, {3.0, 4.0}};
        
        matrix.loadRowMajor(data);
        
        // Act: Modify data after load
        data[0][0] = 999.0;
        
        // Assert: Matrix should not be affected
        double[][] result = matrix.readRowMajor();
        assertEquals(1.0, result[0][0], DELTA);
    }

    @Test
    @DisplayName("loadRowMajor() - Null matrix throws exception")
    public void testLoadRowMajor_NullMatrix() {
        // Arrange
        SharedMatrix matrix = new SharedMatrix();
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            matrix.loadRowMajor(null);
        });
    }

    @Test
    @DisplayName("loadRowMajor() - Null row throws exception")
    public void testLoadRowMajor_NullRow() {
        // Arrange
        SharedMatrix matrix = new SharedMatrix();
        double[][] data = {{1.0, 2.0}, null};
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> matrix.loadRowMajor(data)
        );
        assertTrue(exception.getMessage().contains("All rows must have the same length"));
    }

    @Test
    @DisplayName("loadRowMajor() - Ragged array throws exception")
    public void testLoadRowMajor_RaggedArray() {
        // Arrange
        SharedMatrix matrix = new SharedMatrix();
        double[][] data = {{1.0, 2.0, 3.0}, {4.0, 5.0}};
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> matrix.loadRowMajor(data)
        );
        assertTrue(exception.getMessage().contains("All rows must have the same length"));
    }

    @Test
    @DisplayName("loadRowMajor() - Empty array creates empty matrix")
    public void testLoadRowMajor_EmptyArray() {
        // Arrange
        SharedMatrix matrix = new SharedMatrix();
        
        // Act
        matrix.loadRowMajor(new double[0][0]);
        
        // Assert
        assertEquals(0, matrix.length());
        assertNull(matrix.getOrientation());
    }

    // ==================== loadColumnMajor Tests ====================

    @Test
    @DisplayName("loadColumnMajor() - Normal case 2x2")
    public void testLoadColumnMajor_NormalCase() {
        // Arrange
        SharedMatrix matrix = new SharedMatrix();
        double[][] input = {
            {1.0, 2.0},
            {3.0, 4.0}
        };
        
        // Act
        matrix.loadColumnMajor(input);
        
        // Assert
        // Should have 2 column vectors
        assertEquals(2, matrix.length());
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());
        
        // Verify columns: col0=[1,3], col1=[2,4]
        SharedVector col0 = matrix.get(0);
        assertEquals(2, col0.length());
        assertEquals(1.0, col0.get(0), DELTA);
        assertEquals(3.0, col0.get(1), DELTA);
        
        SharedVector col1 = matrix.get(1);
        assertEquals(2, col1.length());
        assertEquals(2.0, col1.get(0), DELTA);
        assertEquals(4.0, col1.get(1), DELTA);
        
        // readRowMajor should reconstruct original
        assertMatrixEquals(input, matrix.readRowMajor());
    }

    @Test
    @DisplayName("loadColumnMajor() - 3x2 matrix")
    public void testLoadColumnMajor_3x2() {
        // Arrange
        SharedMatrix matrix = new SharedMatrix();
        double[][] input = {
            {1.0, 2.0},
            {3.0, 4.0},
            {5.0, 6.0}
        };
        
        // Act
        matrix.loadColumnMajor(input);
        
        // Assert
        assertEquals(2, matrix.length()); // 2 columns
        assertMatrixEquals(input, matrix.readRowMajor());
    }

    @Test
    @DisplayName("loadColumnMajor() - Empty array creates empty matrix")
    public void testLoadColumnMajor_EmptyArray() {
        // Arrange
        SharedMatrix matrix = new SharedMatrix();
        
        // Act
        matrix.loadColumnMajor(new double[0][0]);
        
        // Assert
        assertEquals(0, matrix.length());
        assertNull(matrix.getOrientation());
        assertArrayEquals(new double[0][0], matrix.readRowMajor());
    }

    @Test
    @DisplayName("loadColumnMajor() - Zero columns creates empty matrix")
    public void testLoadColumnMajor_ZeroColumns() {
        // Arrange
        SharedMatrix matrix = new SharedMatrix();
        
        // Act
        matrix.loadColumnMajor(new double[1][0]);
        
        // Assert
        assertEquals(0, matrix.length());
        assertNull(matrix.getOrientation());
        assertArrayEquals(new double[0][0], matrix.readRowMajor());
    }

    @Test
    @DisplayName("loadColumnMajor() - Null matrix throws exception")
    public void testLoadColumnMajor_NullMatrix() {
        // Arrange
        SharedMatrix matrix = new SharedMatrix();
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> matrix.loadColumnMajor(null)
        );
        assertTrue(exception.getMessage().contains("matrix is null"));
    }

    @Test
    @DisplayName("loadColumnMajor() - Null first row throws exception")
    public void testLoadColumnMajor_NullFirstRow() {
        // Arrange
        SharedMatrix matrix = new SharedMatrix();
        double[][] input = {null, {1.0, 2.0}};
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> matrix.loadColumnMajor(input)
        );
        assertTrue(exception.getMessage().contains("matrix row 0 is null"));
    }

    @Test
    @DisplayName("loadColumnMajor() - Null middle row throws exception")
    public void testLoadColumnMajor_NullMiddleRow() {
        // Arrange
        SharedMatrix matrix = new SharedMatrix();
        double[][] input = {{1.0, 2.0}, null, {3.0, 4.0}};
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> matrix.loadColumnMajor(input)
        );
        assertTrue(exception.getMessage().contains("matrix row 1 is null"));
    }

    @Test
    @DisplayName("loadColumnMajor() - Ragged array throws exception")
    public void testLoadColumnMajor_RaggedArray() {
        // Arrange
        SharedMatrix matrix = new SharedMatrix();
        double[][] input = {{1.0, 2.0, 3.0}, {4.0, 5.0}};
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> matrix.loadColumnMajor(input)
        );
        assertTrue(exception.getMessage().contains("matrix is not defined"));
    }

    // ==================== readRowMajor Tests ====================

    @Test
    @DisplayName("readRowMajor() - From row-major matrix")
    public void testReadRowMajor_FromRowMajor() {
        // Arrange
        double[][] expected = {
            {1.0, 2.0, 3.0},
            {4.0, 5.0, 6.0}
        };
        SharedMatrix matrix = createRowMajorMatrix(expected);
        
        // Act & Assert
        assertMatrixEquals(expected, matrix.readRowMajor());
    }

    @Test
    @DisplayName("readRowMajor() - From column-major matrix")
    public void testReadRowMajor_FromColumnMajor() {
        // Arrange
        double[][] expected = {
            {1.0, 2.0},
            {3.0, 4.0},
            {5.0, 6.0}
        };
        SharedMatrix matrix = createColumnMajorMatrix(expected);
        
        // Act & Assert
        assertMatrixEquals(expected, matrix.readRowMajor());
    }

    @Test
    @DisplayName("readRowMajor() - Empty matrix")
    public void testReadRowMajor_EmptyMatrix() {
        // Arrange
        SharedMatrix matrix = new SharedMatrix();
        
        // Act & Assert
        assertArrayEquals(new double[0][0], matrix.readRowMajor());
    }

    @Test
    @DisplayName("readRowMajor() - Throws exception for inconsistent orientations")
    public void testReadRowMajor_InconsistentOrientations() {
        // Arrange: Create a row-major matrix
        SharedMatrix matrix = new SharedMatrix(new double[][]{
            {1.0, 2.0},
            {3.0, 4.0}
        });
        
        // Act: Transpose only the first row to create inconsistent orientations
        matrix.get(0).transpose();
        
        // Assert: readRowMajor should throw IllegalStateException
        assertThrows(IllegalStateException.class, () -> {
            matrix.readRowMajor();
        });
    }

    @Test
    @DisplayName("readRowMajor() - Throws exception for inconsistent row lengths")
    public void testReadRowMajor_InconsistentLengths() {
        // Arrange: Create a row-major matrix with 2 rows
        SharedMatrix matrix = new SharedMatrix(new double[][]{
            {1.0, 2.0, 3.0},
            {4.0, 5.0, 6.0}
        });
        
        // Create a column-major matrix to use with vecMatMul
        // This will change the first row's length from 3 to 2
        SharedMatrix colMatrix = new SharedMatrix();
        colMatrix.loadColumnMajor(new double[][]{
            {1.0, 2.0},
            {3.0, 4.0},
            {5.0, 6.0}
        });
        
        // Act: Change first row's length using vecMatMul
        matrix.get(0).vecMatMul(colMatrix);
        
        // Assert: Now first row has length 2, second row has length 3
        // readRowMajor should throw IllegalStateException
        assertThrows(IllegalStateException.class, () -> {
            matrix.readRowMajor();
        });
    }

    // ==================== Accessor Tests ====================

    @Test
    @DisplayName("length() - Returns correct number of vectors")
    public void testLength() {
        SharedMatrix matrix1 = new SharedMatrix();
        assertEquals(0, matrix1.length());
        
        SharedMatrix matrix2 = new SharedMatrix(new double[][]{{1.0}, {2.0}, {3.0}});
        assertEquals(3, matrix2.length());
        
        SharedMatrix matrix3 = createColumnMajorMatrix(new double[][]{{1.0, 2.0, 3.0, 4.0}});
        assertEquals(4, matrix3.length()); // 4 columns
    }

    @Test
    @DisplayName("get(i) - Returns correct vector")
    public void testGet() {
        // Arrange
        SharedMatrix matrix = new SharedMatrix(new double[][]{
            {1.0, 2.0},
            {3.0, 4.0}
        });
        
        // Act
        SharedVector row0 = matrix.get(0);
        SharedVector row1 = matrix.get(1);
        
        // Assert
        assertEquals(1.0, row0.get(0), DELTA);
        assertEquals(2.0, row0.get(1), DELTA);
        assertEquals(3.0, row1.get(0), DELTA);
        assertEquals(4.0, row1.get(1), DELTA);
    }

    @Test
    @DisplayName("get(i) - Throws exception for negative index")
    public void testGet_NegativeIndex() {
        // Arrange
        SharedMatrix matrix = new SharedMatrix(new double[][]{{1.0, 2.0}});
        
        // Act & Assert
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            matrix.get(-1);
        });
    }

    @Test
    @DisplayName("get(i) - Throws exception for index at length")
    public void testGet_IndexAtLength() {
        // Arrange
        SharedMatrix matrix = new SharedMatrix(new double[][]{{1.0, 2.0}, {3.0, 4.0}});
        
        // Act & Assert
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            matrix.get(2);
        });
    }

    @Test
    @DisplayName("get(i) - Throws exception for index beyond length")
    public void testGet_IndexBeyondLength() {
        // Arrange
        SharedMatrix matrix = new SharedMatrix(new double[][]{{1.0, 2.0}});
        
        // Act & Assert
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            matrix.get(5);
        });
    }

    @Test
    @DisplayName("getOrientation() - Returns correct orientation")
    public void testGetOrientation() {
        SharedMatrix empty = new SharedMatrix();
        assertNull(empty.getOrientation());
        
        SharedMatrix rowMajor = new SharedMatrix(new double[][]{{1.0, 2.0}});
        assertEquals(VectorOrientation.ROW_MAJOR, rowMajor.getOrientation());
        
        SharedMatrix colMajor = createColumnMajorMatrix(new double[][]{{1.0, 2.0}});
        assertEquals(VectorOrientation.COLUMN_MAJOR, colMajor.getOrientation());
    }
}
