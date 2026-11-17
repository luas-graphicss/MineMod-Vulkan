package io.github.luasgraphicss.minemod.verification.ecosystem.session;

import io.github.luasgraphicss.minemod.adapters.shaderpacks.execution.ShaderPipeline;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.GeometryProgram;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline.FramePlan;
import io.github.luasgraphicss.minemod.verification.ecosystem.shaders.SymbolicBackend;

final class ShaderFrames {
    private final SymbolicBackend backend = new SymbolicBackend();
    private final ShaderPipeline pipeline;

    ShaderFrames(FramePlan plan) {
        this.pipeline = new ShaderPipeline(plan, backend);
        pipeline.open();
    }

    void beginFrame(int frame) {
        backend.startFrame(frame);
        pipeline.beginFrame();
    }

    void drawTerrain() {
        pipeline.drawGeometry(GeometryProgram.TERRAIN);
    }

    void drawTranslucents() {
        pipeline.beginTranslucents();
        pipeline.drawGeometry(GeometryProgram.WATER);
    }

    String finishFrame() {
        pipeline.finishFrame();
        return backend.presented();
    }

    boolean close() {
        pipeline.close();
        return !backend.holdsTargets();
    }
}
