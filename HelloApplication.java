import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

public class HelloApplication extends Application {

    private static final int TILE_SIZE = 40;
    // How long to leave the finished image on screen before closing the window.
    private static final long RESULT_DISPLAY_MS = 2000;

    private static final List<ImageFilter> FILTERS = List.of(
            new GreyScaleFilter(),
            new InvertFilter(),
            new SepiaFilter(),
            new BlurFilter()
    );
    private static final List<String> FILTER_NAMES = List.of("Greyscale", "Invert", "Sepia", "Blur");

    private final Scanner input = new Scanner(System.in);
    private final CanvasRenderer renderer = new CanvasRenderer();
    private final ImageFileIO imageIO = new ImageFileIO();
    private final ImageProcessor processor = new ImageProcessor(renderer);
    private boolean changeImageRequested = false;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Image Processor App");

        Platform.setImplicitExit(false);

        System.out.println("Image Processor App");
        System.out.println("--------------------");

        new Thread(() -> runApp(stage)).start();
    }

    private void runApp(Stage stage) {
        Optional<LoadedImage> current = pickImage();
        if (current.isEmpty()) {
            Platform.exit();
            return;
        }

        boolean running = true;
        while (running) {
            running = showFilterMenu(stage, current.get());
            if (running && changeImageRequested) {
                changeImageRequested = false;
                Optional<LoadedImage> next = pickImage();
                if (next.isEmpty()) {
                    running = false;
                } else {
                    current = next;
                }
            }
        }

        processor.shutdown();
        input.close();
        System.out.println("bye.");
        Platform.exit();
    }
    private boolean showFilterMenu(Stage stage, LoadedImage current) {
        while (true) {
            printMenu();
            int choice = readChoice();

            if (choice >= 1 && choice <= FILTERS.size()) {
                String filterName = FILTER_NAMES.get(choice - 1);
                boolean async = askSyncOrAsync();
                String mode = async ? "async" : "sync";

                openWindow(stage, current.image.getWidth(), current.image.getHeight());
                BufferedImage filtered = processor.process(
                        current.image, FILTERS.get(choice - 1), TILE_SIZE, async);
                imageIO.saveImage(filtered, current.fileName, filterName, mode);
                holdThenCloseWindow(stage);
                continue;
            }

            int changeImageOption = FILTERS.size() + 1;
            int exitOption = FILTERS.size() + 2;
            if (choice == changeImageOption) {
                changeImageRequested = true;
                return true;
            }
            if (choice == exitOption) {
                return false;
            }
            System.out.println("invalid option");
        }
    }

    private void printMenu() {
        System.out.println();
        for (int i = 0; i < FILTER_NAMES.size(); i++) {
            System.out.printf("%d) %s%n", i + 1, FILTER_NAMES.get(i));
        }
        int n = FILTER_NAMES.size();
        System.out.printf("%d) change image%n", n + 1);
        System.out.printf("%d) exit%n", n + 2);
        System.out.print("> ");
    }

    private boolean askSyncOrAsync() {
        while (true) {
            System.out.println("1) Process asynchronously");
            System.out.println("2) Process synchronously");
            System.out.print("> ");
            int choice = readChoice();
            if (choice == 1) return true;
            if (choice == 2) return false;
            System.out.println("enter 1 or 2");
        }
    }

    private int readChoice() {
        if (input.hasNextInt()) {
            return input.nextInt();
        }
        input.next(); 
        return -1;
    }

    private Optional<LoadedImage> pickImage() {
        List<File> images = imageIO.listInputImages();

        if (images.isEmpty()) {
            System.err.println("no images in 'input' folder");
            System.err.println("supported: .jpg .jpeg .png .bmp .gif");
            return Optional.empty();
        }

        File selected;
        if (images.size() == 1) {
            selected = images.get(0);
            System.out.println("image: " + selected.getName());
        } else {
            System.out.println("images in 'input':");
            for (int i = 0; i < images.size(); i++) {
                System.out.printf("%d) %s%n", i + 1, images.get(i).getName());
            }
            System.out.print("> ");
            int choice = readChoice();
            if (choice < 1 || choice > images.size()) {
                System.err.println("invalid choice");
                return Optional.empty();
            }
            selected = images.get(choice - 1);
        }

        Optional<BufferedImage> imageOpt = imageIO.readImage(selected);
        if (imageOpt.isEmpty()) {
            System.err.println("could not read: " + selected.getPath());
            return Optional.empty();
        }

        BufferedImage image = imageOpt.get();
        System.out.printf("%s (%dx%d)%n", selected.getName(), image.getWidth(), image.getHeight());
        return Optional.of(new LoadedImage(image, selected.getName()));
    }

    private void openWindow(Stage stage, int width, int height) {
        CountDownLatch ready = new CountDownLatch(1);
        Platform.runLater(() -> {
            renderer.show(stage, width, height);
            ready.countDown();
        });
        try {
            ready.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    private void holdThenCloseWindow(Stage stage) {
        try {
            Thread.sleep(RESULT_DISPLAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Platform.runLater(() -> renderer.close(stage));
    }

    private record LoadedImage(BufferedImage image, String fileName) {
    }
}
