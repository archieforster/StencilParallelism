package Processing;

import java.lang.Thread;
import java.util.Arrays;

import Patterns.Stencil;

public class BasicComputer {
    private FlatNumArray input_space;
    private FlatNumArray output_space;
    private Stencil stencil;
    private int n_vthreads;

    public void BasicComputer(int n_vthreads){
        this.n_vthreads = n_vthreads;
    }

    public void setInputSpace(FlatNumArray input_space){
        this.input_space = input_space;
        output_space = new FlatNumArray(input_space.getShape());
    }

    public void setStencil(Stencil stencil){
        this.stencil = stencil;
    }

    public void execute(){
        Thread.Builder builder = Thread.ofVirtual().name("stencil_vthread");
    }

    /**
     * Applies stencil to all points in a specified subspace from a given point.
     * This application is inclusive of the subspace endpoint.
     * Applies to points sp + [0,...,0], sp + [0,...,1], ..., sp + subspace_shape
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
        while (!Arrays.equals(dp, subspace_shape)){
            // Calculates vector to next point
            for (int i = 0; i < dimN; i++){
                dp[i] = Math.floorDiv(point_counter,products[i]) % subspace_shape[i];
            }
            // Calculate actual point
            for (int i = 0; i < dimN; i++){
                p[i] = sp[i] + dp[i];
            }
            // Apply stencil and store result
            output_space.set(p,stencil.apply(p,input_space));
            // Iterate to next point
            point_counter++;
        }
    }

}
