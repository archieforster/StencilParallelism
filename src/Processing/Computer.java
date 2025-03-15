package Processing;

import java.lang.Thread;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
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
    private ThreadType thread_type;
    private Integer[] dim_divisor;
    private int iteration_count;
    private SpaceHandler space_handler;
    private Stencil stencil;
    private Thread[] threads;
    private ConcurrentHashMap<Chunk, ChunkExecutionData> chunk_execution_data;
    private Chunk[] chunks;
    private Semaphore complete_chunks;
    private Lock lock = new ReentrantLock();

    public Computer(FlatNumArray input_space, Stencil stencil) {
        space_handler = new SpaceHandler(input_space);
        this.stencil = stencil;
        dim_divisor = new Integer[stencil.getShape().length];// default divisor of 1 per dim, no chunking
        Arrays.fill(dim_divisor, 1);
        // default to fixed ISL with one iteration
        isl_type = ISLType.FIXED_LOOP;
        // default to vthread per chunk
        threadingMode = ThreadingMode.PER_CHUNK;
        // default 1 iteration
        iteration_count = 1;
        // default to virtual threads
        thread_type = ThreadType.VIRTUAL;
    }

    public void setInputSpace(FlatNumArray input_space){
        space_handler = new SpaceHandler(input_space);
    }

    public void setStencil(Stencil stencil){
        this.stencil = stencil;
    }

    public void setDimDivisor(Integer[] dim_divisor){this.dim_divisor = dim_divisor;
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

    public void setThreadType(ThreadType threadType){
        this.thread_type = threadType;
    }

    public FlatNumArray getOutput() {
        return space_handler.getOutputSpace();
    }

    public void execute(){
        // Setup chunks & execution data
        setup_chunks();
        if (isl_type == ISLType.FIXED_LOOP){
            execute_fixed_loop();
        }
        if (isl_type == ISLType.FIXED_LOOP_BORDERS){
            execute_fixed_loop_borders();
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
        complete_chunks = new Semaphore(0);
        for (Chunk chunk: chunks) {
            ChunkExecutionData chunk_data = new ChunkExecutionData();
            chunk_data.iters_complete = 0;
            chunk_data.nbrs_complete = new Semaphore[iteration_count];
            for (int i = 0; i < iteration_count; i++){
                if (i == 0){
                    // Init semaphore for first iteration with "all neighbours complete"
                    chunk_data.nbrs_complete[i] = new Semaphore(chunk.getNeighbours().size());
                } else {
                    // Rest of semaphores init to 0
                    chunk_data.nbrs_complete[i] = new Semaphore(0);
                }

            }
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
    private boolean check_nbrs_incomplete(Chunk chunk){
        ChunkExecutionData chunk_data = chunk_execution_data.get(chunk);

        // Determine if neighbours have completed last iteration, meaning they
        // are on the same iteration or ahead
        for (Chunk nbr : chunk.getNeighbours()){
            ChunkExecutionData nbr_data = chunk_execution_data.get(nbr);
            if (nbr_data.iters_complete < chunk_data.iters_complete) {
                return true;
            };
        }
        return false;
    }

    private ThreadFactory getThreadFactory(){
        switch (thread_type){
            case VIRTUAL:
                return Thread.ofVirtual().name("stencil_vthread").factory();
            case PLATFORM:
                return Thread.ofPlatform().name("stencil_platformthread").factory();
            default:
                // Default to virtual
                return Thread.ofVirtual().name("stencil_vthread").factory();
        }
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
                    for (int iteration = 1; iteration <= iteration_count; iteration++) {
                        // Use semaphores to wait until all nbrs are complete
                        try {
                            chunk_data.nbrs_complete[iteration - 1].acquire(chunk.getNeighbours().size());
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        // Execute over space
                        execute_over_space(chunk.getStartPoint(), chunk.getChunkShape(), iteration);
                        // Update execution data
                        synchronized (chunk_data) {
                            chunk_data.iters_complete++;
                            if (iteration < iteration_count) {
                                for (Chunk nbr: chunk.getNeighbours()) {
                                    ChunkExecutionData nbr_data = chunk_execution_data.get(nbr);
                                    nbr_data.nbrs_complete[iteration].release();
                                }
                            }
                        }
                    }

                    lock.lock();
                    // Write results to output space
                    space_handler.writeToOutput(chunk, chunk_data.iters_complete);
                    lock.unlock();
                    complete_chunks.release();
                };

                execute_with_chunk_threads(task);
                break;

            case POOL:
                // Creates Thread Pool
                ThreadFactory threadFactory = getThreadFactory();
                threadPool = new ThreadPool(threadFactory,max_threads);

                // Recursive task to be done by threads, creates & adds tasks to pool queue
                // Super task per thread, becomes threadPool attribute to enable recursion
                // Iteration reflects iteration number, e.g. 1 is task of doing first iteration
                BiConsumer<Integer, Integer> recursive_task = (chunk_index, iteration) -> {
                    Chunk chunk = chunks[chunk_index];
                    ChunkExecutionData chunk_data = chunk.getExecutionData();

                    // Puts task back into queue if neighbours are not complete
                    if (check_nbrs_incomplete(chunk) || iteration != chunk_data.iters_complete + 1) {
                        Runnable rec_task = () -> threadPool.getRecursiveTask().accept(chunk_index, iteration);
                        threadPool.addTask(rec_task);
                        return;
                    }
                    // Run for iteration
                    execute_over_space(chunk.getStartPoint(), chunk.getChunkShape(), iteration);
                    synchronized (chunk_data) {chunk_data.iters_complete++;}
                    // Add task for next iteration if needed
                    if (iteration < iteration_count) {
                        Runnable new_task = () -> threadPool.getRecursiveTask().accept(chunk_index, iteration + 1);
                        threadPool.addTask(new_task);
                    } else {
                        // Chunk must be complete, write result to output space
                        lock.lock();
                        space_handler.writeToOutput(chunk, chunk_data.iters_complete);
                        lock.unlock();
                        complete_chunks.release();
                    }
                };
                threadPool.setRecursiveTask(recursive_task);

                // Init task per chunk
                Consumer<Integer> init_task = (chunk_index) -> recursive_task.accept(chunk_index, 1);
                execute_with_pool(init_task);
                break;
        }
    }

    private void execute_fixed_loop_borders() {
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
                    for (int iteration = 1; iteration <= iteration_count; iteration++) {
                        // Do inner section immediately
                        execute_over_inner(chunk.getStartPoint(), chunk.getChunkShape(), iteration);
                        // Use semaphores to wait until all nbrs are complete
                        try {
                            chunk_data.nbrs_complete[iteration - 1].acquire(chunk.getNeighbours().size());
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        // Execute over borders once neighbours complete
                        execute_over_borders(chunk.getStartPoint(), chunk.getChunkShape(), iteration);
                        // Update execution data
                        synchronized (chunk_data){
                            chunk_data.iters_complete++;
                            if (iteration < iteration_count) {
                                for (Chunk nbr: chunk.getNeighbours()) {
                                    ChunkExecutionData nbr_data = chunk_execution_data.get(nbr);
                                    nbr_data.nbrs_complete[iteration].release();
                                }
                            }
                        }
                    }

                    lock.lock();
                    // Write results to output space
                    space_handler.writeToOutput(chunk, chunk_data.iters_complete);
                    lock.unlock();
                    complete_chunks.release();
                };

                execute_with_chunk_threads(task);
                break;

            case POOL:
                // Creates Thread Pool
                ThreadFactory threadFactory = getThreadFactory();
                threadPool = new ThreadPool(threadFactory,max_threads);

                // Recursive task to be done by threads, creates & adds tasks to pool queue
                // Super task per thread, becomes threadPool attribute to enable recursion
                // Iteration reflects iteration number, e.g. 1 is task of doing first iteration
                BiConsumer<Integer, Integer> recursive_task = (chunk_index, iteration) -> {
                    Chunk chunk = chunks[chunk_index];
                    ChunkExecutionData chunk_data = chunk.getExecutionData();

                    // Execute inner
                    if (!chunk_data.inner_complete){
                        execute_over_inner(chunk.getStartPoint(), chunk.getChunkShape(), iteration);
                        chunk_data.inner_complete = true;
                    }

                    // Puts task back into queue if neighbours are not complete
                    if (check_nbrs_incomplete(chunk) || iteration != chunk_data.iters_complete + 1) {
                        Runnable rec_task = () -> threadPool.getRecursiveTask().accept(chunk_index, iteration);
                        threadPool.addTask(rec_task);
                        return;
                    }
                    // Run over borders for iteration
                    execute_over_borders(chunk.getStartPoint(), chunk.getChunkShape(), iteration);
                    // Update chunk data
                    synchronized (chunk_data) {
                        chunk_data.iters_complete++;
                        chunk_data.inner_complete = false;
                    }
                    // Add task for next iteration if needed
                    if (iteration < iteration_count) {
                        Runnable new_task = () -> threadPool.getRecursiveTask().accept(chunk_index, iteration + 1);
                        threadPool.addTask(new_task);
                    } else {
                        // Chunk must be complete, write result to output space
                        lock.lock();
                        space_handler.writeToOutput(chunk, chunk_data.iters_complete);
                        lock.unlock();
                        complete_chunks.release();
                    }
                };
                threadPool.setRecursiveTask(recursive_task);

                // Init task per chunk
                Consumer<Integer> init_task = (chunk_index) -> recursive_task.accept(chunk_index, 1);
                execute_with_pool(init_task);
                break;
        }
    }

    /**
     * Blocking. Creates thread pool and initialises input task per chunk into run queue
     * @param chunk_task Task taking chunk index as a parameter
     */
    private void execute_with_pool(Consumer<Integer> chunk_task){
        // Adds tasks for all chunks
        for (int i = 0; i < chunks.length; i++) {
            int chunk_i = i;
            threadPool.addTask( () -> chunk_task.accept(chunk_i) );
        }

        // Starts execution through ThreadPool interface
        threadPool.executeAll();
        try {
            complete_chunks.acquire(chunks.length);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Blocking. Executes task per chunk with a thread assigned per chunk.
     * @param chunk_task Task assigned to each chunk, task uses chunk index as a parameter
     */
    private void execute_with_chunk_threads(Consumer<Integer> chunk_task){
        ThreadFactory threadFactory = getThreadFactory();

        // Create VThread for each chunk
        threads = new Thread[chunks.length];
        for (int i = 0; i < chunks.length; i++) {
            int chunk_i = i;
            threads[i] = threadFactory.newThread( () -> chunk_task.accept(chunk_i) );
        }

        // Start each thread
        for (Thread thread : threads) {
            thread.start();
        }
        // Wait until all threads are complete
        try {
            complete_chunks.acquire(chunks.length);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

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

    private void execute_over_inner(Integer[] sp, Integer[] subspace_shape, int iteration){
        // Map to new execute_over_space by changing sp and subspace_shape
        Integer[] stencil_shape = stencil.getShape();

        Integer[] new_sp = sp.clone();
        for (int i = 0; i < sp.length; i++){
            // Move point inwards according to stencil shape
            new_sp[i] += stencil_shape[i];
        }

        Integer[] new_subspace_shape = subspace_shape.clone();
        for (int i = 0; i < subspace_shape.length; i++){
            // Move inwards by stencil shape in both directions per dimension
            new_subspace_shape[i] -= 2 * stencil_shape[i];
        }

        // Use execute over space
        execute_over_space(new_sp, new_subspace_shape, iteration);

    }

    private void execute_over_borders(Integer[] sp, Integer[] subspace_shape, int iteration){
        int dimN = subspace_shape.length;

        if (dimN == 1){
            execute_over_borders_1d(sp, subspace_shape, iteration);
        }
        if (dimN == 2){
            execute_over_borders_2d(sp, subspace_shape, iteration);
        }
        if (dimN == 3){
            execute_over_borders_3d(sp, subspace_shape, iteration);
        }
    }

    private void execute_over_borders_1d(Integer[] sp, Integer[] subspace_shape, int iteration){
        Integer[] stencil_shape = stencil.getShape();
        Integer[] sp1 = sp.clone();
        Integer[] sp2 = new Integer[sp[0] + subspace_shape[0] - stencil_shape[0]];
        for (Integer[] new_sp : new Integer[][] {sp1, sp2}){
            for (int dx = 0; dx < stencil_shape[0]; dx++){
                Integer[] p = new Integer[] {new_sp[0] + dx};
                // Apply stencil and store result using space_handler
                FlatNumArray input_space = space_handler.getSpace(iteration - 1);
                FlatNumArray output_space = space_handler.getSpace(iteration);
                Number app = stencil.apply(p,input_space);
                output_space.set(p,app);
            }
        }
    }

    private void execute_over_borders_2d(Integer[] sp, Integer[] subspace_shape, int iteration){
        Integer[] stencil_shape = stencil.getShape();
        // Do top and bottom borders with full x
        // sp1 = [Ox,Oy]
        Integer[] sp1 = sp.clone();
        // sp2 = [0x,Oy+cx-ky]
        Integer[] sp2 = new Integer[]{
                sp[0],
                sp[1] + subspace_shape[1] - stencil_shape[1]
        };
        for (Integer[] new_sp : new Integer[][] {sp1, sp2}){
            for (int dx = 0; dx < subspace_shape[0]; dx++){
                for (int dy = 0; dy < stencil_shape[1]; dy++){
                    Integer[] p = new Integer[]{
                            new_sp[0] + dx,
                            new_sp[1] + dy
                    };
                    // Apply stencil and store result using space_handler
                    FlatNumArray input_space = space_handler.getSpace(iteration - 1);
                    FlatNumArray output_space = space_handler.getSpace(iteration);
                    Number app = stencil.apply(p,input_space);
                    output_space.set(p,app);
                }
            }
        }

        // Do side borders
        // sp3 = [Ox,Oy+ky]
        Integer[] sp3 = new Integer[]{
                sp[0],
                sp[1] + stencil_shape[1]
        };
        // sp4 = [0x+cx-kx,Oy+ky]
        Integer[] sp4 = new Integer[]{
                sp[0] + subspace_shape[0] - stencil_shape[0],
                sp[1] + stencil_shape[1]
        };
        for (Integer[] new_sp : new Integer[][] {sp3, sp4}){
            for (int dx = 0; dx < stencil_shape[0]; dx++){
                for (int dy = 0; dy < subspace_shape[1] - 2*stencil_shape[1] ; dy++){
                    Integer[] p = new Integer[]{
                            new_sp[0] + dx,
                            new_sp[1] + dy
                    };
                    // Apply stencil and store result using space_handler
                    FlatNumArray input_space = space_handler.getSpace(iteration - 1);
                    FlatNumArray output_space = space_handler.getSpace(iteration);
                    Number app = stencil.apply(p,input_space);
                    output_space.set(p,app);
                }
            }
        }
    }

    private void execute_over_borders_3d(Integer[] sp, Integer[] subspace_shape, int iteration){
        Integer[] stencil_shape = stencil.getShape();
        // Top 2 sides across full x and z
        // sp1 = [Ox,Oy,Oz]
        Integer[] sp1 = sp.clone();
        // sp2 = [Ox,Oy+cy-ky,Oz]
        Integer[] sp2 = new Integer[]{
                sp[0],
                sp[1] + subspace_shape[1] - stencil_shape[1],
                sp[2]
        };
        for (Integer[] new_sp : new Integer[][] {sp1, sp2}){
            for (int dx = 0; dx < subspace_shape[0]; dx++){
                for (int dy = 0; dy < stencil_shape[1]; dy++){
                    for (int dz = 0; dz < subspace_shape[2]; dz++){
                        Integer[] p = new Integer[]{
                                new_sp[0] + dx,
                                new_sp[1] + dy,
                                new_sp[2] + dz
                        };
                        // Apply stencil and store result using space_handler
                        FlatNumArray input_space = space_handler.getSpace(iteration - 1);
                        FlatNumArray output_space = space_handler.getSpace(iteration);
                        Number app = stencil.apply(p,input_space);
                        output_space.set(p,app);
                    }
                }
            }
        }
        // Side 2 across full z
        // sp3 = [Ox,Oy+ky,Oz]
        Integer[] sp3 = new Integer[]{
                sp[0],
                sp[1] + stencil_shape[1],
                sp[2]
        };
        // sp4 = [Ox+cx-kx,Oy+ky,Oz]
        Integer[] sp4 = new Integer[]{
                sp[0] + subspace_shape[0] - stencil_shape[0],
                sp[1] + stencil_shape[1],
                sp[2]
        };
        for (Integer[] new_sp : new Integer[][] {sp3, sp4}){
            for (int dx = 0; dx < stencil_shape[0]; dx++){
                for (int dy = 0; dy < subspace_shape[1] - 2*stencil_shape[1]; dy++){
                    for (int dz = 0; dz < subspace_shape[2]; dz++){
                        Integer[] p = new Integer[]{
                                new_sp[0] + dx,
                                new_sp[1] + dy,
                                new_sp[2] + dz
                        };
                        // Apply stencil and store result using space_handler
                        FlatNumArray input_space = space_handler.getSpace(iteration - 1);
                        FlatNumArray output_space = space_handler.getSpace(iteration);
                        Number app = stencil.apply(p,input_space);
                        output_space.set(p,app);
                    }
                }
            }
        }
        // Front and Back sides, partial x,y and z
        // sp5 = [0x+kx,Oy+ky,Oz]
        Integer[] sp5 = new Integer[]{
                sp[0] + stencil_shape[0],
                sp[1] + stencil_shape[1],
                sp[2]
        };
        // sp6 = [Ox+kx,Oy+ky,Oz+cz-kz]
        Integer[] sp6 = new Integer[]{
                sp[0] + stencil_shape[0],
                sp[1] + stencil_shape[1],
                sp[2] + subspace_shape[2] - stencil_shape[2],
        };
        for (Integer[] new_sp : new Integer[][] {sp5, sp6}){
            for (int dx = 0; dx < subspace_shape[0] - 2*stencil_shape[0]; dx++){
                for (int dy = 0; dy < subspace_shape[1] - 2*stencil_shape[1]; dy++){
                    for (int dz = 0; dz < stencil_shape[2]; dz++){
                        Integer[] p = new Integer[]{
                                new_sp[0] + dx,
                                new_sp[1] + dy,
                                new_sp[2] + dz
                        };
                        // Apply stencil and store result using space_handler
                        FlatNumArray input_space = space_handler.getSpace(iteration - 1);
                        FlatNumArray output_space = space_handler.getSpace(iteration);
                        Number app = stencil.apply(p,input_space);
                        output_space.set(p,app);
                    }
                }
            }
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
