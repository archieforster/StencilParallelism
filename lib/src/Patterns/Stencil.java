package Patterns;

import Processing.FlatNumArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class Stencil {
    private double ITERATE_THRESHOLD = 0.5;
    public enum COMP_MODE {
        SELECT,
        ITERATE
    }

    private final Integer[] stencil_shape;
    private final Integer[] array_shape;
    private final FlatNumArray activations;
    private final Function<Collection<Number>,Number> computeFunction;

    private boolean weighted;
    private Number oob_default;
    private COMP_MODE comp_mode;
    private Integer[][] neighbour_vectors;

    public Stencil(Integer[] shape, Number[] activations, Function<Collection<Number>,Number> function) {
        this.stencil_shape = shape.clone();
        this.array_shape = transformShape(shape);
        this.activations = new FlatNumArray(shape, activations);
        this.computeFunction = function;

        base_init();
    }

    public Stencil(Integer[] shape, Number[][] activations, Function<Collection<Number>,Number> function) {
        this.stencil_shape = shape.clone();
        this.array_shape = transformShape(shape);
        this.activations = new FlatNumArray(shape, activations);
        this.computeFunction = function;

        base_init();
    }

    public Stencil(Integer[] shape, Number[][][] activations, Function<Collection<Number>,Number> function) {
        this.stencil_shape = shape.clone();
        this.array_shape = transformShape(shape);
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
            if (array_shape.length == 1) {values = get_nbrs_iterate_1d(p,input_space);}
            if (array_shape.length == 2) {values = get_nbrs_iterate_2d(p,input_space);}
            if (array_shape.length == 3) {values = get_nbrs_iterate_3d(p,input_space);}
        }
        if (comp_mode == COMP_MODE.SELECT){
            if (array_shape.length == 1) {values = get_nbrs_select_1d(p,input_space);}
            if (array_shape.length == 2) {values = get_nbrs_select_2d(p,input_space);}
            if (array_shape.length == 3) {values = get_nbrs_select_3d(p,input_space);}
        }
        return computeFunction.apply(values);
    }

    /**
     * Used to manually change between ITERATE and SELECT comp modes
     * @param mode
     */
    public void setCompMode(COMP_MODE mode) {
        comp_mode = mode;
        if (comp_mode == COMP_MODE.SELECT){
            int positive_activations = 0;
            for (Number a : activations){
                if (a.doubleValue() != 0f){
                    positive_activations++;
                }
            }
            setNeighbourVectors(positive_activations);
        }
    }

    /**
     * Base initialiser for Stencil Objects, checks if pattern is weighted,
     * sets initial OOB_Default value and determines if stencils should iterate over all
     * or select individual neighbours
     */
    private void base_init(){
        weighted = checkIfWeighted();
        oob_default = 0;
        setDefaultCompMode();
    }

    /**
     * Used to map input shape to real array shape i.e. input shape of {1} => +-1 either side of middle
     * meaning real shape of {3}. input shape {3,3} => {7,7}, input shape {3,4,2} => {7,9,5}
     * @return transformed shape
     */
    private Integer[] transformShape(Integer[] shape){
        for (int i = 0; i < shape.length; i++){
            shape[i] = 2*shape[i] + 1;
        }
        return shape;
    }

    /**
     * Determines and sets COMP_MODE of stencil application, either to ITERATE
     * through stencil pattern or SELECT neighbours using vectors
     */
    private void setDefaultCompMode(){
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
            setNeighbourVectors(positive_activations);
        }
    }

    /**
     * Stores all vectors to neighbours
     * @param numOfNeighbours number of neighbours to find/store
     */
    private void setNeighbourVectors(int numOfNeighbours){
        neighbour_vectors = new Integer[numOfNeighbours][];
        int n_index = 0;
        if (array_shape.length == 1){
            Integer[] v;
            for (int i = -stencil_shape[0]; i <= stencil_shape[0]; i++){
                v = new Integer[] {i};
                if (getActivationFromVector(v).doubleValue() != 0d){
                    neighbour_vectors[n_index++] = v;
                }
            }
        }
        if (array_shape.length == 2){
            Integer[] v;
            for (int i = -stencil_shape[0]; i <= stencil_shape[0]; i++){
                for (int j = -stencil_shape[1]; j <= stencil_shape[1]; j++){
                    v = new Integer[] {i,j};
                    if (getActivationFromVector(v).doubleValue() != 0d){
                        neighbour_vectors[n_index++] = v;
                    }
                }
            }
        }
        if (array_shape.length == 3){
            Integer[] v;
            for (int i = -stencil_shape[0]; i <= stencil_shape[0]; i++){
                for (int j = -stencil_shape[1]; j <= stencil_shape[1]; j++){
                    for (int k = -stencil_shape[2]; k <= stencil_shape[2]; k++){
                        v = new Integer[] {i,j,k};
                        if (getActivationFromVector(v).doubleValue() != 0d){
                            neighbour_vectors[n_index++] = v;
                        }
                    }
                }
            }
        }

    }

    /**
     * Returns activation given a vector from the central point
     * @param v input vector as Integer[]
     * @return Number activation
     */
    private Number getActivationFromVector(Integer[] v){
        Integer[] av = v.clone();
        for (int i = 0; i < v.length; i++){
            av[i] += stencil_shape[i];
        }
        return activations.get(av);
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
            for (int i = -stencil_shape[0]; i <= stencil_shape[0]; i++){
                si = new Integer[] {i};
                np = new Integer[] {p[0] + i};
                try{
                    values.add(getActivationFromVector(si).doubleValue() *  input_space.get(np).doubleValue());
                } catch (IndexOutOfBoundsException e){
                    values.add(oob_default);
                }
            }
        } else {
            for (int i = -stencil_shape[0]; i <= stencil_shape[0]; i++){
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
            for (int i = -stencil_shape[0]; i <= stencil_shape[0]; i++){
                for (int j = -stencil_shape[1]; j <= stencil_shape[1]; j++){
                    si = new Integer[] {i, j};
                    np = new Integer[] {p[0] + i, p[1] + j};
                    try{
                        values.add(getActivationFromVector(si).doubleValue() * input_space.get(np).doubleValue());
                    } catch (IndexOutOfBoundsException e){
                        values.add(oob_default);
                    }
                }
            }
        } else {
            for (int i = -stencil_shape[0]; i <= stencil_shape[0]; i++){
                for (int j = -stencil_shape[1]; j <= stencil_shape[1]; j++){
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
            for (int i = -stencil_shape[0]; i <= stencil_shape[0]; i++){
                for (int j = -stencil_shape[1]; j <= stencil_shape[1]; j++){
                    for (int k = -stencil_shape[2]; k <= stencil_shape[2]; k++){
                        si = new Integer[] {i, j, k};
                        np = new Integer[] {p[0] + i, p[1] + j, p[2] + k};
                        try{
                            values.add(getActivationFromVector(si).doubleValue() * input_space.get(np).doubleValue());
                        } catch (IndexOutOfBoundsException e){
                            values.add(oob_default);
                        }
                    }
                }
            }
        } else {
            for (int i = -stencil_shape[0]; i <= stencil_shape[0]; i++){
                for (int j = -stencil_shape[1]; j <= stencil_shape[1]; j++){
                    for (int k = -stencil_shape[2]; k <= stencil_shape[2]; k++){
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

    private List<Number> get_nbrs_select_1d(Integer[] p, FlatNumArray input_space){
        List<Number> values = new ArrayList<>();
        if (isWeighted()){
            Integer[] np;
            for (Integer[] v : neighbour_vectors){
                np = new Integer[] {p[0] + v[0]};
                try{
                    values.add(getActivationFromVector(v).doubleValue() * input_space.get(np).doubleValue());
                } catch (IndexOutOfBoundsException e){
                    values.add(oob_default);
                }
            }
        }
        return values;
    }

    private List<Number> get_nbrs_select_2d(Integer[] p, FlatNumArray input_space){
        List<Number> values = new ArrayList<>();
        if (isWeighted()){
            Integer[] np;
            for (Integer[] v : neighbour_vectors){
                np = new Integer[] {p[0] + v[0], p[1] + v[1]};
                try{
                    values.add(getActivationFromVector(v).doubleValue() * input_space.get(np).doubleValue());
                } catch (IndexOutOfBoundsException e){
                    values.add(oob_default);
                }
            }
        }
        return values;
    }

    private List<Number> get_nbrs_select_3d(Integer[] p, FlatNumArray input_space){
        List<Number> values = new ArrayList<>();
        if (isWeighted()){
            Integer[] np;
            for (Integer[] v : neighbour_vectors){
                np = new Integer[] {p[0] + v[0], p[1] + v[1], p[2] + v[2]};
                try{
                    values.add(getActivationFromVector(v).doubleValue() * input_space.get(np).doubleValue());
                } catch (IndexOutOfBoundsException e){
                    values.add(oob_default);
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
