import Patterns.ComputeMode;
import Patterns.Stencil;
import Utils.FlatNumArray;
import org.junit.Test;

public class StencilApplyTest {

    @Test
    public void Apply1dSimpleIterate(){
        FlatNumArray inputSpace = new FlatNumArray(new Integer[] {5},new Integer[] {1,2,3,2,1});
        // Stencil for weighted sum
        Stencil stencil = new Stencil(
                new Integer[] {1},
                new Integer[] {1,2,1},
                x->x.stream().reduce(0, (a,b)-> a.doubleValue() + b.doubleValue())
        );
        stencil.setCompMode(ComputeMode.ITERATE);
        Number out = stencil.apply(new Integer[] {2}, inputSpace).doubleValue();
        assert out.doubleValue() == 10d;
        out = stencil.apply(new Integer[] {1}, inputSpace).doubleValue();
        assert out.doubleValue() == 8d;
        out = stencil.apply(new Integer[] {3}, inputSpace).doubleValue();
        assert out.doubleValue() == 8d;
    }

    @Test
    public void Apply1dOOBIterate(){
        FlatNumArray inputSpace = new FlatNumArray(new Integer[] {5},new Integer[] {1,2,3,2,1});
        // Stencil for weighted sum
        Stencil stencil = new Stencil(
                new Integer[] {1},
                new Integer[] {1,2,1},
                x->x.stream().reduce(0, (a,b)-> a.doubleValue() + b.doubleValue())
        );
        stencil.setCompMode(ComputeMode.ITERATE);
        Number out = stencil.apply(new Integer[] {0}, inputSpace).doubleValue();
        assert out.doubleValue() == 4d; // Default OOB = 0
        stencil.setOOBDefault(20); // Set to 20
        out = stencil.apply(new Integer[] {0}, inputSpace).doubleValue();
        assert out.doubleValue() == 24d; // 1*20 + 2*1 + 1*2 = 24
    }

    @Test
    public void Apply1dSelect(){
        FlatNumArray inputSpace = new FlatNumArray(new Integer[] {5},new Integer[] {1,2,3,2,1});
        // Stencil for weighted sum
        Stencil stencil = new Stencil(
                new Integer[] {1},
                new Integer[] {1,2,1},
                x->x.stream().reduce(0, (a,b)-> a.doubleValue() + b.doubleValue())
        );
        stencil.setCompMode(ComputeMode.SELECT);
        Number out = stencil.apply(new Integer[] {2}, inputSpace).doubleValue();
        assert out.doubleValue() == 10d;
        out = stencil.apply(new Integer[] {1}, inputSpace).doubleValue();
        assert out.doubleValue() == 8d;
        out = stencil.apply(new Integer[] {3}, inputSpace).doubleValue();
        assert out.doubleValue() == 8d;
    }

    @Test
    public void Apply1dOOBSelect(){
        FlatNumArray inputSpace = new FlatNumArray(new Integer[] {5},new Integer[] {1,2,3,2,1});
        // Stencil for weighted sum
        Stencil stencil = new Stencil(
                new Integer[] {1},
                new Integer[] {1,2,1},
                x->x.stream().reduce(0, (a,b)-> a.doubleValue() + b.doubleValue())
        );
        stencil.setCompMode(ComputeMode.SELECT);
        Number out = stencil.apply(new Integer[] {0}, inputSpace).doubleValue();
        assert out.doubleValue() == 4d; // Default OOB = 0
        stencil.setOOBDefault(20); // Set to 20
        out = stencil.apply(new Integer[] {0}, inputSpace).doubleValue();
        assert out.doubleValue() == 24d; // 1*20 + 2*1 + 1*2 = 24
    }

    @Test
    public void Apply2dIterate(){
        FlatNumArray inputSpace = new FlatNumArray(
                new Integer[] {3,3},
                new Integer[][] {
                        {1,1,1},
                        {1,1,1},
                        {1,1,1}
                });
        // Stencil for weighted sum
        Stencil stencil = new Stencil(
                new Integer[] {1,1},
                new Integer[][] {
                        {0,1,0},
                        {1,2,1},
                        {0,1,0}
                },
                x->x.stream().reduce(0, (a,b)-> a.doubleValue() + b.doubleValue())
        );
        stencil.setCompMode(ComputeMode.ITERATE);
        Number out = stencil.apply(new Integer[] {1,1}, inputSpace).doubleValue();
        assert out.doubleValue() == 6d;
        out = stencil.apply(new Integer[] {1,0}, inputSpace).doubleValue();
        assert out.doubleValue() == 5d; // Default 00B = 0
        out = stencil.apply(new Integer[] {0,0}, inputSpace).doubleValue();
        assert out.doubleValue() == 4d;
    }

    @Test
    public void Apply2dSelect(){
        FlatNumArray inputSpace = new FlatNumArray(
                new Integer[] {3,3},
                new Integer[][] {
                        {1,1,1},
                        {1,1,1},
                        {1,1,1}
                });
        // Stencil for weighted sum
        Stencil stencil = new Stencil(
                new Integer[] {1,1},
                new Integer[][] {
                        {0,1,0},
                        {1,2,1},
                        {0,1,0}
                },
                x->x.stream().reduce(0, (a,b)-> a.doubleValue() + b.doubleValue()/6)
        );
        stencil.setCompMode(ComputeMode.SELECT);
        Number out = stencil.apply(new Integer[] {1,1}, inputSpace).doubleValue();
        assert out.doubleValue() == 6d;
        out = stencil.apply(new Integer[] {1,0}, inputSpace).doubleValue();
        assert out.doubleValue() == 5d; // Default 00B = 0
        out = stencil.apply(new Integer[] {0,0}, inputSpace).doubleValue();
        assert out.doubleValue() == 4d;
    }

    @Test
    public void Apply3dIterate(){
        FlatNumArray inputSpace = new FlatNumArray(
                new Integer[] {3,3,3},
                new Integer[][][] {
                        {
                                {1,1,1},
                                {1,1,1},
                                {1,1,1}
                        },
                        {
                                {1,1,1},
                                {1,1,1},
                                {1,1,1}
                        },
                        {
                                {1,1,1},
                                {1,1,1},
                                {1,1,1}
                        }
                });
        // Stencil for weighted sum
        Stencil stencil = new Stencil(
                new Integer[] {1,1,1},
                new Integer[][][]{
                        {
                                {0, 0, 0},
                                {0, 1, 0},
                                {0, 0, 0}
                        },
                        {
                                {0, 1, 0},
                                {1, 2, 1},
                                {0, 1, 0}
                        },
                        {
                                {0, 0, 0},
                                {0, 1, 0},
                                {0, 0, 0}
                        }
                },
                x->x.stream().reduce(0, (a,b)-> a.doubleValue() + b.doubleValue())
        );
        stencil.setCompMode(ComputeMode.ITERATE);
        Number out = stencil.apply(new Integer[] {1,1,1}, inputSpace).doubleValue();
        assert out.doubleValue() == 8d;
        out = stencil.apply(new Integer[] {0,0,0}, inputSpace).doubleValue();
        assert out.doubleValue() == 5d;
    }

    @Test
    public void Apply3dSelect(){
        FlatNumArray inputSpace = new FlatNumArray(
                new Integer[] {3,3,3},
                new Integer[][][] {
                        {
                                {1,1,1},
                                {1,1,1},
                                {1,1,1}
                        },
                        {
                                {1,1,1},
                                {1,1,1},
                                {1,1,1}
                        },
                        {
                                {1,1,1},
                                {1,1,1},
                                {1,1,1}
                        }
                });
        // Stencil for weighted sum
        Stencil stencil = new Stencil(
                new Integer[] {1,1,1},
                new Integer[][][]{
                        {
                                {0, 0, 0},
                                {0, 1, 0},
                                {0, 0, 0}
                        },
                        {
                                {0, 1, 0},
                                {1, 2, 1},
                                {0, 1, 0}
                        },
                        {
                                {0, 0, 0},
                                {0, 1, 0},
                                {0, 0, 0}
                        }
                },
                x->x.stream().reduce(0, (a,b)-> a.doubleValue() + b.doubleValue())
        );
        stencil.setCompMode(ComputeMode.SELECT);
        Number out = stencil.apply(new Integer[] {1,1,1}, inputSpace).doubleValue();
        assert out.doubleValue() == 8d;
        out = stencil.apply(new Integer[] {0,0,0}, inputSpace).doubleValue();
        assert out.doubleValue() == 5d;
    }
}
