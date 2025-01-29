package Processing;

import java.lang.Thread;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

import Patterns.Stencil;


public class BasicComputer {
    private ISLType isl_type;
    private Function condition_func;
    private int dim_divisor;
    private int max_loops;
    private SpaceHandler space_handler;
    private Stencil stencil;
    private Thread[] vthreads;
    private ConcurrentHashMap<Chunk,ChunkExecutionData> chunk_execution_data;
    private Chunk[] chunks;
    private ArrayList<Chunk> chunks_complete;
    private Lock lock = new ReentrantLock();

    public BasicComputer(FlatNumArray input_space, Stencil stencil) {
        space_handler = new SpaceHandler(input_space);
        this.stencil = stencil;
        dim_divisor = 1; // default divisor, no chunking
        // default to fixed ISL with one iteration
        isl_type = ISLType.FIXED_LOOP;
        max_loops = 1;
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

    public void setMaxLoops(int max_loops){
        this.max_loops = max_loops;
    }

    public void execute(){
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

    private void execute_with_pool(){

    }

    private void execute_fixed_loop(){
        ThreadFactory vthreadFactory = Thread.ofVirtual().name("stencil_vthread").factory();
        chunks_complete = new ArrayList<>();
        // Setup chunks & execution data
        setup_chunks();

        // Task per thread
        Consumer<Integer> task = (chunk_i) -> {
            // Get chunk & data
            Chunk chunk = chunks[chunk_i];
            ChunkExecutionData chunk_data = chunk_execution_data.get(chunk);

            // Loop until completed max iterations (non-inclusive as iterations
            // counted from 0
            for (int iteration = 0; iteration < max_loops; iteration++){
                // Spin until all neighbours have completed
                while (iteration > 0 && !check_nbrs_complete(chunk) ) {}

                // Execute over space
                execute_over_space(chunk.getStartPoint(),chunk.getChunkShape(), iteration);
                chunk_data.iters_complete++;
            }

            lock.lock();
            // Write results to output space
            space_handler.writeToOutput(chunk,chunk_data.iters_complete);
            chunks_complete.add(chunk);
            lock.unlock();
        };

        // Create VThread for each chunk
        vthreads = new Thread[chunks.length];
        for (int i = 0; i < chunks.length; i++) {
            int chunk_i = i;
            vthreads[i] = vthreadFactory.newThread((Runnable) () -> {
                task.accept(chunk_i);
            });
        }

        // Start each thread
        for (Thread thread : vthreads) {
            thread.start();
        }
        // Wait until all threads are complete
        while (!all_threads_complete()) {}

        for (Chunk c : chunks){
            System.out.println(Arrays.toString(c.getNeighbours().toArray()));
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
            FlatNumArray input_space = space_handler.getSpace(iteration);
            FlatNumArray output_space = space_handler.getSpace(iteration + 1);
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
