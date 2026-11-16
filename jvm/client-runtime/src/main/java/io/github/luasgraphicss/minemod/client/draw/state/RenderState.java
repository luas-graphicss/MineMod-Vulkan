package io.github.luasgraphicss.minemod.client.draw.state;

import io.github.luasgraphicss.minemod.codec.scene.BatchState;
import io.github.luasgraphicss.minemod.codec.scene.Blending;
import io.github.luasgraphicss.minemod.codec.scene.DepthCompare;
import io.github.luasgraphicss.minemod.codec.scene.FaceCulling;
import java.util.Optional;

public final class RenderState {
    private final Optional<Blending> blending;
    private final boolean testsDepth;
    private final Optional<DepthCompare> depthCompare;
    private final boolean writesDepth;
    private final boolean writesColor;
    private final boolean cullsBackFaces;
    private final boolean offsetsDepth;
    private final float offsetFactor;
    private final float offsetUnits;

    private RenderState(Builder builder) {
        this.blending = builder.blending;
        this.testsDepth = builder.testsDepth;
        this.depthCompare = builder.depthCompare;
        this.writesDepth = builder.writesDepth;
        this.writesColor = builder.writesColor;
        this.cullsBackFaces = builder.cullsBackFaces;
        this.offsetsDepth = builder.offsetsDepth;
        this.offsetFactor = builder.offsetFactor;
        this.offsetUnits = builder.offsetUnits;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isExact() {
        return blending.isPresent() && (!testsDepth || depthCompare.isPresent());
    }

    public void applyTo(BatchState.Builder batch) {
        batch.blending(blending.orElse(Blending.ALPHA));
        if (testsDepth) {
            batch.depthTest(depthCompare.orElse(DepthCompare.LESS_OR_EQUAL));
        }
        if (writesDepth) {
            batch.depthWrite();
        }
        if (!writesColor) {
            batch.depthOnly();
        }
        batch.culling(cullsBackFaces ? FaceCulling.BACK_FACES : FaceCulling.DISABLED);
        if (offsetsDepth) {
            batch.depthBias(offsetUnits, offsetFactor);
        }
    }

    public static final class Builder {
        private Optional<Blending> blending = Optional.of(Blending.DISABLED);
        private boolean testsDepth;
        private Optional<DepthCompare> depthCompare = Optional.of(DepthCompare.ALWAYS);
        private boolean writesDepth;
        private boolean writesColor = true;
        private boolean cullsBackFaces;
        private boolean offsetsDepth;
        private float offsetFactor;
        private float offsetUnits;

        private Builder() {}

        public Builder blend(int sourceColor, int destinationColor) {
            this.blending = BlendTranslation.of(sourceColor, destinationColor);
            return this;
        }

        public Builder depthTest(int function) {
            this.testsDepth = true;
            this.depthCompare = DepthTranslation.of(function);
            return this;
        }

        public Builder depthWrite(boolean isWritten) {
            this.writesDepth = isWritten;
            return this;
        }

        public Builder colorWrite(boolean isWritten) {
            this.writesColor = isWritten;
            return this;
        }

        public Builder cullBackFaces(boolean isCulled) {
            this.cullsBackFaces = isCulled;
            return this;
        }

        public Builder polygonOffset(float factor, float units) {
            this.offsetsDepth = true;
            this.offsetFactor = factor;
            this.offsetUnits = units;
            return this;
        }

        public RenderState build() {
            return new RenderState(this);
        }
    }
}
