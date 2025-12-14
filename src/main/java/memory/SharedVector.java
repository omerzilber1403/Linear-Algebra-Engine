package memory;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;

public class SharedVector {

    private double[] vector;
    private VectorOrientation orientation;
    private ReadWriteLock lock = new java.util.concurrent.locks.ReentrantReadWriteLock();

    public SharedVector(double[] vector, VectorOrientation orientation) {
        // TODO: store vector data and its orientation
        this.vector = vector;
        this.orientation = orientation;
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
            for (int i = 0; i < vector.length; i++) {
                vector[i] += other.get(i);
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
            if (this.orientation != other.getOrientation()) {
                throw new IllegalArgumentException(
                    "Vectors must have the same orientation to add. " +
                    "This: " + this.orientation + ", Other: " + other.getOrientation());
            }
            double result = 0.0;
            for (int i = 0; i < vector.length; i++) {
                result += this.vector[i] * other.get(i);
            }
            return result;
        } finally {
            other.readUnlock();
            lock.readLock().unlock();
        }
    }

    public void vecMatMul(SharedMatrix matrix) {
        // TODO: compute row-vector × matrix
    }
}
