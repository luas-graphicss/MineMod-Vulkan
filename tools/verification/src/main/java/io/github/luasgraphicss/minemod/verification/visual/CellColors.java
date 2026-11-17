package io.github.luasgraphicss.minemod.verification.visual;

import java.awt.image.BufferedImage;

final class CellColors {
    private static final int CHANNELS = 3;
    private static final int CHANNEL_MASK = 0xFF;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;
    private static final double FLAT_SPREAD = 2.0;

    private final int columns;
    private final int rows;
    private final double[] means;

    private CellColors(int columns, int rows, double[] means) {
        this.columns = columns;
        this.rows = rows;
        this.means = means;
    }

    enum Orientation {
        UPRIGHT,
        UPSIDE_DOWN,
        MIRRORED
    }

    static CellColors of(BufferedImage image, int cellSize, Orientation orientation) {
        int width = image.getWidth();
        int height = image.getHeight();
        int columns = (width + cellSize - 1) / cellSize;
        int rows = (height + cellSize - 1) / cellSize;
        double[] sums = new double[columns * rows * CHANNELS];
        int[] pixels = new int[columns * rows];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int sourceX = orientation == Orientation.MIRRORED ? width - 1 - x : x;
                int sourceY = orientation == Orientation.UPSIDE_DOWN ? height - 1 - y : y;
                int color = image.getRGB(sourceX, sourceY);
                int cell = y / cellSize * columns + x / cellSize;
                sums[cell * CHANNELS] += color >> RED_SHIFT & CHANNEL_MASK;
                sums[cell * CHANNELS + 1] += color >> GREEN_SHIFT & CHANNEL_MASK;
                sums[cell * CHANNELS + 2] += color & CHANNEL_MASK;
                pixels[cell]++;
            }
        }
        for (int cell = 0; cell < pixels.length; cell++) {
            for (int channel = 0; channel < CHANNELS; channel++) {
                sums[cell * CHANNELS + channel] /= pixels[cell];
            }
        }
        return new CellColors(columns, rows, sums);
    }

    int columns() {
        return columns;
    }

    int cells() {
        return columns * rows;
    }

    double differenceAt(int cell, CellColors other) {
        double widest = 0;
        for (int channel = 0; channel < CHANNELS; channel++) {
            int index = cell * CHANNELS + channel;
            widest = Math.max(widest, Math.abs(means[index] - other.means[index]));
        }
        return widest;
    }

    double differingFraction(CellColors other, int channelTolerance) {
        int differing = 0;
        for (int cell = 0; cell < cells(); cell++) {
            if (differenceAt(cell, other) > channelTolerance) {
                differing++;
            }
        }
        return (double) differing / cells();
    }

    double meanError(CellColors other) {
        double total = 0;
        for (int index = 0; index < means.length; index++) {
            total += Math.abs(means[index] - other.means[index]);
        }
        return total / means.length;
    }

    boolean isFlat() {
        for (int channel = 0; channel < CHANNELS; channel++) {
            double lowest = Double.MAX_VALUE;
            double highest = 0;
            for (int cell = 0; cell < cells(); cell++) {
                lowest = Math.min(lowest, means[cell * CHANNELS + channel]);
                highest = Math.max(highest, means[cell * CHANNELS + channel]);
            }
            if (highest - lowest > FLAT_SPREAD) {
                return false;
            }
        }
        return true;
    }
}
