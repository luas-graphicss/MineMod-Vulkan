package io.github.luasgraphicss.minemod.verification.journey.run;

import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyStep;
import io.github.luasgraphicss.minemod.verification.journey.record.JourneyRecord;
import io.github.luasgraphicss.minemod.verification.journey.record.JourneyRecord.Status;
import io.github.luasgraphicss.minemod.verification.journey.record.JourneyRecord.StepOutcome;
import java.util.ArrayList;
import java.util.List;

public final class JourneyRules {
    public static final String CANDIDATE_MOD = "minemod-vulkan";

    private JourneyRules() {}

    public static List<String> findViolations(JourneyEvidence evidence) {
        List<String> violations = new ArrayList<>();
        JourneyRecord record = evidence.record();
        if (evidence.exit().timedOut()) {
            violations.add("the instance did not end within " + evidence.plan().timeoutMinutes()
                    + " minute(s); see " + evidence.exit().log());
        } else if (evidence.exit().exitCode() != 0) {
            violations.add("the instance ended with exit code " + evidence.exit().exitCode() + "; see "
                    + evidence.exit().log());
        }
        List<JourneyStep> steps = evidence.plan().steps();
        for (int index = 0; index < steps.size(); index++) {
            JourneyStep step = steps.get(index);
            String subject = "step " + index + " (" + step.description() + ") ";
            if (index >= record.steps().size()) {
                violations.add(subject + "never ran");
                break;
            }
            StepOutcome outcome = record.steps().get(index);
            if (outcome.kind() != step.kind() || !outcome.argument().equals(step.argument())) {
                violations.add(subject + "was recorded as " + outcome.kind().label() + " " + outcome.argument());
            } else if (outcome.status() != Status.COMPLETED) {
                violations.add(subject + outcome.status().label() + ": " + outcome.detail());
            }
        }
        if (violations.isEmpty() && !record.finished()) {
            violations.add("the record stops before the journey ended");
        }
        return violations;
    }

    public static List<String> findCandidateViolations(JourneyRecord record) {
        List<String> violations = new ArrayList<>();
        if (!record.ranWith(CANDIDATE_MOD)) {
            violations.add("the instance ran without the " + CANDIDATE_MOD + " mod, with " + record.mods());
        }
        if (record.openGlProvider() != null) {
            violations.add("the instance ran with the compatibility provider " + record.openGlProvider());
        }
        return violations;
    }
}
