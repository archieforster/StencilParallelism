package Processing;

public class Chunk {
    Integer[] start_point;
    Integer[] chunk_shape;

    public Chunk(Integer[] sp, Integer[] shape){
        start_point = sp;
        chunk_shape = shape;
    }

    public Integer[] getStartPoint(){
        return start_point;
    }

    public Integer[] getChunkShape(){
        return chunk_shape;
    }

}
