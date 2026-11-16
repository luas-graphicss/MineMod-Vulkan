package io.github.luasgraphicss.minemod.client.draw.frame;

import io.github.luasgraphicss.minemod.client.draw.state.RenderState;
import io.github.luasgraphicss.minemod.client.draw.vertex.GameVertexLayout;
import io.github.luasgraphicss.minemod.codec.scene.BatchState;
import io.github.luasgraphicss.minemod.codec.scene.PrimitiveMode;
import io.github.luasgraphicss.minemod.codec.scene.QuadOrder;
import io.github.luasgraphicss.minemod.codec.scene.Shading;
import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Set;

public final class GameDraw {
    private static final float[] IDENTITY = {
        1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f,
    };
    private static final float[] UNTINTED = {1.0f, 1.0f, 1.0f, 1.0f};

    private final Builder draw;

    private GameDraw(Builder draw) {
        this.draw = draw;
    }

    public static Builder builder(GameVertexLayout layout, PrimitiveMode mode, ByteBuffer vertices, int vertexCount) {
        return new Builder(layout, mode, vertices, vertexCount);
    }

    GameVertexLayout layout() {
        return draw.layout;
    }

    PrimitiveMode mode() {
        return draw.mode;
    }

    ByteBuffer vertices() {
        return draw.vertices;
    }

    int vertexCount() {
        return draw.vertexCount;
    }

    GameView view() {
        return draw.view;
    }

    RenderState state() {
        return draw.state;
    }

    Set<Shading> shading() {
        return draw.shading;
    }

    int program() {
        return draw.program;
    }

    ResourceHandle texture() {
        return draw.texture;
    }

    float[] modelView() {
        return draw.modelView;
    }

    float[] colorModulator() {
        return draw.colorModulator;
    }

    float alphaCutoff() {
        return draw.alphaCutoff;
    }

    float lineWidth() {
        return draw.lineWidth;
    }

    BatchOptions options() {
        return draw.options;
    }

    public static final class Builder {
        private final GameVertexLayout layout;
        private final PrimitiveMode mode;
        private final ByteBuffer vertices;
        private final int vertexCount;
        private GameView view;
        private RenderState state = RenderState.builder().build();
        private Set<Shading> shading = EnumSet.noneOf(Shading.class);
        private int program = BatchState.BUILT_IN_PROGRAM;
        private ResourceHandle texture;
        private float[] modelView = IDENTITY;
        private float[] colorModulator = UNTINTED;
        private float alphaCutoff;
        private float lineWidth = 1.0f;
        private BatchOptions options = BatchOptions.UNCHANGED;

        private Builder(GameVertexLayout layout, PrimitiveMode mode, ByteBuffer vertices, int vertexCount) {
            this.layout = layout;
            this.mode = mode;
            this.vertices = vertices;
            this.vertexCount = vertexCount;
        }

        public Builder view(GameView seenThrough) {
            this.view = seenThrough;
            return this;
        }

        public Builder state(RenderState captured) {
            this.state = captured;
            return this;
        }

        public Builder shading(Set<Shading> steps) {
            this.shading = steps.isEmpty() ? EnumSet.noneOf(Shading.class) : EnumSet.copyOf(steps);
            return this;
        }

        public Builder program(int staged) {
            this.program = staged;
            return this;
        }

        public Builder texture(ResourceHandle sampled) {
            this.texture = sampled;
            return this;
        }

        public Builder modelView(float[] matrix) {
            this.modelView = matrix.clone();
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

        public Builder clip(int left, int bottom, int width, int height) {
            this.options = options.clippedTo(left, bottom, width, height);
            return this;
        }

        public Builder textureTransform(float[] uRow, float[] vRow) {
            this.options = options.transformedBy(uRow, vRow);
            return this;
        }

        public Builder quadOrder(QuadOrder order) {
            this.options = options.orderedBy(order);
            return this;
        }

        public GameDraw build() {
            if (view == null) {
                throw new IllegalStateException("a draw is seen through a view");
            }
            return new GameDraw(this);
        }
    }
}
