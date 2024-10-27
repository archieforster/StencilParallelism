import Processing.FlatNumArray;
import org.junit.Test;
import static org.junit.Assert.*;

public class FlatArrayTest {

    @Test
    public void Test3DIndexing(){
        FlatNumArray arr = new FlatNumArray(new Integer[] {2,3,3}, new Integer[][][] {
                {
                    {1,2,3},
                    {4,5,6},
                    {7,8,9}
                },{
                    {10,11,12},
                    {13,14,15},
                    {16,17,18}
        }
        } );

        assertEquals(9,arr.get(new Integer[]{0,2,2}));
        assertEquals(2,arr.get(new Integer[]{0,0,1}));
        assertEquals(14,arr.get(new Integer[]{1,1,1}));
    }

    @Test
    public void Test2DIndexing(){
        FlatNumArray arr = new FlatNumArray(new Integer[] {2,3}, new Integer[][]
                {{1,2,3}, {4,5,6}}
        );

        assertEquals(6,arr.get(new Integer[]{1,2}));
        assertEquals(2,arr.get(new Integer[]{0,1}));
        assertEquals(4,arr.get(new Integer[]{1,0}));
    }

    @Test
    public void Test1DIndexing(){
        FlatNumArray arr = new FlatNumArray(new Integer[] {6}, new Integer[]
                {1,2,3,4,5,6}
        );

        assertEquals(1,arr.get(new Integer[]{0}));
        assertEquals(2,arr.get(new Integer[]{1}));
        assertEquals(6,arr.get(new Integer[]{5}));
    }
}
