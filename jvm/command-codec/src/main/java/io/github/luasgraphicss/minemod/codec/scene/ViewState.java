package io.github.luasgraphicss.minemod.codec.scene;

import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import java.nio.ByteBuffer;

public final class ViewState {
    public static final int MAX_VIEWS = 32;

    private static final float[] LIGHT_FROM_ABOVE = {0.0f, 1.0f, 0.0f};
    private static final float[] NO_LIGHT = {0.0f, 0.0f, 0.0f};

    private final int view;
    private final float[] projection;
    private final Fog fog;
    private final float[] firstLight;
    private final float[] secondLight;
    private final long lightmap;
    private final long overlay;

    private ViewState(Builder builder) {
        this.view = builder.view;
        this.projection = builder.projection;
        this.fog = builder.fog;
        this.firstLight = builder.firstLight;
        this.secondLight = builder.secondLight;
        this.lightmap = builder.lightmap;
        this.overlay = builder.overlay;
    }

    public static Builder builder(int view, float[] projection, Fog fog) {
        if (view < 0 || view >= MAX_VIEWS) {
            throw new IllegalArgumentException("a frame has views 0 to " + (MAX_VIEWS - 1) + ", found " + view);
        }
        return new Builder(view, SceneFields.matrix(projection), fog);
    }

    void put(ByteBuffer payload) {
        payload.putInt(view).putInt(fog.shape().code());
        SceneFields.putFloats(payload, projection);
        fog.putColorAndRange(payload);
        SceneFields.putFloats(payload, firstLight);
        SceneFields.putFloats(payload, secondLight);
        payload.putLong(lightmap).putLong(overlay);
    }

    public static final class Builder {
        private final int view;
        private final float[] projection;
        private final Fog fog;
        private float[] firstLight = LIGHT_FROM_ABOVE;
        private float[] secondLight = NO_LIGHT;
        private long lightmap = SceneFields.NO_TEXTURE;
        private long overlay = SceneFields.NO_TEXTURE;

        private Builder(int view, float[] projection, Fog fog) {
            this.view = view;
            this.projection = projection;
            this.fog = fog;
        }

        public Builder lights(float[] first, float[] second) {
            this.firstLight = SceneFields.direction(first);
            this.secondLight = SceneFields.direction(second);
            return this;
        }

        public Builder lightmap(ResourceHandle texture) {
            this.lightmap = SceneFields.texture(texture);
            return this;
        }

        public Builder overlay(ResourceHandle texture) {
            this.overlay = SceneFields.texture(texture);
            return this;
        }

        public ViewState build() {
            return new ViewState(this);
        }
    }
}
