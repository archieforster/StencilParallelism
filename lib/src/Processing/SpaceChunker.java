package Processing;

public class SpaceChunker {
    Integer[] input_shape;
    int dim_divisor;

    public SpaceChunker(Integer[] input_shape, int dim_divisor) {
        this.input_shape = input_shape;
        this.dim_divisor = dim_divisor;
    }

    public Chunk[] getRegularChunks(){
        int dimN = input_shape.length;

        if (dimN == 1){
            return reg_chunk_1d(input_shape);
        }
        else if (dimN == 2){
            return reg_chunk_2d(input_shape);
        }
        else if (dimN == 3){
            return reg_chunk_3d(input_shape);
        }

        return new Chunk[]{};
    }

    /**
     * Chucks the input space shape assuming it's 1D
     * @return
     */
    private Chunk[] reg_chunk_1d(Integer[] input_shape){
        int chunk_n = dim_divisor;
        Integer[] chunk_size = new Integer[] {Math.floorDiv(input_shape[0], dim_divisor)};
        Chunk[] res_chunks = new Chunk[chunk_n];

        Integer[] sp = new Integer[] {0};

        // Do regular chunks
        Integer[] chunk_space = chunk_size;
        for (int i = 0; i < dim_divisor - 1; i++){
            res_chunks[i] = new Chunk(sp, chunk_space);
            sp[0] += chunk_space[0] + 1;
        }
        // Do final chunk to encapsulate rest of space
        chunk_space = new Integer[]{chunk_size[0] + (input_shape[0] % dim_divisor)};
        res_chunks[chunk_n - 1] = new Chunk(sp, chunk_space);

        return res_chunks;
    }

    /**
     * Chucks the input space shape assuming it's 2D
     * @return
     */
    private Chunk[] reg_chunk_2d(Integer[] input_shape){
        int chunk_n = (int) Math.pow(dim_divisor, 2);
        Integer[] chunk_size = new Integer[] {
                Math.floorDiv(input_shape[0], chunk_n),
                Math.floorDiv(input_shape[1], chunk_n)
        };
        Chunk[] res_chunks = new Chunk[chunk_n];

        Integer[] sp = new Integer[] {0,0};
        Integer[] chunk_space;
        int chunk_count = 0;

        // Loop in x direction until last x-dim chunk
        for (int x = 0; x < dim_divisor - 1; x++){
            sp[1] = 0;
            // Loop in y direction until last y-dim chunk
            chunk_space = chunk_size;
            for (int y = 0; y < dim_divisor - 1; y++){
                // Do regular chunks
                res_chunks[chunk_count++] = new Chunk(sp, chunk_space);
                sp[1] += chunk_space[1] + 1;
            }
            // Do final irregular y-dim chunk
            chunk_space = new Integer[]{chunk_size[0], chunk_size[1] + (input_shape[1] % dim_divisor)};
            res_chunks[chunk_count++] = new Chunk(sp, chunk_space);
            sp[0] += chunk_space[0] + 1;
        }
        // Do final loop of irregular x-dim chunk
        chunk_space = new Integer[]{chunk_size[0] + (input_shape[0] % dim_divisor), chunk_size[1]};
        sp[1] = 0;
        for (int y = 0; y < dim_divisor - 1; y++){
            // Do regular chunks;
            res_chunks[chunk_count++] = new Chunk(sp, chunk_space);
            sp[1] += chunk_space[1] + 1;
        }
        // Do final irregular x-dim and y-dim chunk
        chunk_space = new Integer[]{
                chunk_size[0] + (input_shape[0] % dim_divisor),
                chunk_size[1] + (input_shape[1] % dim_divisor)
        };
        res_chunks[chunk_count] = new Chunk(sp, chunk_space);

        return res_chunks;
    }

    /**
     * Chucks the input space shape assuming it's 3D
     * @return
     */
    Chunk[] reg_chunk_3d(Integer[] input_shape){
        int chunk_n = (int) Math.pow(dim_divisor, 3);
        Integer[] chunk_size = new Integer[] {
                Math.floorDiv(input_shape[0], chunk_n),
                Math.floorDiv(input_shape[1], chunk_n),
                Math.floorDiv(input_shape[2], chunk_n)
        };
        Chunk[] res_chunks = new Chunk[chunk_n];

        Integer[] sp = new Integer[] {0,0,0};
        Integer[] chunk_space;
        int chunk_count = 0;

        Integer[] subspace = new Integer[]{
                input_shape[1],
                input_shape[2]};
        Chunk[] chunked_subspace = reg_chunk_2d(subspace);

        // Regular chunks in x direction
        for (int x = 0; x < dim_divisor - 1; x++){
            chunk_space = chunk_size;
            for (Chunk chunk : chunked_subspace){
                sp[1] = chunk.getStartPoint()[0];
                sp[2] = chunk.getStartPoint()[1];
                chunk_space[1] = chunk.getChunkShape()[0];
                chunk_space[2] = chunk.getChunkShape()[1];

                res_chunks[chunk_count++] = new Chunk(sp, chunk_space);
            }
            sp[0] += chunk_space[0] + 1;
        }
        // Irregular chunks in x direction
        chunk_space = new Integer[]{
                chunk_size[0] + (input_shape[0] % dim_divisor),
                chunk_size[1],
                chunk_size[2]
        };
        for (Chunk chunk : chunked_subspace){
            sp[1] = chunk.getStartPoint()[0];
            sp[2] = chunk.getStartPoint()[1];
            chunk_space[1] = chunk.getChunkShape()[0];
            chunk_space[2] = chunk.getChunkShape()[1];

            res_chunks[chunk_count++] = new Chunk(sp, chunk_space);
        }

        return res_chunks;
    }

    /**
     * Vector sum - adds together vector entries (must have same lengths)
     * @param v1
     * @param v2
     * @return Integer[] v1 + v2
     */
    private Integer[] v_sum(Integer[] v1, Integer[] v2){
        Integer[] res = new Integer[v1.length];
        for (int i = 0; i < v1.length; i++){
            res[i] = v1[i] + v2[i];
        }
        return res;
    }

}
