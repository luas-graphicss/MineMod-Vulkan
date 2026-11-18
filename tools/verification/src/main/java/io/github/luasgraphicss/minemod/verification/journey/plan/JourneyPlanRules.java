package io.github.luasgraphicss.minemod.verification.journey.plan;

import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyPlan.Scene;
import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyStep.Kind;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public final class JourneyPlanRules {
    public static final String FILE_PACK = "file/";
    public static final String PACK_SEPARATOR = ",";

    private static final Pattern LABEL = Pattern.compile("[a-z0-9][a-z0-9-]*");
    private static final String UNLABELLED = " is not named in lowercase letters, digits and hyphens";
    private static final Pattern OPTION = Pattern.compile("[A-Za-z][A-Za-z0-9_.]*");
    private static final String OPTIONS_VERSION = "version";
    private static final Pattern POSITIVE = Pattern.compile("[1-9][0-9]{0,5}");
    private static final Set<String> HUD_STATES = Set.of("shown", "hidden");
    private static final Set<String> SCREENS = Set.of("pause", "none");

    private JourneyPlanRules() {}

    public static List<String> findViolations(JourneyPlan plan) {
        List<String> violations = new ArrayList<>();
        if (plan.timeoutMinutes() <= 0 || plan.stepTimeoutSeconds() <= 0) {
            violations.add("the timeouts of the instance and of a step are positive");
        }
        if (!LABEL.matcher(plan.world().name()).matches()) {
            violations.add("world " + plan.world().name() + UNLABELLED);
        }
        plan.packs().stream()
                .filter(pack -> !LABEL.matcher(pack).matches())
                .forEach(pack -> violations.add("pack " + pack + UNLABELLED));
        plan.options().forEach((option, value) -> {
            if (!OPTION.matcher(option).matches() || value.lines().count() > 1) {
                violations.add("option " + option + " does not fit one line of the options file");
            }
        });
        if (!plan.options().containsKey(OPTIONS_VERSION)) {
            violations.add("the options do not name the version of their file, so the game would upgrade their values");
        }
        violations.addAll(stepViolations(plan));
        violations.addAll(sceneViolations(plan));
        return violations;
    }

    private static List<String> stepViolations(JourneyPlan plan) {
        List<String> violations = new ArrayList<>();
        List<JourneyStep> steps = plan.steps();
        if (steps.isEmpty() || steps.getFirst().kind() != Kind.WAIT_FOR_TITLE) {
            violations.add("the journey starts by waiting for the title screen");
        }
        if (steps.isEmpty() || steps.getLast().kind() != Kind.QUIT) {
            violations.add("the journey ends by quitting the game");
        }
        Set<String> labels = new HashSet<>();
        for (int index = 0; index < steps.size(); index++) {
            JourneyStep step = steps.get(index);
            String subject = "step " + index + " (" + step.description() + ") ";
            violationOf(step, plan).ifPresent(violation -> violations.add(subject + violation));
            if (isLabelled(step) && !labels.add(step.kind() + " " + step.argument())) {
                violations.add(subject + "repeats a name");
            }
        }
        return violations;
    }

    private static Optional<String> violationOf(JourneyStep step, JourneyPlan plan) {
        String argument = step.argument();
        return switch (step.kind()) {
            case WAIT_FOR_TITLE, CREATE_WORLD, OPEN_WORLD, LEAVE_WORLD, RELOAD, QUIT ->
                unless(argument.isEmpty(), "takes no argument");
            case COMMAND, ATTEMPT, EXPECT_DIMENSION -> unless(!argument.isBlank(), "needs an argument");
            case WAIT, SETTLE -> unless(POSITIVE.matcher(argument).matches(), "needs a positive number of ticks");
            case CAPTURE, INSPECT -> unless(LABEL.matcher(argument).matches(), "has a name that" + UNLABELLED);
            case HUD -> unless(HUD_STATES.contains(argument), "is one of " + HUD_STATES);
            case SCREEN -> unless(SCREENS.contains(argument), "is one of " + SCREENS);
            case SELECT_PACKS -> unless(selectsListedPacks(argument, plan), "selects packs the plan does not install");
        };
    }

    private static boolean selectsListedPacks(String argument, JourneyPlan plan) {
        if (argument.isEmpty()) {
            return true;
        }
        for (String selected : argument.split(PACK_SEPARATOR, -1)) {
            if (!selected.startsWith(FILE_PACK) || !plan.packs().contains(selected.substring(FILE_PACK.length()))) {
                return false;
            }
        }
        return true;
    }

    private static List<String> sceneViolations(JourneyPlan plan) {
        List<String> violations = new ArrayList<>();
        List<String> captures = plan.argumentsOf(Kind.CAPTURE);
        Set<String> compared = new HashSet<>();
        for (Scene scene : plan.scenes()) {
            String subject = "scene " + scene.capture() + " ";
            if (!captures.contains(scene.capture())) {
                violations.add(subject + "compares a capture the journey never takes");
            }
            if (!compared.add(scene.capture())) {
                violations.add(subject + "is listed twice");
            }
            boolean hasThresholds = scene.cellSize() > 0
                    && scene.channelTolerance() >= 0
                    && scene.differingCells() >= 0
                    && scene.differingCells() <= 1
                    && scene.meanError() >= 0;
            if (!hasThresholds) {
                violations.add(subject + "has thresholds outside their range");
            }
        }
        return violations;
    }

    private static boolean isLabelled(JourneyStep step) {
        return step.kind() == Kind.CAPTURE || step.kind() == Kind.INSPECT;
    }

    private static Optional<String> unless(boolean held, String violation) {
        return held ? Optional.empty() : Optional.of(violation);
    }
}
