package io.github.luasgraphicss.minemod.verification.differential;

import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.verification.scenario.simulation.DirectDriver;
import io.github.luasgraphicss.minemod.verification.scenario.simulation.SimulatedLayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DifferentialSuite {
    public static final String NAME = "gl-differential";
    public static final String PROVIDER = "simulated";

    private static final long BASE_SEED = 0x676C64696666L;
    private static final int SCRIPTS_PER_PROFILE = 500;
    private static final int STEPS_PER_SCRIPT = 60;

    private DifferentialSuite() {}

    public static DifferentialResult run() {
        List<String> differences = new ArrayList<>();
        for (ContextProfile profile : ContextProfile.values()) {
            for (int index = 0; index < SCRIPTS_PER_PROFILE; index++) {
                long seed = BASE_SEED + index;
                List<ScriptStep> script = ScriptGenerator.generate(seed, profile, STEPS_PER_SCRIPT);
                firstDifference(script, profile, profile)
                        .ifPresent(difference -> differences.add(profile + " seed " + seed + ": " + difference));
            }
        }
        return new DifferentialResult(SCRIPTS_PER_PROFILE * ContextProfile.values().length, differences);
    }

    static Optional<String> firstDifference(
            List<ScriptStep> script, ContextProfile layerProfile, ContextProfile referenceProfile) {
        SimulatedLayer layer = new SimulatedLayer(layerProfile);
        layer.beginFrame();
        layer.enterCallback();
        List<String> layerObservations = new ScriptPlayer(layer).play(script);
        layer.exitCallback();
        layer.present();

        DirectDriver reference = new DirectDriver(referenceProfile);
        List<String> referenceObservations = new ScriptPlayer(reference).play(script);

        Optional<String> observed = firstMismatch("observation", referenceObservations, layerObservations);
        if (observed.isPresent()) {
            return observed;
        }
        Optional<String> called = firstMismatch("provider call", reference.providerCalls(), layer.providerCalls());
        if (called.isPresent()) {
            return called;
        }
        return layer.ownershipViolations().stream().findFirst();
    }

    private static Optional<String> firstMismatch(String subject, List<String> reference, List<String> layer) {
        int shared = Math.min(reference.size(), layer.size());
        for (int index = 0; index < shared; index++) {
            if (!reference.get(index).equals(layer.get(index))) {
                return Optional.of(subject + " " + index + ": the reference has " + reference.get(index)
                        + ", the layer has " + layer.get(index));
            }
        }
        if (reference.size() != layer.size()) {
            return Optional.of(subject + " count: the reference has " + reference.size() + ", the layer has "
                    + layer.size());
        }
        return Optional.empty();
    }
}
