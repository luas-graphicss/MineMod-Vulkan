package io.github.luasgraphicss.minemod.verification.scenario.vanilla;

import io.github.luasgraphicss.minemod.verification.journey.evidence.RendererReport;
import io.github.luasgraphicss.minemod.verification.journey.record.Inspection;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioLog;
import java.util.Optional;

final class NativeRendering {
    private NativeRendering() {}

    static Optional<RendererReport> expectFrames(Inspection inspection, ScenarioLog log) {
        String subject = subjectOf(inspection);
        Optional<RendererReport> report = inspection.renderer();
        if (report.isEmpty()) {
            log.fail(subject + "the native renderer reported nothing");
            return report;
        }
        log.expect(report.get().frames() > 0, subject + "frames measured by the native renderer");
        if (inspection.world().isPresent()) {
            log.expect(
                    report.get().drawsIn(RendererReport.WORLD_PASS) > 0,
                    subject + "draws in the world pass of the native renderer");
        }
        return report;
    }

    static String subjectOf(Inspection inspection) {
        return "inspection " + inspection.name() + ": ";
    }
}
