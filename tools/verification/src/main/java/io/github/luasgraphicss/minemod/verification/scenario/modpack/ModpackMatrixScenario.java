package io.github.luasgraphicss.minemod.verification.scenario.modpack;

import io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline.FramePlan;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline.PlanningOutcome;
import io.github.luasgraphicss.minemod.verification.ecosystem.pack.ModRole;
import io.github.luasgraphicss.minemod.verification.ecosystem.pack.Modpack;
import io.github.luasgraphicss.minemod.verification.ecosystem.pack.Modpack.Expectation;
import io.github.luasgraphicss.minemod.verification.ecosystem.pack.Modpack.PackMod;
import io.github.luasgraphicss.minemod.verification.ecosystem.pack.ModpackFiles;
import io.github.luasgraphicss.minemod.verification.ecosystem.pack.ModpackRules;
import io.github.luasgraphicss.minemod.verification.ecosystem.session.Launch;
import io.github.luasgraphicss.minemod.verification.ecosystem.session.PackRun;
import io.github.luasgraphicss.minemod.verification.ecosystem.session.PackSession;
import io.github.luasgraphicss.minemod.verification.ecosystem.shaders.FixturePacks;
import io.github.luasgraphicss.minemod.verification.scenario.RepositoryScenario;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioEnvironment;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioLog;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioReport;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import io.github.luasgraphicss.minemod.verification.targets.storage.TargetManifestFile;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

public final class ModpackMatrixScenario implements RepositoryScenario {
    public static final String NAME = "modpack-matrix";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public ScenarioReport run(ScenarioEnvironment environment) throws IOException {
        ScenarioLog log = new ScenarioLog();
        TargetManifest targets = TargetManifestFile.inRepository(environment.repositoryRoot()).read();
        SortedMap<String, Modpack> packs = ModpackFiles.readFrom(environment.repositoryRoot());
        List<String> violations = ModpackRules.findViolations(packs, targets);
        PlanningOutcome shaderPack = new FixturePacks(environment.repositoryRoot())
                .plan(FixturePacks.CHAINED_PASSES, FixturePacks.WITHOUT_COMPUTE);
        violations.forEach(log::fail);
        shaderPack.problems().forEach(problem -> log.fail(FixturePacks.CHAINED_PASSES + ": " + problem));
        if (!log.report().passed()) {
            return log.report();
        }
        FramePlan shaderPlan = shaderPack.plan().orElseThrow();
        for (Modpack pack : packs.values()) {
            play(pack, shaderPlan, log);
        }
        Optional<Modpack> drilled = packs.values().stream()
                .filter(pack -> pack.expectation() == Expectation.RUNS && pack.roles().contains(ModRole.SHADER_LOADER))
                .findFirst();
        log.expect(drilled.isPresent(), "a pack with a shader loader to drill the recovery on");
        if (drilled.isPresent()) {
            RecoveryDrill.run(drilled.get(), shaderPlan, log);
        }
        return log.report();
    }

    private static void play(Modpack pack, FramePlan shaderPlan, ScenarioLog log) {
        Launch launch = Launch.first(FixturePacks.CHAINED_PASSES);
        PackRun composed = PackSession.run(pack, shaderPlan, launch);
        log.expectEqual(
                pack.expectation() == Expectation.REFUSED_BEFORE_WORLD,
                composed.refusedBeforeWorld(),
                pack.id() + " is refused before the world");
        composed.failures().forEach(failure -> log.fail(pack.id() + ": " + failure));
        if (!composed.refusedBeforeWorld()) {
            for (PackMod mod : pack.mods()) {
                PackRun alone = PackSession.run(pack.only(mod), shaderPlan, launch);
                alone.failures().forEach(failure -> log.fail(pack.id() + ": " + mod.id() + " alone: " + failure));
                log.expectEqual(
                        alone.journalsByMod().get(mod.id()),
                        composed.journalsByMod().get(mod.id()),
                        pack.id() + ": what " + mod.id() + " does alone and in the pack");
            }
        }
        log.observe(pack.id() + " (" + pack.loader() + " " + pack.release() + "): " + composed.summary());
    }
}
