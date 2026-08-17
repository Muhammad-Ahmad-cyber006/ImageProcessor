import javafx.animation.AnimationTimer;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CanvasRenderer {

    private static final double MAX_SCREEN_FRACTION = 0.7;

    private final BlockingQueue<Tile> pendingTiles = new LinkedBlockingQueue<>();
    private GraphicsContext graphics;
    private double scale = 1.0;
    private boolean drawLoopStarted = false;


    public void show(Stage stage, int imageWidth, int imageHeight) {
        pendingTiles.clear();

        scale = computeScale(imageWidth, imageHeight);
        int windowWidth = (int) Math.round(imageWidth * scale);
        int windowHeight = (int) Math.round(imageHeight * scale);

        Canvas canvas = new Canvas(windowWidth, windowHeight);
        graphics = canvas.getGraphicsContext2D();
        graphics.setImageSmoothing(true);
        graphics.clearRect(0, 0, windowWidth, windowHeight);

        Group root = new Group(canvas);
        stage.setScene(new Scene(root, windowWidth, windowHeight));
        stage.setResizable(true);
        stage.setAlwaysOnTop(false);
        stage.centerOnScreen();
        stage.show();

        startDrawLoopOnce();
    }


    public void close(Stage stage) {
        stage.close();
    }

    private void startDrawLoopOnce() {
        if (drawLoopStarted) {
            return;
        }
        drawLoopStarted = true;
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                Tile tile;
                while ((tile = pendingTiles.poll()) != null) {
                    draw(tile);
                }
            }
        }.start();
    }

    private double computeScale(int imageWidth, int imageHeight) {
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double maxWidth = screen.getWidth() * MAX_SCREEN_FRACTION;
        double maxHeight = screen.getHeight() * MAX_SCREEN_FRACTION;
        double widthScale = maxWidth / imageWidth;
        double heightScale = maxHeight / imageHeight;
        return Math.min(1.0, Math.min(widthScale, heightScale));
    }

    private void draw(Tile tile) {
        graphics.drawImage(SwingFXUtils.toFXImage(tile.image(), null),
                tile.x() * scale, tile.y() * scale,
                tile.width() * scale, tile.height() * scale);
    }

    public void enqueue(Tile tile) {
        try {
            pendingTiles.put(tile);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupted while queuing tile for drawing");
        }
    }
}
