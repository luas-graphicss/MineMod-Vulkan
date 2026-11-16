package io.github.luasgraphicss.minemod.codec.scene;

import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Set;

public final class BatchState {
    private static final int DEPTH_TEST = 1;
    private static final int DEPTH_WRITE = 1 << 1;
    private static final int DEPTH_ONLY = 1 << 2;
    private static final float[] IDENTITY = {
        1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f,
    };
    private static final float[] UNTINTED = {1.0f, 1.0f, 1.0f, 1.0f};

    private final Builder state;

    private BatchState(Builder state) {
        this.state = state;
    }

    public static Builder builder(BatchSlot slot, int view, Set<VertexElement> elements, PrimitiveMode mode) {
        if (view < 0 || view >= ViewState.MAX_VIEWS) {
            throw new IllegalArgumentException(
                    "a frame has views 0 to " + (ViewState.MAX_VIEWS - 1) + ", found " + view);
        }
        if (!elements.contains(VertexElement.POSITION)) {
            throw new IllegalArgumentException("a vertex format needs a position: " + elements);
        }
        return new Builder(slot, view, EnumSet.copyOf(elements), mode);
    }

    int vertexBytes() {
        return VertexElement.strideOf(state.elements);
    }

    void put(ByteBuffer payload) {
        payload.putInt(state.slot.code()).putInt(state.view).putInt(VertexElement.maskOf(state.elements));
        payload.putInt(state.mode.code()).putInt(state.blending.code()).putInt(state.depth);
        payload.putInt(state.depthCompare.code()).putInt(state.culling.code());
        payload.putInt(Shading.maskOf(state.shading)).putInt(0).putLong(state.texture);
        SceneFields.putFloats(payload, state.modelView);
        SceneFields.putFloats(payload, state.colorModulator);
        payload.putFloat(state.alphaCutoff).putFloat(state.lineWidth);
        payload.putFloat(state.depthBiasConstant).putFloat(state.depthBiasSlope);
    }

    public static final class Builder {
        private final BatchSlot slot;
        private final int view;
        private final Set<VertexElement> elements;
        private final PrimitiveMode mode;
        private Blending blending = Blending.DISABLED;
        private int depth;
        private DepthCompare depthCompare = DepthCompare.ALWAYS;
        private FaceCulling culling = FaceCulling.BACK_FACES;
        private Set<Shading> shading = EnumSet.noneOf(Shading.class);
        private long texture = SceneFields.NO_TEXTURE;
        private float[] modelView = IDENTITY;
        private float[] colorModulator = UNTINTED;
        private float alphaCutoff;
        private float lineWidth = 1.0f;
        private float depthBiasConstant;
        private float depthBiasSlope;

        private Builder(BatchSlot slot, int view, Set<VertexElement> elements, PrimitiveMode mode) {
            this.slot = slot;
            this.view = view;
            this.elements = elements;
            this.mode = mode;
        }

        public Builder blending(Blending mode) {
            this.blending = mode;
            return this;
        }

        public Builder depthTest(DepthCompare compare) {
            this.depth |= DEPTH_TEST;
            this.depthCompare = compare;
            return this;
        }

        public Builder depthWrite() {
            this.depth |= DEPTH_WRITE;
            return this;
        }

        public Builder depthOnly() {
            this.depth |= DEPTH_ONLY;
            return this;
        }

        public Builder culling(FaceCulling faces) {
            this.culling = faces;
            return this;
        }

        public Builder shading(Set<Shading> steps) {
            this.shading = steps.isEmpty() ? EnumSet.noneOf(Shading.class) : EnumSet.copyOf(steps);
            return this;
        }

        public Builder texture(ResourceHandle sampled) {
            this.texture = SceneFields.texture(sampled);
            return this;
        }

        public Builder modelView(float[] matrix) {
            this.modelView = SceneFields.matrix(matrix);
            return this;
        }

        public Builder colorModulator(float red, float green, float blue, float alpha) {
            this.colorModulator = new float[] {red, green, blue, alpha};
            return this;
        }

        public Builder alphaCutoff(float threshold) {
            this.alphaCutoff = threshold;
            return this;
        }

        public Builder lineWidth(float pixels) {
            this.lineWidth = pixels;
            return this;
        }

        public Builder depthBias(float constant, float slope) {
            this.depthBiasConstant = constant;
            this.depthBiasSlope = slope;
            return this;
        }

        public BatchState build() {
            return new BatchState(this);
        }
    }
}
