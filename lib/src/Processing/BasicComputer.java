package Processing;

import java.lang.Thread;
import Patterns.Stencil;

public class BasicComputer {
    private FlatNumArray input_space;
    private FlatNumArray output_space;
    private Stencil stencil;

    public void BasicComputer(int n_vthreads){

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

    private void execute_over_space(Integer[] start_point, Integer[] subspace_shape){

    }

}
