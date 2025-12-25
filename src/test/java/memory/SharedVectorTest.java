package memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for SharedVector.
 * Tests cover: add, negate, transpose, dot, vecMatMul
 * Including normal cases, edge cases, and negative tests.
 */
@DisplayName("SharedVector Unit Tests - All Operations")
public class SharedVectorTest {

    private static final double DELTA = 1e-9;

    // ==================== Helper Methods ====================

    /**
     * Creates a row-major SharedVector from the given array.
     */
    private SharedVector createRow(double... values) {
        return new SharedVector(values, VectorOrientation.ROW_MAJOR);
    }

    /**
     * Creates a column-major SharedVector from the given array.
     */
    private SharedVector createCol(double... values) {
        return new SharedVector(values, VectorOrientation.COLUMN_MAJOR);
    }

    /**
     * Asserts that the SharedVector contains the expected values.
     */
    private void assertVectorEquals(double[] expected, SharedVector actual) {
        assertEquals(expected.length, actual.length(), "Vector length mismatch");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual.get(i), DELTA, 
                "Mismatch at index " + i);
        }
    }

    /**
     * Creates a column-major SharedMatrix from a 2D array.
     */
    private SharedMatrix createColumnMajorMatrix(double[][] data) {
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(data);
        return matrix;
    }

    // ==================== Tests for add() ====================

    @Test
    @DisplayName("add() - Two row vectors addition")
    public void testAdd_NormalCase_TwoRowVectors() {
        // Arrange
        SharedVector v1 = createRow(1.0, 2.0, 3.0);
        SharedVector v2 = createRow(4.0, 5.0, 6.0);

        // Act
        v1.add(v2);

        // Assert
        assertVectorEquals(new double[]{5.0, 7.0, 9.0}, v1);
        assertEquals(VectorOrientation.ROW_MAJOR, v1.getOrientation());
    }

    @Test
    @DisplayName("add() - Two column vectors addition")
    public void testAdd_NormalCase_TwoColumnVectors() {
        // Arrange
        SharedVector v1 = createCol(1.0, 2.0, 3.0);
        SharedVector v2 = createCol(10.0, 20.0, 30.0);

        // Act
        v1.add(v2);

        // Assert
        assertVectorEquals(new double[]{11.0, 22.0, 33.0}, v1);
        assertEquals(VectorOrientation.COLUMN_MAJOR, v1.getOrientation());
    }

    @Test
    @DisplayName("add() - With negative numbers")
    public void testAdd_WithNegativeNumbers() {
        // Arrange
        SharedVector v1 = createRow(5.0, -3.0, 2.0);
        SharedVector v2 = createRow(-2.0, 7.0, -1.0);

        // Act
        v1.add(v2);

        // Assert
        assertVectorEquals(new double[]{3.0, 4.0, 1.0}, v1);
    }

    @Test
    @DisplayName("add() - Adding zero vector (identity)")
    public void testAdd_WithZeros() {
        // Arrange
        SharedVector v1 = createRow(1.0, 2.0, 3.0);
        SharedVector v2 = createRow(0.0, 0.0, 0.0);

        // Act
        v1.add(v2);

        // Assert
        assertVectorEquals(new double[]{1.0, 2.0, 3.0}, v1);
    }

    @Test
    @DisplayName("add() - Single element vectors")
    public void testAdd_SingleElement() {
        // Arrange
        SharedVector v1 = createRow(5.0);
        SharedVector v2 = createRow(3.0);

        // Act
        v1.add(v2);

        // Assert
        assertVectorEquals(new double[]{8.0}, v1);
    }

    @Test
    @DisplayName("add() - Throws exception for different lengths")
    public void testAdd_ThrowsException_DifferentLengths() {
        // Arrange
        SharedVector v1 = createRow(1.0, 2.0, 3.0);
        SharedVector v2 = createRow(4.0, 5.0);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> v1.add(v2)
        );
        assertTrue(exception.getMessage().contains("same length"));
    }

    @Test
    @DisplayName("add() - Throws exception for different orientations")
    public void testAdd_ThrowsException_DifferentOrientations() {
        // Arrange
        SharedVector v1 = createRow(1.0, 2.0, 3.0);
        SharedVector v2 = createCol(4.0, 5.0, 6.0);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> v1.add(v2)
        );
        assertTrue(exception.getMessage().contains("same orientation"));
    }

    @Test
    @DisplayName("add() - Throws NullPointerException for null input")
    public void testAdd_ThrowsNullPointerException() {
        // Arrange
        SharedVector v = createRow(1.0, 2.0, 3.0);

        // Act & Assert
        assertThrows(NullPointerException.class, () -> v.add(null));
    }

    // ==================== Tests for negate() ====================

    @Test
    @DisplayName("negate() - Normal case with mixed positive/negative values")
    public void testNegate_NormalCase() {
        // Arrange
        SharedVector v = createRow(1.0, -2.0, 3.0, -4.0);

        // Act
        v.negate();

        // Assert
        assertVectorEquals(new double[]{-1.0, 2.0, -3.0, 4.0}, v);
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
    }

    @Test
    @DisplayName("negate() - Column vector orientation preserved")
    public void testNegate_ColumnVector() {
        // Arrange
        SharedVector v = createCol(5.0, -10.0, 15.0);

        // Act
        v.negate();

        // Assert
        assertVectorEquals(new double[]{-5.0, 10.0, -15.0}, v);
        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());
    }

    @Test
    @DisplayName("negate() - All zeros remain zeros")
    public void testNegate_AllZeros() {
        // Arrange
        SharedVector v = createRow(0.0, 0.0, 0.0);

        // Act
        v.negate();

        // Assert
        assertVectorEquals(new double[]{0.0, 0.0, 0.0}, v);
    }

    @Test
    @DisplayName("negate() - Single element vector")
    public void testNegate_SingleElement() {
        // Arrange
        SharedVector v = createRow(42.0);

        // Act
        v.negate();

        // Assert
        assertVectorEquals(new double[]{-42.0}, v);
    }

    @Test
    @DisplayName("negate() - Double negation returns original values")
    public void testNegate_DoublenegateReturnsOriginal() {
        // Arrange
        SharedVector v = createRow(1.0, 2.0, 3.0);

        // Act
        v.negate();
        v.negate();

        // Assert
        assertVectorEquals(new double[]{1.0, 2.0, 3.0}, v);
    }

    // ==================== Tests for transpose() ====================

    @Test
    @DisplayName("transpose() - Row vector becomes column vector")
    public void testTranspose_RowToColumn() {
        // Arrange
        SharedVector v = createRow(1.0, 2.0, 3.0);

        // Act
        v.transpose();

        // Assert
        assertVectorEquals(new double[]{1.0, 2.0, 3.0}, v); // values unchanged
        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());
    }

    @Test
    @DisplayName("transpose() - Column vector becomes row vector")
    public void testTranspose_ColumnToRow() {
        // Arrange
        SharedVector v = createCol(4.0, 5.0, 6.0);

        // Act
        v.transpose();

        // Assert
        assertVectorEquals(new double[]{4.0, 5.0, 6.0}, v); // values unchanged
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
    }

    @Test
    @DisplayName("transpose() - Double transpose returns original orientation")
    public void testTranspose_DoubleTransposeReturnsOriginalOrientation() {
        // Arrange
        SharedVector v = createRow(1.0, 2.0, 3.0);
        VectorOrientation original = v.getOrientation();

        // Act
        v.transpose();
        v.transpose();

        // Assert
        assertEquals(original, v.getOrientation());
        assertVectorEquals(new double[]{1.0, 2.0, 3.0}, v);
    }

    @Test
    @DisplayName("transpose() - Single element vector")
    public void testTranspose_SingleElement() {
        // Arrange
        SharedVector v = createRow(7.0);

        // Act
        v.transpose();

        // Assert
        assertVectorEquals(new double[]{7.0}, v);
        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());
    }

    // ==================== Tests for dot() ====================

    @Test
    @DisplayName("dot() - Row vector times column vector")
    public void testDot_RowTimesColumn() {
        // Arrange
        SharedVector row = createRow(1.0, 2.0, 3.0);
        SharedVector col = createCol(4.0, 5.0, 6.0);

        // Act
        double result = row.dot(col);

        // Assert: 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
        assertEquals(32.0, result, DELTA);
    }

    @Test
    @DisplayName("dot() - Column vector times row vector")
    public void testDot_ColumnTimesRow() {
        // Arrange
        SharedVector col = createCol(2.0, 3.0);
        SharedVector row = createRow(5.0, 7.0);

        // Act
        double result = col.dot(row);

        // Assert: 2*5 + 3*7 = 10 + 21 = 31
        assertEquals(31.0, result, DELTA);
    }

    @Test
    @DisplayName("dot() - With negative numbers")
    public void testDot_WithNegativeNumbers() {
        // Arrange
        SharedVector row = createRow(1.0, -2.0, 3.0);
        SharedVector col = createCol(-1.0, 2.0, -3.0);

        // Act
        double result = row.dot(col);

        // Assert: 1*(-1) + (-2)*2 + 3*(-3) = -1 - 4 - 9 = -14
        assertEquals(-14.0, result, DELTA);
    }

    @Test
    @DisplayName("dot() - With zeros returns zero")
    public void testDot_WithZeros() {
        // Arrange
        SharedVector row = createRow(1.0, 0.0, 3.0);
        SharedVector col = createCol(0.0, 5.0, 0.0);

        // Act
        double result = row.dot(col);

        // Assert: 1*0 + 0*5 + 3*0 = 0
        assertEquals(0.0, result, DELTA);
    }

    @Test
    @DisplayName("dot() - Single element vectors")
    public void testDot_SingleElement() {
        // Arrange
        SharedVector row = createRow(5.0);
        SharedVector col = createCol(3.0);

        // Act
        double result = row.dot(col);

        // Assert: 5*3 = 15
        assertEquals(15.0, result, DELTA);
    }

    @Test
    @DisplayName("dot() - Throws exception for same orientation (both rows)")
    public void testDot_ThrowsException_SameOrientation_BothRows() {
        // Arrange
        SharedVector v1 = createRow(1.0, 2.0, 3.0);
        SharedVector v2 = createRow(4.0, 5.0, 6.0);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> v1.dot(v2)
        );
        assertTrue(exception.getMessage().contains("different orientations"));
    }

    @Test
    @DisplayName("dot() - Throws exception for same orientation (both columns)")
    public void testDot_ThrowsException_SameOrientation_BothColumns() {
        // Arrange
        SharedVector v1 = createCol(1.0, 2.0, 3.0);
        SharedVector v2 = createCol(4.0, 5.0, 6.0);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> v1.dot(v2)
        );
        assertTrue(exception.getMessage().contains("different orientations"));
    }

    @Test
    @DisplayName("dot() - Throws exception for different lengths")
    public void testDot_ThrowsException_DifferentLengths() {
        // Arrange
        SharedVector row = createRow(1.0, 2.0, 3.0);
        SharedVector col = createCol(4.0, 5.0);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> row.dot(col)
        );
        assertTrue(exception.getMessage().contains("same length"));
    }

    @Test
    @DisplayName("dot() - Throws NullPointerException for null input")
    public void testDot_ThrowsNullPointerException() {
        // Arrange
        SharedVector row = createRow(1.0, 2.0, 3.0);

        // Act & Assert
        assertThrows(NullPointerException.class, () -> row.dot(null));
    }

    // ==================== Tests for vecMatMul() ====================

    @Test
    @DisplayName("vecMatMul() - Normal 3x3 matrix multiplication")
    public void testVecMatMul_NormalCase() {
        // Arrange: [1, 2, 3] × [[1, 2, 3],
        //                         [4, 5, 6],
        //                         [7, 8, 9]]ᵀ (column-major)
        SharedVector row = createRow(1.0, 2.0, 3.0);
        double[][] matrixData = {
            {1.0, 2.0, 3.0},
            {4.0, 5.0, 6.0},
            {7.0, 8.0, 9.0}
        };
        SharedMatrix matrix = createColumnMajorMatrix(matrixData);

        // Act
        row.vecMatMul(matrix);

        // Assert
        // Result[0] = row · col0 = [1,2,3]·[1,4,7] = 1+8+21 = 30
        // Result[1] = row · col1 = [1,2,3]·[2,5,8] = 2+10+24 = 36
        // Result[2] = row · col2 = [1,2,3]·[3,6,9] = 3+12+27 = 42
        assertVectorEquals(new double[]{30.0, 36.0, 42.0}, row);
        assertEquals(VectorOrientation.ROW_MAJOR, row.getOrientation());
    }

    @Test
    @DisplayName("vecMatMul() - Result vector length changes")
    public void testVecMatMul_ResultLengthChanges() {
        // Arrange: [2, 3, 4] × [[1, 5],
        //                          [2, 6],
        //                          [3, 7]]ᵀ (3 rows, 2 cols)
        // Vector length must match column height (3)
        SharedVector row = createRow(2.0, 3.0, 4.0);
        double[][] matrixData = {
            {1.0, 5.0},
            {2.0, 6.0},
            {3.0, 7.0}
        };
        SharedMatrix matrix = createColumnMajorMatrix(matrixData);

        // Act
        row.vecMatMul(matrix);

        // Assert
        // Result[0] = [2,3,4]·[1,2,3] = 2+6+12 = 20
        // Result[1] = [2,3,4]·[5,6,7] = 10+18+28 = 56
        assertEquals(2, row.length());
        assertVectorEquals(new double[]{20.0, 56.0}, row);
    }

    @Test
    @DisplayName("vecMatMul() - Single column matrix")
    public void testVecMatMul_SingleColumnMatrix() {
        // Arrange: [1, 2, 3] × [[5],
        //                         [6],
        //                         [7]]ᵀ (single column)
        SharedVector row = createRow(1.0, 2.0, 3.0);
        double[][] matrixData = {
            {5.0},
            {6.0},
            {7.0}
        };
        SharedMatrix matrix = createColumnMajorMatrix(matrixData);

        // Act
        row.vecMatMul(matrix);

        // Assert
        // Result[0] = [1,2,3]·[5,6,7] = 5+12+21 = 38
        assertEquals(1, row.length());
        assertVectorEquals(new double[]{38.0}, row);
    }

    @Test
    @DisplayName("vecMatMul() - With all zeros")
    public void testVecMatMul_WithZeros() {
        // Arrange
        SharedVector row = createRow(0.0, 0.0);
        double[][] matrixData = {
            {1.0, 2.0},
            {3.0, 4.0}
        };
        SharedMatrix matrix = createColumnMajorMatrix(matrixData);

        // Act
        row.vecMatMul(matrix);

        // Assert
        assertVectorEquals(new double[]{0.0, 0.0}, row);
    }

    @Test
    @DisplayName("vecMatMul() - With negative numbers")
    public void testVecMatMul_WithNegativeNumbers() {
        // Arrange
        SharedVector row = createRow(1.0, -2.0);
        double[][] matrixData = {
            {3.0, -1.0},
            {-4.0, 2.0}
        };
        SharedMatrix matrix = createColumnMajorMatrix(matrixData);

        // Act
        row.vecMatMul(matrix);

        // Assert
        // Result[0] = [1,-2]·[3,-4] = 3+8 = 11
        // Result[1] = [1,-2]·[-1,2] = -1-4 = -5
        assertVectorEquals(new double[]{11.0, -5.0}, row);
    }

    @Test
    @DisplayName("vecMatMul() - Throws exception when vector is not row-major")
    public void testVecMatMul_ThrowsException_NotRowMajor() {
        // Arrange
        SharedVector col = createCol(1.0, 2.0, 3.0);
        double[][] matrixData = {
            {1.0, 2.0, 3.0},
            {4.0, 5.0, 6.0},
            {7.0, 8.0, 9.0}
        };
        SharedMatrix matrix = createColumnMajorMatrix(matrixData);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> col.vecMatMul(matrix)
        );
        assertTrue(exception.getMessage().contains("row major"));
    }

    @Test
    public void testVecMatMul_ThrowsException_MatrixNotColumnMajor() {
        // Arrange
        SharedVector row = createRow(1.0, 2.0, 3.0);
        // Create a row-major matrix instead
        SharedMatrix matrix = new SharedMatrix(new double[][]{
            {1.0, 2.0, 3.0},
            {4.0, 5.0, 6.0},
            {7.0, 8.0, 9.0}
        });

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> row.vecMatMul(matrix)
        );
        assertTrue(exception.getMessage().contains("COLUMN_MAJOR"));
    }

    @Test
    public void testVecMatMul_ThrowsException_DimensionMismatch() {
        // Arrange: vector length is 2, but matrix column height is 3
        SharedVector row = createRow(1.0, 2.0);
        double[][] matrixData = {
            {1.0, 4.0},
            {2.0, 5.0},
            {3.0, 6.0}
        };
        SharedMatrix matrix = createColumnMajorMatrix(matrixData);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> row.vecMatMul(matrix)
        );
        assertTrue(exception.getMessage().contains("not definded") || 
                   exception.getMessage().contains("not defined"));
    }
}
