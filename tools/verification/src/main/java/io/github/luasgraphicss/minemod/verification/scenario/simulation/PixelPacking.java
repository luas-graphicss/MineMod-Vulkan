package io.github.luasgraphicss.minemod.verification.scenario.simulation;

final class PixelPacking {
    private static final String PIXEL_STORE = "glPixelStorei";
    private static final long GL_PACK_ALIGNMENT = 0x0D05;
    private static final long GL_RGB = 0x1907;
    private static final long DEFAULT_ALIGNMENT = 4;
    private static final int RGB_COMPONENTS = 3;
    private static final int RGBA_COMPONENTS = 4;
    private static final int WIDTH_ARGUMENT = 2;
    private static final int HEIGHT_ARGUMENT = 3;
    private static final int FORMAT_ARGUMENT = 4;

    private long alignment = DEFAULT_ALIGNMENT;

    void follow(String function, long[] arguments) {
        if (function.equals(PIXEL_STORE) && arguments[0] == GL_PACK_ALIGNMENT) {
            alignment = arguments[1];
        }
    }

    long bytesWritten(long[] readPixelsArguments) {
        long components = readPixelsArguments[FORMAT_ARGUMENT] == GL_RGB ? RGB_COMPONENTS : RGBA_COMPONENTS;
        long rowBytes = readPixelsArguments[WIDTH_ARGUMENT] * components;
        long stride = (rowBytes + alignment - 1) / alignment * alignment;
        return stride * readPixelsArguments[HEIGHT_ARGUMENT];
    }
}
