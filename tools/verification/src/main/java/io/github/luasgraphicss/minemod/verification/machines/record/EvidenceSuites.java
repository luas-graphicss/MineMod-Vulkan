package io.github.luasgraphicss.minemod.verification.machines.record;

import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.Evidence;
import io.github.luasgraphicss.minemod.verification.machines.probe.SuiteResults.SuiteOutcome;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class EvidenceSuites {
    private static final String ADAPTERS = "io.github.luasgraphicss.minemod.adapters.presentation.";
    private static final Map<Evidence, List<String>> SUITES = Map.of(
            Evidence.WINDOW_PRESENTATION,
            List.of(ADAPTERS + "glfw.NativeGlfwOnWindowTest", ADAPTERS + "display.NativeDisplayOnWindowTest"),
            Evidence.SHARED_IMAGE_INTEROP,
            List.of("io.github.luasgraphicss.minemod.compatibility.runtime.SharedSceneOnZinkTest"));

    private EvidenceSuites() {}

    static List<String> findShortfalls(Evidence evidence, Map<String, SuiteOutcome> outcomes) {
        List<String> shortfalls = new ArrayList<>();
        for (String suite : SUITES.get(evidence)) {
            String simpleName = suite.substring(suite.lastIndexOf('.') + 1);
            SuiteOutcome outcome = outcomes.get(suite);
            if (outcome == null) {
                shortfalls.add(simpleName + " has no report");
            } else if (!outcome.ranEveryTest()) {
                shortfalls.add(simpleName + " ran " + outcome.tests() + " test(s) with " + outcome.skipped()
                        + " skipped and " + (outcome.failures() + outcome.errors()) + " failed");
            }
        }
        return shortfalls;
    }
}
