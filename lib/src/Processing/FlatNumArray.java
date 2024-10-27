package Processing;


import Exceptions.IndexShapeException;
import Exceptions.InvalidArrayShapeException;

import java.util.*;
import java.util.function.Consumer;

public class FlatNumArray implements Iterable<Number> {

    private final Integer[] shape;
    private final Number[] array;
    private Integer arrayLength;
    private Integer[] d_chunk_size;

    public FlatNumArray(Integer[] shape) {
        this.shape = shape;
        init_d_chunk_size();

        int size = 1;
        for (Integer i : shape) {
            size *= i;
        }
        arrayLength = size;
        array = new Number[arrayLength];

    }
    
    
    /**
     * 1D flat array constructor
     * @param data
     */
    public FlatNumArray(Integer[] shape, Number[] data) {
        if (shape[0] != data.length || shape.length != 1) {
            throw new InvalidArrayShapeException(
                    String.format("Shape argument %s does not match the shape of the input data %s",
                            Arrays.toString(shape),
                            "["+data.length+"]"));
        }
        this.shape = shape;
        init_d_chunk_size();

        array = data.clone();
        arrayLength = array.length;
    }

    /**
     * 2D flat array constructor
     * @param data
     */
    public FlatNumArray(Integer[] shape, Number[][] data) {
        Integer[] shape_check = new Integer[] {data.length, data[0].length};
        for (Number[] arr : data){
            if (arr.length != shape[1]){
                throw new InvalidArrayShapeException(
                        String.format("Input 2D array does not have a consistent shape," +
                                "as sub arrays have different lengths"));
            }
        }
        if (!Arrays.equals(shape, shape_check)){
            throw new InvalidArrayShapeException(
                    String.format("Shape argument %s does not match the shape of the input data %s",
                            Arrays.toString(shape),
                            Arrays.toString(shape_check)));
        }
        this.shape = shape;
        init_d_chunk_size();

        // Create flat array
        arrayLength = shape[0] * shape[1];
        array = new Number[arrayLength];
        // Insert all array values
        int i = 0;
        for (Number[] arr : data){
            for (Number v : arr){
                array[i++] = v;
            }
        }
    }

    /**
     * 3D flat array constructor
     * @param data
     */
    public FlatNumArray(Integer[] shape, Number[][][] data) {
        Integer[] shape_check = new Integer[] {data.length, data[0].length, data[0][0].length};
        for (Number[][] outer_arr : data){
            if (outer_arr.length != shape[1]){
                throw new InvalidArrayShapeException(
                        String.format("Input 3D array does not have a consistent shape," +
                                " as sub arrays have different lengths"));
            }
            for (Number[] arr : outer_arr){
                if (arr.length != shape[2]){
                    throw new InvalidArrayShapeException(
                            String.format("Input 3D array does not have a consistent shape," +
                                    " as sub arrays have different lengths"));
                }
            }
        }
        if (!Arrays.equals(shape, shape_check)){
            throw new InvalidArrayShapeException(
                    String.format("Shape argument %s does not match the shape of the input data %s",
                            Arrays.toString(shape),
                            Arrays.toString(shape_check)));
        }
        this.shape = shape;
        init_d_chunk_size();

        // Create flat array
        arrayLength = shape[0] * shape[1] * shape[2];
        array = new Number[arrayLength];
        // Insert all array values
        int i = 0;
        for (Number[][] outer_arr : data){
            for (Number[] arr : outer_arr){
                for (Number v : arr){
                    array[i++] = v;
                }
            }
        }
    }

    /**
     * Initialises dimension chunk size array used to quickly index/lookup
     * elements in the flat array
     */
    private void init_d_chunk_size() {
        d_chunk_size = new Integer[shape.length];
        int size = 1;
        for (int i = 1; i < shape.length; i++) {
            size *= shape[i];
        }
        d_chunk_size[0] = size;
        for (int i = 1; i < shape.length; i++) {
            d_chunk_size[i] = d_chunk_size[i - 1] / shape[i];
        }
    }

    /**
     * Returns the element from the given index
     * @param index N-dimensional index as ArrayList
     * @return Value of array from the index
     */
    public Number get(Integer[] index){
        // Checks for valid index throwing Runtime Exceptions
        if (index.length != shape.length){
            throw new IndexShapeException(
                    String.format("Shape of index %s does not match data with shape %s.",
                            Arrays.toString(index), Arrays.toString(shape)));
        }
        for (int dimIndex = 0; dimIndex < shape.length; dimIndex++){
            if ( index[dimIndex] < 0 || index[dimIndex] >= shape[dimIndex]){
                throw new IndexOutOfBoundsException(
                        String.format("Index %s is out of bounds for data with shape %s.",
                                Arrays.toString(index), Arrays.toString(shape)));
            }
        }

        int flatIndex = 0;
        // Use dimension chunk size to quickly find flat index
        // E.g. for shape(3,2,2), index (2,1,1) is at 2*4 + 1*2 + 1*1, where 4,2,1 are chunk sizes
        for (int dimIndex = 0; dimIndex < shape.length; dimIndex++) {
            flatIndex += d_chunk_size[dimIndex] * index[dimIndex];
        }

        return array[flatIndex];
    }

    public Integer[] getShape() {
        return shape;
    }

    public int length() {
        return arrayLength;
    }

    @Override
    public Iterator<Number> iterator() {
        return new Iterator<Number>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < arrayLength;
            }

            @Override
            public Number next() {
                return array[index++];
            }
        };
    }

    @Override
    public void forEach(Consumer<? super Number> action) {
        Iterable.super.forEach(action);
    }

    @Override
    public Spliterator<Number> spliterator() {
        return Iterable.super.spliterator();
    }

    // N-dimensional array
    /*

     * Flattens input N-dimensional numerical data into a 1-dimensional ArrayList
     * @param data N-dimensional ArrayList
     * @return 1 Dimensional ArrayList

    private List<?> flatten_ND(Collection<?> data) {
        List<Object> list = new ArrayList<>();
        for (Object element : data) {
            if (element instanceof Collection<?>) {
                list.addAll(flatten_ND((Collection<?>) element));
            } else{
                list.add(element);
            }
        }
        return list;
    }
    */
    /*
    N-dimensional implementation TBD

    public FlatNumArray(Integer[] shape, ArrayList<?> data) {
        this.shape = shape;

        arrayLength = 1;
        for (Integer dim : shape) {
            arrayLength *= dim;
        }
        array = flatten_ND(data).toArray(new Number[arrayLength]);

        init_d_chunk_size();
    }
    */
}
