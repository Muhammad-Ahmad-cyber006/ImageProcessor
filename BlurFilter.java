import java.awt.image.BufferedImage;
public class BlurFilter implements ImageFilter {

    private static final int RADIUS = 2;

    @Override
    public int paddingRadius() {
        return RADIUS;
    }

    @Override
    public BufferedImage apply(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();
        int[] source = original.getRGB(0, 0, width, height, null, 0, width);
        int[] result = new int[source.length];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y * width + x] = averageNeighbourhood(source, width, height, x, y);
            }
        }

        BufferedImage blurred = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        blurred.setRGB(0, 0, width, height, result, 0, width);
        return blurred;
    }

    private int averageNeighbourhood(int[] pixels, int width, int height, int x, int y) {
        long r = 0, g = 0, b = 0;
        int count = 0;

        for (int dy = -RADIUS; dy <= RADIUS; dy++) {
            int ny = y + dy;
            if (ny < 0 || ny >= height) continue;
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                int nx = x + dx;
                if (nx < 0 || nx >= width) continue;
                int rgb = pixels[ny * width + nx];
                r += (rgb >> 16) & 0xFF;
                g += (rgb >> 8) & 0xFF;
                b += rgb & 0xFF;
                count++;
            }
        }

        return (((int) (r / count)) << 16) | (((int) (g / count)) << 8) | ((int) (b / count));
    }
}
