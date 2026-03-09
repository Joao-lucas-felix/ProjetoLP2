package org.example.pixelforging;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PixelForging {
    private static final int COLOR_BLOCK_WIDTH = 50;
    private static final int COLOR_BLOCK_HEIGHT = 50;
    private static final int COLORS_PER_ROW_DEFAULT = 3;
    private static final int COLOR_NUM_DEFAULT = 6;

    public static BufferedImage decodeImage(String path) throws IOException {
        return ImageIO.read(new File(path));
    }

    public static List<Color> listPixelsOrdered(BufferedImage img){
        List<Color> colors = new ArrayList<>();
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x =0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x,y);
                Color c = new Color(rgb, true);
                colors.add(c);
            }
        }
        return  colors;
    }
    public static List<Color> listPixelsParallel(BufferedImage img) {
        int height = img.getHeight();
        int workers = Math.min(32, img.getWidth());
        List<Color> result = new ArrayList<>();

        try (
                ExecutorService executor = Executors.newFixedThreadPool(workers);
        ) {
            List<Future<List<Color>>> futures = new ArrayList<>();

            for (int y = 0; y < height; y++) {
                final int line = y;
                futures.add(
                        executor.submit(
                                () -> {
                                    List<Color> lineColors = new ArrayList<>();
                                    for (int x = 0; x < img.getWidth(); x++) {
                                        int rgb = img.getRGB(x, line);
                                        lineColors.add(new Color(rgb, true));
                                    }
                                    return lineColors;
                                }
                        )
                );

            }

            for (Future<List<Color>> f : futures) {
                try {
                    result.addAll(f.get());
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }

            executor.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
    public static List<Color> getUniqueColors(List<Color> colors) {

        Map<Integer, Integer> freq = new HashMap<>();

        for (Color c : colors) {

            int rgb = c.getRGB();

            freq.put(rgb, freq.getOrDefault(rgb, 0) + 1);
        }

        List<Color> unique = new ArrayList<>();

        for (Integer rgb : freq.keySet()) {
            unique.add(new Color(rgb, true));
        }

        unique.sort((a, b) ->
                freq.get(b.getRGB()) - freq.get(a.getRGB())
        );

        return unique;
    }

    public static float[] rgbToHsl(Color c) {

        float r = c.getRed() / 255f;
        float g = c.getGreen() / 255f;
        float b = c.getBlue() / 255f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));

        float h, s, l;

        l = (max + min) / 2f;

        if (max == min) {
            h = 0;
            s = 0;
        } else {

            float delta = max - min;

            s = delta / (1 - Math.abs(2 * l - 1));

            if (max == r)
                h = ((g - b) / delta) % 6;
            else if (max == g)
                h = (b - r) / delta + 2;
            else
                h = (r - g) / delta + 4;

            h *= 60;

            if (h < 0) h += 360;
        }

        return new float[]{h, s, l};
    }
    public static BufferedImage createColorPalette(
            List<Color> colors,
            int colorsPerRow,
            int width,
            int height
    ) {

        int rows = (int) Math.ceil(colors.size() / (double) colorsPerRow);

        BufferedImage palette = new BufferedImage(
                colorsPerRow * width,
                rows * height,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g = palette.createGraphics();

        for (int i = 0; i < colors.size(); i++) {

            int row = i / colorsPerRow;
            int col = i % colorsPerRow;

            g.setColor(colors.get(i));

            g.fillRect(
                    col * width,
                    row * height,
                    width,
                    height
            );
        }

        g.dispose();

        return palette;
    }

    public static void saveImage(BufferedImage img, String path) throws IOException {
        ImageIO.write(img, "png", new File(path));
    }

    public static BufferedImage extractColorPalette(
            BufferedImage image,
            int colorsPerRow,
            int colorWidth,
            int colorHeight,
            int colorNum
    ) throws InterruptedException {

        if (colorsPerRow == 0) colorsPerRow = COLORS_PER_ROW_DEFAULT;
        if (colorWidth == 0) colorWidth = COLOR_BLOCK_WIDTH;
        if (colorHeight == 0) colorHeight = COLOR_BLOCK_HEIGHT;
        if (colorNum == 0) colorNum = COLOR_NUM_DEFAULT;

        List<Color> pixels = listPixelsParallel(image);

        List<Color> unique = getUniqueColors(pixels);

        List<Color> top = unique.subList(0, Math.min(colorNum, unique.size()));

        return createColorPalette(top, colorsPerRow, colorWidth, colorHeight);
    }

    public static void main(String[] args) throws Exception {

        BufferedImage img = decodeImage("input.png");

        BufferedImage palette = extractColorPalette(
                img,
                3,
                50,
                50,
                6
        );

        saveImage(palette, "palette.png");
    }
}
