import java.awt.image.BufferedImage;

public class SepiaFilter implements ImageFilter {

    @Override
    public BufferedImage apply(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();

        int[] pixels = original.getRGB(0, 0, width, height, null, 0, width);
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = toSepia(pixels[i]);
        }

        BufferedImage sepia = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        sepia.setRGB(0, 0, width, height, pixels, 0, width);
        return sepia;
    }

    private int toSepia(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        int sepiaR = clamp((int) (0.393 * r + 0.769 * g + 0.189 * b));
        int sepiaG = clamp((int) (0.349 * r + 0.686 * g + 0.168 * b));
        int sepiaB = clamp((int) (0.272 * r + 0.534 * g + 0.131 * b));

        return (sepiaR << 16) | (sepiaG << 8) | sepiaB;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
