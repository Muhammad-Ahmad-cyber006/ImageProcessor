import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;


public class ImageFileIO {

    private static final String INPUT_DIR = "input";
    private static final String OUTPUT_DIR = "output";
    private static final List<String> SUPPORTED_EXTENSIONS =
            List.of(".jpg", ".jpeg", ".png", ".bmp", ".gif");

    public List<File> listInputImages() {
        File inputDir = new File(INPUT_DIR);
        File[] files = inputDir.listFiles(this::isSupportedImage);
        if (files == null) {
            return List.of();
        }
        return Arrays.stream(files)
                .sorted(Comparator.comparing(File::getName))
                .toList();
    }

    private boolean isSupportedImage(File file) {
        if (!file.isFile()) {
            return false;
        }
        String name = file.getName().toLowerCase(Locale.ROOT);
        return SUPPORTED_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    public Optional<BufferedImage> readImage(File file) {
        try {
            return Optional.ofNullable(ImageIO.read(file));
        } catch (IOException e) {
            System.err.println("Unable to read image: " + file.getPath());
            return Optional.empty();
        }
    }

    public void saveImage(BufferedImage image, String originalFileName, String filterName, String mode) {
        if (image == null) {
            System.err.println("Nothing to save, image was null");
            return;
        }

        String baseName = stripExtension(originalFileName);
        String outputName = String.format("filtered_%s_%s_%s_%s.png",
                baseName, filterName.toLowerCase(Locale.ROOT), mode, uniqueSuffix());
        File outputFile = new File(OUTPUT_DIR, outputName);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            System.err.println("Could not create output directory: " + parentDir);
            return;
        }

        try {
            ImageIO.write(image, "png", outputFile);
            System.out.println("saved: " + outputFile.getPath());
        } catch (IOException e) {
            System.err.println("Failed to save image: " + e.getMessage());
        }
    }

    private String uniqueSuffix() {
        return String.valueOf(System.currentTimeMillis() % 1_000_000);
    }

    private String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}

