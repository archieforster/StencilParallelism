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
            while (!run_queue.isEmpty() || active_threads > 0){
                Runnable task = run_queue.poll();
                if (task != null) {
                    active_threads_lock.lock();
                    active_threads++;
                    active_threads_lock.unlock();

                    task.run();

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

//        // Utilise lazy eval:
//        // 1) Check if threads is not initialised (i.e. not started)
//        // 2) Check all threads are inactive (i.e. running processes)
//        // 3) Check contents of queue (i.e. if all processes are complete)
//        while(threads[0] != null || !run_queue.isEmpty() || threads_active()){
//            for(int i = 0; i < threads.length; i++){
//                // If thread is inactive or uninitialised, allocate & start new task
//                if(threads[i] == null || !threads[i].isAlive()){
//                    Runnable task = run_queue.poll();
//                    // Ensure task is non-null (run_queue has been emptied)
//                    // since last check
//                    if(task != null){
//                        threads[i] = thread_factory.newThread(task);
//                        threads[i].start();
//                    }
//                }
//            }
//        }
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
