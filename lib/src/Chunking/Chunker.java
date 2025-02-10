package Chunking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Chunker {
    Integer[] input_shape;
    int dim_divisor;
    // Use Start Point of chunk as key
    HashMap<String, Chunk> computed_chunks;

    public Chunker(Integer[] input_shape, int dim_divisor) {
        this.input_shape = input_shape;
        this.dim_divisor = dim_divisor;
    }

    public Chunk[] getRegularChunks(){
        int dimN = input_shape.length;
        computed_chunks = new HashMap<>();{};

        if (dimN == 1){
            reg_chunk_1d(input_shape);
        }
        else if (dimN == 2){
            reg_chunk_2d(input_shape);
        }
        else if (dimN == 3){
            reg_chunk_3d(input_shape);
        }

        // Compute & Set Chunk Neighbours
        setChunkNeighbours();

        return computed_chunks.values().toArray(new Chunk[0]);
    }

    /**
     * Finds and sets chunk neighbours. These are all chunks that are adjacent in at least
     * 1 dimension (8 surrounding chunks in 3x3 grid, does not include chunk itself)
     */
    private void setChunkNeighbours(){
        for (Chunk chunk : computed_chunks.values()){
            ArrayList<Chunk> nbrs = chunk.getNeighbours();
            Integer[] sp = chunk.getStartPoint();
            Integer[] chunk_shape = chunk.getChunkShape();
            Integer[] reg_chunk_shape = new Integer[chunk_shape.length];
            for (int i = 0; i < reg_chunk_shape.length; i++){
                reg_chunk_shape[i] = Math.floorDiv(input_shape[i], dim_divisor);
            }

            // Checks all surrounding chunks (diagonally adjacent chunks need to be included)
            for (int x = -reg_chunk_shape[0]; x <= reg_chunk_shape[0]; x += reg_chunk_shape[0]){
                for (int y = -reg_chunk_shape[1]; y <= reg_chunk_shape[1]; y += reg_chunk_shape[1]){
                    if (x != 0 || y != 0){
                        Integer[] nbr = sp.clone();
                        nbr[0] += x;
                        nbr[1] += y;
                        String nbr_string = Arrays.toString(nbr);
                        if (computed_chunks.containsKey(nbr_string)){ nbrs.add(computed_chunks.get(nbr_string)); }
                    }
                }
            }
        }
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
            computed_chunks.put(Arrays.toString(sp.clone()), new Chunk(sp.clone(), chunk_space.clone()));
            //res_chunks[i] = new Chunk(sp.clone(), chunk_space.clone());
            sp[0] += chunk_space[0];
        }
        // Do final chunk to encapsulate rest of space
        chunk_space = new Integer[]{chunk_size[0] + (input_shape[0] % dim_divisor)};
        computed_chunks.put(Arrays.toString(sp.clone()), new Chunk(sp.clone(), chunk_space.clone()));
        //res_chunks[chunk_n - 1] = new Chunk(sp.clone(), chunk_space.clone());

        return res_chunks;
    }

    /**
     * Chucks the input space shape assuming it's 2D
     * @return
     */
    private Chunk[] reg_chunk_2d(Integer[] input_shape){
        int chunk_n = (int) Math.pow(dim_divisor, 2);
        Integer[] chunk_size = new Integer[] {
                Math.floorDiv(input_shape[0], dim_divisor),
                Math.floorDiv(input_shape[1], dim_divisor)
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
                computed_chunks.put(Arrays.toString(sp.clone()), new Chunk(sp.clone(), chunk_space.clone()));
                //res_chunks[chunk_count++] = new Chunk(sp.clone(), chunk_space.clone());
                sp[1] += chunk_space[1];
            }
            // Do final irregular y-dim chunk
            chunk_space = new Integer[]{chunk_size[0], chunk_size[1] + (input_shape[1] % dim_divisor)};
            computed_chunks.put(Arrays.toString(sp.clone()), new Chunk(sp.clone(), chunk_space.clone()));
            //res_chunks[chunk_count++] = new Chunk(sp.clone(), chunk_space.clone());
            sp[0] += chunk_space[0];
        }
        // Do final loop of irregular x-dim chunk
        chunk_space = new Integer[]{chunk_size[0] + (input_shape[0] % dim_divisor), chunk_size[1]};
        sp[1] = 0;
        for (int y = 0; y < dim_divisor - 1; y++){
            // Do regular chunks;
            computed_chunks.put(Arrays.toString(sp.clone()), new Chunk(sp.clone(), chunk_space.clone()));
            //res_chunks[chunk_count++] = new Chunk(sp.clone(), chunk_space.clone());
            sp[1] += chunk_space[1];
        }
        // Do final irregular x-dim and y-dim chunk
        chunk_space = new Integer[]{
                chunk_size[0] + (input_shape[0] % dim_divisor),
                chunk_size[1] + (input_shape[1] % dim_divisor)
        };
        computed_chunks.put(Arrays.toString(sp.clone()), new Chunk(sp.clone(), chunk_space.clone()));
        //res_chunks[chunk_count] = new Chunk(sp.clone(), chunk_space.clone());

        return res_chunks;
    }

    /**
     * Chucks the input space shape assuming it's 3D
     * @return
     */
    Chunk[] reg_chunk_3d(Integer[] input_shape){
        int chunk_n = (int) Math.pow(dim_divisor, 3);
        Integer[] chunk_size = new Integer[] {
                Math.floorDiv(input_shape[0], dim_divisor),
                Math.floorDiv(input_shape[1], dim_divisor),
                Math.floorDiv(input_shape[2], dim_divisor)
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

                computed_chunks.put(Arrays.toString(sp.clone()), new Chunk(sp.clone(), chunk_space.clone()));
                //res_chunks[chunk_count++] = new Chunk(sp.clone(), chunk_space.clone());
            }
            sp[0] += chunk_space[0];
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

            computed_chunks.put(Arrays.toString(sp.clone()), new Chunk(sp.clone(), chunk_space.clone()));
            //res_chunks[chunk_count++] = new Chunk(sp.clone(), chunk_space.clone());
        }

        return res_chunks;
    }

}
