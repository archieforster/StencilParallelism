package Utils;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

public class ThreadPool {

    private static Queue<Runnable> run_queue;
    private final ThreadFactory thread_factory;
    private final Thread[] threads;
    private int active_threads;
    private final Lock active_threads_lock;
    private BiConsumer<Integer,Integer> recursive_task;

    public ThreadPool(ThreadFactory threadFactory, int max_threads) {
        this.thread_factory = threadFactory;

        // Create run queue
        run_queue = new LinkedBlockingDeque<>();
        // Init thread array/container
        this.threads = new Thread[max_threads];
        this.active_threads = 0;
        this.active_threads_lock = new ReentrantLock();

        // Init empty runnable for recursive task
        recursive_task = (x,y) -> {};
    }

    public void addTask(Runnable task){
        run_queue.add(task);
    }

    public void setRecursiveTask(BiConsumer<Integer,Integer> task){
        recursive_task = task;
    }
    public BiConsumer<Integer, Integer> getRecursiveTask(){
        return recursive_task;
    }

    /**
     * Blocking. Begins to execute all tasks in queue, will terminate once
     * all tasks have been executed. Tasks may be added to same queue during execution.
     */
    public void executeAll(){
        active_threads = 0;
        Runnable thread_task = () -> {
            // Poll queue whilst there are tasks or active threads
            while (!run_queue.isEmpty() || active_threads > 0){
                Runnable task = run_queue.poll();
                // Checks if poll returned a task
                if (task != null) {
                    // Safe increment of shared active_threads
                    active_threads_lock.lock();
                    active_threads++;
                    active_threads_lock.unlock();

                    task.run();

                    // Safe decrement of shared active_threads when task complete
                    active_threads_lock.lock();
                    active_threads--;
                    active_threads_lock.unlock();
                }
            }
        };

        for (int i = 0; i < threads.length; i++){
            threads[i] = thread_factory.newThread(thread_task);
            threads[i].start();
        }
    }

    private boolean threads_active(){
        for (Thread thread : threads) {
            if (thread != null && thread.isAlive()) {
                return true;
            }
        }
        return false;
    }




}
