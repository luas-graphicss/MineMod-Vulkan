package io.github.luasgraphicss.minemod.verification.journey.run;

import io.github.luasgraphicss.minemod.verification.journey.instance.CandidateMod;
import io.github.luasgraphicss.minemod.verification.journey.instance.GameInstance;
import io.github.luasgraphicss.minemod.verification.journey.instance.InstanceDirectory;
import io.github.luasgraphicss.minemod.verification.journey.instance.InstanceExit;
import io.github.luasgraphicss.minemod.verification.journey.instance.InstanceLauncher;
import io.github.luasgraphicss.minemod.verification.journey.instance.InstanceRequest;
import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyPlan;
import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyPlanFile;
import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyPlanRules;
import io.github.luasgraphicss.minemod.verification.journey.record.JourneyRecord;
import io.github.luasgraphicss.minemod.verification.journey.record.JourneyRecordFile;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioEnvironment;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioLog;
import io.github.luasgraphicss.minemod.verification.scenario.target.GameTarget;
import io.github.luasgraphicss.minemod.verification.targets.storage.TargetManifestFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

public final class JourneyRunner {
    private static final String JOURNEYS = "tests/integration/vanilla-journey";
    private static final String PACKS = "packs";
    private static final String SCRATCH = "vanilla-journey";

    private final InstanceLauncher launcher;

    public JourneyRunner(InstanceLauncher launcher) {
        this.launcher = launcher;
    }

    public Optional<JourneyEvidence> run(
            ScenarioEnvironment environment, GameTarget target, String planName, GameInstance instance, ScenarioLog log)
            throws IOException, InterruptedException {
        Path repositoryRoot = environment.repositoryRoot();
        String subject = instance.label() + ": ";
        if (!target.isListedIn(TargetManifestFile.inRepository(repositoryRoot).read())) {
            log.fail(target.id() + " is not a target of the manifest");
            return Optional.empty();
        }
        Path driverBuild = repositoryRoot.resolve(JOURNEYS).resolve(target.id());
        if (!Files.isDirectory(driverBuild)) {
            log.fail(target.id() + " has no journey driver under " + JOURNEYS);
            return Optional.empty();
        }
        JourneyPlanFile planFile = JourneyPlanFile.inRepository(repositoryRoot, planName);
        JourneyPlan plan = planFile.read();
        List<String> planViolations = JourneyPlanRules.findViolations(plan);
        planViolations.forEach(violation -> log.fail("plan " + planName + ": " + violation));
        Optional<Path> candidateMod = instance == GameInstance.CANDIDATE
                ? CandidateMod.builtFor(repositoryRoot, target)
                : Optional.empty();
        if (instance == GameInstance.CANDIDATE && candidateMod.isEmpty()) {
            log.fail(target.loaderAdapter() + " has no mod jar; build it with \"" + CandidateMod.buildCommand(target)
                    + "\"");
        }
        if (!planViolations.isEmpty() || (instance == GameInstance.CANDIDATE && candidateMod.isEmpty())) {
            return Optional.empty();
        }

        Path directory = environment
                .cacheDirectory()
                .resolve(SCRATCH)
                .resolve(target.id())
                .resolve(planName)
                .resolve(instance.label());
        InstanceDirectory.prepare(directory, plan, repositoryRoot.resolve(JOURNEYS).resolve(PACKS));
        InstanceExit exit = launcher.launch(new InstanceRequest(
                instance,
                repositoryRoot,
                driverBuild,
                planFile.path(),
                directory,
                candidateMod,
                Duration.ofMinutes(plan.timeoutMinutes())));
        Optional<JourneyRecord> record = JourneyRecordFile.readFrom(directory);
        if (record.isEmpty()) {
            log.fail(subject + "the instance ended with exit code " + exit.exitCode()
                    + " and recorded nothing; see " + exit.log());
            return Optional.empty();
        }
        if (!record.get().plan().equals(plan.name()) || !record.get().planSha256().equals(planFile.sha256())) {
            log.fail(subject + "the record is not of plan " + planName + " as the repository has it");
            return Optional.empty();
        }
        return Optional.of(new JourneyEvidence(instance, plan, record.get(), exit, directory));
    }
}
