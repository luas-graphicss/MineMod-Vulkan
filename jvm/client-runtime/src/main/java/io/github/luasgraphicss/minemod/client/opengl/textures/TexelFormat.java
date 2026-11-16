package io.github.luasgraphicss.minemod.client.opengl.textures;

import java.nio.ByteBuffer;
import java.util.Optional;

public enum TexelFormat {
    RGBA(4),
    RGB(3),
    RED_GREEN(2),
    RED(1);

    private static final int OPENGL_RGBA = 0x1908;
    private static final int OPENGL_RGB = 0x1907;
    private static final int OPENGL_RED_GREEN = 0x8227;
    private static final int OPENGL_RED = 0x1903;
    private static final byte OPAQUE = (byte) 0xFF;

    private final int bytes;

    TexelFormat(int bytes) {
        this.bytes = bytes;
    }

    public static Optional<TexelFormat> ofOpenGl(int format) {
        switch (format) {
            case OPENGL_RGBA:
                return Optional.of(RGBA);
            case OPENGL_RGB:
                return Optional.of(RGB);
            case OPENGL_RED_GREEN:
                return Optional.of(RED_GREEN);
            case OPENGL_RED:
                return Optional.of(RED);
            default:
                return Optional.empty();
        }
    }

    public int bytes() {
        return bytes;
    }

    void readAsRgba(ByteBuffer source, int at, byte[] rgba, int into) {
        byte first = source.get(at);
        switch (this) {
            case RGBA:
                rgba[into] = first;
                rgba[into + 1] = source.get(at + 1);
                rgba[into + 2] = source.get(at + 2);
                rgba[into + 3] = source.get(at + 3);
                break;
            case RGB:
                rgba[into] = first;
                rgba[into + 1] = source.get(at + 1);
                rgba[into + 2] = source.get(at + 2);
                rgba[into + 3] = OPAQUE;
                break;
            case RED_GREEN:
                rgba[into] = first;
                rgba[into + 1] = first;
                rgba[into + 2] = first;
                rgba[into + 3] = source.get(at + 1);
                break;
            default:
                rgba[into] = first;
                rgba[into + 1] = first;
                rgba[into + 2] = first;
                rgba[into + 3] = first;
                break;
        }
    }
}
