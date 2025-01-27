import Processing.Chunker;
import Processing.Chunk;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChunkingTest {

    @Test
    public void Test1DPerfectChunkingC1(){
        Integer[] space = new Integer[]{10};

        // Case 1: divisor = 2
        Chunker chunker = new Chunker(space,2);
        Chunk[] chunks = chunker.getRegularChunks();

        // Check number of chunks is correct
        assert chunks.length == 2;

        // Check start points are correct
        assert chunks[0].getStartPoint()[0] == 0;
        assert chunks[1].getStartPoint()[0] == 5;
        // Check chunk spaces are both same & correct
        assert chunks[0].getChunkShape()[0] == 5;
        assert chunks[1].getChunkShape()[0] == 5;
    }

    @Test
    public void Test1DPerfectChunkingC2(){
        Integer[] space = new Integer[]{10};
        // Case 2: divisor = 5
        Chunker chunker = new Chunker(space,5);
        Chunk[] chunks = chunker.getRegularChunks();
        // Check number of chunks
        assert chunks.length == 5;
        for (int i = 0; i < chunks.length; i++) {
            assert chunks[i].getStartPoint()[0] == i*2;
        }
        // Check chunk spaces are all same
        for (int i = 0; i < chunks.length; i++) {
            assert chunks[i].getChunkShape()[0] == 2;
        }
    }

    @Test
    public void Test1DImperfectChunkingC1(){
        Integer[] space = new Integer[]{10};

        // Case 1: divisor = 3
        Chunker chunker = new Chunker(space,3);
        Chunk[] chunks = chunker.getRegularChunks();

        // Check number of chunks is correct
        assert chunks.length == 3;
        // Check start points
        assert chunks[0].getStartPoint()[0] == 0;
        assert chunks[1].getStartPoint()[0] == 3;
        assert chunks[2].getStartPoint()[0] == 6;
        // Check chunk shapes
        assert chunks[0].getChunkShape()[0] == 3;
        assert chunks[1].getChunkShape()[0] == 3;
        assert chunks[2].getChunkShape()[0] == 4; // Larger last chunk
    }

    @Test
    public void Test1DImperfectChunkingC2(){
        Integer[] space = new Integer[]{10};
        // Case 2: divisor = 7
        Chunker chunker = new Chunker(space,7);
        Chunk[] chunks = chunker.getRegularChunks();
        assert chunks.length == 7;
        // Check first 6 chunks
        for (int i = 0; i < chunks.length - 1; i++) {
            assert chunks[i].getStartPoint()[0] == i*1;
            assert chunks[i].getChunkShape()[0] == 1;
        }
        // Check final chunk
        assert chunks[6].getStartPoint()[0] == 6;
        assert chunks[6].getChunkShape()[0] == 4;
    }

    @Test
    public void Test2DPerfectChunkingC1() {
        Integer[] space = new Integer[]{10,10};

        // Case 1: divisor = 2 -> Chunk shape {5,5}
        Chunker chunker = new Chunker(space,2);
        Chunk[] chunks = chunker.getRegularChunks();

        // Divides each dim into 2 -> 2*2 = 4 chunks
        assert chunks.length == 4;
        // Check Start points
        List<Integer[]> chunk_sps_expected = Arrays.asList(new Integer[][]{
                {0,0},
                {0,5},
                {5,0},
                {5,5},
        });
        ArrayList<Integer[]> chunk_sps = new ArrayList();
        for (Chunk chunk : chunks) {
            chunk_sps.add(chunk.getStartPoint());
        }
        for (Integer[] sp : chunk_sps){
            int finds = 0;
            for (Integer[] sp_expected : chunk_sps_expected){
                if (Arrays.equals(sp,sp_expected)){ finds++; }
            }
            assert finds == 1;
        }
        // Check chunk shapes are all same
        for (Chunk chunk : chunks) {
            assert Arrays.equals(chunk.getChunkShape(), new Integer[]{5,5});
        }
    }

    @Test
    public void Test2DPerfectChunkingC2() {
        // Case 2 - input shape {12,9}, divisor 3
        Integer[] space = new Integer[]{12,9};

        // Case 1: divisor = 2 -> Chunk shape {5,5}
        Chunker chunker = new Chunker(space,3);
        Chunk[] chunks = chunker.getRegularChunks();

        // Divides each dim into 3 -> 3*3 = 9 chunks
        assert chunks.length == 9;
        // Check Start points
        List<Integer[]> chunk_sps_expected = Arrays.asList(new Integer[][]{
                {0,0},
                {0,3},
                {0,6},
                {4,0},
                {4,3},
                {4,6},
                {8,0},
                {8,3},
                {8,6}
        });
        ArrayList<Integer[]> chunk_sps = new ArrayList();
        for (Chunk chunk : chunks) {
            chunk_sps.add(chunk.getStartPoint());
        }
        for (Integer[] sp : chunk_sps){
            int finds = 0;
            for (Integer[] sp_expected : chunk_sps_expected){
                if (Arrays.equals(sp,sp_expected)){ finds++; }
            }
            assert finds == 1;
        }
        // Check chunk shapes are all same
        for (Chunk chunk : chunks) {
            assert Arrays.equals(chunk.getChunkShape(), new Integer[]{4,3});
        }
    }

    @Test
    public void Test2DImperfectChunkingC1(){
        Integer[] space = new Integer[]{10,10};

        // Case 1: divisor = 3 -> Chunk shapes {3,3}, {3,4}, {4,3}, {4,4}
        Chunker chunker = new Chunker(space,3);
        Chunk[] chunks = chunker.getRegularChunks();

        assert chunks.length == 9;
        // Check Start points
        List<Integer[]> chunk_sps_expected = Arrays.asList(new Integer[][]{
                {0,0},
                {0,3},
                {0,6},
                {3,0},
                {3,3},
                {3,6},
                {6,0},
                {6,3},
                {6,6}
        });
        ArrayList<Integer[]> chunk_sps = new ArrayList();
        for (Chunk chunk : chunks) {
            chunk_sps.add(chunk.getStartPoint());
        }
        for (Integer[] sp : chunk_sps){
            int finds = 0;
            for (Integer[] sp_expected : chunk_sps_expected){
                if (Arrays.equals(sp,sp_expected)){ finds++; }
            }
            assert finds == 1;
        }

        // Check chunk shapes
        int regRregC = 0;
        int regRirregC = 0;
        int irregRregC = 0;
        int irregRirregC = 0;
        for (Chunk chunk : chunks) {
            if (Arrays.equals(chunk.getChunkShape(), new Integer[]{3,3})) {regRregC++;}
            if (Arrays.equals(chunk.getChunkShape(), new Integer[]{3,4})) {regRirregC++;}
            if (Arrays.equals(chunk.getChunkShape(), new Integer[]{4,3})) {irregRregC++;}
            if (Arrays.equals(chunk.getChunkShape(), new Integer[]{4,4})) {irregRirregC++;}
        }
        assert regRregC == 4;
        assert regRirregC == 2;
        assert irregRregC == 2;
        assert irregRirregC == 1;
    }

    @Test
    public void Test2DImperfectChunkingC2(){
        Integer[] space = new Integer[]{10,10};

        // Case 2: divisor = 4 -> Chunk shapes {2,2}, {2,4}, {4,2}, {4,4}
        Chunker chunker = new Chunker(space,4);
        Chunk[] chunks = chunker.getRegularChunks();

        assert chunks.length == 16;
        // Check Start points
        List<Integer[]> chunk_sps_expected = Arrays.asList(new Integer[][]{
                {0,0},
                {0,2},
                {0,4},
                {0,6},
                {2,0},
                {2,2},
                {2,4},
                {2,6},
                {4,0},
                {4,2},
                {4,4},
                {4,6},
                {6,0},
                {6,2},
                {6,4},
                {6,6}
        });
        ArrayList<Integer[]> chunk_sps = new ArrayList();
        for (Chunk chunk : chunks) {
            chunk_sps.add(chunk.getStartPoint());
        }
        for (Integer[] sp : chunk_sps){
            int finds = 0;
            for (Integer[] sp_expected : chunk_sps_expected){
                if (Arrays.equals(sp,sp_expected)){ finds++; }
            }
            assert finds == 1;
        }

        // Check chunk shapes
        int regRregC = 0;
        int regRirregC = 0;
        int irregRregC = 0;
        int irregRirregC = 0;
        for (Chunk chunk : chunks) {
            if (Arrays.equals(chunk.getChunkShape(), new Integer[]{2,2})) {regRregC++;}
            if (Arrays.equals(chunk.getChunkShape(), new Integer[]{2,4})) {regRirregC++;}
            if (Arrays.equals(chunk.getChunkShape(), new Integer[]{4,2})) {irregRregC++;}
            if (Arrays.equals(chunk.getChunkShape(), new Integer[]{4,4})) {irregRirregC++;}
        }
        assert regRregC == 9;
        assert regRirregC == 3;
        assert irregRregC == 3;
        assert irregRirregC == 1;
    }

    @Test
    public void Test3DPerfectChunking(){
        Integer[] space = new Integer[]{10,10,10};

        Chunker chunker = new Chunker(space,2);
        Chunk[] chunks = chunker.getRegularChunks();

        // Check n.o. chunks
        assert chunks.length == 8;

        // Check Start points
        List<Integer[]> chunk_sps_expected = Arrays.asList(new Integer[][]{
                {0,0,0},
                {0,0,5},
                {0,5,0},
                {0,5,5},
                {5,0,0},
                {5,0,5},
                {5,5,0},
                {5,5,5}
        });
        ArrayList<Integer[]> chunk_sps = new ArrayList();
        for (Chunk chunk : chunks) {
            chunk_sps.add(chunk.getStartPoint());
        }
        for (Integer[] sp : chunk_sps){
            int finds = 0;
            for (Integer[] sp_expected : chunk_sps_expected){
                if (Arrays.equals(sp,sp_expected)){ finds++; }
            }
            assert finds == 1;
        }

        // Check chunk shapes are all same
        for(Chunk chunk : chunks){
            assert Arrays.equals(chunk.getChunkShape(), new Integer[]{5,5,5});
        }
    }

    @Test
    public void Test3DImperfectChunking(){
        Integer[] space = new Integer[]{10,10,10};

        Chunker chunker = new Chunker(space,3);
        Chunk[] chunks = chunker.getRegularChunks();

        // Check n.o. chunks
        assert chunks.length == 27;

        // Check Start points
        List<Integer[]> chunk_sps_expected = Arrays.asList(new Integer[][]{
                {0,0,0},
                {0,0,3},
                {0,0,6},
                {0,3,0},
                {0,3,3},
                {0,3,6},
                {0,6,0},
                {0,6,3},
                {0,6,6},
                {3,0,0},
                {3,0,3},
                {3,0,6},
                {3,3,0},
                {3,3,3},
                {3,3,6},
                {3,6,0},
                {3,6,3},
                {3,6,6},
                {6,0,0},
                {6,0,3},
                {6,0,6},
                {6,3,0},
                {6,3,3},
                {6,3,6},
                {6,6,0},
                {6,6,3},
                {6,6,6}
        });
        ArrayList<Integer[]> chunk_sps = new ArrayList();
        for (Chunk chunk : chunks) {
            chunk_sps.add(chunk.getStartPoint());
        }
        for (Integer[] sp : chunk_sps){
            int finds = 0;
            for (Integer[] sp_expected : chunk_sps_expected){
                if (Arrays.equals(sp,sp_expected)){ finds++; }
            }
            assert finds == 1;
        }

        //Check chunk spaces
        int regXregYregZ = 0;
        int regXregYirregZ = 0;
        int regXirregYregZ = 0;
        int regXirregYirregZ = 0;
        int irregXregYregZ = 0;
        int irregXregYirregZ = 0;
        int irregXirregYregZ = 0;
        int irregXirregYirregZ = 0;
        for (Chunk chunk : chunks) {
            Integer[] chunk_shape = chunk.getChunkShape();
            if (Arrays.equals(chunk_shape,new Integer[]{3,3,3})) {regXregYregZ++;}
            if (Arrays.equals(chunk_shape,new Integer[]{3,3,4})) {regXregYirregZ++;}
            if (Arrays.equals(chunk_shape,new Integer[]{3,4,3})) {regXirregYregZ++;}
            if (Arrays.equals(chunk_shape,new Integer[]{3,4,4})) {regXirregYirregZ++;}
            if (Arrays.equals(chunk_shape,new Integer[]{4,3,3})) {irregXregYregZ++;}
            if (Arrays.equals(chunk_shape,new Integer[]{4,3,4})) {irregXregYirregZ++;}
            if (Arrays.equals(chunk_shape,new Integer[]{4,4,3})) {irregXirregYregZ++;}
            if (Arrays.equals(chunk_shape,new Integer[]{4,4,4})) {irregXirregYirregZ++;}
        }
        assert regXregYregZ == 8;
        assert regXregYirregZ == 4;
        assert regXirregYregZ == 4;
        assert irregXregYregZ == 4;
        assert regXirregYirregZ == 2;
        assert irregXregYirregZ == 2;
        assert irregXirregYregZ == 2;
        assert irregXirregYirregZ == 1;
    }
}
