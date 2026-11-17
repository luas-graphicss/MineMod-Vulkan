package io.github.luasgraphicss.minemod.verification.machines.record;

import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.Evidence;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.HostSystem;
import io.github.luasgraphicss.minemod.verification.machines.probe.HarnessReport;
import io.github.luasgraphicss.minemod.verification.machines.probe.SuiteResults.SuiteOutcome;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MachineRecorder {
    public record Recording(ReferenceMachine machine, List<String> unproven) {}

    private MachineRecorder() {}

    public static Recording record(
            String id, HostSystem system, HarnessReport harness, Map<String, SuiteOutcome> suites) {
        List<Evidence> proven = new ArrayList<>();
        List<String> unproven = new ArrayList<>();
        for (Evidence evidence : Evidence.values()) {
            List<String> shortfalls = evidence == Evidence.NATIVE_PRESENTATION
                    ? shortfallsOf(harness)
                    : EvidenceSuites.findShortfalls(evidence, suites);
            if (shortfalls.isEmpty()) {
                proven.add(evidence);
            } else {
                unproven.add(evidence.label() + ": " + String.join("; ", shortfalls));
            }
        }
        ReferenceMachine machine = new ReferenceMachine(id, system, harness.device(), List.copyOf(proven));
        return new Recording(machine, List.copyOf(unproven));
    }

    private static List<String> shortfallsOf(HarnessReport harness) {
        if (harness.presentedWithoutValidationMessages()) {
            return List.of();
        }
        String validation = harness.validationMessages().isPresent()
                ? harness.validationMessages().getAsInt() + " validation message(s)"
                : "validation not requested";
        return List.of("the harness presented " + harness.presentedFrames() + " frame(s) with " + validation);
    }
}
