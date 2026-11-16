package io.github.luasgraphicss.minemod.client.opengl.textures;

import io.github.luasgraphicss.minemod.client.textures.TexelArea;
import java.nio.ByteBuffer;

public final class TexelUnpacker {
    private TexelUnpacker() {}

    public static ByteBuffer toRgba(ByteBuffer source, TexelFormat format, PixelStore store, int width, int height) {
        long sourceBytes = store.sourceBytes(format, width, height);
        long rgbaBytes = (long) width * height * TexelArea.TEXEL_BYTES;
        if (width < 1 || height < 1 || rgbaBytes > Integer.MAX_VALUE || source.remaining() < sourceBytes) {
            throw new IllegalArgumentException("unpacking " + width + "x" + height + " " + format + " texels reads "
                    + sourceBytes + " bytes, found " + source.remaining());
        }
        byte[] rgba = new byte[(int) rgbaBytes];
        int rowBytes = width * TexelArea.TEXEL_BYTES;
        long rowStride = store.rowStride(format, width);
        long firstByte = source.position() + store.firstByte(format, width);
        for (int row = 0; row < height; row++) {
            int rowStart = (int) (firstByte + row * rowStride);
            if (format == TexelFormat.RGBA) {
                ByteBuffer packedRow = source.duplicate();
                packedRow.position(rowStart);
                packedRow.get(rgba, row * rowBytes, rowBytes);
            } else {
                expandRow(source, format, rowStart, rgba, row * rowBytes, width);
            }
        }
        return ByteBuffer.wrap(rgba);
    }

    private static void expandRow(ByteBuffer source, TexelFormat format, int rowStart, byte[] rgba, int into, int width) {
        for (int texel = 0; texel < width; texel++) {
            format.readAsRgba(source, rowStart + texel * format.bytes(), rgba, into + texel * TexelArea.TEXEL_BYTES);
        }
    }
}
