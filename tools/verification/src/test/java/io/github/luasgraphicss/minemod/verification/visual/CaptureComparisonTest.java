package io.github.luasgraphicss.minemod.verification.visual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyPlan.Scene;
import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.jupiter.api.Test;

class CaptureComparisonTest {
    private static final Scene SCENE = new Scene("stage", 16, 32, 0.03, 6.0);

    @Test
    void theSameFrameMatches() {
        SceneVerdict verdict = CaptureComparison.compare(Pictures.scene(), Pictures.scene(), SCENE);

        assertTrue(verdict.matches(), verdict.differences().toString());
        assertEquals(0.0, verdict.meanError());
        assertEquals(0.0, verdict.differingCells());
    }

    @Test
    void aShadeOfDifferenceInsideTheToleranceMatches() {
        SceneVerdict verdict =
                CaptureComparison.compare(Pictures.scene(), Pictures.brightened(Pictures.scene(), 4), SCENE);

        assertTrue(verdict.matches(), verdict.differences().toString());
        assertTrue(verdict.meanError() > 0 && verdict.meanError() <= 4.0, String.valueOf(verdict.meanError()));
    }

    @Test
    void aFrameLitDifferentlyDoesNotMatch() {
        SceneVerdict verdict =
                CaptureComparison.compare(Pictures.scene(), Pictures.brightened(Pictures.scene(), 60), SCENE);

        assertEquals(2, verdict.differences().size(), verdict.differences().toString());
        assertTrue(verdict.differences().get(0).contains("of the 16-pixel cells differ by more than 32 of 255"));
        assertTrue(verdict.differences().get(1).startsWith("the mean error is "), verdict.differences().get(1));
    }

    @Test
    void missingGeometryDoesNotMatch() {
        SceneVerdict verdict =
                CaptureComparison.compare(Pictures.scene(), Pictures.withoutTheWall(Pictures.scene()), SCENE);

        assertEquals(1, verdict.differences().size(), verdict.differences().toString());
        assertTrue(verdict.differences().getFirst().startsWith("8.33% of the 16-pixel cells differ"));
    }

    @Test
    void namesAFrameDrawnUpsideDownOrMirrored() {
        BufferedImage scene = Pictures.scene();

        List<String> upsideDown =
                CaptureComparison.compare(scene, Pictures.upsideDown(scene), SCENE).differences();
        List<String> mirrored =
                CaptureComparison.compare(scene, Pictures.mirrored(scene), SCENE).differences();

        assertEquals("the candidate matches the reference upside down", upsideDown.getLast());
        assertEquals("the candidate matches the reference mirrored", mirrored.getLast());
    }

    @Test
    void namesAFrameThatIsOnlyCleared() {
        List<String> differences =
                CaptureComparison.compare(Pictures.scene(), Pictures.flat(0x000000), SCENE).differences();

        assertTrue(
                differences.contains("the candidate is one flat color where the reference has content"),
                differences.toString());
    }

    @Test
    void framesOfDifferentSizesDoNotMatch() {
        BufferedImage smaller = new BufferedImage(64, 48, BufferedImage.TYPE_INT_RGB);

        assertEquals(
                List.of("the candidate is 64x48 and the reference is 128x96"),
                CaptureComparison.compare(Pictures.scene(), smaller, SCENE).differences());
    }
}
