package io.github.luasgraphicss.minemod.verification.visual;

import java.awt.image.BufferedImage;

final class Pictures {
    static final int WIDTH = 128;
    static final int HEIGHT = 96;

    private static final int SKY = 0x78A7FF;
    private static final int GROUND = 0x5B8C3A;
    private static final int WALL = 0xC8C8C8;
    private static final int HORIZON = 60;
    private static final int WALL_LEFT = 16;
    private static final int WALL_RIGHT = 48;
    private static final int WALL_TOP = 30;

    private Pictures() {}

    static BufferedImage scene() {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                boolean isWall = x >= WALL_LEFT && x < WALL_RIGHT && y >= WALL_TOP && y < HORIZON;
                image.setRGB(x, y, isWall ? WALL : y < HORIZON ? SKY : GROUND);
            }
        }
        return image;
    }

    static BufferedImage flat(int color) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                image.setRGB(x, y, color);
            }
        }
        return image;
    }

    static BufferedImage upsideDown(BufferedImage image) {
        BufferedImage turned = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                turned.setRGB(x, image.getHeight() - 1 - y, image.getRGB(x, y));
            }
        }
        return turned;
    }

    static BufferedImage mirrored(BufferedImage image) {
        BufferedImage turned = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                turned.setRGB(image.getWidth() - 1 - x, y, image.getRGB(x, y));
            }
        }
        return turned;
    }

    static BufferedImage brightened(BufferedImage image, int amount) {
        BufferedImage brighter = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int color = image.getRGB(x, y);
                int red = Math.min(255, (color >> 16 & 0xFF) + amount);
                int green = Math.min(255, (color >> 8 & 0xFF) + amount);
                int blue = Math.min(255, (color & 0xFF) + amount);
                brighter.setRGB(x, y, red << 16 | green << 8 | blue);
            }
        }
        return brighter;
    }

    static BufferedImage withoutTheWall(BufferedImage image) {
        BufferedImage emptied = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                emptied.setRGB(x, y, image.getRGB(x, y) == (0xFF000000 | WALL) ? SKY : image.getRGB(x, y));
            }
        }
        return emptied;
    }
}
