package test;

import Patterns.Stencil;
import Processing.Computer;
import Utils.FlatNumArray;
import Processing.ISLType;
import Processing.ThreadingMode;
import org.testng.annotations.Test;

public class BasicComputeTest {


    @Test
    public void testBasicCompute() {
        FlatNumArray inputSpace = new FlatNumArray(
                new Integer[] {3,3},
                new Integer[][] {
                        {1,1,1},
                        {1,2,1},
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
        // Set default for Out-Of-Bound values as 0
        stencil.setOOBDefault(0);

        Computer computer = new Computer(inputSpace, stencil);
        computer.setThreadingMode(ThreadingMode.POOL,4);
        computer.setDimDivisor(3);
        computer.setISLType(ISLType.FIXED_LOOP);
        computer.setMaxLoops(3);
        computer.execute();
        for (Number n : computer.getOutput()) {
            System.out.println(n);
        }
        assert true;
    }
}
