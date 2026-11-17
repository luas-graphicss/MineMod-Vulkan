package io.github.luasgraphicss.minemod.verification.visual;

import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyPlan.Scene;
import io.github.luasgraphicss.minemod.verification.visual.CellColors.Orientation;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CaptureComparison {
    private static final double PERCENT = 100.0;

    private CaptureComparison() {}

    public static SceneVerdict compare(BufferedImage reference, BufferedImage candidate, Scene scene) {
        if (reference.getWidth() != candidate.getWidth() || reference.getHeight() != candidate.getHeight()) {
            return new SceneVerdict(0, 1, List.of("the candidate is " + sizeOf(candidate) + " and the reference is "
                    + sizeOf(reference)));
        }
        CellColors expected = CellColors.of(reference, scene.cellSize(), Orientation.UPRIGHT);
        CellColors drawn = CellColors.of(candidate, scene.cellSize(), Orientation.UPRIGHT);
        double differingCells = expected.differingFraction(drawn, scene.channelTolerance());
        double meanError = expected.meanError(drawn);
        List<String> differences = new ArrayList<>();
        if (differingCells > scene.differingCells()) {
            differences.add(String.format(
                    Locale.ROOT,
                    "%.2f%% of the %d-pixel cells differ by more than %d of 255, above the %.2f%% allowed",
                    differingCells * PERCENT,
                    scene.cellSize(),
                    scene.channelTolerance(),
                    scene.differingCells() * PERCENT));
        }
        if (meanError > scene.meanError()) {
            differences.add(String.format(
                    Locale.ROOT,
                    "the mean error is %.2f of 255, above the %.2f allowed",
                    meanError,
                    scene.meanError()));
        }
        if (!differences.isEmpty()) {
            differences.addAll(causesOf(expected, drawn, candidate, scene));
        }
        return new SceneVerdict(meanError, differingCells, differences);
    }

    private static List<String> causesOf(CellColors expected, CellColors drawn, BufferedImage candidate, Scene scene) {
        List<String> causes = new ArrayList<>();
        if (drawn.isFlat() && !expected.isFlat()) {
            causes.add("the candidate is one flat color where the reference has content");
        }
        if (matches(expected, candidate, scene, Orientation.UPSIDE_DOWN)) {
            causes.add("the candidate matches the reference upside down");
        }
        if (matches(expected, candidate, scene, Orientation.MIRRORED)) {
            causes.add("the candidate matches the reference mirrored");
        }
        return causes;
    }

    private static boolean matches(CellColors expected, BufferedImage candidate, Scene scene, Orientation orientation) {
        CellColors turned = CellColors.of(candidate, scene.cellSize(), orientation);
        return expected.differingFraction(turned, scene.channelTolerance()) <= scene.differingCells()
                && expected.meanError(turned) <= scene.meanError();
    }

    private static String sizeOf(BufferedImage image) {
        return image.getWidth() + "x" + image.getHeight();
    }
}
