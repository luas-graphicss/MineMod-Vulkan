package io.github.luasgraphicss.minemod.adapters.frame.draw;

import io.github.luasgraphicss.minemod.codec.scene.PrimitiveMode;
import java.util.Optional;

final class PrimitiveModes {
    static final int LINES = 1;
    static final int LINE_LOOP = 2;
    static final int LINE_STRIP = 3;
    private static final int TRIANGLES = 4;
    private static final int TRIANGLE_STRIP = 5;
    private static final int TRIANGLE_FAN = 6;
    private static final int QUADS = 7;
    private static final int QUAD_STRIP = 8;
    private static final int POLYGON = 9;

    private PrimitiveModes() {}

    static Optional<PrimitiveMode> of(int mode) {
        switch (mode) {
            case LINES:
            case LINE_LOOP:
            case LINE_STRIP:
                return Optional.of(PrimitiveMode.LINES);
            case TRIANGLES:
                return Optional.of(PrimitiveMode.TRIANGLES);
            case TRIANGLE_STRIP:
            case QUAD_STRIP:
                return Optional.of(PrimitiveMode.TRIANGLE_STRIP);
            case TRIANGLE_FAN:
            case POLYGON:
                return Optional.of(PrimitiveMode.TRIANGLE_FAN);
            case QUADS:
                return Optional.of(PrimitiveMode.QUADS);
            default:
                return Optional.empty();
        }
    }

    static boolean drawsLines(int mode) {
        return mode == LINES || mode == LINE_LOOP || mode == LINE_STRIP;
    }
}
