package Processing;

import java.lang.Thread;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import Chunking.Chunk;
import Chunking.ChunkExecutionData;
import Chunking.Chunker;
import Patterns.Stencil;
import Utils.FlatNumArray;
import Utils.ThreadPool;


public class Computer {
    private ISLType isl_type;
    private ThreadingMode threadingMode;
    private Function<Collection,Boolean> condition_func;
    private ThreadPool threadPool;
    private int max_threads;
    private int dim_divisor;
    private int iteration_count;
    private SpaceHandler space_handler;
    private Stencil stencil;
    private Thread[] vthreads;
    private ConcurrentHashMap<Chunk, ChunkExecutionData> chunk_execution_data;
    private Chunk[] chunks;
    private Lock lock = new ReentrantLock();

    public Computer(FlatNumArray input_space, Stencil stencil) {
        space_handler = new SpaceHandler(input_space);
        this.stencil = stencil;
        dim_divisor = 1; // default divisor, no chunking
        // default to fixed ISL with one iteration
        isl_type = ISLType.FIXED_LOOP;
        // default to vthread per chunk
        threadingMode = ThreadingMode.PER_CHUNK;
        iteration_count = 1;
    }

    public void setInputSpace(FlatNumArray input_space){
        space_handler = new SpaceHandler(input_space);
    }

    public void setStencil(Stencil stencil){
        this.stencil = stencil;
    }

    public void setDimDivisor(int dim_divisor){
        this.dim_divisor = dim_divisor;
    }

    public void setISLType(ISLType isl_type){
        this.isl_type = isl_type;
    }
    public void setISLType(ISLType isl_type, Function condition_func){
        this.isl_type = isl_type;
        this.condition_func = condition_func;
    }

    public void setThreadingMode(ThreadingMode threadingMode){
        this.threadingMode = threadingMode;
    }
    public void setThreadingMode(ThreadingMode threadingMode, int max_threads){
        this.threadingMode = threadingMode;
        this.max_threads = max_threads;
    }

    public void setMaxLoops(int max_loops){
        this.iteration_count = max_loops;
    }

    public void execute(){
        // Setup chunks & execution data
        setup_chunks();
        if (isl_type == ISLType.FIXED_LOOP){
            execute_fixed_loop();
        }
    }

    /**
     * Creates chunks for the input space and creates data structure
     * for chunk execution data
     */
    private void setup_chunks(){
        Integer[] space_shape = space_handler.getSpace(0).getShape();
        Chunker chunker = new Chunker(space_shape,dim_divisor);
        chunks = chunker.getRegularChunks();
        chunk_execution_data = new ConcurrentHashMap<>();
        for (Chunk chunk: chunks) {
            ChunkExecutionData chunk_data = new ChunkExecutionData();
            chunk_data.iters_complete = 0;
            chunk_data.nbrs_complete = 0;
            chunk.setExecutionData(chunk_data);
            chunk_execution_data.put(chunk, chunk_data);
        }
    }

    /**
     * Checks if neighbouring chunks have completed their previous iteration.
     * E.g. if chunk A has completed 4 iterations, returns true if all neighbours
     * have also completed 4 iterations, returns false otherwise
     * @param chunk
     * @return
     */
    private boolean check_nbrs_complete(Chunk chunk){
        ChunkExecutionData chunk_data = chunk_execution_data.get(chunk);

        // Determine if neighbours have completed last iteration, meaning they
        // are on the same iteration or ahead
        for (Chunk nbr : chunk.getNeighbours()){
            ChunkExecutionData nbr_data = chunk_execution_data.get(nbr);
            if (nbr_data.iters_complete < chunk_data.iters_complete) {
                return false;
            };
        }
        return true;
    }

    /**
     * Blocking. Creates thread pool and initialises input task per chunk into run queue
     * @param chunk_task Task taking chunk index as a parameter
     */
    private void execute_with_pool(Consumer <Integer> chunk_task){
        // Adds tasks for all chunks
        for (int i = 0; i < chunks.length; i++) {
            int chunk_i = i;
            threadPool.addTask( () -> chunk_task.accept(chunk_i) );
        }

        // Starts execution through ThreadPool interface
        threadPool.executeAll();
    }

    /**
     * Blocking. Executes task per chunk with a thread assigned per chunk.
     * @param chunk_task Task assigned to each chunk, task uses chunk index as a parameter
     */
    private void execute_with_chunk_threads(Consumer<Integer> chunk_task){
        ThreadFactory vthreadFactory = Thread.ofVirtual().name("stencil_vthread").factory();

        // Create VThread for each chunk
        vthreads = new Thread[chunks.length];
        for (int i = 0; i < chunks.length; i++) {
            int chunk_i = i;
            vthreads[i] = vthreadFactory.newThread( () -> chunk_task.accept(chunk_i) );
        }

        // Start each thread
        for (Thread thread : vthreads) {
            thread.start();
        }
        // Wait until all threads are complete
        while (!all_threads_complete()) {}

    }

    /**
     * Executes fixed loop ISL
     */
    private void execute_fixed_loop() {
        // Task per thread, takes chunk index as parameter
        Consumer<Integer> task;

        switch (threadingMode) {
            case PER_CHUNK:
                // Task per thread, each assigned a chunk to work on
                task = (chunk_i) -> {
                    // Get chunk & data
                    Chunk chunk = chunks[chunk_i];
                    ChunkExecutionData chunk_data = chunk_execution_data.get(chunk);

                    // Loop until completed max iterations (non-inclusive as iterations
                    // counted from 0
                    for (int iteration = 0; iteration < iteration_count; iteration++) {
                        // Spin until all neighbours have completed
                        while (iteration > 0 && !check_nbrs_complete(chunk)) {}

                        // Execute over space
                        execute_over_space(chunk.getStartPoint(), chunk.getChunkShape(), iteration);
                        chunk_data.iters_complete++;
                    }

                    lock.lock();
                    // Write results to output space
                    space_handler.writeToOutput(chunk, chunk_data.iters_complete);
                    lock.unlock();
                };

                execute_with_chunk_threads(task);
                break;

            case POOL:
                // Creates Thread Pool
                ThreadFactory vthreadFactory = Thread.ofVirtual().name("stencil_vthread").factory();
                threadPool = new ThreadPool(vthreadFactory,max_threads);

                // Recursive task to be done by threads, creates & adds tasks to pool queue
                // Super task per thread, becomes threadPool attribute to enable recursion
                // Iteration reflects iteration number, e.g. 1 is task of doing first iteration
                BiConsumer<Integer, Integer> recursive_task = (chunk_index, iteration) -> {
                    Chunk chunk = chunks[chunk_index];
                    ChunkExecutionData chunk_data = chunk_execution_data.get(chunk);

                    // Puts task back into queue if neighbours are not complete
                    if (!check_nbrs_complete(chunk)){
                        Runnable rec_task = () -> threadPool.getRecursiveTask().accept(chunk_index, iteration);
                        threadPool.addTask(rec_task);
                        return;
                    }
                    // Run for iteration
                    execute_over_space(chunk.getStartPoint(), chunk.getChunkShape(), iteration);
                    chunk_data.iters_complete++;
                    // Add task for next iteration if needed
                    if (iteration <= iteration_count) {
                        Runnable rec_task = () -> threadPool.getRecursiveTask().accept(chunk_index, iteration + 1);
                        threadPool.addTask(rec_task);
                    } else {
                        // Chunk must be complete, write result to output space
                        lock.lock();
                        space_handler.writeToOutput(chunk, chunk_data.iters_complete);
                        lock.unlock();
                    }
                };
                threadPool.setRecursiveTask(recursive_task);

                // Init task per chunk
                Consumer<Integer> init_task = (chunk_index) -> recursive_task.accept(chunk_index, 1);
                execute_with_pool(init_task);
                break;
        }
    }

    public FlatNumArray getOutput() {
        return space_handler.getOutputSpace();
    }

    private boolean all_threads_complete(){
        for (Thread thread : vthreads) {
            if (thread.isAlive()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Applies stencil to all points in a specified subspace from a given point.
     * Applies to points sp + [0,...,0], sp + [0,...,1], ..., sp + [subspace_shape - (1)]
     * @param sp Starting point as Integer[]
     * @param subspace_shape Shape of subspace as Integer[]
     */
    private void execute_over_space(Integer[] sp, Integer[] subspace_shape, int iteration){
        int dimN = subspace_shape.length;
        Integer[] dp = new Integer[dimN];
        Integer[] p = new Integer[dimN];

        // Calculate products for dimension sizes, work backwards
        // e.g. subspace_shape = [2,5,6] -> products = [30,6,1]
        // Used to "mod" a counter into vector e.g. counter = 21 -> [0,3,3] since 21 = 0*30 + 3*6 + 3*1
        int[] products = new int[dimN];
        products[dimN-1] = 1;
        for (int i = dimN - 2; i >= 0; i--){
            products[i] = subspace_shape[i+1] * products[i+1];
        }

        // Iterate over space
        int point_counter = 0;
        while (!check_zero_array(dp) || point_counter == 0 || point_counter == 1){
            // Calculates vector to next point
            for (int i = 0; i < dimN; i++){
                dp[i] = Math.floorDiv(point_counter,products[i]) % subspace_shape[i];
            }
            // Calculate actual point
            for (int i = 0; i < dimN; i++){
                p[i] = sp[i] + dp[i];
            }
            // Apply stencil and store result using space_handler
            FlatNumArray input_space = space_handler.getSpace(iteration - 1);
            FlatNumArray output_space = space_handler.getSpace(iteration);
            Number app = stencil.apply(p,input_space);
            output_space.set(p,app);
            // Iterate to next point
            point_counter++;
        }
    }

    /**
     * Checks if numerical array is entirely zeros
     * @param arr
     * @return Boolean whether all elements are 0
     */
    private boolean check_zero_array(Number[] arr){
        // Return false if uninitialised
        if (arr[0] == null) return false;
        for (Number e : arr){
            if (e.doubleValue() != 0) return false;
        }
        return true;
    }

}
