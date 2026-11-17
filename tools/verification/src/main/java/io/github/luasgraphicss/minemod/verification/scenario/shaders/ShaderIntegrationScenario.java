package io.github.luasgraphicss.minemod.verification.scenario.shaders;

import io.github.luasgraphicss.minemod.adapters.shaderpacks.execution.ShaderPipeline;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.GeometryProgram;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.ProgramStage;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline.FramePlan;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline.PlanningOutcome;
import io.github.luasgraphicss.minemod.verification.ecosystem.shaders.FixturePacks;
import io.github.luasgraphicss.minemod.verification.ecosystem.shaders.SymbolicBackend;
import io.github.luasgraphicss.minemod.verification.scenario.RepositoryScenario;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioEnvironment;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioLog;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioReport;
import java.io.IOException;
import java.util.List;

public final class ShaderIntegrationScenario implements RepositoryScenario {
    public static final String NAME = "shader-integration";

    private static final String HISTORY_BUFFER = "history-buffer";
    private static final String COMPUTE_BLOOM = "compute-bloom";
    private static final String CUSTOM_UNIFORM = "custom-uniform";
    private static final int FRAMES = 3;
    private static final String CHAINED_FRAME = "final(composite1(composite(translucents@f%1$d over "
            + "deferred(opaque@f%1$d, depth(opaque)@f%1$d, shadows@f%1$d), translucents@f%1$d over opaque@f%1$d), "
            + "depth(opaque+translucents)@f%1$d))";
    private static final String HISTORY_OF_THREE_FRAMES = "final(composite(translucents@f3 over opaque@f3, "
            + "composite(translucents@f2 over opaque@f2, composite(translucents@f1 over opaque@f1, empty))))";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public ScenarioReport run(ScenarioEnvironment environment) throws IOException {
        ScenarioLog log = new ScenarioLog();
        FixturePacks packs = new FixturePacks(environment.repositoryRoot());
        chainPasses(packs, log);
        keepHistory(packs, log);
        gateCompute(packs, log);
        refuseWhatCannotBeHonored(packs, log);
        return log.report();
    }

    private static void chainPasses(FixturePacks packs, ScenarioLog log) throws IOException {
        PlanningOutcome outcome = packs.plan(FixturePacks.CHAINED_PASSES, FixturePacks.WITHOUT_COMPUTE);
        if (outcome.plan().isEmpty()) {
            outcome.problems().forEach(problem -> log.fail(FixturePacks.CHAINED_PASSES + ": " + problem));
            return;
        }
        FramePlan plan = outcome.plan().get();
        SymbolicBackend backend = new SymbolicBackend();
        ShaderPipeline pipeline = new ShaderPipeline(plan, backend);
        pipeline.open();
        for (int frame = 1; frame <= FRAMES; frame++) {
            log.expectEqual(
                    CHAINED_FRAME.formatted(frame), present(pipeline, backend, frame), "chained passes, frame " + frame);
        }
        pipeline.close();
        log.expect(!backend.holdsTargets(), "a closed pipeline to hold no target");
        pipeline.open();
        log.expectEqual(CHAINED_FRAME.formatted(1), present(pipeline, backend, 1), "chained passes after a reload");
        pipeline.close();
        log.expectEqual(List.of(2, 2), List.of(backend.allocations(), backend.releases()), "allocations and releases");
        log.expect(plan.targets().rendersShadows(), "the pack to render its shadow map");
        log.observe(FixturePacks.CHAINED_PASSES + ": shadows, deferred lighting over the depth without translucents and "
                + plan.passesOf(ProgramStage.COMPOSITE).size() + " composites reach the frame as "
                + CHAINED_FRAME.formatted(FRAMES));
    }

    private static void keepHistory(FixturePacks packs, ScenarioLog log) throws IOException {
        PlanningOutcome outcome = packs.plan(HISTORY_BUFFER, FixturePacks.WITHOUT_COMPUTE);
        if (outcome.plan().isEmpty()) {
            outcome.problems().forEach(problem -> log.fail(HISTORY_BUFFER + ": " + problem));
            return;
        }
        SymbolicBackend backend = new SymbolicBackend();
        ShaderPipeline pipeline = new ShaderPipeline(outcome.plan().get(), backend);
        pipeline.open();
        String presented = "";
        for (int frame = 1; frame <= FRAMES; frame++) {
            presented = present(pipeline, backend, frame);
        }
        pipeline.close();
        log.expectEqual(HISTORY_OF_THREE_FRAMES, presented, "history buffer after " + FRAMES + " frames");
        log.observe(HISTORY_BUFFER + ": a buffer that is not cleared carries each frame into the next: " + presented);
    }

    private static void gateCompute(FixturePacks packs, ScenarioLog log) throws IOException {
        PlanningOutcome without = packs.plan(COMPUTE_BLOOM, FixturePacks.WITHOUT_COMPUTE);
        PlanningOutcome with = packs.plan(COMPUTE_BLOOM, FixturePacks.WITH_COMPUTE);
        log.expectEqual(
                List.of("composite runs 2 compute shader(s), which this graphics profile does not support"),
                without.problems(),
                "compute pack on a profile without compute");
        log.expect(
                with.plan().isPresent()
                        && with.plan().get().passesOf(ProgramStage.COMPOSITE).get(0).computeShaders() == 2,
                "the compute pack to plan its 2 compute shaders on a profile with compute");
        log.observe(COMPUTE_BLOOM + ": refused without compute, planned with it");
    }

    private static void refuseWhatCannotBeHonored(FixturePacks packs, ScenarioLog log) throws IOException {
        PlanningOutcome outcome = packs.plan(CUSTOM_UNIFORM, FixturePacks.WITH_COMPUTE);
        log.expectEqual(
                List.of(
                        "uniform.float.dayFade is a custom uniform, whose expression this integration does not evaluate",
                        "composite declares uniform dayFade, which the pipeline does not provide"),
                outcome.problems(),
                "pack with a custom uniform");
        log.observe(CUSTOM_UNIFORM + ": refused with " + outcome.problems().size() + " diagnosed problem(s)");
    }

    private static String present(ShaderPipeline pipeline, SymbolicBackend backend, int frame) {
        backend.startFrame(frame);
        pipeline.beginFrame();
        pipeline.drawGeometry(GeometryProgram.TERRAIN);
        pipeline.beginTranslucents();
        pipeline.drawGeometry(GeometryProgram.WATER);
        pipeline.finishFrame();
        return backend.presented();
    }
}
