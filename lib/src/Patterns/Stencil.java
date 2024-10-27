package Patterns;

import Processing.FlatNumArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class Stencil {
    private double ITERATE_THRESHOLD = 0.5;

    private final Integer[] shape;
    private final FlatNumArray activations;
    private final Function<Collection<Number>,Number> computeFunction;

    private boolean weighted;
    private Number oob_default;
    private COMP_MODE comp_mode;

    public Stencil(Integer[] shape, Number[] activations, Function<Collection<Number>,Number> function) {
        this.shape = shape;
        this.activations = new FlatNumArray(shape, activations);
        this.computeFunction = function;

        base_init();
    }

    public Stencil(Integer[] shape, Number[][] activations, Function<Collection<Number>,Number> function) {
        this.shape = shape;
        this.activations = new FlatNumArray(shape, activations);
        this.computeFunction = function;

        base_init();
    }

    public Stencil(Integer[] shape, Number[][][] activations, Function<Collection<Number>,Number> function) {
        this.shape = shape;
        this.activations = new FlatNumArray(shape, activations);
        this.computeFunction = function;

        base_init();
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

    public void setOOBDefault(Number oob_default) {
        this.oob_default = oob_default;
    }

    public Function<Collection<Number>,Number> getComputeFunction() {
        return computeFunction;
    }

    public Number apply(Integer[] p, FlatNumArray input_space){
        List values = new ArrayList();
        if (comp_mode == COMP_MODE.ITERATE){
            if (shape.length == 1) {values = get_nbrs_iterate_1d(p,input_space);}
            if (shape.length == 2) {values = get_nbrs_iterate_2d(p,input_space);}
            if (shape.length == 3) {values = get_nbrs_iterate_3d(p,input_space);}
        }
        if (comp_mode == COMP_MODE.SELECT){

        }
        return computeFunction.apply(values);
    }

    /**
     * Base initialiser for Stencil Objects, checks if pattern is weighted,
     * sets initial OOB_Default value and determines if stencils should iterate over all
     * or select individual neighbours
     */
    private void base_init(){
        weighted = checkIfWeighted();
        oob_default = 0;

        // Calculate proportion of stencil that has activation
        int positive_activations = 0;
        for (Number a : activations){
            if (a.doubleValue() != 0f){
                positive_activations++;
            }
        }
        if (positive_activations/activations.length() > ITERATE_THRESHOLD){
            comp_mode = COMP_MODE.ITERATE;
        }
        else{
            comp_mode = COMP_MODE.SELECT;
        }

    }

    private enum COMP_MODE {
        SELECT,
        ITERATE
    }

    /**
     * Gets neighbour values according to 1D stencil pattern (weighting if required)
     * @param p point of stencil application
     * @param input_space whole input space to pull values from
     * @return list of (weighted) values
     */
    private List<Number> get_nbrs_iterate_1d(Integer[] p, FlatNumArray input_space){
        List<Number> values = new ArrayList<>();
        Integer[] np;
        if (isWeighted()){
            Integer[] si;
            for (int i = -shape[0]; i <= shape[0]; i++){
                si = new Integer[] {i};
                np = new Integer[] {p[0] + i};
                try{
                    values.add(activations.get(si).doubleValue() *  input_space.get(np).doubleValue());
                } catch (IndexOutOfBoundsException e){
                    values.add(oob_default);
                }
            }
        } else {
            for (int i = -shape[0]; i <= shape[0]; i++){
                np = new Integer[] {p[0] + i};
                try{
                    values.add(input_space.get(np));
                } catch (IndexOutOfBoundsException e){
                    values.add(oob_default);
                }
            }
        }
        return values;
    }

    /**
     * Gets neighbour values according to 2D stencil pattern (weighting if required)
     * @param p point of stencil application
     * @param input_space whole input space to pull values from
     * @return list of (weighted) values
     */
    private List<Number> get_nbrs_iterate_2d(Integer[] p, FlatNumArray input_space){
        List<Number> values = new ArrayList<>();
        Integer[] np;
        if (isWeighted()){
            Integer[] si;
            for (int i = -shape[0]; i <= shape[0]; i++){
                for (int j = -shape[1]; j <= shape[1]; j++){
                    si = new Integer[] {i, j};
                    np = new Integer[] {p[0] + i, p[1] + j};
                    try{
                        values.add(activations.get(si).doubleValue() * input_space.get(np).doubleValue());
                    } catch (IndexOutOfBoundsException e){
                        values.add(oob_default);
                    }
                }
            }
        } else {
            for (int i = -shape[0]; i <= shape[0]; i++){
                for (int j = -shape[1]; j <= shape[1]; j++){
                    np = new Integer[] {p[0] + i, p[1] + j};
                    try{
                        values.add(input_space.get(np));
                    } catch (IndexOutOfBoundsException e){
                        values.add(oob_default);
                    }
                }
            }
        }
        return values;
    }

    /**
     * Gets neighbour values according to 3D stencil pattern (weighting if required)
     * @param p point of stencil application
     * @param input_space whole input space to pull values from
     * @return list of (weighted) values
     */
    private List<Number> get_nbrs_iterate_3d(Integer[] p, FlatNumArray input_space){
        List<Number> values = new ArrayList<>();
        Integer[] np;
        if (isWeighted()){
            Integer[] si;
            for (int i = -shape[0]; i <= shape[0]; i++){
                for (int j = -shape[1]; j <= shape[1]; j++){
                    for (int k = -shape[2]; k <= shape[2]; k++){
                        si = new Integer[] {i, j, k};
                        np = new Integer[] {p[0] + i, p[1] + j, p[2] + k};
                        try{
                            values.add(activations.get(si).doubleValue() * input_space.get(np).doubleValue());
                        } catch (IndexOutOfBoundsException e){
                            values.add(oob_default);
                        }
                    }
                }
            }
        } else {
            for (int i = -shape[0]; i <= shape[0]; i++){
                for (int j = -shape[1]; j <= shape[1]; j++){
                    for (int k = -shape[2]; k <= shape[2]; k++){
                        np = new Integer[] {p[0] + i, p[1] + j, p[2] + k};
                        try{
                            values.add(input_space.get(np));
                        } catch (IndexOutOfBoundsException e){
                            values.add(oob_default);
                        }
                    }
                }
            }
        }
        return values;
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
