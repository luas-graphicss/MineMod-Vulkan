package io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.GeometryProgram;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.ProgramStage;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.support.PackSketch;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.DepthSnapshot;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.TextureFormat;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.uniforms.StandardUniforms;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class FramePlannerTest {
    private static final PipelineCapabilities WITH_COMPUTE = new PipelineCapabilities(true, 16, 8);
    private static final PipelineCapabilities WITHOUT_COMPUTE = new PipelineCapabilities(false, 8, 4);

    @Test
    void aFullscreenPassWritesTheOtherSideAndLaterPassesReadIt() {
        FramePlan plan = planOf(new PackSketch()
                .fragment("composite", "uniform sampler2D colortex0;", "/* DRAWBUFFERS:0 */")
                .fragment("composite1", "uniform sampler2D colortex0;", "/* DRAWBUFFERS:1 */")
                .fragment("final", "uniform sampler2D colortex0;", "uniform sampler2D colortex1;"));

        List<PassPlan> composites = plan.passesOf(ProgramStage.COMPOSITE);
        PassPlan last = plan.passesOf(ProgramStage.FINAL).get(0);

        assertEquals("{colortex0=colortex0.MAIN}", composites.get(0).samplers().toString());
        assertEquals("[colortex0.ALT]", composites.get(0).outputs().toString());
        assertEquals("{colortex0=colortex0.ALT}", composites.get(1).samplers().toString());
        assertEquals("[colortex1.ALT]", composites.get(1).outputs().toString());
        assertEquals("{colortex0=colortex0.ALT, colortex1=colortex1.ALT}", last.samplers().toString());
        assertTrue(last.outputs().isEmpty());
    }

    @Test
    void geometryWritesInPlaceOnTheSideEarlierPassesLeftReadable() {
        FramePlan plan = planOf(new PackSketch()
                .fragment("prepare", "/* DRAWBUFFERS:4 */")
                .fragment(
                        "gbuffers_terrain",
                        "uniform sampler2D colortex4;",
                        "uniform sampler2D gtexture;",
                        "/* DRAWBUFFERS:04 */"));

        PassPlan terrain = plan.geometryPass(GeometryProgram.TERRAIN);

        assertEquals("{colortex4=colortex4.ALT}", terrain.samplers().toString());
        assertEquals(Collections.singleton("gtexture"), terrain.gameSamplers());
        assertEquals("[colortex0.MAIN, colortex4.ALT]", terrain.outputs().toString());
        assertEquals("gbuffers_terrain", plan.geometryPass(GeometryProgram.WATER).program());
        assertEquals(PassPlan.BUILT_IN_PROGRAM, plan.geometryPass(GeometryProgram.SKYBASIC).program());
        assertEquals("[colortex0.MAIN]", plan.geometryPass(GeometryProgram.SKYBASIC).outputs().toString());
    }

    @Test
    void aFlipPropertyOverridesWhatThePassWrote() {
        FramePlan plan = planOf(new PackSketch()
                .fragment("composite", "/* DRAWBUFFERS:0 */")
                .fragment("final", "uniform sampler2D colortex0;", "uniform sampler2D colortex2;")
                .property("flip.composite.colortex0", "false")
                .property("flip.composite.colortex2", "true"));

        assertEquals(
                "{colortex0=colortex0.MAIN, colortex2=colortex2.ALT}",
                plan.passesOf(ProgramStage.FINAL).get(0).samplers().toString());
    }

    @Test
    void theBuffersAFrameLeavesFlippedAreSwappedBeforeTheNextOne() {
        FramePlan plan = planOf(new PackSketch()
                .fragment("deferred", "/* DRAWBUFFERS:03 */")
                .fragment("composite", "uniform sampler2D colortex3;", "/* DRAWBUFFERS:0 */")
                .fragment("shadow", "/* DRAWBUFFERS:0 */")
                .fragment("shadowcomp", "/* DRAWBUFFERS:1 */"));

        assertEquals("[colortex3, shadowcolor1]", plan.swappedAtFrameEnd().toString());
        assertTrue(planOf(new PackSketch().fragment("gbuffers_terrain", "/* DRAWBUFFERS:0 */"))
                .swappedAtFrameEnd()
                .isEmpty());
    }

    @Test
    void withoutAFinalProgramTheFrameShowsTheFirstColorTarget() {
        FramePlan plan = planOf(new PackSketch().fragment("composite", "/* DRAWBUFFERS:0 */"));

        PassPlan last = plan.passesOf(ProgramStage.FINAL).get(0);

        assertEquals(PassPlan.BUILT_IN_PROGRAM, last.program());
        assertEquals("{colortex0=colortex0.ALT}", last.samplers().toString());
    }

    @Test
    void snapshotsTheDepthOnlyWhenAProgramSamplesIt() {
        FramePlan sampling = planOf(new PackSketch()
                .fragment("deferred", "uniform sampler2D depthtex1;", "uniform sampler2D depthtex0;")
                .fragment("composite", "uniform sampler2D depthtex2;"));
        FramePlan plain = planOf(new PackSketch().fragment("composite", "uniform sampler2D depthtex0;"));

        assertEquals(
                EnumSet.of(DepthSnapshot.WITHOUT_HAND, DepthSnapshot.WITHOUT_TRANSLUCENTS),
                sampling.targets().depthSnapshots());
        assertTrue(plain.targets().depthSnapshots().isEmpty());
        assertFalse(plain.targets().rendersShadows());
    }

    @Test
    void rendersShadowsForAPackThatSamplesThemWithoutAShadowProgram() {
        FramePlan plan = planOf(new PackSketch()
                .fragment(
                        "composite",
                        "uniform sampler2D shadow;",
                        "uniform sampler2D watershadow;",
                        "uniform sampler2D shadowcolor0;",
                        "const int shadowMapResolution = 2048;"));

        PassPlan composite = plan.passesOf(ProgramStage.COMPOSITE).get(0);

        assertEquals(PassPlan.BUILT_IN_PROGRAM, plan.passesOf(ProgramStage.SHADOW).get(0).program());
        assertEquals(2048, plan.targets().shadowMapResolution());
        assertEquals(
                "{shadow=shadowtex1, shadowcolor0=shadowcolor0.MAIN, watershadow=shadowtex0}",
                composite.samplers().toString());
        assertEquals(Collections.singleton(0), plan.targets().shadowColorTargets());
    }

    @Test
    void theShadowProgramWritesTheShadowColorsThatShadowCompositesFlip() {
        FramePlan plan = planOf(new PackSketch()
                .fragment("shadow", "/* DRAWBUFFERS:01 */")
                .fragment("shadowcomp", "uniform sampler2D shadowcolor0;", "/* DRAWBUFFERS:0 */")
                .fragment("composite", "uniform sampler2D shadowcolor0;", "uniform sampler2D shadowtex0;"));

        assertEquals(
                "[shadowcolor0.MAIN, shadowcolor1.MAIN]",
                plan.passesOf(ProgramStage.SHADOW).get(0).outputs().toString());
        assertEquals(
                "[shadowcolor0.ALT]",
                plan.passesOf(ProgramStage.SHADOW_COMPOSITE).get(0).outputs().toString());
        assertEquals(
                "{shadowcolor0=shadowcolor0.ALT, shadowtex0=shadowtex0}",
                plan.passesOf(ProgramStage.COMPOSITE).get(0).samplers().toString());
        assertEquals(1024, plan.targets().shadowMapResolution());
    }

    @Test
    void allocatesTheTargetsThePassesTouchWithTheDeclaredFormats() {
        FramePlan plan = planOf(new PackSketch()
                .fragment("composite", "/* RENDERTARGETS: 1,9 */")
                .fragment(
                        "final",
                        "uniform sampler2D colortex9;",
                        "/* const int colortex1Format = RGBA16F; const bool colortex1Clear = false; */"));

        assertEquals(Arrays.asList(0, 1, 9), Arrays.asList(plan.targets().colorTargets().keySet().toArray()));
        assertEquals(TextureFormat.RGBA16F, plan.targets().colorTargets().get(1).format());
        assertFalse(plan.targets().colorTargets().get(1).clearedEachFrame());
        assertEquals(TextureFormat.RGBA, plan.targets().colorTargets().get(9).format());
        assertTrue(plan.targets().colorTargets().get(0).clearedEachFrame());
    }

    @Test
    void runsComputeShadersOnlyOnAProfileThatHasThem() {
        PackSketch pack = new PackSketch().computing("composite", 2);

        assertEquals(2, planOf(pack).passesOf(ProgramStage.COMPOSITE).get(0).computeShaders());
        assertEquals(
                Collections.singletonList(
                        "composite runs 2 compute shader(s), which this graphics profile does not support"),
                problemsOf(pack, WITHOUT_COMPUTE));
    }

    @Test
    void refusesUniformsThePipelineDoesNotProvideAsDeclared() {
        assertEquals(
                Arrays.asList(
                        "composite declares uniform moodOfTheAuthor, which the pipeline does not provide",
                        "composite declares uniform frameTimeCounter as vec3, the pipeline provides float"),
                problemsOf(
                        new PackSketch()
                                .fragment(
                                        "composite",
                                        "uniform float moodOfTheAuthor;",
                                        "uniform vec3 frameTimeCounter;",
                                        "uniform float viewWidth;"),
                        WITH_COMPUTE));
    }

    @Test
    void refusesSamplersNoTargetProvides() {
        assertEquals(
                Arrays.asList(
                        "gbuffers_terrain draws geometry and cannot sample colortex0, only colortex4 and above",
                        "composite samples colortex16, which neither the pipeline nor the game provides"),
                problemsOf(
                        new PackSketch()
                                .fragment("composite", "uniform sampler2D colortex16;")
                                .fragment("gbuffers_terrain", "uniform sampler2D colortex0;"),
                        WITH_COMPUTE));
    }

    @Test
    void refusesDrawBuffersBeyondTheProfile() {
        assertEquals(
                Arrays.asList(
                        "composite writes 5 draw buffers, the profile allows 4",
                        "composite1 writes draw buffer 9, the profile has 8"),
                problemsOf(
                        new PackSketch()
                                .fragment("composite", "/* RENDERTARGETS: 0,1,2,3,4 */")
                                .fragment("composite1", "/* RENDERTARGETS: 9 */"),
                        WITHOUT_COMPUTE));
    }

    @Test
    void refusesWhatTheIntegrationDoesNotRunInsteadOfSkippingIt() {
        assertEquals(
                Arrays.asList(
                        "uniform.float.fade is a custom uniform, whose expression this integration does not evaluate",
                        "gbuffers_terrain has a geometry shader, a stage this integration does not run",
                        "gbuffers_unicorn is not a program this integration runs",
                        "flip.composite.depthtex0 names depthtex0, which is not a buffer that flips",
                        "colortex0 asks for the format RGBA64F, which is not known",
                        "shadowMapResolution is 0, expected 1 to 16384"),
                problemsOf(
                        new PackSketch()
                                .withGeometryShader("gbuffers_terrain")
                                .fragment("gbuffers_unicorn", "void main() {}")
                                .fragment(
                                        "composite",
                                        "uniform sampler2D shadowtex0;",
                                        "const int colortex0Format = RGBA64F;",
                                        "const int shadowMapResolution = 0;")
                                .property("uniform.float.fade", "smooth(1, frameTime, 2, 2)")
                                .property("flip.composite.depthtex0", "true"),
                        WITH_COMPUTE));
    }

    private static FramePlan planOf(PackSketch pack) {
        PlanningOutcome outcome = new FramePlanner(StandardUniforms.bundled(), WITH_COMPUTE).plan(pack.build());
        assertEquals(Collections.emptyList(), outcome.problems());
        return outcome.plan().get();
    }

    private static List<String> problemsOf(PackSketch pack, PipelineCapabilities capabilities) {
        PlanningOutcome outcome = new FramePlanner(StandardUniforms.bundled(), capabilities).plan(pack.build());
        assertFalse(outcome.plan().isPresent());
        return outcome.problems();
    }
}
