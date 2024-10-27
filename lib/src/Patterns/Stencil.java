package Patterns;

import Processing.FlatNumArray;

import java.util.Collection;
import java.util.function.Function;

public class Stencil {

    private final Integer[] shape;
    private final FlatNumArray activations;
    private final Function<Collection<Number>,Number> computeFunction;
    private final boolean weighted;

    public Stencil(Integer[] shape, Number[] activations, Function<Collection<Number>,Number> function) {
        this.shape = shape;
        this.activations = new FlatNumArray(shape, activations);
        this.computeFunction = function;

        weighted = checkIfWeighted();
    }

    public Stencil(Integer[] shape, Number[][] activations, Function<Collection<Number>,Number> function) {
        this.shape = shape;
        this.activations = new FlatNumArray(shape, activations);
        this.computeFunction = function;

        weighted = checkIfWeighted();
    }

    public Stencil(Integer[] shape, Number[][][] activations, Function<Collection<Number>,Number> function) {
        this.shape = shape;
        this.activations = new FlatNumArray(shape, activations);
        this.computeFunction = function;

        weighted = checkIfWeighted();
    }

    /* N-dimensional constructor
    public Stencil(Integer[] shape, ArrayList<?> activations, Function<Collection<Number>,Number> function) {
        this.shape = shape;
        this.activations = new FlatNumArray(shape, activations);
        this.computeFunction = function;

        weighted = checkIfWeighted();
    }
    */

    /**
     * Returns whether the stencil pattern weights its neighbours as part of its computation
     * @return boolean
     */
    public boolean isWeighted() {
        return weighted;
    }

    public Integer[] getShape() {
        return shape;
    }

    public Function<Collection<Number>,Number> getComputeFunction() {
        return computeFunction;
    }

    /**
     * Checks if a stencil given its activations is weighted. Unweighted patterns will
     * only have activation 'weights' of 0 or 1.
     * @return boolean of whether input activations are weighted
     */
    private boolean checkIfWeighted(){
        for (Number a : activations ){
            if (a.doubleValue() != 0 && a.doubleValue() != 1){
                return true;
            }
        }
        return false;
    }
}
