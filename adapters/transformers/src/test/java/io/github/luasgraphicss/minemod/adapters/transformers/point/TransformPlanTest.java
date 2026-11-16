package io.github.luasgraphicss.minemod.adapters.transformers.point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class TransformPlanTest {
    private static final MethodTarget RENDER_WORLD =
            new MethodTarget("game/render/WorldRenderer", "renderWorld", "(FJ)V");
    private static final MethodTarget UPLOAD_SECTION =
            new MethodTarget("game/render/SectionUploader", "upload", "(Lgame/render/Section;)V");

    @Test
    void keepsPointsInDeclarationOrder() {
        TransformPoint frameBegin = TransformPoint.hook("frame.begin", RENDER_WORLD, 1);
        TransformPoint frameEnd = TransformPoint.hook("frame.end", RENDER_WORLD, 3);
        TransformPoint sectionUpload = TransformPoint.replacement("terrain.section-upload", UPLOAD_SECTION);

        TransformPlan plan = new TransformPlan(Arrays.asList(frameBegin, frameEnd, sectionUpload));

        assertEquals(Arrays.asList(frameBegin, frameEnd, sectionUpload), new ArrayList<>(plan.points()));
        assertEquals(Arrays.asList(frameBegin, frameEnd), plan.pointsAt(RENDER_WORLD));
        assertEquals(sectionUpload, plan.find("terrain.section-upload").orElseThrow(AssertionError::new));
        assertFalse(plan.find("terrain.section-build").isPresent());
    }

    @Test
    void aReplacementExpectsExactlyOneBody() {
        assertEquals(1, TransformPoint.replacement("terrain.section-upload", UPLOAD_SECTION).expectedMatches());
    }

    @Test
    void rejectsAHookThatExpectsNoMatch() {
        assertThrows(IllegalArgumentException.class, () -> TransformPoint.hook("frame.begin", RENDER_WORLD, 0));
    }

    @Test
    void rejectsAnIdDeclaredTwice() {
        TransformPoint first = TransformPoint.hook("frame.begin", RENDER_WORLD, 1);
        TransformPoint second = TransformPoint.replacement("frame.begin", UPLOAD_SECTION);

        assertThrows(IllegalArgumentException.class, () -> new TransformPlan(Arrays.asList(first, second)));
    }

    @Test
    void rejectsAHookIntoAMethodThePlanReplaces() {
        TransformPoint replacement = TransformPoint.replacement("terrain.section-upload", UPLOAD_SECTION);
        TransformPoint hook = TransformPoint.hook("terrain.section-uploaded", UPLOAD_SECTION, 1);

        assertThrows(IllegalArgumentException.class, () -> new TransformPlan(Arrays.asList(replacement, hook)));
        assertThrows(IllegalArgumentException.class, () -> new TransformPlan(Arrays.asList(hook, replacement)));
    }
}
