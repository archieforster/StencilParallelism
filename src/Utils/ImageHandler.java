package Utils;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageHandler {
    public Integer[][] loadPng(String filePath) {
        // Read file
        BufferedImage image = null;
        try {
            image = ImageIO.read(new File(filePath));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        Integer[][] result = new Integer[height][width];

        // Load pixel data into 2D array
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y][x] = image.getRGB(x, y);
            }
        }
        return result;
    }

    public void savePng(FlatNumArray image_data, String filePath) throws IOException {
        int height = image_data.getShape()[1];
        int width = image_data.getShape()[0];

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Convert array back to image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, image_data.get(new Integer[] {x,y}).intValue());
            }
        }

        ImageIO.write(image, "jpg", new File(filePath));
    }
}
