package memory;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;

public class SharedVector {

    private double[] vector; 
    private VectorOrientation orientation; 
    private ReadWriteLock lock = new java.util.concurrent.locks.ReentrantReadWriteLock();

    public SharedVector(double[] vector, VectorOrientation orientation) {
        // TODO: store vector data and its orientation
        if (vector == null) {
            throw new NullPointerException("vector is null");
        }
        this.vector = vector;
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

        // A read lock is required to prevent another thread
        // from modifying the vector while it is being read
        lock.readLock().lock();
        try {
            return vector[index];
        } finally {
            lock.readLock().unlock();
        }  
    }

    public int length() {
        // TODO: return vector length

        // A read lock is required to prevent another thread
        // from modifying the vector while its length is read
        lock.readLock().lock();
        try {   
            return vector.length;
        } finally {
            lock.readLock().unlock();
        }
    }

    public VectorOrientation getOrientation() {
        // TODO: return vector orientation

        // A read lock is required to prevent another thread
        // from changing the orientation while it is being read
        lock.readLock().lock();
        try {   
            return orientation;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void writeLock() {
        // TODO: acquire write lock

        // Allows a thread to acquire exclusive access
        // in order to perform multiple write operations safely
        lock.writeLock().lock();
    }

    public void writeUnlock() {
        // TODO: release write lock

        // Releases the write lock previously acquired by a thread
        lock.writeLock().unlock();
    }

    public void readLock() {
        // TODO: acquire read lock

        // Allows a thread to safely perform multiple
        // read operations without concurrent modification
        lock.readLock().lock();
    }

    public void readUnlock() {
        // TODO: release read lock

        // Releases the read lock previously acquired by a thread
        lock.readLock().unlock();
    }

    public void transpose() {
        // TODO: transpose vector

        // A write lock is required because this thread modifies
        // a shared field, and other threads must not access it concurrently
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

        // A write lock is required because this thread modifies
        // the current vector
        lock.writeLock().lock();
        // A read lock is required on the other vector to prevent
        // another thread from modifying it while its values are read
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
            // Locks are always released to avoid deadlock between threads
            other.readUnlock();
            lock.writeLock().unlock();
        }
    }

    public void negate() {
        // TODO: negate vector

        // A write lock is required because this thread
        // modifies all elements of the shared vector
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

        // Read locks are required to prevent either thread
        // from modifying the vectors while computing the dot product
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

        // A read lock is required to prevent another thread
        // from modifying the vector while the multiplication is computed
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
        // A write lock is required to prevent other threads
        // from accessing the vector while it is being replaced
        lock.writeLock().lock(); 
        try {
            this.vector = newVector;

        } finally {
            lock.writeLock().unlock();
        }
    }
}
