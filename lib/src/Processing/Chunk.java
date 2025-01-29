package Processing;

import java.util.ArrayList;

public class Chunk {
    private Integer[] start_point;
    private Integer[] chunk_shape;
    private ArrayList<Chunk> neighbours;
    private ChunkExecutionData execution_data;

    public Chunk(Integer[] sp, Integer[] shape){
        start_point = sp;
        chunk_shape = shape;
        neighbours = new ArrayList<Chunk>();
    }

    public Integer[] getStartPoint(){
        return start_point;
    }

    public Integer[] getChunkShape(){
        return chunk_shape;
    }

    public ArrayList<Chunk> getNeighbours(){
        return neighbours;
    }

    public void setExecutionData(ChunkExecutionData execution_data) {
        this.execution_data = execution_data;
    }

    public ChunkExecutionData getExecutionData(){
        return execution_data;
    }
}
