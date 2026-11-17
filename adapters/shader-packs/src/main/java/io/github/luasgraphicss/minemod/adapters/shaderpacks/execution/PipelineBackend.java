package io.github.luasgraphicss.minemod.adapters.shaderpacks.execution;

import io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline.PassPlan;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.DepthSnapshot;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.PackTarget;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.RenderTargets;
import java.util.List;

public interface PipelineBackend {
    void allocate(RenderTargets targets);

    void clear(List<Integer> colorTargets);

    void renderShadows(PassPlan pass);

    void snapshotDepth(DepthSnapshot snapshot);

    void bindGeometry(PassPlan pass);

    void drawFullscreen(PassPlan pass);

    void swapSides(List<PackTarget> targets);

    void release();
}
