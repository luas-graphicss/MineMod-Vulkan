package io.github.luasgraphicss.minemod.adapters.shaderpacks.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.GeometryProgram;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline.FramePlan;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline.FramePlanner;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline.PipelineCapabilities;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.support.PackSketch;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.support.RecordingBackend;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.uniforms.StandardUniforms;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ShaderPipelineTest {
    private final RecordingBackend backend = new RecordingBackend();
    private final ShaderPipeline pipeline = new ShaderPipeline(plan(), backend);

    @Test
    void runsTheStagesOfAFrameAroundTheGeometryOfTheGame() {
        pipeline.open();
        pipeline.beginFrame();
        pipeline.drawGeometry(GeometryProgram.TERRAIN);
        pipeline.drawGeometry(GeometryProgram.HAND);
        pipeline.beginTranslucents();
        pipeline.drawGeometry(GeometryProgram.WATER);
        pipeline.finishFrame();
        pipeline.close();

        assertEquals(
                Arrays.asList(
                        "allocate [0, 1]",
                        "clear [0]",
                        "shadows built-in",
                        "fullscreen prepare",
                        "geometry gbuffers_terrain",
                        "snapshot WITHOUT_HAND",
                        "geometry built-in",
                        "snapshot WITHOUT_TRANSLUCENTS",
                        "fullscreen deferred",
                        "geometry gbuffers_terrain",
                        "fullscreen composite",
                        "fullscreen final",
                        "swap [colortex1]",
                        "release"),
                backend.journal());
    }

    @Test
    void aFrameWithoutAHandStillSnapshotsTheDepthBeforeTheTranslucents() {
        pipeline.open();
        pipeline.beginFrame();
        pipeline.beginTranslucents();
        pipeline.finishFrame();
        pipeline.beginFrame();

        assertEquals(
                Arrays.asList(
                        "allocate [0, 1]",
                        "clear [0]",
                        "shadows built-in",
                        "fullscreen prepare",
                        "snapshot WITHOUT_HAND",
                        "snapshot WITHOUT_TRANSLUCENTS",
                        "fullscreen deferred",
                        "fullscreen composite",
                        "fullscreen final",
                        "swap [colortex1]",
                        "clear [0]",
                        "shadows built-in",
                        "fullscreen prepare"),
                backend.journal());
    }

    @Test
    void refusesStagesOutOfFrameOrder() {
        assertThrows(IllegalStateException.class, pipeline::beginFrame);
        pipeline.open();
        pipeline.beginFrame();

        IllegalStateException refusal =
                assertThrows(IllegalStateException.class, () -> pipeline.drawGeometry(GeometryProgram.WATER));

        assertEquals(
                "the pipeline of sketch cannot draw WATER while OPAQUE, expected TRANSLUCENT", refusal.getMessage());
        assertThrows(IllegalStateException.class, pipeline::finishFrame);
    }

    @Test
    void aClosedPipelineReleasesOnceAndRendersNothingMore() {
        pipeline.open();
        pipeline.close();

        assertThrows(IllegalStateException.class, pipeline::close);
        assertThrows(IllegalStateException.class, pipeline::beginFrame);
        assertEquals(Arrays.asList("allocate [0, 1]", "release"), backend.journal());
    }

    private static FramePlan plan() {
        return new FramePlanner(StandardUniforms.bundled(), new PipelineCapabilities(false, 8, 8))
                .plan(new PackSketch()
                        .fragment("prepare", "/* DRAWBUFFERS:1 */", "const bool colortex1Clear = false;")
                        .fragment("gbuffers_terrain", "uniform sampler2D gtexture;")
                        .fragment(
                                "deferred",
                                "uniform sampler2D depthtex1;",
                                "uniform sampler2D depthtex2;",
                                "uniform sampler2D shadowtex0;")
                        .fragment("composite", "uniform sampler2D colortex0;")
                        .fragment("final", "uniform sampler2D colortex0;")
                        .build())
                .plan()
                .get();
    }
}
