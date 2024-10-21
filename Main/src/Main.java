import Patterns.Stencil;
import Processing.FlatNumArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        int[] shape = {2,3};
        ArrayList activations = new ArrayList();
        activations.add(List.of(1,2,1));
        activations.add(List.of(1,3,4));

        Stencil stencil = new Stencil(shape, activations, x->x.stream().reduce(0, (a,b)-> a.doubleValue() + b.doubleValue()));

        FlatNumArray flatNumArray = new FlatNumArray(shape,activations);
        System.out.println(flatNumArray.get(new int[]{1, 2}));
    }
}