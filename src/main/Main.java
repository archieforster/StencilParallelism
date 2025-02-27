package main;

import Patterns.ComputeMode;
import Patterns.Stencil;
import Processing.Computer;
import Processing.ISLType;
import Processing.ThreadType;
import Processing.ThreadingMode;
import Utils.FlatNumArray;
import Utils.ImageHandler;

import java.awt.*;
import java.io.*;
import java.util.Collection;
import java.util.function.Function;

public class Main {

    public static void main(String[] args) throws IOException {


//        test_pool_vs_per_chunk_2D(1); // Same n.o. vthreads in pool & per chunk 2D
//        test_pool_vs_per_chunk_3D(1); // Same n.o. vthreads in pool & per chunk 3D
//        test_pool_threads_with_chunks(1); // N.o. threads in pool for different n.o. chunks 2D - virtual and platform
//        test_iterate_select_threshold(1); // Fullness of stencil for iterate & select in 3D
//        test_conway_gol();
    }

    public static void test_conway_gol(){
        conway_gol(
                ThreadingMode.PER_CHUNK,
                ThreadType.VIRTUAL,
                4,
                2,
                500
        );
    }

    public static void test_pool_threads_with_chunks(int tests_per_datapoint) throws IOException {
        int TEST_NUM = tests_per_datapoint;
        String results_path = "C:\\Users\\archi\\Documents\\UniWork\\Dissertation\\";
        PrintWriter writer_virtual = new PrintWriter(results_path + "test_pool_vthread.csv");
        PrintWriter writer_platform = new PrintWriter(results_path + "test_pool_platform.csv");
        for (int dim_divisor = 1; dim_divisor <= 8; dim_divisor++) {
            for (int thread_c = 1; thread_c <= 16; thread_c++) {
                long total_vthread = 0;
                long total_platform = 0;
                for (int t = 0; t < TEST_NUM; t++){
                    total_vthread += blur_giraffe_test(
                            ComputeMode.ITERATE,
                            ThreadingMode.POOL,
                            ThreadType.VIRTUAL,
                            thread_c,
                            dim_divisor,
                            5
                    );
                    total_platform += blur_giraffe_test(
                            ComputeMode.ITERATE,
                            ThreadingMode.POOL,
                            ThreadType.PLATFORM,
                            thread_c,
                            dim_divisor,
                            5
                    );
                }
                long avg_vthread = total_vthread / TEST_NUM;
                long avg_platform = total_platform / TEST_NUM;
                if (thread_c < 16){
                    writer_virtual.append(avg_vthread+",");
                    writer_platform.append(avg_platform+",");
                } else {
                    writer_virtual.append(avg_vthread+"");
                    writer_platform.append(avg_platform+"");
                }
                System.out.println("Pool of " + thread_c + " threads for " + dim_divisor*dim_divisor + " chunks");
            }
            writer_virtual.println();
            writer_virtual.flush();
            writer_platform.println();
            writer_platform.flush();
        }
    }

    private static void test_pool_vs_per_chunk_2D(int tests_per_datapoint) throws FileNotFoundException {
        int TEST_NUM = tests_per_datapoint;
        String results_path = "C:\\Users\\archi\\Documents\\UniWork\\Dissertation\\test_pool_vs_per_chunk_2d.csv";
        PrintWriter writer = new PrintWriter(results_path);
        for (int dim_divisor = 1; dim_divisor <= 8; dim_divisor++) {
            long total = 0;
            for (int t = 0; t < TEST_NUM; t++) {
                total += blur_giraffe_test(
                        ComputeMode.ITERATE,
                        ThreadingMode.POOL,
                        ThreadType.VIRTUAL,
                        dim_divisor * dim_divisor, // square so that it's one per chunk
                        dim_divisor,
                        5
                );
            }

            long avg = total / TEST_NUM;
            if (dim_divisor < 8){
                writer.append(avg+",");
            } else {
                writer.append(avg+"");
            }
            System.out.println("Pool with " + dim_divisor * dim_divisor + " threads & chunks");
        }

        writer.println();

        for (int dim_divisor = 1; dim_divisor <= 8; dim_divisor++) {
            long total = 0;
            for (int t = 0; t < TEST_NUM; t++) {
                total += blur_giraffe_test(
                        ComputeMode.ITERATE,
                        ThreadingMode.PER_CHUNK,
                        ThreadType.VIRTUAL,
                        dim_divisor * dim_divisor, // square so that it's one per chunk
                        dim_divisor,
                        5
                );
            }

            long avg = total / TEST_NUM;
            if (dim_divisor < 8){
                writer.append(avg+",");
            } else {
                writer.append(avg+"");
            }
            System.out.println("Per Chunk with " + dim_divisor * dim_divisor + " threads & chunks");
        }

        writer.flush();
    }

    private static void test_pool_vs_per_chunk_3D(int tests_per_datapoint) throws FileNotFoundException {
        int TEST_NUM = tests_per_datapoint;
        int MAX_DIM_DIVISOR = 5;
        String results_path = "C:\\Users\\archi\\Documents\\UniWork\\Dissertation\\test_pool_vs_per_chunk_3d.csv";
        PrintWriter writer = new PrintWriter(results_path);

        Stencil stencil = new Stencil(
                new Integer[] {2,2,2},
                new Integer[][][] {
                        {
                                {0,0,0,0,0},
                                {0,0,0,0,0},
                                {0,0,1,0,0},
                                {0,0,0,0,0},
                                {0,0,0,0,0}
                        },
                        {
                                {0,0,0,0,0},
                                {0,0,0,0,0},
                                {0,0,1,0,0},
                                {0,0,0,0,0},
                                {0,0,0,0,0}
                        },
                        {
                                {0,0,1,0,0},
                                {0,0,1,0,0},
                                {1,1,1,1,1},
                                {0,0,1,0,0},
                                {0,0,1,0,0}
                        },
                        {
                                {0,0,0,0,0},
                                {0,0,0,0,0},
                                {0,0,1,0,0},
                                {0,0,0,0,0},
                                {0,0,0,0,0}
                        },
                        {
                                {0,0,0,0,0},
                                {0,0,0,0,0},
                                {0,0,1,0,0},
                                {0,0,0,0,0},
                                {0,0,0,0,0}
                        }
                },
                (x) -> x.stream().reduce(0,(a,b) -> a.doubleValue() + b.doubleValue()).doubleValue() / 13
        );

        for (int dim_divisor = 1; dim_divisor <= MAX_DIM_DIVISOR; dim_divisor++) {
            long total = 0;
            for (int t = 0; t < TEST_NUM; t++) {
                total += computation_test_3d(
                        ComputeMode.SELECT,
                        stencil,
                        ThreadingMode.PER_CHUNK,
                        ThreadType.VIRTUAL,
                        dim_divisor * dim_divisor, // square so that it's one per chunk
                        dim_divisor,
                        5
                );
            }

            long avg = total / TEST_NUM;
            if (dim_divisor < MAX_DIM_DIVISOR){
                writer.append(avg+",");
            } else {
                writer.append(avg+"");
            }
            System.out.println("Pool with " + dim_divisor * dim_divisor * dim_divisor + " threads & chunks");
        }

        writer.println();

        for (int dim_divisor = 1; dim_divisor <= MAX_DIM_DIVISOR; dim_divisor++) {
            long total = 0;
            for (int t = 0; t < TEST_NUM; t++) {
                total += computation_test_3d(
                        ComputeMode.SELECT,
                        stencil,
                        ThreadingMode.POOL,
                        ThreadType.VIRTUAL,
                        dim_divisor * dim_divisor, // square so that it's one per chunk
                        dim_divisor,
                        5
                );
            }

            long avg = total / TEST_NUM;
            if (dim_divisor < MAX_DIM_DIVISOR){
                writer.append(avg+",");
            } else {
                writer.append(avg+"");
            }
            System.out.println("Per Chunk with " + dim_divisor * dim_divisor * dim_divisor + " threads & chunks");
        }

        writer.flush();

    }

    public static void test_iterate_select_threshold(int tests_per_datapoint) throws FileNotFoundException {
        int TEST_NUM = tests_per_datapoint;
        long[] select_results = new long[11*11];
        long[] iterate_results = new long[11*11];
        for (int activations = 0; activations <= 7*7*7; activations+=7) {
            final int total_activations;
            if (activations == 0) {
                total_activations = 1;
            } else {
                total_activations = activations;
            }
            Function<Collection<Number>,Number> f =
                    (x) -> x.stream().reduce(0, (a,b) -> a.doubleValue() + b.doubleValue()).doubleValue() / total_activations;

            Integer[][][] pattern = new Integer[7][7][7];
            int used_activations = 0;
            for (int i = 0; i < pattern.length; i++) {
                for (int j = 0; j < pattern[i].length; j++) {
                    for (int k = 0; k < pattern[i][j].length; k++) {
                        if (used_activations < total_activations) {
                            pattern[i][j][k] = 1;
                            used_activations++;
                        } else {
                            pattern[i][j][k] = 0;
                        }
                    }
                }
            }
            Stencil stencil = new Stencil(
                    new Integer[] {3,3,3},
                    pattern,
                    f
            );
            long total_time_select = 0;
            long total_time_iterate = 0;
            for (int t = 1; t <= TEST_NUM; t++){
                total_time_select += computation_test_3d(
                        ComputeMode.SELECT,
                        stencil,
                        ThreadingMode.PER_CHUNK,
                        ThreadType.VIRTUAL,
                        5*5*5,
                        5,
                        1
                );
                total_time_iterate += computation_test_3d(
                        ComputeMode.ITERATE,
                        stencil,
                        ThreadingMode.PER_CHUNK,
                        ThreadType.VIRTUAL,
                        5*5*5,
                        5,
                        1
                );
            }
            select_results[activations/7] = total_time_select / TEST_NUM;
            iterate_results[activations/7] = total_time_iterate / TEST_NUM;
            System.out.println("Activations " + activations + ": Select = " + (total_time_select / TEST_NUM) + " - Iterate = " + (total_time_iterate / TEST_NUM));
        }

        String results_path = "C:\\Users\\archi\\Documents\\UniWork\\Dissertation\\test_select_vs_iterate.csv";
        PrintWriter writer = new PrintWriter(results_path);
        for (int i = 0; i < select_results.length - 1; i++) {
            writer.append(select_results[i]+",");
        }
        writer.append(select_results[select_results.length-1]+"\n");
        for (int i = 0; i < iterate_results.length - 1; i++) {
            writer.append(iterate_results[i]+",");
        }
        writer.append(iterate_results[iterate_results.length-1]+"");

    }

    public static long blur_giraffe_test(
            ComputeMode stencil_compute_mode,
            ThreadingMode threading_mode,
            ThreadType thread_type,
            int max_threads,
            int dim_divisor,
            int isl_loops
    ) {
        String image_path = "C:\\Users\\archi\\Downloads\\lil_giraffe.jpg";

        ImageHandler image_handler = new ImageHandler();
        Integer[][] image_data = image_handler.loadPng(image_path);
        Integer[] image_shape = new Integer[] {image_data.length ,image_data[0].length};

        Integer[][] image_data_r =  new Integer[image_shape[0]][image_shape[1]];
        Integer[][] image_data_g =  new Integer[image_shape[0]][image_shape[1]];
        Integer[][] image_data_b =  new Integer[image_shape[0]][image_shape[1]];
        for (int i = 0; i < image_data.length; i++) {
            for (int j = 0; j < image_data[i].length; j++) {
                Color c = new Color(image_data[i][j]);
                image_data_r[i][j] = c.getRed();
                image_data_g[i][j] = c.getGreen();
                image_data_b[i][j] = c.getBlue();
            }
        }

        FlatNumArray flat_data_r = new FlatNumArray(image_shape,image_data_r);
        FlatNumArray flat_data_g = new FlatNumArray(image_shape,image_data_g);
        FlatNumArray flat_data_b = new FlatNumArray(image_shape,image_data_b);

        Stencil stencil = new Stencil(
//                new Integer[] {2,2},
//                new Integer[][] {
//                        {2,1,0,-1,-2},
//                        {2,1,0,-1,-2},
//                        {2,1,0,-1,-2},
//                        {2,1,0,-1,-2},
//                        {2,1,0,-1,-2}
//                },
                new Integer[] {1,1},
                new Integer[][] {
                        {1,2,1},
                        {2,2,2},
                        {1,2,1}
                },
                x->(x.stream().reduce(0, (a,b) -> (a.doubleValue() + b.doubleValue())).doubleValue() / 14)
        );
        stencil.setCompMode(stencil_compute_mode);

        // Compute for red
        Computer stencil_computer = new Computer(flat_data_r,stencil);
        stencil_computer.setThreadingMode(threading_mode,max_threads);
        stencil_computer.setThreadType(thread_type);
        stencil_computer.setDimDivisor(dim_divisor);
        stencil_computer.setISLType(ISLType.FIXED_LOOP);
        stencil_computer.setMaxLoops(isl_loops);

        // Start recording
        long start_time = System.nanoTime();

        stencil_computer.execute();

        // Copy output to new array
        FlatNumArray output_r = new FlatNumArray(stencil_computer.getOutput().getShape(),
                stencil_computer.getOutput().asArray());
        // Compute for green
        stencil_computer.setInputSpace(flat_data_g);
        stencil_computer.execute();
        // Copy output to new array
        FlatNumArray output_g = new FlatNumArray(stencil_computer.getOutput().getShape(),
                stencil_computer.getOutput().asArray());
        // Compute for blue
        stencil_computer.setInputSpace(flat_data_b);
        stencil_computer.execute();
        // Copy output to new array
        FlatNumArray output_b = new FlatNumArray(stencil_computer.getOutput().getShape(),
                stencil_computer.getOutput().asArray());
        // Stop recording
        long end_time = System.nanoTime();
        // Rest is for storage, disable for test
//
//        // Repackage
//        FlatNumArray output = new FlatNumArray(image_shape);
//        for (int i = 0; i < image_shape[0]; i++) {
//            for (int j = 0; j < image_shape[1]; j++) {
//                Integer[] p = new Integer[]{i,j};
//                int r = Math.max(0,output_r.get(p).intValue());
//                int g = Math.max(0, output_g.get(p).intValue());
//                int b = Math.max(0, output_b.get(p).intValue());
//                Color c = new Color(r,g,b);
//                output.set(p,c.getRGB());
//            }
//        }
//
//        String output_path = "C:\\Users\\archi\\Documents\\UniWork\\Dissertation\\output.jpg";
//        try {
//            image_handler.savePng(output,output_path);
//        } catch (IOException e) {
//            System.out.println(e);
//        }
        return (end_time - start_time) / 1_000_000;
    }

    public static long computation_test_3d(
            ComputeMode stencil_compute_mode,
            Stencil stencil,
            ThreadingMode threading_mode,
            ThreadType thread_type,
            int max_threads,
            int dim_divisor,
            int isl_loops
    ){
        stencil.setCompMode(stencil_compute_mode);

        Integer[] input_shape = new Integer[] {50,50,50};
        FlatNumArray input = new FlatNumArray(input_shape);
        for (int i = 0; i < input_shape[0]; i++){
            for (int j = 0; j < input_shape[1]; j++){
                for (int k = 0; k < input_shape[2]; k++){
                    input.set(new Integer[] {i,j,k}, (i + j + k) % 20);
                }
            }
        }

        Computer stencil_computer = new Computer(input,stencil);
        stencil_computer.setThreadingMode(threading_mode,max_threads);
        stencil_computer.setThreadType(thread_type);
        stencil_computer.setDimDivisor(dim_divisor);
        stencil_computer.setISLType(ISLType.FIXED_LOOP);
        stencil_computer.setMaxLoops(isl_loops);

        long start_time = System.nanoTime();
        stencil_computer.execute();
        long end_time = System.nanoTime();
        return (end_time - start_time) / 1_000_000;

    }

    public static long conway_gol(
            ThreadingMode threading_mode,
            ThreadType thread_type,
            int max_threads,
            int dim_divisor,
            int isl_loops
    ){
        Function<Collection<Number>, Number> gol_func = (x) -> {
            boolean live = x.contains(-1) || x.contains(-1.0);
            long nbr_count = x.stream().filter((n) -> n.intValue() == 1).count();
            if (live) {
                if (nbr_count < 2){ //Fewer than 2 neighbours dies
                    return 0;
                }
                if (nbr_count == 2 || nbr_count == 3){ // 2 or 3 neighbours survives
                    return 1;
                }
                // Greater than 3 neighbours dies
                return 0;
            }else{
                if (nbr_count == 3){ // Dead cell becomes alive if it has 3 neighbours
                    return 1;
                }
                // Otherwise stays dead
                return 0;
            }
        };
        Stencil stencil = new Stencil(
                new Integer[] {1,1},
                new Integer[][] {
                        {1,1,1},
                        {1,-1,1},
                        {1,1,1}
                },
                gol_func
        );

        Integer[] input_shape = new Integer[] {20,20};
        Integer[][] input = new Integer[][] {
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,1,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        };

        FlatNumArray input_data = new FlatNumArray(input_shape,input);
        Computer computer = new Computer(input_data,stencil);
        computer.setThreadingMode(threading_mode);
        computer.setThreadType(thread_type);
        computer.setDimDivisor(dim_divisor);
        computer.setISLType(ISLType.FIXED_LOOP);
        computer.setMaxLoops(isl_loops);

        long start_time = System.nanoTime();
        computer.execute();
        long end_time = System.nanoTime();

        FlatNumArray out = computer.getOutput();

        System.out.println("\n====INPUT====\n");
        for (int i = 0; i < 20; i++){
            for (int j = 0; j < 20; j++){
                int v = input[i][j];
                System.out.print(v);
            }
            System.out.println();
        }
        System.out.println("\n====AFTER====\n");
        for (int i = 0; i < 20; i++){
            for (int j = 0; j < 20; j++){
                Integer[] p = new Integer[] {i,j};
                int v = out.get(p).intValue();
                System.out.print(v);
            }
            System.out.println();
        }

        return (end_time - start_time) / 1_000_000;
    }
}