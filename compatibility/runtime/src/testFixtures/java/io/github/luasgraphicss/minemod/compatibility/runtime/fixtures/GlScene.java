package io.github.luasgraphicss.minemod.compatibility.runtime.fixtures;

import io.github.luasgraphicss.minemod.compatibility.layer.CompatibilityLayer;

public final class GlScene {
    public static final int[] MAGENTA = {255, 0, 255, 255};
    public static final int[] GREEN = {0, 255, 0, 255};
    public static final int[] BLACK = {0, 0, 0, 255};
    private static final int GL_COLOR_BUFFER_BIT = 0x4000;
    private static final int GL_RGBA = 0x1908;
    private static final int GL_UNSIGNED_BYTE = 0x1401;
    private static final float CHANNEL_MAXIMUM = 255.0F;
    private static final int CHANNELS = 4;
    private final CompatibilityLayer layer;

    public GlScene(CompatibilityLayer layer) {
        this.layer = layer;
    }

    public long call(String function, long... words) {
        return layer.call(function, words);
    }

    public void clear(int[] color) {
        call(
                "glClearColor",
                GlWords.of(color[0] / CHANNEL_MAXIMUM),
                GlWords.of(color[1] / CHANNEL_MAXIMUM),
                GlWords.of(color[2] / CHANNEL_MAXIMUM),
                GlWords.of(color[3] / CHANNEL_MAXIMUM));
        call("glClear", GL_COLOR_BUFFER_BIT);
    }

    public int[] pixelAt(int x, int y) {
        try (NativeBlock pixel = new NativeBlock(CHANNELS)) {
            call("glReadPixels", x, y, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixel.address());
            return new int[] {pixel.byteAt(0), pixel.byteAt(1), pixel.byteAt(2), pixel.byteAt(3)};
        }
    }

    public byte[] pixels(int width, int height) {
        try (NativeBlock block = new NativeBlock(width * height * CHANNELS)) {
            call("glReadPixels", 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, block.address());
            return block.bytes();
        }
    }

    public int generate(String generator) {
        try (NativeBlock name = new NativeBlock(Integer.BYTES)) {
            call(generator, 1, name.address());
            return name.intAt(0);
        }
    }

    public int integer(int parameter) {
        try (NativeBlock answer = new NativeBlock(Integer.BYTES)) {
            call("glGetIntegerv", parameter, answer.address());
            return answer.intAt(0);
        }
    }
}
