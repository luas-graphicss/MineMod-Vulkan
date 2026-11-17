package io.github.luasgraphicss.minemod.verification.visual;

import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyPlan.Scene;
import io.github.luasgraphicss.minemod.verification.visual.CellColors.Orientation;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

final class DifferenceImage {
    private static final String FORMAT = "png";
    private static final int MARK = 0xFF0000;
    private static final int HALF_OF_EACH_CHANNEL = 0x7F7F7F;

    private DifferenceImage() {}

    static void write(BufferedImage reference, BufferedImage candidate, Scene scene, Path file) throws IOException {
        CellColors expected = CellColors.of(reference, scene.cellSize(), Orientation.UPRIGHT);
        CellColors drawn = CellColors.of(candidate, scene.cellSize(), Orientation.UPRIGHT);
        BufferedImage marked =
                new BufferedImage(candidate.getWidth(), candidate.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < candidate.getHeight(); y++) {
            for (int x = 0; x < candidate.getWidth(); x++) {
                int cell = y / scene.cellSize() * expected.columns() + x / scene.cellSize();
                int color = candidate.getRGB(x, y);
                boolean differs = expected.differenceAt(cell, drawn) > scene.channelTolerance();
                marked.setRGB(x, y, differs ? halved(color) + halved(MARK) : color);
            }
        }
        ImageIO.write(marked, FORMAT, file.toFile());
    }

    private static int halved(int color) {
        return color >> 1 & HALF_OF_EACH_CHANNEL;
    }
}
