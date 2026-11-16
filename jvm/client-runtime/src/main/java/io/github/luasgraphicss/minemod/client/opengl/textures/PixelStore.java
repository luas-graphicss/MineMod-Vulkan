package io.github.luasgraphicss.minemod.client.opengl.textures;

public final class PixelStore {
    private static final int UNPACK_ROW_LENGTH = 0x0CF2;
    private static final int UNPACK_SKIP_ROWS = 0x0CF3;
    private static final int UNPACK_SKIP_PIXELS = 0x0CF4;
    private static final int UNPACK_ALIGNMENT = 0x0CF5;
    private static final int DEFAULT_ALIGNMENT = 4;

    private int rowLength;
    private int skippedRows;
    private int skippedPixels;
    private int alignment = DEFAULT_ALIGNMENT;

    public void set(int parameter, int value) {
        switch (parameter) {
            case UNPACK_ROW_LENGTH:
                rowLength = Math.max(0, value);
                break;
            case UNPACK_SKIP_ROWS:
                skippedRows = Math.max(0, value);
                break;
            case UNPACK_SKIP_PIXELS:
                skippedPixels = Math.max(0, value);
                break;
            case UNPACK_ALIGNMENT:
                if (value == 1 || value == 2 || value == 4 || value == 8) {
                    alignment = value;
                }
                break;
            default:
                break;
        }
    }

    public long sourceBytes(TexelFormat format, int width, int height) {
        return firstByte(format, width) + (height - 1) * rowStride(format, width) + (long) width * format.bytes();
    }

    long firstByte(TexelFormat format, int width) {
        return skippedRows * rowStride(format, width) + (long) skippedPixels * format.bytes();
    }

    long rowStride(TexelFormat format, int width) {
        long rowBytes = (long) (rowLength > 0 ? rowLength : width) * format.bytes();
        return (rowBytes + alignment - 1) / alignment * alignment;
    }
}
