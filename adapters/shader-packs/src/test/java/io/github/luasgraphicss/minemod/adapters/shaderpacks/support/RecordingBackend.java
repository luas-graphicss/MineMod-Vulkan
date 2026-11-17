package io.github.luasgraphicss.minemod.adapters.shaderpacks.support;

import io.github.luasgraphicss.minemod.adapters.shaderpacks.execution.PipelineBackend;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline.PassPlan;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.DepthSnapshot;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.PackTarget;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.RenderTargets;
import java.util.ArrayList;
import java.util.List;

public final class RecordingBackend implements PipelineBackend {
    private final List<String> journal = new ArrayList<>();

    public List<String> journal() {
        return journal;
    }

    @Override
    public void allocate(RenderTargets targets) {
        journal.add("allocate " + targets.colorTargets().keySet());
    }

    @Override
    public void clear(List<Integer> colorTargets) {
        journal.add("clear " + colorTargets);
    }

    @Override
    public void renderShadows(PassPlan pass) {
        journal.add("shadows " + pass.program());
    }

    @Override
    public void snapshotDepth(DepthSnapshot snapshot) {
        journal.add("snapshot " + snapshot);
    }

    @Override
    public void bindGeometry(PassPlan pass) {
        journal.add("geometry " + pass.program());
    }

    @Override
    public void drawFullscreen(PassPlan pass) {
        journal.add("fullscreen " + pass.program());
    }

    @Override
    public void swapSides(List<PackTarget> targets) {
        journal.add("swap " + targets);
    }

    @Override
    public void release() {
        journal.add("release");
    }
}
