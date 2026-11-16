package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.draw;

import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.luasgraphicss.minemod.codec.scene.PrimitiveMode;
import java.util.Optional;

final class PrimitiveModes {
    private PrimitiveModes() {}

    static Optional<PrimitiveMode> of(VertexFormat.Mode mode) {
        switch (mode) {
            case QUADS:
                return Optional.of(PrimitiveMode.QUADS);
            case TRIANGLES:
                return Optional.of(PrimitiveMode.TRIANGLES);
            case TRIANGLE_STRIP:
                return Optional.of(PrimitiveMode.TRIANGLE_STRIP);
            case TRIANGLE_FAN:
                return Optional.of(PrimitiveMode.TRIANGLE_FAN);
            case LINES:
                return Optional.of(PrimitiveMode.LINES);
            default:
                return Optional.empty();
        }
    }
}
