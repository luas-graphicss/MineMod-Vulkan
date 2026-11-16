package io.github.luasgraphicss.minemod.client.draw.frame;

import io.github.luasgraphicss.minemod.codec.scene.Fog;
import io.github.luasgraphicss.minemod.codec.scene.FogShape;
import io.github.luasgraphicss.minemod.codec.scene.ViewState;
import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import java.util.Arrays;

public final class GameView {
    private static final float FAR_FOG_START = 1.0e30f;
    private static final float FAR_FOG_END = 2.0e30f;
    private static final int COLOR_VALUES = 4;
    private static final long NO_TEXTURE = 0;

    private final float[] projection;
    private final float[] fogColor;
    private final float fogStart;
    private final float fogEnd;
    private final FogShape fogShape;
    private final float[] firstLight;
    private final float[] secondLight;
    private final ResourceHandle lightmap;
    private final ResourceHandle overlay;

    private GameView(Builder builder) {
        this.projection = builder.projection;
        this.fogColor = builder.fogColor;
        this.fogStart = builder.fogStart;
        this.fogEnd = builder.fogEnd;
        this.fogShape = builder.fogShape;
        this.firstLight = builder.firstLight;
        this.secondLight = builder.secondLight;
        this.lightmap = builder.lightmap;
        this.overlay = builder.overlay;
    }

    public static Builder builder(float[] projection) {
        return new Builder(projection.clone());
    }

    ViewState toScene(int index) {
        Fog fog = new Fog(fogColor[0], fogColor[1], fogColor[2], fogColor[3], fogStart, fogEnd, fogShape);
        ViewState.Builder view = ViewState.builder(index, projection, fog);
        if (firstLight != null) {
            view.lights(firstLight, secondLight);
        }
        if (lightmap != null) {
            view.lightmap(lightmap);
        }
        if (overlay != null) {
            view.overlay(overlay);
        }
        return view.build();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof GameView)) {
            return false;
        }
        GameView view = (GameView) other;
        return Arrays.equals(projection, view.projection)
                && Arrays.equals(fogColor, view.fogColor)
                && Float.compare(fogStart, view.fogStart) == 0
                && Float.compare(fogEnd, view.fogEnd) == 0
                && fogShape == view.fogShape
                && Arrays.equals(firstLight, view.firstLight)
                && Arrays.equals(secondLight, view.secondLight)
                && bitsOf(lightmap) == bitsOf(view.lightmap)
                && bitsOf(overlay) == bitsOf(view.overlay);
    }

    @Override
    public int hashCode() {
        int hash = Arrays.hashCode(projection);
        hash = 31 * hash + Arrays.hashCode(fogColor);
        hash = 31 * hash + Float.hashCode(fogStart);
        hash = 31 * hash + Float.hashCode(fogEnd);
        hash = 31 * hash + fogShape.hashCode();
        hash = 31 * hash + Arrays.hashCode(firstLight);
        hash = 31 * hash + Arrays.hashCode(secondLight);
        hash = 31 * hash + Long.hashCode(bitsOf(lightmap));
        return 31 * hash + Long.hashCode(bitsOf(overlay));
    }

    private static long bitsOf(ResourceHandle texture) {
        return texture == null ? NO_TEXTURE : texture.bits();
    }

    public static final class Builder {
        private final float[] projection;
        private float[] fogColor = new float[COLOR_VALUES];
        private float fogStart = FAR_FOG_START;
        private float fogEnd = FAR_FOG_END;
        private FogShape fogShape = FogShape.SPHERE;
        private float[] firstLight;
        private float[] secondLight;
        private ResourceHandle lightmap;
        private ResourceHandle overlay;

        private Builder(float[] projection) {
            this.projection = projection;
        }

        public Builder fog(float[] color, float start, float end, FogShape shape) {
            if (color.length != COLOR_VALUES) {
                throw new IllegalArgumentException("a fog color has " + COLOR_VALUES + " values, found " + color.length);
            }
            boolean isMeasurable = end > start;
            this.fogColor = color.clone();
            this.fogStart = isMeasurable ? start : FAR_FOG_START;
            this.fogEnd = isMeasurable ? end : FAR_FOG_END;
            this.fogShape = shape;
            return this;
        }

        public Builder lights(float[] first, float[] second) {
            this.firstLight = first.clone();
            this.secondLight = second.clone();
            return this;
        }

        public Builder lightmap(ResourceHandle texture) {
            this.lightmap = texture;
            return this;
        }

        public Builder overlay(ResourceHandle texture) {
            this.overlay = texture;
            return this;
        }

        public GameView build() {
            return new GameView(this);
        }
    }
}
