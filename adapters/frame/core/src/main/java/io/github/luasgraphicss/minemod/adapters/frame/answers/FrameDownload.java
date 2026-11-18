package io.github.luasgraphicss.minemod.adapters.frame.answers;

import io.github.luasgraphicss.minemod.adapters.frame.session.FrameScene;
import io.github.luasgraphicss.minemod.client.frame.FramePixels;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Optional;

public final class FrameDownload {
    private static final int BYTE_MASK = 0xFF;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;
    private static final int ALPHA_SHIFT = 24;

    private FrameDownload() {}

    public static void asPackedColors(FrameScene scene, int width, int height, IntBuffer into) {
        Optional<FramePixels> frame = frameOf(scene, width, height);
        if (!frame.isPresent() || into.remaining() < width * height) {
            return;
        }
        ByteBuffer rgba = frame.get().rgba();
        int first = into.position();
        for (int row = 0; row < height; row++) {
            int from = rgba.position() + (height - 1 - row) * width * FramePixels.PIXEL_BYTES;
            for (int column = 0; column < width; column++) {
                int pixel = from + column * FramePixels.PIXEL_BYTES;
                into.put(
                        first + row * width + column,
                        (rgba.get(pixel + 3) & BYTE_MASK) << ALPHA_SHIFT
                                | (rgba.get(pixel) & BYTE_MASK) << RED_SHIFT
                                | (rgba.get(pixel + 1) & BYTE_MASK) << GREEN_SHIFT
                                | (rgba.get(pixel + 2) & BYTE_MASK));
            }
        }
    }

    public static void asRgba(FrameScene scene, int width, int height, ByteBuffer into) {
        Optional<FramePixels> frame = frameOf(scene, width, height);
        int rowBytes = width * FramePixels.PIXEL_BYTES;
        if (!frame.isPresent() || into.remaining() < (long) rowBytes * height) {
            return;
        }
        ByteBuffer rgba = frame.get().rgba();
        for (int row = 0; row < height; row++) {
            ByteBuffer frameRow = rgba.duplicate();
            frameRow.position(rgba.position() + (height - 1 - row) * rowBytes);
            frameRow.limit(frameRow.position() + rowBytes);
            ByteBuffer imageRow = into.duplicate();
            imageRow.position(into.position() + row * rowBytes);
            imageRow.put(frameRow);
        }
    }

    private static Optional<FramePixels> frameOf(FrameScene scene, int width, int height) {
        return scene.lastFrame().filter(frame -> frame.width() == width && frame.height() == height);
    }
}
