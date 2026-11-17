package io.github.luasgraphicss.minemod.adapters.shaderpacks.execution;

import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.GeometryProgram;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.ProgramStage;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline.FramePlan;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline.PassPlan;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.ColorTargetSpec;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.DepthSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ShaderPipeline {
    private enum Phase {
        CLOSED,
        BETWEEN_FRAMES,
        OPAQUE,
        TRANSLUCENT
    }

    private final FramePlan plan;
    private final PipelineBackend backend;
    private Phase phase = Phase.CLOSED;
    private boolean handSnapshotTaken;

    public ShaderPipeline(FramePlan plan, PipelineBackend backend) {
        this.plan = Objects.requireNonNull(plan, "plan");
        this.backend = Objects.requireNonNull(backend, "backend");
    }

    public void open() {
        require(Phase.CLOSED, "open");
        backend.allocate(plan.targets());
        phase = Phase.BETWEEN_FRAMES;
    }

    public void beginFrame() {
        require(Phase.BETWEEN_FRAMES, "begin a frame");
        backend.clear(targetsClearedEachFrame());
        for (PassPlan shadows : plan.passesOf(ProgramStage.SHADOW)) {
            backend.renderShadows(shadows);
        }
        drawFullscreen(ProgramStage.SHADOW_COMPOSITE);
        drawFullscreen(ProgramStage.PREPARE);
        handSnapshotTaken = false;
        phase = Phase.OPAQUE;
    }

    public void drawGeometry(GeometryProgram geometry) {
        require(geometry.translucent() ? Phase.TRANSLUCENT : Phase.OPAQUE, "draw " + geometry);
        if (geometry == GeometryProgram.HAND) {
            snapshotWithoutHand();
        }
        backend.bindGeometry(plan.geometryPass(geometry));
    }

    public void beginTranslucents() {
        require(Phase.OPAQUE, "begin the translucents");
        snapshotWithoutHand();
        if (plan.targets().depthSnapshots().contains(DepthSnapshot.WITHOUT_TRANSLUCENTS)) {
            backend.snapshotDepth(DepthSnapshot.WITHOUT_TRANSLUCENTS);
        }
        drawFullscreen(ProgramStage.DEFERRED);
        phase = Phase.TRANSLUCENT;
    }

    public void finishFrame() {
        require(Phase.TRANSLUCENT, "finish the frame");
        drawFullscreen(ProgramStage.COMPOSITE);
        drawFullscreen(ProgramStage.FINAL);
        if (!plan.swappedAtFrameEnd().isEmpty()) {
            backend.swapSides(plan.swappedAtFrameEnd());
        }
        phase = Phase.BETWEEN_FRAMES;
    }

    public void close() {
        if (phase == Phase.CLOSED) {
            throw new IllegalStateException("the pipeline of " + plan.packName() + " is already closed");
        }
        backend.release();
        phase = Phase.CLOSED;
    }

    private void snapshotWithoutHand() {
        if (!handSnapshotTaken && plan.targets().depthSnapshots().contains(DepthSnapshot.WITHOUT_HAND)) {
            backend.snapshotDepth(DepthSnapshot.WITHOUT_HAND);
        }
        handSnapshotTaken = true;
    }

    private void drawFullscreen(ProgramStage stage) {
        for (PassPlan pass : plan.passesOf(stage)) {
            backend.drawFullscreen(pass);
        }
    }

    private List<Integer> targetsClearedEachFrame() {
        List<Integer> cleared = new ArrayList<>();
        for (Map.Entry<Integer, ColorTargetSpec> target : plan.targets().colorTargets().entrySet()) {
            if (target.getValue().clearedEachFrame()) {
                cleared.add(target.getKey());
            }
        }
        return cleared;
    }

    private void require(Phase expected, String action) {
        if (phase != expected) {
            throw new IllegalStateException("the pipeline of " + plan.packName() + " cannot " + action + " while "
                    + phase + ", expected " + expected);
        }
    }
}
