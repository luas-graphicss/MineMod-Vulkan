package io.github.luasgraphicss.minemod.compatibility.runtime.fixtures;

public final class GlWords {
    private static final long LOW_WORD = 0xFFFFFFFFL;

    private GlWords() {
    }

    public static long of(float value) {
        return Float.floatToRawIntBits(value) & LOW_WORD;
    }

    public static long of(double value) {
        return Double.doubleToRawLongBits(value);
    }
}
