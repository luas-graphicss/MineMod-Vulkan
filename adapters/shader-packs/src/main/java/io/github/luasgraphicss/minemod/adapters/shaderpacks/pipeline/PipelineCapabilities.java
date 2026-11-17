package io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline;

public final class PipelineCapabilities {
    private final boolean computeShaders;
    private final int colorTargets;
    private final int drawBuffers;

    public PipelineCapabilities(boolean computeShaders, int colorTargets, int drawBuffers) {
        if (colorTargets < 1 || drawBuffers < 1) {
            throw new IllegalArgumentException(
                    "a pipeline has color targets and draw buffers: " + colorTargets + ", " + drawBuffers);
        }
        this.computeShaders = computeShaders;
        this.colorTargets = colorTargets;
        this.drawBuffers = drawBuffers;
    }

    public boolean computeShaders() {
        return computeShaders;
    }

    public int colorTargets() {
        return colorTargets;
    }

    public int drawBuffers() {
        return drawBuffers;
    }
}
