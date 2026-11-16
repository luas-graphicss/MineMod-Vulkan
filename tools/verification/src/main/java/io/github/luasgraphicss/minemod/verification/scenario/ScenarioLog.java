package io.github.luasgraphicss.minemod.verification.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ScenarioLog {
    private final List<String> observations = new ArrayList<>();
    private final List<String> failures = new ArrayList<>();

    public void observe(String observation) {
        observations.add(observation);
    }

    public void fail(String failure) {
        failures.add(failure);
    }

    public void expect(boolean held, String expectation) {
        if (!held) {
            failures.add("expected " + expectation);
        }
    }

    public void expectEqual(Object expected, Object actual, String subject) {
        if (!Objects.equals(expected, actual)) {
            failures.add(subject + ": expected " + expected + ", found " + actual);
        }
    }

    public ScenarioReport report() {
        return new ScenarioReport(observations, failures);
    }
}
