import java.awt.image.BufferedImage;
public class InvertFilter implements ImageFilter {

    @Override
    public BufferedImage apply(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();

        int[] pixels = original.getRGB(0, 0, width, height, null, 0, width);
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = invert(pixels[i]);
        }

        BufferedImage inverted = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        inverted.setRGB(0, 0, width, height, pixels, 0, width);
        return inverted;
    }

    private int invert(int rgb) {
        int r = 255 - ((rgb >> 16) & 0xFF);
        int g = 255 - ((rgb >> 8) & 0xFF);
        int b = 255 - (rgb & 0xFF);
        return (r << 16) | (g << 8) | b;
    }
}
