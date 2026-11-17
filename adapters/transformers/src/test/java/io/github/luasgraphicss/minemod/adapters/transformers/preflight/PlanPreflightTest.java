package io.github.luasgraphicss.minemod.adapters.transformers.preflight;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.adapters.transformers.application.ForeignTransform;
import io.github.luasgraphicss.minemod.adapters.transformers.callsite.CallSiteRedirect;
import io.github.luasgraphicss.minemod.adapters.transformers.fixtures.FixtureClasses;
import io.github.luasgraphicss.minemod.adapters.transformers.fixtures.GameWindow;
import io.github.luasgraphicss.minemod.adapters.transformers.gate.TransformViolation;
import io.github.luasgraphicss.minemod.adapters.transformers.gate.TransformViolation.Kind;
import io.github.luasgraphicss.minemod.adapters.transformers.point.TransformKind;
import io.github.luasgraphicss.minemod.adapters.transformers.point.TransformPoint;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import io.github.luasgraphicss.minemod.adapters.transformers.rewrite.CallSiteRewriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlanPreflightTest {
    private static final String CLIENT_SHA1 = "0f275bc1547d01fa5f56ba34bdc87d981ee12daf";
    private static final ClassSource GAME =
            name -> name.equals(FixtureClasses.GAME_WINDOW)
                    ? Optional.of(FixtureClasses.classFileOf(GameWindow.class))
                    : Optional.empty();
    private static final ClassSource EMPTY_ARCHIVE = name -> Optional.empty();

    @Test
    void acceptsAGameWhoseCallsMatchThePlan() throws IOException {
        assertEquals(Collections.emptyList(), kindsFound(planExpecting(1, 2), GAME, noForeignTransforms()));
    }

    @Test
    void blocksAGameThatCallsTheLibraryADifferentNumberOfTimes() throws IOException {
        assertEquals(
                Collections.singletonList(Kind.UNEXPECTED_MATCH_COUNT),
                kindsFound(planExpecting(1, 3), GAME, noForeignTransforms()));
    }

    @Test
    void blocksAGameWithoutTheClassesOfThePlan() throws IOException {
        assertEquals(
                Arrays.asList(Kind.NOT_APPLIED, Kind.NOT_APPLIED),
                kindsFound(planExpecting(1, 2), EMPTY_ARCHIVE, noForeignTransforms()));
    }

    @Test
    void blocksAHookOnAMethodAnotherTransformerReplaces() throws IOException {
        List<ForeignTransform> replacedByAMod = Collections.singletonList(
                new ForeignTransform("borderless-window 1.4.0", FixtureClasses.OPEN, TransformKind.REPLACEMENT));

        assertEquals(
                Collections.singletonList(Kind.CONCURRENT_REPLACEMENT),
                kindsFound(planExpecting(1, 2), GAME, replacedByAMod));
    }

    private static ReleasePlan planExpecting(int createdWindows, int swaps) {
        return new ReleasePlan(
                "1.13",
                CLIENT_SHA1,
                Arrays.asList(
                        new CallSiteRedirect(
                                TransformPoint.hook("window-create", FixtureClasses.OPEN, createdWindows),
                                FixtureClasses.CREATE_WINDOW),
                        new CallSiteRedirect(
                                TransformPoint.hook("swap-buffers", FixtureClasses.FLIP_TWICE, swaps),
                                FixtureClasses.SWAP_BUFFERS)));
    }

    private static List<Kind> kindsFound(ReleasePlan plan, ClassSource classes, List<ForeignTransform> foreign)
            throws IOException {
        CallSiteRewriter rewriter = new CallSiteRewriter(plan.redirects(), FixtureClasses.WINDOW_HOOKS);
        List<Kind> kinds = new ArrayList<>();
        for (TransformViolation violation :
                PlanPreflight.findViolations(plan.transformPlan(), rewriter, classes, foreign)) {
            kinds.add(violation.kind());
        }
        return kinds;
    }

    private static List<ForeignTransform> noForeignTransforms() {
        return Collections.emptyList();
    }
}
