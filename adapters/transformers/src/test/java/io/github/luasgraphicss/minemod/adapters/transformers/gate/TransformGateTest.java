package io.github.luasgraphicss.minemod.adapters.transformers.gate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.adapters.transformers.application.ForeignTransform;
import io.github.luasgraphicss.minemod.adapters.transformers.application.PointApplication;
import io.github.luasgraphicss.minemod.adapters.transformers.gate.TransformViolation.Kind;
import io.github.luasgraphicss.minemod.adapters.transformers.point.MethodTarget;
import io.github.luasgraphicss.minemod.adapters.transformers.point.TransformKind;
import io.github.luasgraphicss.minemod.adapters.transformers.point.TransformPlan;
import io.github.luasgraphicss.minemod.adapters.transformers.point.TransformPoint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransformGateTest {
    private static final MethodTarget RENDER_WORLD =
            new MethodTarget("game/render/WorldRenderer", "renderWorld", "(FJ)V");
    private static final MethodTarget UPLOAD_SECTION =
            new MethodTarget("game/render/SectionUploader", "upload", "(Lgame/render/Section;)V");
    private static final TransformPlan PLAN = new TransformPlan(Arrays.asList(
            TransformPoint.hook("frame.begin", RENDER_WORLD, 2),
            TransformPoint.replacement("terrain.section-upload", UPLOAD_SECTION)));

    @Test
    void acceptsEveryPointMatchingAsDeclared() {
        assertEquals(Collections.emptyList(), violationsWhen(applied(2, 1)));
    }

    @Test
    void blocksAPointThatMatchedNothing() {
        assertEquals(
                Collections.singletonList(new TransformViolation(
                        Kind.NO_MATCH,
                        "point terrain.section-upload matched nothing in "
                                + "game/render/SectionUploader.upload(Lgame/render/Section;)V, expected 1")),
                violationsWhen(applied(2, 0)));
    }

    @Test
    void blocksAPointThatMatchedMoreTimesThanDeclared() {
        assertEquals(
                Collections.singletonList(new TransformViolation(
                        Kind.UNEXPECTED_MATCH_COUNT,
                        "point frame.begin matched 3 times in game/render/WorldRenderer.renderWorld(FJ)V"
                                + ", expected 2")),
                violationsWhen(applied(3, 1)));
    }

    @Test
    void blocksAPointThatMatchedFewerTimesThanDeclared() {
        assertEquals(Collections.singletonList(Kind.UNEXPECTED_MATCH_COUNT), kindsOf(violationsWhen(applied(1, 1))));
    }

    @Test
    void blocksADeclaredPointThatWasNeverApplied() {
        List<PointApplication> onlyTheHook = Collections.singletonList(new PointApplication("frame.begin", 2));

        assertEquals(
                Collections.singletonList(new TransformViolation(
                        Kind.NOT_APPLIED,
                        "point terrain.section-upload was never applied to "
                                + "game/render/SectionUploader.upload(Lgame/render/Section;)V")),
                violationsWhen(onlyTheHook));
    }

    @Test
    void blocksAnApplicationThePlanDoesNotDeclare() {
        List<PointApplication> applications = new ArrayList<>(applied(2, 1));
        applications.add(new PointApplication("entity.draw", 1));

        assertEquals(Collections.singletonList(Kind.UNDECLARED_POINT), kindsOf(violationsWhen(applications)));
    }

    @Test
    void blocksAPointAppliedMoreThanOnce() {
        List<PointApplication> applications = new ArrayList<>(applied(2, 1));
        applications.add(new PointApplication("terrain.section-upload", 1));

        assertEquals(Collections.singletonList(Kind.DUPLICATE_APPLICATION), kindsOf(violationsWhen(applications)));
    }

    @Test
    void blocksAReplacementOverAMethodAThirdPartyTransforms() {
        ForeignTransform thirdPartyHook =
                new ForeignTransform("mapmod.mixins.json", UPLOAD_SECTION, TransformKind.HOOK);

        assertEquals(
                Collections.singletonList(new TransformViolation(
                        Kind.REPLACEMENT_OVER_FOREIGN_TRANSFORM,
                        "point terrain.section-upload replaces "
                                + "game/render/SectionUploader.upload(Lgame/render/Section;)V"
                                + ", which mapmod.mixins.json also transforms")),
                TransformGate.findViolations(PLAN, applied(2, 1), Collections.singletonList(thirdPartyHook)));
    }

    @Test
    void blocksAThirdPartyReplacingAHookedMethod() {
        ForeignTransform thirdPartyReplacement =
                new ForeignTransform("fastrender.mixins.json", RENDER_WORLD, TransformKind.REPLACEMENT);

        assertEquals(
                Collections.singletonList(new TransformViolation(
                        Kind.CONCURRENT_REPLACEMENT,
                        "fastrender.mixins.json replaces game/render/WorldRenderer.renderWorld(FJ)V"
                                + ", which point frame.begin hooks")),
                TransformGate.findViolations(
                        PLAN, applied(2, 1), Collections.singletonList(thirdPartyReplacement)));
    }

    @Test
    void letsAThirdPartyHookShareAHookedMethod() {
        ForeignTransform thirdPartyHook = new ForeignTransform("mapmod.mixins.json", RENDER_WORLD, TransformKind.HOOK);

        assertEquals(
                Collections.emptyList(),
                TransformGate.findViolations(PLAN, applied(2, 1), Collections.singletonList(thirdPartyHook)));
    }

    private static List<PointApplication> applied(int frameBeginMatches, int sectionUploadMatches) {
        return Arrays.asList(
                new PointApplication("frame.begin", frameBeginMatches),
                new PointApplication("terrain.section-upload", sectionUploadMatches));
    }

    private static List<TransformViolation> violationsWhen(List<PointApplication> applications) {
        return TransformGate.findViolations(PLAN, applications, Collections.<ForeignTransform>emptyList());
    }

    private static List<Kind> kindsOf(List<TransformViolation> violations) {
        List<Kind> kinds = new ArrayList<>();
        for (TransformViolation violation : violations) {
            kinds.add(violation.kind());
        }
        return kinds;
    }
}
