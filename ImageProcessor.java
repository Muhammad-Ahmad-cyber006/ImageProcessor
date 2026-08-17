import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageProcessor {

    private static final int THREAD_POOL_SIZE = 8;

    private static final int SIMULATED_TILE_DELAY_MS = 8;

    private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private final CanvasRenderer renderer;

    public ImageProcessor(CanvasRenderer renderer) {
        this.renderer = renderer;
    }

    public BufferedImage process(BufferedImage image, ImageFilter filter, int tileSize, boolean async) {
        long startTime = System.currentTimeMillis();
        BufferedImage result = async
                ? processAsync(image, filter, tileSize)
                : processSync(image, filter, tileSize);
        System.out.printf("done in %d ms%n", System.currentTimeMillis() - startTime);
        return result;
    }

    public void shutdown() {
        executorService.shutdown();
    }

    private BufferedImage processSync(BufferedImage image, ImageFilter filter, int tileSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = combined.createGraphics();

        List<TileRegion> regions = tileRegions(width, height, tileSize);
        int done = 0;
        for (TileRegion region : regions) {
            Tile tile = processTile(image, filter, region);
            g.drawImage(tile.image(), tile.x(), tile.y(), null);
            renderer.enqueue(tile);
            simulateWork();
            done++;
            printProgress(done, regions.size());
        }
        g.dispose();

        System.out.println();
        return combined;
    }

    private BufferedImage processAsync(BufferedImage image, ImageFilter filter, int tileSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        List<CompletableFuture<Tile>> futures = submitTileJobs(image, filter, tileSize);
        futures.forEach(future -> future.thenAccept(renderer::enqueue)
                .exceptionally(ex -> {
                    System.err.println("tile error: " + ex.getMessage());
                    return null;
                }));

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        System.out.println();

        return assembleImage(futures, width, height);
    }

    private List<CompletableFuture<Tile>> submitTileJobs(BufferedImage image, ImageFilter filter, int tileSize) {
        List<TileRegion> regions = tileRegions(image.getWidth(), image.getHeight(), tileSize);
        List<CompletableFuture<Tile>> futures = new ArrayList<>();
        AtomicInteger done = new AtomicInteger(0);

        for (TileRegion region : regions) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                Tile tile = processTile(image, filter, region);
                simulateWork();
                printProgress(done.incrementAndGet(), regions.size());
                return tile;
            }, executorService));
        }
        return futures;
    }

    private Tile processTile(BufferedImage image, ImageFilter filter, TileRegion region) {
        int pad = filter.paddingRadius();
        if (pad <= 0) {
            BufferedImage subImage = image.getSubimage(region.x(), region.y(), region.width(), region.height());
            return new Tile(filter.apply(subImage), region.x(), region.y(), region.width(), region.height());
        }

        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        int paddedX = Math.max(0, region.x() - pad);
        int paddedY = Math.max(0, region.y() - pad);
        int paddedWidth = Math.min(imageWidth, region.x() + region.width() + pad) - paddedX;
        int paddedHeight = Math.min(imageHeight, region.y() + region.height() + pad) - paddedY;

        BufferedImage paddedSource = image.getSubimage(paddedX, paddedY, paddedWidth, paddedHeight);
        BufferedImage paddedResult = filter.apply(paddedSource);

        int cropX = region.x() - paddedX;
        int cropY = region.y() - paddedY;
        BufferedImage cropped = paddedResult.getSubimage(cropX, cropY, region.width(), region.height());

        return new Tile(cropped, region.x(), region.y(), region.width(), region.height());
    }

    private void simulateWork() {
        try {
            Thread.sleep(Math.max(0, SIMULATED_TILE_DELAY_MS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void printProgress(int done, int total) {
        int barWidth = 30;
        int filled = (int) ((double) done / total * barWidth);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < barWidth; i++) {
            bar.append(i < filled ? '#' : '-');
        }
        System.out.printf("\r[%s] %d/%d", bar, done, total);
    }

    private List<TileRegion> tileRegions(int width, int height, int tileSize) {
        List<TileRegion> regions = new ArrayList<>();
        for (int y = 0; y < height; y += tileSize) {
            int tileHeight = Math.min(tileSize, height - y);
            for (int x = 0; x < width; x += tileSize) {
                int tileWidth = Math.min(tileSize, width - x);
                regions.add(new TileRegion(x, y, tileWidth, tileHeight));
            }
        }
        return regions;
    }

    private BufferedImage assembleImage(List<CompletableFuture<Tile>> futures, int width, int height) {
        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = combined.createGraphics();
        for (CompletableFuture<Tile> future : futures) {
            try {
                Tile tile = future.get();
                g.drawImage(tile.image(), tile.x(), tile.y(), null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                System.err.println("tile failed: " + e.getMessage());
            }
        }
        g.dispose();
        return combined;
    }

    private record TileRegion(int x, int y, int width, int height) {
    }
}
