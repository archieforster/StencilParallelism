package Processing;

import java.lang.Thread;
import java.util.Arrays;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import Patterns.Stencil;

public class BasicComputer {
    private FlatNumArray input_space;
    private FlatNumArray output_space;
    private Stencil stencil;
    private Thread[] vthreads;
    private int dim_divisor;
    private int vthreads_complete;
    private Lock lock = new ReentrantLock();

    public BasicComputer(FlatNumArray input_space, Stencil stencil) {
        this.input_space = input_space;
        output_space = new FlatNumArray(input_space.getShape());
        this.stencil = stencil;
        dim_divisor = 1; // default divisor, no chunking
    }

    public void setInputSpace(FlatNumArray input_space){
        this.input_space = input_space;
        output_space = new FlatNumArray(input_space.getShape());
    }

    public void setStencil(Stencil stencil){
        this.stencil = stencil;
    }

    public void setDimDivisor(int dim_divisor){
        this.dim_divisor = dim_divisor;
    }

    public void execute(){
        ThreadFactory vthreadFactory = Thread.ofVirtual().name("stencil_vthread").factory();
        SpaceChunker chunker = new SpaceChunker(input_space.getShape(),dim_divisor);
        Chunk[] chunks = chunker.getRegularChunks();

        // Create VThread for each chunk
        vthreads = new Thread[chunks.length];
        for (int i = 0; i < chunks.length; i++) {
            Chunk chunk = chunks[i];
            Runnable task = () -> {
                execute_over_space(chunk.getStartPoint(),chunk.getChunkShape());
                lock.lock();
                vthreads_complete++;
                lock.unlock();
            };
            vthreads[i] = vthreadFactory.newThread(task);
        }

        // Start each thread
        for (Thread thread : vthreads) {
            thread.start();
        }
        // Wait until all threads are complete
        while (vthreads_complete < vthreads.length) {}
    }

    public FlatNumArray getOutput() {
        return output_space;
    }

    /**
     * Applies stencil to all points in a specified subspace from a given point.
     * Applies to points sp + [0,...,0], sp + [0,...,1], ..., sp + [subspace_shape - (1)]
     * @param sp Starting point as Integer[]
     * @param subspace_shape Shape of subspace as Integer[]
     */
    private void execute_over_space(Integer[] sp, Integer[] subspace_shape){
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
        while (!checkZeroArray(dp) || point_counter == 0 || point_counter == 1){
            // Calculates vector to next point
            for (int i = 0; i < dimN; i++){
                dp[i] = Math.floorDiv(point_counter,products[i]) % subspace_shape[i];
            }
            // Calculate actual point
            for (int i = 0; i < dimN; i++){
                p[i] = sp[i] + dp[i];
            }
            // Apply stencil and store result
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
    private boolean checkZeroArray(Number[] arr){
        // Return false if uninitialised
        if (arr[0] == null) return false;
        int n_zeros = 0;
        for (int i = 0; i < arr.length; i++){
            if (arr[i].intValue() == 0){ n_zeros++; }
        }
        return n_zeros == arr.length;
    }

}
