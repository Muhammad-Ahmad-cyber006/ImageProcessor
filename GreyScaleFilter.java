import java.awt.image.BufferedImage;

public class GreyScaleFilter implements ImageFilter {

    private static final double RED_WEIGHT = 0.2126;
    private static final double GREEN_WEIGHT = 0.7152;
    private static final double BLUE_WEIGHT = 0.0722;

    @Override
    public BufferedImage apply(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();

        int[] pixels = original.getRGB(0, 0, width, height, null, 0, width);
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = toGrey(pixels[i]);
        }

        BufferedImage grey = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        grey.setRGB(0, 0, width, height, pixels, 0, width);
        return grey;
    }

    private int toGrey(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int gray = (int) (RED_WEIGHT * r + GREEN_WEIGHT * g + BLUE_WEIGHT * b);
        return (gray << 16) | (gray << 8) | gray;
    }
}
