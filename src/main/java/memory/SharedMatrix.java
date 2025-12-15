package memory;

public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors

    public SharedMatrix() {
        // TODO: initialize empty matrix
        this.vectors = new SharedVector[0];
    }

    public SharedMatrix(double[][] matrix) {
        // TODO: construct matrix as row-major SharedVectors
        if (matrix == null) {
            throw new IllegalArgumentException("Input matrix is null.");
        }
        if (matrix.length == 0) {
            this.vectors = new SharedVector[0];
            return;
        }
        if (matrix[0] == null) {
            throw new IllegalArgumentException("All rows must have the same length.");
        }
        int rowSize= matrix[0].length;
        for ( int i = 1; i < matrix.length; i++ ) {
            if (matrix[i] == null || matrix[i].length != rowSize) {
                throw new IllegalArgumentException("All rows must have the same length.");
            }
        }
        vectors = new SharedVector[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            double[] clone = matrix[i].clone();
            vectors[i] = new SharedVector(clone, VectorOrientation.ROW_MAJOR);
        }
    }

    public void loadRowMajor(double[][] matrix) {
        // TODO: replace internal data with new row-major matrix
        if (matrix == null) {
            throw new IllegalArgumentException("Input matrix is null.");
        }
        if (matrix.length == 0) {
            this.vectors = new SharedVector[0];
            return;
        }
        if (matrix[0] == null) {
            throw new IllegalArgumentException("All rows must have the same length.");
        }
        int rowSize= matrix[0].length;
        for ( int i = 1; i < matrix.length; i++ ) {
            if (matrix[i] == null || matrix[i].length != rowSize) {
                throw new IllegalArgumentException("All rows must have the same length.");
            }
        }
        SharedVector[] newVectors = new SharedVector[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            double[] clone = matrix[i].clone();
            newVectors[i] = new SharedVector(clone, VectorOrientation.ROW_MAJOR);
        }
        this.vectors = newVectors;  
    }

    public void loadColumnMajor(double[][] matrix) {
        // TODO: replace internal data with new column-major matrix 
        if (matrix == null) {
            throw new IllegalArgumentException("Input matrix is null.");
        }  
        if (matrix.length == 0) {
            this.vectors = new SharedVector[0];
            return;
        }
        if (matrix[0] == null) {
            throw new IllegalArgumentException("All columns must have the same length.");
        }
        int colSize= matrix[0].length;
        for ( int i = 1; i < matrix.length; i++ ) {
            if (matrix[i] == null || matrix[i].length != colSize) {
                throw new IllegalArgumentException("All columns must have the same length.");
            }
        }
        SharedVector[] newVectors = new SharedVector[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            double[] clone = matrix[i].clone();
            newVectors[i] = new SharedVector(clone, VectorOrientation.COLUMN_MAJOR);
        }
        this.vectors = newVectors;

    }

    public double[][] readRowMajor() {
        // TODO: return matrix contents as a row-major double[][]
        SharedVector[] local = vectors;
        if (local.length == 0) {
            return new double[0][0];
        }
        acquireAllVectorReadLocks(local);
        try {
            VectorOrientation o = local[0].getOrientation();
            for (int i = 1; i < local.length; i++) {
                if (local[i].getOrientation() != o) {
                    throw new IllegalStateException("Inconsistent vector orientations in matrix");
                }
            }
            if (o == VectorOrientation.ROW_MAJOR) {
                int rows = local.length;
                int cols = local[0].length();

                for (int r = 1; r < rows; r++) {
                    if (local[r].length() != cols) {
                        throw new IllegalStateException("Inconsistent row lengths in matrix");
                    }
                }

                double[][] out = new double[rows][cols];
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        out[r][c] = local[r].get(c);
                    }
                }
                return out;
            } else {
                int cols = local.length;
                int rows = local[0].length();

                for (int c = 1; c < cols; c++) {
                    if (local[c].length() != rows) {
                        throw new IllegalStateException("Inconsistent column lengths in matrix");
                    }
                }

                double[][] out = new double[rows][cols];
                for (int c = 0; c < cols; c++) {
                    for (int r = 0; r < rows; r++) {
                        out[r][c] = local[c].get(r);
                    }
                }
                return out;
            }
        } finally {
            releaseAllVectorReadLocks(local);
        }

    }

    public SharedVector get(int index) {
        // TODO: return vector at index
        return vectors[index];
    }

    public int length() {
        // TODO: return number of stored vectors
        return vectors.length;
    }

    public VectorOrientation getOrientation() {
        // TODO: return orientation
        if (vectors.length == 0) {
            return null;  
        }
        return vectors[0].getOrientation();
    }

    private void acquireAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: acquire read lock for each vector
        for (SharedVector v: vecs) {
            v.readLock();
        }
    }

    private void releaseAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: release read locks
        for (SharedVector v: vecs) {
            v.readUnlock();
        }
    }

    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: acquire write lock for each vector
        for (SharedVector v: vecs) {
            v.writeLock();
        }
    }

    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: release write locks
        for (SharedVector v: vecs) {
            v.writeUnlock();
        }
    }
}