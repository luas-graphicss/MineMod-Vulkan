package io.github.luasgraphicss.minemod.adapters.frame.draw;

import io.github.luasgraphicss.minemod.adapters.frame.arrays.Geometry;
import io.github.luasgraphicss.minemod.client.draw.state.RenderState;
import io.github.luasgraphicss.minemod.codec.scene.PrimitiveMode;
import io.github.luasgraphicss.minemod.codec.scene.Shading;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public final class CapturedDraw {
    private final Builder draw;

    private CapturedDraw(Builder draw) {
        this.draw = draw;
    }

    static Builder of(Geometry geometry, PrimitiveMode mode) {
        return new Builder(geometry, mode);
    }

    public Geometry geometry() {
        return draw.geometry;
    }

    public PrimitiveMode mode() {
        return draw.mode;
    }

    public Set<Shading> shading() {
        return Collections.unmodifiableSet(draw.shading);
    }

    public int sampledTexture() {
        return draw.sampledTexture;
    }

    public int lightmapTexture() {
        return draw.lightmapTexture;
    }

    public float[] projection() {
        return draw.projection.clone();
    }

    public float[] modelView() {
        return draw.modelView.clone();
    }

    public Optional<float[]> fogColor() {
        return Optional.ofNullable(draw.fogColor).map(float[]::clone);
    }

    public float fogStart() {
        return draw.fogStart;
    }

    public float fogEnd() {
        return draw.fogEnd;
    }

    public Optional<float[][]> lights() {
        return Optional.ofNullable(draw.lights);
    }

    public Optional<float[]> color() {
        return Optional.ofNullable(draw.color).map(float[]::clone);
    }

    public Optional<float[][]> textureRows() {
        return Optional.ofNullable(draw.textureRows);
    }

    public float alphaCutoff() {
        return draw.alphaCutoff;
    }

    public float lineWidth() {
        return draw.lineWidth;
    }

    public RenderState state() {
        return draw.state;
    }

    static final class Builder {
        private final PrimitiveMode mode;
        private final Set<Shading> shading = EnumSet.noneOf(Shading.class);
        private Geometry geometry;
        private int sampledTexture;
        private int lightmapTexture;
        private float[] projection;
        private float[] modelView;
        private float[] fogColor;
        private float fogStart;
        private float fogEnd;
        private float[][] lights;
        private float[] color;
        private float[][] textureRows;
        private float alphaCutoff;
        private float lineWidth;
        private RenderState state;

        private Builder(Geometry geometry, PrimitiveMode mode) {
            this.geometry = geometry;
            this.mode = mode;
        }

        Geometry geometry() {
            return geometry;
        }

        Builder geometry(Geometry shaped) {
            this.geometry = shaped;
            return this;
        }

        Builder shade(Shading step) {
            shading.add(step);
            return this;
        }

        Builder sample(int texture) {
            this.sampledTexture = texture;
            return this;
        }

        Builder lightBy(int texture) {
            this.lightmapTexture = texture;
            return this;
        }

        Builder seenThrough(float[] projectionMatrix, float[] modelViewMatrix) {
            this.projection = projectionMatrix;
            this.modelView = modelViewMatrix;
            return this;
        }

        Builder fog(float[] fogColorValues, float start, float end) {
            this.fogColor = fogColorValues;
            this.fogStart = start;
            this.fogEnd = end;
            return this;
        }

        Builder lights(float[] first, float[] second) {
            this.lights = new float[][] {first, second};
            return this;
        }

        Builder color(float[] modulator) {
            this.color = modulator;
            return this;
        }

        Builder textureRows(float[] horizontal, float[] vertical) {
            this.textureRows = new float[][] {horizontal, vertical};
            return this;
        }

        Builder surface(float cutoff, float width, RenderState renderState) {
            this.alphaCutoff = cutoff;
            this.lineWidth = width;
            this.state = renderState;
            return this;
        }

        CapturedDraw build() {
            return new CapturedDraw(this);
        }
    }
}
