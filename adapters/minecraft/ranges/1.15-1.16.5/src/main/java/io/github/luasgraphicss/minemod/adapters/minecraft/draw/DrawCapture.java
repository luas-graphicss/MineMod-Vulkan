package io.github.luasgraphicss.minemod.adapters.minecraft.draw;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction.FixedPipeline;
import io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction.MatrixStack;
import io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction.TextureUnits;
import io.github.luasgraphicss.minemod.adapters.minecraft.frame.GameFrame;
import io.github.luasgraphicss.minemod.client.draw.frame.GameDraw;
import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

public final class DrawCapture {
    private static final int COLUMN_VALUES = 4;
    private static final int TRANSLATION_COLUMN = 3;
    private static final int HORIZONTAL_ROW = 0;
    private static final int VERTICAL_ROW = 1;

    private DrawCapture() {}

    public static void immediate(ByteBuffer contents, int mode, VertexFormat format, int vertexCount) {
        if (vertexCount <= 0) {
            return;
        }
        ByteBuffer vertices = contents.duplicate().order(ByteOrder.nativeOrder());
        vertices.clear();
        vertices.limit(vertexCount * format.getVertexSize());
        record(
                new Geometry(format, mode, vertices, vertexCount),
                FixedPipeline.current().matrices().modelView().current());
    }

    public static void record(Geometry geometry, float[] modelView) {
        FixedPipeline pipeline = FixedPipeline.current();
        VertexFormat format = geometry.format();
        Optional<DrawShape> shape = DrawShape.of(geometry);
        boolean isSampled = !PrimitiveModes.drawsLines(geometry.mode()) && FixedShading.samples(pipeline, format);
        Optional<ResourceHandle> texture = isSampled ? SampledTexture.bound() : Optional.empty();
        if (GameFrame.leavesTheWorldOut()
                || !shape.isPresent()
                || isSampled != texture.isPresent()
                || !GameFrame.drawsIntoTheWindow()) {
            GameFrame.leaveOut();
            return;
        }
        DrawShape drawn = shape.get();
        GameDraw.Builder draw = GameDraw.builder(drawn.layout(), drawn.mode(), drawn.vertices(), drawn.vertexCount())
                .view(SceneViews.seenThrough(pipeline, format, geometry.mode(), modelView))
                .state(GameFrame.state().renderState())
                .shading(FixedShading.of(pipeline, format, geometry.mode()))
                .alphaCutoff(pipeline.alphaTest().cutoff())
                .modelView(modelView)
                .lineWidth(pipeline.lineWidth());
        if (isSampled) {
            draw.texture(texture.get());
            moveTexture(draw, pipeline.units().matrix(TextureUnits.SAMPLED));
        }
        if (!VertexLayouts.stores(format, DefaultVertexFormat.ELEMENT_COLOR)) {
            float[] color = pipeline.color();
            draw.colorModulator(color[0], color[1], color[2], color[3]);
        }
        GameFrame.state().clipInto(draw);
        GameFrame.record(draw.build());
    }

    private static void moveTexture(GameDraw.Builder draw, MatrixStack coordinates) {
        if (!coordinates.isIdentity()) {
            float[] moved = coordinates.current();
            draw.textureTransform(rowOf(moved, HORIZONTAL_ROW), rowOf(moved, VERTICAL_ROW));
        }
    }

    private static float[] rowOf(float[] columnMajor, int row) {
        return new float[] {
            columnMajor[row], columnMajor[COLUMN_VALUES + row], columnMajor[TRANSLATION_COLUMN * COLUMN_VALUES + row]
        };
    }

    public static final class Geometry {
        private final VertexFormat format;
        private final int mode;
        private final ByteBuffer vertices;
        private final int vertexCount;

        public Geometry(VertexFormat format, int mode, ByteBuffer vertices, int vertexCount) {
            this.format = format;
            this.mode = mode;
            this.vertices = vertices;
            this.vertexCount = vertexCount;
        }

        VertexFormat format() {
            return format;
        }

        int mode() {
            return mode;
        }

        ByteBuffer vertices() {
            return vertices;
        }

        int vertexCount() {
            return vertexCount;
        }
    }
}
