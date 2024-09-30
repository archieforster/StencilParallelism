package Processing;


import Exceptions.IndexShapeException;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class FlatNumArray implements Iterable<Number> {

    private final int[] shape;
    private final Number[] array;
    private Integer arrayLength;

    public FlatNumArray(int[] shape, ArrayList<?> data) {
        this.shape = shape;

        arrayLength = 1;
        for (Integer dim : shape) {
            arrayLength *= dim;
        }
        array = flatten(data).toArray(new Number[arrayLength]);
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
    private List<Number> flatten(ArrayList<?> data) {
        return data.stream()
                .flatMap(element -> {
                    if (element instanceof ArrayList<?>) {
                        return flatten((ArrayList<?>) element).stream();
                    } else{
                        return Stream.of((Number) element);
                    }
                } )
                .toList();
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
