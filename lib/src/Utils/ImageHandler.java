package Utils;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageHandler {
    public int[][] loadPng(String filePath) throws IOException {
        // Read file
        BufferedImage image = ImageIO.read(new File(filePath));

        int width = image.getWidth();
        int height = image.getHeight();
        int[][] result = new int[height][width];

        // Load pixel data into 2D array
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y][x] = image.getRGB(x, y);
            }
        }
        return result;
    }

    public void savePng(int[][] image_data, String filePath) throws IOException {
        int height = image_data.length;
        int width = image_data[0].length;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Convert array back to image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, image_data[y][x]);
            }
        }

        ImageIO.write(image, "png", new File(filePath));
    }
}
