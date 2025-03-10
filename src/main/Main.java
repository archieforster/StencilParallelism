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

    private static final String results_path = System.getProperty("user.dir") + "/Output/";
    private static final String resources_path = System.getProperty("user.dir") + "/Resources/";

    public static void main(String[] args) throws FileNotFoundException {
        try {
            test_pool_threads_with_chunks(25); // N.o. threads in pool for different n.o. chunks 2D - virtual and platform
            test_pool_vs_per_chunk_2D(25); // Same n.o. vthreads in pool & per chunk 2D
            test_pool_vs_per_chunk_3D(25); // Same n.o. vthreads in pool & per chunk 3D
            test_iterate_select_threshold(25); // Fullness of stencil for iterate & select in 3D
//        test_conway_gol();
        } catch (Exception e) {
            PrintWriter w = new PrintWriter(results_path+"error.txt");
            w.println(e.getMessage());
            System.out.println(e.getMessage());
        }

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
        PrintWriter writer_virtual = new PrintWriter(results_path + "test_pool_vthread.csv");
        PrintWriter writer_platform = new PrintWriter(results_path + "test_pool_platform.csv");
        PrintWriter writer_virtual_bi = new PrintWriter(results_path + "test_pool_vthread_bi.csv");
        PrintWriter writer_platform_bi = new PrintWriter(results_path + "test_pool_platform_bi.csv");
        for (int dim_divisor = 1; dim_divisor <= 8; dim_divisor++) {
            for (int thread_c = 1; thread_c <= 16; thread_c++) {
                long total_vthread = 0;
                long total_platform = 0;
                long total_vthread_bi = 0;
                long total_platform_bi = 0;
                for (int t = 0; t < TEST_NUM; t++){
                    total_vthread += blur_giraffe_test(
                            ComputeMode.ITERATE,
                            ThreadingMode.POOL,
                            ThreadType.VIRTUAL,
                            thread_c,
                            dim_divisor,
                            ISLType.FIXED_LOOP
                    );
                    total_platform += blur_giraffe_test(
                            ComputeMode.ITERATE,
                            ThreadingMode.POOL,
                            ThreadType.PLATFORM,
                            thread_c,
                            dim_divisor,
                            ISLType.FIXED_LOOP
                    );
                    total_vthread_bi += blur_giraffe_test(
                            ComputeMode.ITERATE,
                            ThreadingMode.POOL,
                            ThreadType.VIRTUAL,
                            thread_c,
                            dim_divisor,
                            ISLType.FIXED_LOOP_BORDERS
                    );
                    total_platform_bi += blur_giraffe_test(
                            ComputeMode.ITERATE,
                            ThreadingMode.POOL,
                            ThreadType.PLATFORM,
                            thread_c,
                            dim_divisor,
                            ISLType.FIXED_LOOP_BORDERS
                    );
                }
                long avg_vthread = total_vthread / TEST_NUM;
                long avg_platform = total_platform / TEST_NUM;
                long avg_vthread_bi = total_vthread_bi / TEST_NUM;
                long avg_platform_bi = total_platform_bi / TEST_NUM;
                if (thread_c < 16){
                    writer_virtual.append(avg_vthread+",");
                    writer_platform.append(avg_platform+",");
                    writer_virtual_bi.append(avg_vthread_bi+",");
                    writer_platform_bi.append(avg_platform_bi+",");
                } else {
                    writer_virtual.append(avg_vthread+"");
                    writer_platform.append(avg_platform+"");
                    writer_virtual_bi.append(avg_vthread_bi+"");
                    writer_platform_bi.append(avg_platform_bi+"");
                }
                writer_virtual.flush();
                writer_platform.flush();
                writer_virtual_bi.flush();
                writer_platform_bi.flush();
                System.out.println("Pool of " + thread_c + " threads for " + dim_divisor*dim_divisor + " chunks");
            }
            writer_virtual.println();
            writer_platform.println();
            writer_virtual_bi.println();
            writer_platform_bi.println();
            writer_virtual.flush();
            writer_platform.flush();
            writer_virtual_bi.flush();
            writer_platform_bi.flush();
        }
    }

    private static void test_pool_vs_per_chunk_2D(int tests_per_datapoint) throws FileNotFoundException {
        int TEST_NUM = tests_per_datapoint;
        int MAX_DIM_DIVISOR = 16;
        PrintWriter writer_vthread = new PrintWriter(results_path + "test_pool_vs_chunk_vthread_2d.csv");
        PrintWriter writer_platform = new PrintWriter(results_path + "test_pool_vs_chunk_platform_2d.csv");
        PrintWriter writer_vthread_bi = new PrintWriter(results_path + "test_pool_vs_chunk_vthread_2d_bi.csv");
        PrintWriter writer_platform_bi = new PrintWriter(results_path + "test_pool_vs_chunk_platform_2d_bi.csv");
        for (int dim_divisor = 1; dim_divisor <= MAX_DIM_DIVISOR; dim_divisor++) {
            long total_vthread = 0;
            long total_platform = 0;
            long total_vthread_bi = 0;
            long total_platform_bi = 0;
            for (int t = 0; t < TEST_NUM; t++) {
                total_vthread += blur_giraffe_test(
                        ComputeMode.ITERATE,
                        ThreadingMode.POOL,
                        ThreadType.VIRTUAL,
                        dim_divisor * dim_divisor, // square so that it's one per chunk
                        dim_divisor,
                        ISLType.FIXED_LOOP
                );
                total_platform += blur_giraffe_test(
                        ComputeMode.ITERATE,
                        ThreadingMode.POOL,
                        ThreadType.PLATFORM,
                        dim_divisor * dim_divisor, // square so that it's one per chunk
                        dim_divisor,
                        ISLType.FIXED_LOOP
                );
                total_vthread_bi += blur_giraffe_test(
                        ComputeMode.ITERATE,
                        ThreadingMode.POOL,
                        ThreadType.VIRTUAL,
                        dim_divisor * dim_divisor, // square so that it's one per chunk
                        dim_divisor,
                        ISLType.FIXED_LOOP_BORDERS
                );
                total_platform_bi += blur_giraffe_test(
                        ComputeMode.ITERATE,
                        ThreadingMode.POOL,
                        ThreadType.PLATFORM,
                        dim_divisor * dim_divisor, // square so that it's one per chunk
                        dim_divisor,
                        ISLType.FIXED_LOOP_BORDERS
                );
            }

            long avg_vthread = total_vthread / TEST_NUM;
            long avg_platform = total_platform / TEST_NUM;
            long avg_vthread_bi = total_vthread_bi / TEST_NUM;
            long avg_platform_bi = total_platform_bi / TEST_NUM;
            if (dim_divisor < MAX_DIM_DIVISOR){
                writer_vthread.append(avg_vthread+",");
                writer_platform.append(avg_platform+",");
                writer_vthread_bi.append(avg_vthread_bi+",");
                writer_platform_bi.append(avg_platform_bi+",");
            } else {
                writer_vthread.append(avg_vthread+"");
                writer_platform.append(avg_platform+"");
                writer_vthread_bi.append(avg_vthread_bi+"");
                writer_platform_bi.append(avg_platform_bi+"");
            }
            System.out.println("Pool with " + dim_divisor * dim_divisor + " threads & chunks");
            writer_vthread.flush();
            writer_platform.flush();
            writer_vthread_bi.flush();
            writer_platform_bi.flush();
        }

        writer_vthread.println();
        writer_platform.println();
        writer_vthread_bi.println();
        writer_platform_bi.println();

        for (int dim_divisor = 1; dim_divisor <= MAX_DIM_DIVISOR; dim_divisor++) {
            long total_vthread = 0;
            long total_platform = 0;
            long total_vthread_bi = 0;
            long total_platform_bi = 0;
            for (int t = 0; t < TEST_NUM; t++) {
                total_vthread += blur_giraffe_test(
                        ComputeMode.ITERATE,
                        ThreadingMode.PER_CHUNK,
                        ThreadType.VIRTUAL,
                        dim_divisor * dim_divisor, // square so that it's one per chunk
                        dim_divisor,
                        ISLType.FIXED_LOOP
                );
                total_platform += blur_giraffe_test(
                        ComputeMode.ITERATE,
                        ThreadingMode.PER_CHUNK,
                        ThreadType.PLATFORM,
                        dim_divisor * dim_divisor, // square so that it's one per chunk
                        dim_divisor,
                        ISLType.FIXED_LOOP
                );
                total_vthread_bi += blur_giraffe_test(
                        ComputeMode.ITERATE,
                        ThreadingMode.PER_CHUNK,
                        ThreadType.VIRTUAL,
                        dim_divisor * dim_divisor, // square so that it's one per chunk
                        dim_divisor,
                        ISLType.FIXED_LOOP_BORDERS
                );
                total_platform_bi += blur_giraffe_test(
                        ComputeMode.ITERATE,
                        ThreadingMode.PER_CHUNK,
                        ThreadType.PLATFORM,
                        dim_divisor * dim_divisor, // square so that it's one per chunk
                        dim_divisor,
                        ISLType.FIXED_LOOP_BORDERS
                );

            }

            long avg_vthread = total_vthread / TEST_NUM;
            long avg_platform = total_platform / TEST_NUM;
            long avg_vthread_bi = total_vthread_bi / TEST_NUM;
            long avg_platform_bi = total_platform_bi / TEST_NUM;
            if (dim_divisor < MAX_DIM_DIVISOR){
                writer_vthread.append(avg_vthread+",");
                writer_platform.append(avg_platform+",");
                writer_vthread_bi.append(avg_vthread_bi+",");
                writer_platform_bi.append(avg_platform_bi+",");
            } else {
                writer_vthread.append(avg_vthread+"");
                writer_platform.append(avg_platform+"");
                writer_vthread_bi.append(avg_vthread_bi+"");
                writer_platform_bi.append(avg_platform_bi+"");
            }
            System.out.println("Per Chunk with " + dim_divisor * dim_divisor + " threads & chunks");
            writer_vthread.flush();
            writer_platform.flush();
            writer_vthread_bi.flush();
            writer_platform_bi.flush();
        }
    }

    private static void test_pool_vs_per_chunk_3D(int tests_per_datapoint) throws FileNotFoundException {
        int TEST_NUM = tests_per_datapoint;
        int MAX_DIM_DIVISOR = 7;
        PrintWriter writer_vthread = new PrintWriter(results_path + "test_pool_vs_chunk_vthread_3d.csv");
        PrintWriter writer_platform = new PrintWriter(results_path + "test_pool_vs_chunk_platform_3d.csv");
        PrintWriter writer_vthread_bi = new PrintWriter(results_path + "test_pool_vs_chunk_vthread_3d_bi.csv");
        PrintWriter writer_platform_bi = new PrintWriter(results_path + "test_pool_vs_chunk_platform_3d_bi.csv");

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
            long total_vthread = 0;
            long total_platform = 0;
            long total_vthread_bi = 0;
            long total_platform_bi = 0;
            for (int t = 0; t < TEST_NUM; t++) {
                total_vthread += computation_test_3d(
                        ComputeMode.SELECT,
                        stencil,
                        ThreadingMode.POOL,
                        ThreadType.VIRTUAL,
                        dim_divisor * dim_divisor, // square so that it's one per chunk
                        dim_divisor,
                        ISLType.FIXED_LOOP
                );
                total_platform += computation_test_3d(
                        ComputeMode.SELECT,
                        stencil,
                        ThreadingMode.POOL,
                        ThreadType.PLATFORM,
                        dim_divisor * dim_divisor, // square so that it's one per chunk
                        dim_divisor,
                        ISLType.FIXED_LOOP
                );
                total_vthread_bi += computation_test_3d(
                        ComputeMode.SELECT,
                        stencil,
                        ThreadingMode.POOL,
                        ThreadType.VIRTUAL,
                        dim_divisor * dim_divisor, // square so that it's one per chunk
                        dim_divisor,
                        ISLType.FIXED_LOOP_BORDERS
                );
                total_platform_bi += computation_test_3d(
                        ComputeMode.SELECT,
                        stencil,
                        ThreadingMode.POOL,
                        ThreadType.PLATFORM,
                        dim_divisor * dim_divisor, // square so that it's one per chunk
                        dim_divisor,
                        ISLType.FIXED_LOOP_BORDERS
                );
            }

            long avg_vthread = total_vthread / TEST_NUM;
            long avg_platform = total_platform / TEST_NUM;
            long avg_vthread_bi = total_vthread_bi / TEST_NUM;
            long avg_platform_bi = total_platform_bi / TEST_NUM;
            if (dim_divisor < MAX_DIM_DIVISOR){
                writer_vthread.append(avg_vthread+",");
                writer_platform.append(avg_platform+",");
                writer_vthread_bi.append(avg_vthread_bi+",");
                writer_platform_bi.append(avg_platform_bi+",");
            } else {
                writer_vthread.append(avg_vthread+"");
                writer_platform.append(avg_platform+"");
                writer_vthread_bi.append(avg_vthread_bi+"");
                writer_platform_bi.append(avg_platform_bi+"");
            }
            System.out.println("Pool with " + dim_divisor * dim_divisor * dim_divisor + " threads & chunks");
            writer_platform.flush();
            writer_vthread.flush();
            writer_platform_bi.flush();
            writer_vthread_bi.flush();
        }

        writer_vthread.println();
        writer_platform.println();
        writer_vthread_bi.println();
        writer_platform_bi.println();

        for (int dim_divisor = 1; dim_divisor <= MAX_DIM_DIVISOR; dim_divisor++) {
            long total_vthread = 0;
            long total_platform = 0;
            long total_vthread_bi = 0;
            long total_platform_bi = 0;
            for (int t = 0; t < TEST_NUM; t++) {
                total_vthread += computation_test_3d(
                        ComputeMode.SELECT,
                        stencil,
                        ThreadingMode.PER_CHUNK,
                        ThreadType.VIRTUAL,
                        dim_divisor * dim_divisor, // square so that it's one per chunk
                        dim_divisor,
                        ISLType.FIXED_LOOP
                );
                total_platform += computation_test_3d(
                        ComputeMode.SELECT,
                        stencil,
                        ThreadingMode.PER_CHUNK,
                        ThreadType.PLATFORM,
                        dim_divisor * dim_divisor, // square so that it's one per chunk
                        dim_divisor,
                        ISLType.FIXED_LOOP
                );
                total_vthread_bi += computation_test_3d(
                        ComputeMode.SELECT,
                        stencil,
                        ThreadingMode.PER_CHUNK,
                        ThreadType.VIRTUAL,
                        dim_divisor * dim_divisor, // square so that it's one per chunk
                        dim_divisor,
                        ISLType.FIXED_LOOP_BORDERS
                );
                total_platform_bi += computation_test_3d(
                        ComputeMode.SELECT,
                        stencil,
                        ThreadingMode.PER_CHUNK,
                        ThreadType.PLATFORM,
                        dim_divisor * dim_divisor, // square so that it's one per chunk
                        dim_divisor,
                        ISLType.FIXED_LOOP_BORDERS
                );
            }

            long avg_vthread = total_vthread / TEST_NUM;
            long avg_platform = total_platform / TEST_NUM;
            long avg_vthread_bi = total_vthread_bi / TEST_NUM;
            long avg_platform_bi = total_platform_bi / TEST_NUM;
            if (dim_divisor < MAX_DIM_DIVISOR){
                writer_vthread.append(avg_vthread+",");
                writer_platform.append(avg_platform+",");
                writer_vthread_bi.append(avg_vthread_bi+",");
                writer_platform_bi.append(avg_platform_bi+",");
            } else {
                writer_vthread.append(avg_vthread+"");
                writer_platform.append(avg_platform+"");
                writer_vthread_bi.append(avg_vthread_bi+"");
                writer_platform_bi.append(avg_platform_bi+"");
            }
            System.out.println("Per Chunk with " + dim_divisor * dim_divisor * dim_divisor + " threads & chunks");
            writer_platform.flush();
            writer_vthread.flush();
            writer_platform_bi.flush();
            writer_vthread_bi.flush();
        }

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
                        ISLType.FIXED_LOOP
                );
                total_time_iterate += computation_test_3d(
                        ComputeMode.ITERATE,
                        stencil,
                        ThreadingMode.PER_CHUNK,
                        ThreadType.VIRTUAL,
                        5*5*5,
                        5,
                        ISLType.FIXED_LOOP
                );
            }
            select_results[activations/7] = total_time_select / TEST_NUM;
            iterate_results[activations/7] = total_time_iterate / TEST_NUM;
            System.out.println("Activations " + activations + ": Select = " + (total_time_select / TEST_NUM) + " - Iterate = " + (total_time_iterate / TEST_NUM));
        }

        PrintWriter writer = new PrintWriter(results_path + "test_select_vs_iterate.csv");
        for (int i = 0; i < select_results.length - 1; i++) {
            writer.append(select_results[i]+",");
        }
        writer.append(select_results[select_results.length-1]+"\n");
        for (int i = 0; i < iterate_results.length - 1; i++) {
            writer.append(iterate_results[i]+",");
        }
        writer.append(iterate_results[iterate_results.length-1]+"");
        writer.flush();
    }

    public static long blur_giraffe_test(
            ComputeMode stencil_compute_mode,
            ThreadingMode threading_mode,
            ThreadType thread_type,
            int max_threads,
            int dim_divisor,
            ISLType isl_type
    ) {
        String image_path = resources_path + "lil_giraffe.jpg";
        int isl_loops = 5;

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
        stencil_computer.setISLType(isl_type);
        stencil_computer.setMaxLoops(isl_loops);

        // Start recording
        long start_time = System.nanoTime();
        stencil_computer.execute();
        long end_time = System.nanoTime();
        long total_time = end_time - start_time;

        // Copy output to new array
        FlatNumArray output_r = new FlatNumArray(stencil_computer.getOutput().getShape(),
                stencil_computer.getOutput().asArray());
        // Compute for green
        stencil_computer.setInputSpace(flat_data_g);
        start_time = System.nanoTime();
        stencil_computer.execute();
        end_time = System.nanoTime();
        total_time += end_time - start_time;
        // Copy output to new array
        FlatNumArray output_g = new FlatNumArray(stencil_computer.getOutput().getShape(),
                stencil_computer.getOutput().asArray());
        // Compute for blue
        stencil_computer.setInputSpace(flat_data_b);
        start_time = System.nanoTime();
        stencil_computer.execute();
        end_time = System.nanoTime();
        total_time += end_time - start_time;
        // Copy output to new array
        FlatNumArray output_b = new FlatNumArray(stencil_computer.getOutput().getShape(),
                stencil_computer.getOutput().asArray());
        // Stop recording

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
        return total_time / 1_000_000;
    }

    public static long computation_test_3d(
            ComputeMode stencil_compute_mode,
            Stencil stencil,
            ThreadingMode threading_mode,
            ThreadType thread_type,
            int max_threads,
            int dim_divisor,
            ISLType isl_type
    ){
        int isl_loops = 5;
        stencil.setCompMode(stencil_compute_mode);

        Integer[] input_shape = new Integer[] {100,100,100};
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
        stencil_computer.setISLType(isl_type);
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
                return 0; // Greater than 3 neighbours dies
            }else{
                if (nbr_count == 3){ // Dead cell becomes alive if it has 3 neighbours
                    return 1;
                }
                return 0; // Otherwise stays dead
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