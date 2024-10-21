package Processing;


import Exceptions.IndexShapeException;
import Exceptions.InvalidArrayShapeException;

import java.util.*;
import java.util.function.Consumer;

public class FlatNumArray implements Iterable<Number> {

    private final int[] shape;
    private final Number[] array;
    private Integer arrayLength;

    /**
     * 1D flat array constructor
     * @param data
     */
    public FlatNumArray(Number[] data) {
        shape = new int[] {data.length};
        array = data.clone();
        arrayLength = array.length;
    }

    /**
     * 2D flat array constructor
     * @param data
     */
    public FlatNumArray(Number[][] data) {
        shape = new int[] {data.length, data[0].length};
        for (Number[] arr : data){
            if (arr.length != shape[1]){
                throw new InvalidArrayShapeException(
                        String.format("Input 2D array does not have a consistent shape," +
                                "as sub arrays have different lengths"));
            }
        }
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
    public FlatNumArray(Number[][][] data) {
        shape = new int[] {data.length, data[0].length, data[0][0].length};
        for (Number[][] outer_arr : data){
            if (outer_arr.length != shape[1]){
                throw new InvalidArrayShapeException(
                        String.format("Input 3D array does not have a consistent shape," +
                                "as sub arrays have different lengths"));
            }
            for (Number[] arr : outer_arr){
                if (arr.length != shape[2]){
                    throw new InvalidArrayShapeException(
                            String.format("Input 3D array does not have a consistent shape," +
                                    "as sub arrays have different lengths"));
                }
            }
        }
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

    public FlatNumArray(int[] shape, ArrayList<?> data) {
        this.shape = shape;

        arrayLength = 1;
        for (Integer dim : shape) {
            arrayLength *= dim;
        }
        array = flatten_ND(data).toArray(new Number[arrayLength]);
    }

    /**
     * Returns the element from the given index
     * @param index N-dimensional index as ArrayList
     * @return Value of array from the index
     */
    public Number get(int[] index){
        // Checks for valid index throwing Runtime Exceptions
        if (index.length != shape.length){
            throw new IndexShapeException(
                    String.format("Shape of index {0} does not match data with shape {1}.",
                            Arrays.toString(index), Arrays.toString(shape)));
        }
        for (int dimIndex = 0; dimIndex < shape.length; dimIndex++){
            if ( index[dimIndex] < 0 || index[dimIndex] >= shape[dimIndex]){
                throw new IndexOutOfBoundsException(
                        String.format("Index {0} is out of bounds for data with shape {1}.",
                                Arrays.toString(index), Arrays.toString(shape)));
            }
        }

        int flatIndex = 0;
        // Sum products of all 0...n-1 dimensions of index with shape
        // e.g. Shape (3,4,3) with index (1,2,1) -> 3*1 + 2*4
        for (int dimIndex = 0; dimIndex <= shape.length - 2; dimIndex++) {
            flatIndex += shape[dimIndex] * index[dimIndex];
        }
        // Add final nth dimension's index
        // e.g. ^^ flatIndex -> flatIndex + 3
        flatIndex += index[shape.length - 1];

        return array[flatIndex];
    }

    /**
     * Flattens input N-dimensional numerical data into a 1-dimensional ArrayList
     * @param data N-dimensional ArrayList
     * @return 1 Dimensional ArrayList
     */
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
}
