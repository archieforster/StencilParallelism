package Processing;

import Chunking.Chunk;
import Utils.FlatNumArray;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SpaceHandler {
    private ConcurrentHashMap<Integer, FlatNumArray> iterationSpace;
    private FlatNumArray initial_space;
    private FlatNumArray output_space;
    private int min_iteration;
    private int max_iteration;
    private Lock lock = new ReentrantLock();

    public SpaceHandler(FlatNumArray initial_space){
        this.initial_space = initial_space;
        this.output_space = new FlatNumArray(initial_space.getShape());

        iterationSpace = new ConcurrentHashMap<>();
        iterationSpace.put(0, initial_space);

        min_iteration = 0;
        max_iteration = 0;
    }

    /**
     * Returns space related to iteration of computation. Creates new space when next
     * buffer space is required.
     * @param iteration
     * @return FlatNumArray of space, null if iteration lookup fails.
     */
    public FlatNumArray getSpace(int iteration) {
        // Add buffer space if requesting next "layer" of space
        lock.lock();
        if (iteration == max_iteration + 1) {
            FlatNumArray next_space = new FlatNumArray(initial_space.getShape());
            iterationSpace.put(iteration, next_space);
            max_iteration++;
            lock.unlock();
            return next_space;
        }
        lock.unlock();

        // Return lookup
        FlatNumArray next_space = iterationSpace.get(iteration);
        return next_space;
    }

    /**
     * Writes contents related to a chunk to the output data
     * @param chunk
     * @param iteration Iteration number related to value (0 is initial value, 1 is value
     *                  after first iteration etc.)
     */
    public void writeToOutput(Chunk chunk, int iteration) {
        Integer[] sp = chunk.getStartPoint();
        Integer[] shape = chunk.getChunkShape();

        int dimN = shape.length;
        Integer[] dp = new Integer[dimN];
        Integer[] p = new Integer[dimN];

        // Calculate products for dimension sizes, work backwards
        // e.g. subspace_shape = [2,5,6] -> products = [30,6,1]
        // Used to "mod" a counter into vector e.g. counter = 21 -> [0,3,3] since 21 = 0*30 + 3*6 + 3*1
        int[] products = new int[dimN];
        products[dimN-1] = 1;
        for (int i = dimN - 2; i >= 0; i--){
            products[i] = shape[i+1] * products[i+1];
        }

        // Iterate over space
        int point_counter = 0;
        while (!check_zero_array(dp) || point_counter == 0 || point_counter == 1){
            // Calculates vector to next point
            for (int i = 0; i < dimN; i++){
                dp[i] = Math.floorDiv(point_counter,products[i]) % shape[i];
            }
            // Calculate actual point
            for (int i = 0; i < dimN; i++){
                p[i] = sp[i] + dp[i];
            }

            // Copy to output space
            output_space.set(p,iterationSpace.get(iteration).get(p));

            // Iterate to next point
            point_counter++;
        }

    }

    public FlatNumArray getOutputSpace() {
        return output_space;
    }

    private boolean check_zero_array(Number[] arr){
        // Return false if uninitialised
        if (arr[0] == null) return false;
        for (Number e : arr){
            if (e.doubleValue() != 0) return false;
        }
        return true;
    }
}
