import java.awt.image.BufferedImage;
public interface ImageFilter {
    BufferedImage apply(BufferedImage image);


    default int paddingRadius() {
        return 0;
    }
}
