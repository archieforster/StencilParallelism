package Utils;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;

public class ThreadPool {

    private BlockingQueue<Runnable> run_queue;
    private ThreadFactory thread_factory;
    private Thread[] threads;

    public ThreadPool(ThreadFactory threadFactory, int max_threads) {
        this.thread_factory = threadFactory;

        // Create run queue
        this.run_queue = new LinkedBlockingDeque<>();
        // Create thread array/container
        this.threads = new Thread[max_threads];
    }

    public void addTask(Runnable task){
        run_queue.add(task);
    }

    public void executeAll(){
        // Utilise lazy eval - check all threads are inactive, then check queue
        while(threads_active() || !run_queue.isEmpty()){
            for(Thread thread : threads){
                // If thread is inactive, allocate & start new task
                if(!thread.isAlive()){
                    Runnable task = run_queue.poll();
                    // Ensure task is non-null (run_queue has been emptied)
                    // since last check
                    if(task != null){
                        thread = thread_factory.newThread(task);
                        thread.start();
                    }
                }
            }
        }
    }

    private boolean threads_active(){
        for (Thread thread : threads) {
            if (thread.isAlive()) {
                return true;
            }
        }
        return false;
    }




}
