package io.github.luasgraphicss.minemod.client.diagnostics;

import io.github.luasgraphicss.minemod.bridge.SceneMetrics;
import io.github.luasgraphicss.minemod.client.reload.ReloadOutcome;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DiagnosticLines {
    private static final String TITLE = "MineMod Vulkan";
    private static final double NANOSECONDS_PER_MILLISECOND = 1_000_000.0;
    private static final double BYTES_PER_MEBIBYTE = 1024.0 * 1024.0;

    private DiagnosticLines() {}

    public static List<String> describe(SceneMetrics metrics, ReloadOutcome reload) {
        List<String> lines = new ArrayList<>();
        lines.add(TITLE);
        lines.add(String.format(
                Locale.ROOT,
                "frame %.2f ms mean, %.2f ms worst over %d",
                metrics.meanFrameNanoseconds() / NANOSECONDS_PER_MILLISECOND,
                metrics.worstFrameNanoseconds() / NANOSECONDS_PER_MILLISECOND,
                metrics.framesMeasured()));
        lines.add(String.format(
                Locale.ROOT,
                "textures %.1f MiB peak, staging %.1f MiB peak",
                metrics.peakTextureBytes() / BYTES_PER_MEBIBYTE,
                metrics.peakStagingBytes() / BYTES_PER_MEBIBYTE));
        lines.add(String.format(
                Locale.ROOT,
                "%d pipelines peak, %d resources retiring, generation %d",
                metrics.peakPipelines(),
                metrics.retiredResources(),
                metrics.resourceGeneration()));
        lines.add(describe(reload));
        return lines;
    }

    private static String describe(ReloadOutcome reload) {
        if (reload.status() == ReloadOutcome.Status.REJECTED) {
            return "shaders: reload refused, previous programs kept (" + firstLine(reload.report()) + ")";
        }
        if (reload.replacedPrograms() == 0) {
            return "shaders: built-in programs";
        }
        return "shaders: " + reload.replacedPrograms() + " replaced by resource packs";
    }

    private static String firstLine(String report) {
        int newline = report.indexOf('\n');
        return newline < 0 ? report : report.substring(0, newline);
    }
}
