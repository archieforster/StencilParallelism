import Patterns.Stencil;


public class Main {
    public static void main(String[] args) {

        Integer[] shape = {5};
        Integer[] activations = new Integer[]{1,2,3,4,5};

        Stencil stencil = new Stencil(shape, activations, x->x.stream().reduce(0, (a,b)-> a.doubleValue() + b.doubleValue()));

    }
}