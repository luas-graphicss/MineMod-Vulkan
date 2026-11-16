package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.screenshot;

import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.client.frame.FramePixels;
import io.github.luasgraphicss.minemod.client.session.ClientRenderer;
import java.nio.ByteBuffer;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import org.lwjgl.system.MemoryUtil;

public final class FrameDownload {
    private static final int ALPHA_OFFSET = 3;
    private static final byte OPAQUE = (byte) 0xFF;

    private FrameDownload() {}

    public static void intoRgba(long pixels, int width, int height, boolean isOpaque) {
        long bytes = (long) width * height * FramePixels.PIXEL_BYTES;
        if (bytes < 1 || bytes > Integer.MAX_VALUE) {
            return;
        }
        ByteBuffer image = MemoryUtil.memByteBuffer(pixels, (int) bytes);
        MemoryUtil.memSet(image, 0);
        Optional<FramePixels> frame = showsTheWindow() ? lastFrame() : Optional.empty();
        if (!frame.isPresent() || frame.get().width() != width || frame.get().height() != height) {
            return;
        }
        copyBottomRowFirst(frame.get(), image);
        if (isOpaque) {
            hideNothing(image);
        }
    }

    private static boolean showsTheWindow() {
        int downloaded = ClientRendering.openGl().textures().bound();
        return downloaded == Minecraft.getInstance().getMainRenderTarget().getColorTextureId();
    }

    private static Optional<FramePixels> lastFrame() {
        return ClientRendering.running().flatMap(ClientRenderer::readFrame);
    }

    private static void copyBottomRowFirst(FramePixels frame, ByteBuffer image) {
        int rowBytes = frame.width() * FramePixels.PIXEL_BYTES;
        ByteBuffer rgba = frame.rgba();
        for (int row = 0; row < frame.height(); row++) {
            ByteBuffer frameRow = rgba.duplicate();
            frameRow.position((frame.height() - 1 - row) * rowBytes);
            frameRow.limit(frameRow.position() + rowBytes);
            ByteBuffer imageRow = image.duplicate();
            imageRow.position(row * rowBytes);
            imageRow.put(frameRow);
        }
    }

    private static void hideNothing(ByteBuffer image) {
        for (int alpha = ALPHA_OFFSET; alpha < image.limit(); alpha += FramePixels.PIXEL_BYTES) {
            image.put(alpha, OPAQUE);
        }
    }
}
