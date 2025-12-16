package scheduling;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class TiredThread extends Thread implements Comparable<TiredThread> {

    private static final Runnable POISON_PILL = () -> {}; // Special task to signal shutdown

    private final int id; // Worker index assigned by the executor
    private final double fatigueFactor; // Multiplier for fatigue calculation

    private final AtomicBoolean alive = new AtomicBoolean(true); // Indicates if the worker should keep running

    // Single-slot handoff queue; executor will put tasks here
    private final BlockingQueue<Runnable> handoff = new ArrayBlockingQueue<>(1);

    private final AtomicBoolean busy = new AtomicBoolean(false); // Indicates if the worker is currently executing a task

    private final AtomicLong timeUsed = new AtomicLong(0); // Total time spent executing tasks
    private final AtomicLong timeIdle = new AtomicLong(0); // Total time spent idle
    private final AtomicLong idleStartTime = new AtomicLong(0); // Timestamp when the worker became idle

    public TiredThread(int id, double fatigueFactor) {
        this.id = id;
        this.fatigueFactor = fatigueFactor;
        this.idleStartTime.set(System.nanoTime());
        setName(String.format("FF=%.2f", fatigueFactor));
    }

    public int getWorkerId() {
        return id;
    }

    public double getFatigue() {
        return fatigueFactor * timeUsed.get();
    }

    public boolean isBusy() {
        return busy.get();
    }

    public long getTimeUsed() {
        return timeUsed.get();
    }

    public long getTimeIdle() {
        return timeIdle.get();
    }

    /**
     * Assign a task to this worker.
     * This method is non-blocking: if the worker is not ready to accept a task,
     * it throws IllegalStateException.
     */
    public void newTask(Runnable task) {
       // TODO
       if (task == null) {
        throw new IllegalArgumentException("No task to execute");
       }
       if (!alive.get()) {
        throw new IllegalStateException("Worker is shutting down or has stopped");
       }
       if (busy.get()) {
        throw new IllegalStateException("Worker is busy");
       }
       boolean flag = handoff.offer(task);
       if (!flag) {
        throw new IllegalStateException("Worker already assigned to a task");
       }
    }

    /**
     * Request this worker to stop after finishing current task.
     * Inserts a poison pill so the worker wakes up and exits.
     */
    public void shutdown() {
       // TODO
       if (!alive.compareAndSet(true, false)){
        return;
       }
       handoff.offer(POISON_PILL);
       interrupt();
    }

    @Override
    public void run() {
        // Start as idle
        idleStartTime.set(System.nanoTime());

        while (true) {
            try {
                // Block until a task is available
                Runnable task = handoff.take();

                // If we got a poison pill or shutdown was requested, exit
                if (task == POISON_PILL || !alive.get()) {
                    break;
                }

                // Transition to busy
                busy.set(true);

                // Account for idle time up to now
                long idleStarted = idleStartTime.getAndSet(0);
                if (idleStarted != 0) {
                    timeIdle.addAndGet(System.nanoTime() - idleStarted);
                }

                // Execute task and account for used time
                long start = System.nanoTime();
                try {
                    task.run();
                } finally {
                    long used = System.nanoTime() - start;
                    timeUsed.addAndGet(used);

                    // Transition back to idle
                    busy.set(false);
                    idleStartTime.set(System.nanoTime());
                }
            } catch (InterruptedException ie) {
                // If interrupted during shutdown, exit; otherwise keep going
                if (!alive.get()) {
                    break;
                }
            }
        }

        // Finalize idle accounting if we exit while idle
        if (!busy.get()) {
            long idleStarted = idleStartTime.getAndSet(0);
            if (idleStarted != 0) {
                timeIdle.addAndGet(System.nanoTime() - idleStarted);
            }
        }
    }

    @Override
    public int compareTo(TiredThread o) {
        //TODO
        return Double.compare(fatigueFactor, o.getFatigue());
    }
}