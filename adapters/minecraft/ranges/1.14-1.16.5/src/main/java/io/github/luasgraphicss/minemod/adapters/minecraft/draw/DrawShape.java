package io.github.luasgraphicss.minemod.adapters.minecraft.draw;

import io.github.luasgraphicss.minemod.client.draw.vertex.GameVertexLayout;
import io.github.luasgraphicss.minemod.codec.scene.PrimitiveMode;
import java.nio.ByteBuffer;
import java.util.Optional;

final class DrawShape {
    private final GameVertexLayout layout;
    private final PrimitiveMode mode;
    private final ByteBuffer vertices;
    private final int vertexCount;

    private DrawShape(GameVertexLayout layout, PrimitiveMode mode, ByteBuffer vertices, int vertexCount) {
        this.layout = layout;
        this.mode = mode;
        this.vertices = vertices;
        this.vertexCount = vertexCount;
    }

    static Optional<DrawShape> of(DrawCapture.Geometry geometry) {
        if (PrimitiveModes.drawsLines(geometry.mode())) {
            return LineGeometry.widened(
                            geometry.format(), geometry.mode(), geometry.vertices(), geometry.vertexCount())
                    .map(widened -> new DrawShape(
                            LineGeometry.layout(), PrimitiveMode.LINES, widened, LineGeometry.vertexCountOf(widened)));
        }
        Optional<PrimitiveMode> mode = PrimitiveModes.of(geometry.mode());
        Optional<GameVertexLayout> layout = VertexLayouts.of(geometry.format());
        if (!mode.isPresent() || !layout.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(new DrawShape(layout.get(), mode.get(), geometry.vertices(), geometry.vertexCount()));
    }

    GameVertexLayout layout() {
        return layout;
    }

    PrimitiveMode mode() {
        return mode;
    }

    ByteBuffer vertices() {
        return vertices;
    }

    int vertexCount() {
        return vertexCount;
    }
}
