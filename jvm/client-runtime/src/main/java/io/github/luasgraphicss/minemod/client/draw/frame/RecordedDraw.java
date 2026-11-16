package io.github.luasgraphicss.minemod.client.draw.frame;

import io.github.luasgraphicss.minemod.codec.scene.BatchState;

final class RecordedDraw {
    private final BatchState state;
    private final int vertexOffset;
    private final int vertexBytes;
    private final BatchOptions options;

    RecordedDraw(BatchState state, int vertexOffset, int vertexBytes, BatchOptions options) {
        this.state = state;
        this.vertexOffset = vertexOffset;
        this.vertexBytes = vertexBytes;
        this.options = options;
    }

    BatchState state() {
        return state;
    }

    int vertexOffset() {
        return vertexOffset;
    }

    int vertexBytes() {
        return vertexBytes;
    }

    BatchOptions options() {
        return options;
    }
}
