import Patterns.ComputeMode;
import Patterns.Stencil;
import Processing.Computer;
import Processing.ISLType;
import Processing.ThreadingMode;
import Utils.FlatNumArray;
import Utils.ImageHandler;

import java.io.IOException;


public class Main {
    public static void main(String[] args) {
        String image_path = "C:\\Users\\archi\\Downloads\\pngwing.com.png";

        ImageHandler image_handler = new ImageHandler();
        Integer[][] image_data = image_handler.loadPng(image_path);

        Integer[] image_shape = new Integer[] {image_data.length ,image_data[0].length};
        FlatNumArray flat_data = new FlatNumArray(image_shape,image_data);

        Stencil stencil = new Stencil(
//                new Integer[] {2,2},
//                new Integer[][] {
//                        {1,2,3,2,1},
//                        {2,3,4,3,2},
//                        {3,4,5,4,3},
//                        {2,3,4,3,2},
//                        {1,2,3,2,1}
//                },
                new Integer[] {1,1},
                new Integer[][] {
                        {0,1,0},
                        {1,2,1},
                        {0,1,0}
                },
                x->(x.stream().reduce(0, (a,b) -> (a.doubleValue() + b.doubleValue()))).doubleValue()/6
        );
        stencil.setCompMode(ComputeMode.ITERATE);

        Computer stencil_computer = new Computer(flat_data,stencil);
        stencil_computer.setThreadingMode(ThreadingMode.POOL,4);
        stencil_computer.setDimDivisor(4);
        stencil_computer.setISLType(ISLType.FIXED_LOOP);
        stencil_computer.setMaxLoops(1);

        stencil_computer.execute();

        // Repackage to image
        FlatNumArray output = stencil_computer.getOutput();
        String output_path = "C:\\Users\\archi\\Documents\\UniWork\\Dissertation\\output.png";
        try {
            image_handler.savePng(output,output_path);
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}