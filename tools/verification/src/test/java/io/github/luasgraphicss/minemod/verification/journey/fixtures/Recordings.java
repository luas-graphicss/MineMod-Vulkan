package io.github.luasgraphicss.minemod.verification.journey.fixtures;

import io.github.luasgraphicss.minemod.verification.journey.instance.GameInstance;
import io.github.luasgraphicss.minemod.verification.journey.instance.InstanceExit;
import io.github.luasgraphicss.minemod.verification.journey.instance.InstanceLauncher;
import io.github.luasgraphicss.minemod.verification.journey.instance.InstanceRequest;
import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyPlan;
import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyPlanFile;
import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyStep;
import io.github.luasgraphicss.minemod.verification.journey.record.Inspection;
import io.github.luasgraphicss.minemod.verification.journey.record.JourneyRecord;
import io.github.luasgraphicss.minemod.verification.journey.record.JourneyRecord.Status;
import io.github.luasgraphicss.minemod.verification.journey.record.JourneyRecord.StepOutcome;
import io.github.luasgraphicss.minemod.verification.journey.record.JourneyRecordFile;
import io.github.luasgraphicss.minemod.verification.json.RepositoryJson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public final class Recordings {
    public static final List<String> REFERENCE_MODS =
            List.of("fabricloader 0.20.1", "java 21", "minecraft 1.21.1", "minemod-journey 0.1.0");
    public static final List<String> CANDIDATE_MODS = List.of(
            "fabricloader 0.20.1", "java 21", "minecraft 1.21.1", "minemod-journey 0.1.0", "minemod-vulkan 0.1.0");

    private static final String LOG = "launch.log";
    private static final String PLAN_EXTENSION = ".json";

    private Recordings() {}

    @FunctionalInterface
    public interface Recorder {
        JourneyRecord record(InstanceRequest request) throws IOException;
    }

    public static InstanceLauncher endingWith(int exitCode, Recorder recorder) {
        return request -> {
            RepositoryJson.write(request.directory().resolve(JourneyRecordFile.NAME), recorder.record(request));
            return new InstanceExit(exitCode, false, request.directory().resolve(LOG));
        };
    }

    public static InstanceLauncher recording(Recorder recorder) {
        return endingWith(0, recorder);
    }

    public static JourneyRecord faithful(InstanceRequest request, BiFunction<JourneyPlan, String, Inspection> inspected)
            throws IOException {
        JourneyPlanFile planFile = planFileOf(request);
        JourneyPlan plan = planFile.read();
        List<StepOutcome> outcomes = new ArrayList<>();
        List<Inspection> inspections = new ArrayList<>();
        for (JourneyStep step : plan.steps()) {
            outcomes.add(new StepOutcome(outcomes.size(), step.kind(), step.argument(), Status.COMPLETED, ""));
            if (step.kind() == JourneyStep.Kind.INSPECT) {
                inspections.add(inspected.apply(plan, step.argument()));
            }
        }
        List<String> mods = request.instance() == GameInstance.CANDIDATE ? CANDIDATE_MODS : REFERENCE_MODS;
        return new JourneyRecord(plan.name(), planFile.sha256(), mods, null, outcomes, inspections, true);
    }

    public static JourneyRecord stoppedAt(JourneyRecord record, int step, Status status, String detail) {
        List<StepOutcome> outcomes = new ArrayList<>(record.steps().subList(0, step));
        StepOutcome stopped = record.steps().get(step);
        outcomes.add(new StepOutcome(step, stopped.kind(), stopped.argument(), status, detail));
        return new JourneyRecord(
                record.plan(),
                record.planSha256(),
                record.mods(),
                record.openGlProvider(),
                outcomes,
                record.inspections(),
                false);
    }

    public static JourneyRecord with(JourneyRecord record, List<String> mods, String openGlProvider) {
        return new JourneyRecord(
                record.plan(),
                record.planSha256(),
                mods,
                openGlProvider,
                record.steps(),
                record.inspections(),
                record.finished());
    }

    private static JourneyPlanFile planFileOf(InstanceRequest request) {
        String file = request.planFile().getFileName().toString();
        String name = file.substring(0, file.length() - PLAN_EXTENSION.length());
        return JourneyPlanFile.inRepository(request.repositoryRoot(), name);
    }
}
