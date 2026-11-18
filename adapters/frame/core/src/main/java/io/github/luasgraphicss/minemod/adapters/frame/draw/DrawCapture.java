package io.github.luasgraphicss.minemod.adapters.frame.draw;

import io.github.luasgraphicss.minemod.adapters.frame.arrays.Geometry;
import io.github.luasgraphicss.minemod.adapters.frame.pipeline.FixedPipeline;
import io.github.luasgraphicss.minemod.adapters.frame.pipeline.FogState;
import io.github.luasgraphicss.minemod.adapters.frame.pipeline.Matrices;
import io.github.luasgraphicss.minemod.adapters.frame.pipeline.MatrixStack;
import io.github.luasgraphicss.minemod.adapters.frame.pipeline.TextureUnits;
import io.github.luasgraphicss.minemod.codec.scene.PrimitiveMode;
import io.github.luasgraphicss.minemod.codec.scene.Shading;
import io.github.luasgraphicss.minemod.codec.scene.VertexElement;
import java.util.Optional;

public final class DrawCapture {
    private static final int COLUMN_VALUES = 4;
    private static final int TRANSLATION_COLUMN = 3;
    private static final int HORIZONTAL_ROW = 0;
    private static final int VERTICAL_ROW = 1;

    private DrawCapture() {}

    public static Optional<CapturedDraw> of(FixedPipeline pipeline, Geometry geometry, BoundTextures bound) {
        boolean drawsLines = PrimitiveModes.drawsLines(geometry.mode());
        Optional<PrimitiveMode> mode = PrimitiveModes.of(geometry.mode());
        if (!mode.isPresent() || geometry.vertexCount() == 0) {
            return Optional.empty();
        }
        CapturedDraw.Builder draw = CapturedDraw.of(drawsLines ? LineSegments.widened(geometry) : geometry, mode.get());
        float[] modelView = pipeline.matrices().modelView().current();
        if (drawsLines) {
            draw.shade(Shading.WIDE_LINES);
        } else {
            sample(pipeline.units(), draw, bound);
            lightByTheLightmap(pipeline.units(), draw, bound);
            if (lightByDirection(pipeline, draw, modelView)) {
                modelView = Matrices.identity();
            }
        }
        FogState fog = pipeline.fog();
        if (fog.isEnabled()) {
            draw.shade(Shading.FOG).fog(fog.color(), fog.start(), fog.end());
        }
        if (!geometry.has(VertexElement.COLOR)) {
            draw.color(pipeline.color());
        }
        draw.seenThrough(pipeline.matrices().projection().current(), modelView)
                .surface(pipeline.alphaTest().cutoff(), pipeline.lineWidth(), pipeline.surface().renderState());
        return Optional.of(draw.build());
    }

    private static void sample(TextureUnits units, CapturedDraw.Builder draw, BoundTextures bound) {
        boolean isSampled = units.isEnabled(TextureUnits.SAMPLED)
                && !units.generatesCoordinates(TextureUnits.SAMPLED)
                && draw.geometry().has(VertexElement.TEXTURE);
        if (!isSampled) {
            return;
        }
        draw.shade(Shading.TEXTURE).sample(bound.on(TextureUnits.SAMPLED));
        MatrixStack coordinates = units.matrix(TextureUnits.SAMPLED);
        if (!coordinates.isIdentity()) {
            float[] moved = coordinates.current();
            draw.textureRows(rowOf(moved, HORIZONTAL_ROW), rowOf(moved, VERTICAL_ROW));
        }
    }

    private static void lightByTheLightmap(TextureUnits units, CapturedDraw.Builder draw, BoundTextures bound) {
        if (!units.isEnabled(TextureUnits.LIGHTMAP)) {
            return;
        }
        if (!draw.geometry().has(VertexElement.LIGHT)) {
            draw.geometry(ConstantLight.lit(draw.geometry(), units.coordinates(TextureUnits.LIGHTMAP)));
        }
        draw.shade(Shading.LIGHTMAP).lightBy(bound.on(TextureUnits.LIGHTMAP));
    }

    private static boolean lightByDirection(FixedPipeline pipeline, CapturedDraw.Builder draw, float[] modelView) {
        if (!pipeline.lights().areShining() || !draw.geometry().has(VertexElement.NORMAL)) {
            return false;
        }
        draw.geometry(EyeSpace.moved(draw.geometry(), modelView))
                .shade(Shading.NORMAL)
                .lights(pipeline.lights().eyeDirection(0), pipeline.lights().eyeDirection(1));
        return true;
    }

    private static float[] rowOf(float[] columnMajor, int row) {
        return new float[] {
            columnMajor[row], columnMajor[COLUMN_VALUES + row], columnMajor[TRANSLATION_COLUMN * COLUMN_VALUES + row]
        };
    }
}
