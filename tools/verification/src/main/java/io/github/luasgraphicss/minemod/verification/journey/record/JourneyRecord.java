package io.github.luasgraphicss.minemod.verification.journey.record;

import com.fasterxml.jackson.annotation.JsonValue;
import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyStep;
import java.util.List;
import java.util.Optional;

public record JourneyRecord(
        String plan,
        String planSha256,
        List<String> mods,
        String openGlProvider,
        List<StepOutcome> steps,
        List<Inspection> inspections,
        boolean finished) {

    public JourneyRecord {
        mods = List.copyOf(mods);
        steps = List.copyOf(steps);
        inspections = List.copyOf(inspections);
    }

    public enum Status {
        COMPLETED("completed"),
        FAILED("failed"),
        TIMED_OUT("timed-out");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        @JsonValue
        public String label() {
            return label;
        }
    }

    public record StepOutcome(int index, JourneyStep.Kind kind, String argument, Status status, String detail) {}

    public Optional<Inspection> inspection(String name) {
        return inspections.stream()
                .filter(inspection -> inspection.name().equals(name))
                .findFirst();
    }

    public boolean ranWith(String mod) {
        return mods.stream().anyMatch(loaded -> loaded.startsWith(mod + " "));
    }
}
