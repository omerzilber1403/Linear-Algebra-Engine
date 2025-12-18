package scheduling;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutor {

    private final TiredThread[] workers;
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);

    public TiredExecutor(int numThreads) {
        // TODO
        if (numThreads <= 0) {
            throw new IllegalArgumentException("Non positive number of threads");
        }
        workers = new TiredThread[numThreads]; // placeholder
        for (int i = 0; i < numThreads; i++) {
            workers[i] = new TiredThread(i, ThreadLocalRandom.current().nextDouble(0.5, 1.5));
            workers[i].start();
            idleMinHeap.add(workers[i]);
        }
    }

    public void submit(Runnable task) {
        // TODO
        if (task == null) {
            throw new IllegalArgumentException("task is null");
        }

        while (true) {
            final TiredThread worker;
            try {
                worker = idleMinHeap.take(); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("submit interrupted", e);
            }

            inFlight.incrementAndGet();

            Runnable wrapped = () -> {
                try {
                    task.run();
                } finally {
                    idleMinHeap.add(worker);

                    if (inFlight.decrementAndGet() == 0) {
                        synchronized (this) {
                            this.notifyAll();
                        }
                    }
                }
            };

            try {
                worker.newTask(wrapped);
                return;                 
            } catch (RuntimeException ex) {
                idleMinHeap.add(worker);
                if (inFlight.decrementAndGet() == 0) {
                    synchronized (this) {
                        this.notifyAll();
                    }
                }
                throw ex;
            }
        }
    }

    public void submitAll(Iterable<Runnable> tasks) {
        // TODO: submit tasks one by one and wait until all finish
        if (tasks == null) {
            throw new IllegalArgumentException("tasks is null");
        }

        for (Runnable t : tasks) {
            submit(t);
        }

        synchronized (this) {
            while (inFlight.get() > 0) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void shutdown() throws InterruptedException {
        // TODO
        for (TiredThread worker : workers) {
            worker.shutdown();
        }
        for (TiredThread worker : workers) {
            worker.join();
        }
        idleMinHeap.clear();
    }

    public synchronized String getWorkerReport() {
        // TODO: return readable statistics for each worker
        return null;
    }
}
