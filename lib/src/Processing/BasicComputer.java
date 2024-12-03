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

    private void execute_over_space(Integer[] sp, Integer[] subspace_shape){
        Integer[] dp = new Integer[subspace_shape.length];
        for (int i = 0; i < subspace_shape.length; i++){
            dp[i] = 0;
        }

        while (!Arrays.equals(dp, subspace_shape)){

        }
    }

}
