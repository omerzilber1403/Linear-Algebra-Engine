package memory;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;

public class SharedVector {

    private double[] vector; // [1,2,3]
    private VectorOrientation orientation; // COLUMN_MAJOR
    private ReadWriteLock lock = new java.util.concurrent.locks.ReentrantReadWriteLock();

    public SharedVector(double[] vector, VectorOrientation orientation) {
        // TODO: store vector data and its orientation
        if (vector == null) {
            throw new NullPointerException("vector is null");
        }
        this.vector = vector.clone();
        if (orientation == null) {
            throw new IllegalArgumentException("Orientation cannot be null.");
        }
        if (orientation != VectorOrientation.ROW_MAJOR && orientation != VectorOrientation.COLUMN_MAJOR) {
            throw new IllegalArgumentException("Invalid orientation.");
        }
        if (orientation == VectorOrientation.ROW_MAJOR) {
            this.orientation = VectorOrientation.ROW_MAJOR;
        } else {
            this.orientation = VectorOrientation.COLUMN_MAJOR;
        }
    }

    public double get(int index) {
        // TODO: return element at index (read-locked)
        lock.readLock().lock();
        try {
            return vector[index];
        } finally {
            lock.readLock().unlock();
        }  
    }

    public int length() {
        // TODO: return vector length
        lock.readLock().lock();
        try {   
            return vector.length;
        } finally {
            lock.readLock().unlock();
        }
    }

    public VectorOrientation getOrientation() {
        // TODO: return vector orientation
        lock.readLock().lock();
        try {   
            return orientation;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void writeLock() {
        // TODO: acquire write lock
        lock.writeLock().lock();
    }

    public void writeUnlock() {
        // TODO: release write lock
        lock.writeLock().unlock();
    }

    public void readLock() {
        // TODO: acquire read lock
        lock.readLock().lock();
    }

    public void readUnlock() {
        // TODO: release read lock
        lock.readLock().unlock();
    }

    public void transpose() {
        // TODO: transpose vector
        lock.writeLock().lock();
        try {
            if (orientation == VectorOrientation.ROW_MAJOR) {
                orientation = VectorOrientation.COLUMN_MAJOR;
            } else {
                orientation = VectorOrientation.ROW_MAJOR;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    public void add(SharedVector other) {
        // TODO: add two vectors
        lock.writeLock().lock();
        other.readLock();
        try {
            if (other.length() != this.length()) {
                throw new IllegalArgumentException("Vectors must be of the same length to add.");
            }
            if (this.orientation != other.getOrientation()) {
                throw new IllegalArgumentException(
                    "Vectors must have the same orientation to add. " +
                    "This: " + this.orientation + ", Other: " + other.getOrientation()
                );
            }
            for (int i = 0; i < vector.length; i++) {
                vector[i] += other.vector[i];
            }
        } finally {
            other.readUnlock();
            lock.writeLock().unlock();
        }
    }

    public void negate() {
        // TODO: negate vector
        lock.writeLock().lock();
        try {
            if (vector == null) {
                throw new NullPointerException("vector is null");
            }
            for (int i = 0; i < vector.length; i++) {
                vector[i] = -vector[i];
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public double dot(SharedVector other) {
        // TODO: compute dot product (row · column)
        lock.readLock().lock();
        other.readLock();  
        try {
            if (other.length() != this.length()) {
                throw new IllegalArgumentException("Vectors must be of the same length to compute dot product.");
            }
            if (this.orientation == other.getOrientation()) {
                throw new IllegalArgumentException("Vectors must be of different orientations");
            }
            double result = 0.0;
            for (int i = 0; i < vector.length; i++) {
                result += this.vector[i] * other.vector[i];
            }
            return result;
        } finally {
            other.readUnlock();
            lock.readLock().unlock();
        }
    }

    public void vecMatMul(SharedMatrix matrix) {
        // TODO: compute row-vector × matrix
        lock.readLock().lock();
        double[] newVector;
        try {
            if (orientation != VectorOrientation.ROW_MAJOR ) {
                throw new IllegalArgumentException ("vector is not a row major");
            }  
            if (matrix.getOrientation() != VectorOrientation.COLUMN_MAJOR) {
                throw new IllegalArgumentException("vecMatMul supports only COLUMN_MAJOR matrices");
            }
            if (matrix.length() ==0 ) {
                throw new IllegalArgumentException("matrix is empty");
            }
            if (matrix.get(0).length() != length()) {
                throw new IllegalArgumentException("the multuply is not definded");
            }
            newVector = new double[matrix.length()];
            for(int i = 0; i < matrix.length(); i++){
                newVector[i] = this.dot(matrix.get(i)); 
            }
        } finally {
            lock.readLock().unlock();
        }
        lock.writeLock().lock(); 
        try {
            this.vector = newVector;

        } finally {
            lock.writeLock().unlock();
        }
    }
}
