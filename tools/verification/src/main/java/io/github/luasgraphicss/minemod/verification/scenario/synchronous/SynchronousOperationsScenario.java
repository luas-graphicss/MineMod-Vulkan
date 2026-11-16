package io.github.luasgraphicss.minemod.verification.scenario.synchronous;

import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.state.submission.FlushReason;
import io.github.luasgraphicss.minemod.compatibility.translation.query.QueryCalls;
import io.github.luasgraphicss.minemod.verification.capability.ReportedConditions;
import io.github.luasgraphicss.minemod.verification.scenario.Scenario;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioLog;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioReport;
import io.github.luasgraphicss.minemod.verification.scenario.simulation.DirectDriver;
import io.github.luasgraphicss.minemod.verification.scenario.simulation.SimulatedLayer;
import java.util.List;

public final class SynchronousOperationsScenario implements Scenario {
    private static final int GL_INVALID_OPERATION = 0x0502;
    private static final List<String> REPORTED_ACROSS_NATIVE_WORK = List.of(
            "waiting for a bridge: transform feedback across native draws",
            "waiting for a bridge: query result across native draws",
            "outside the contract: glDrawBuffer on the front buffer");

    @Override
    public String name() {
        return "synchronous-operations";
    }

    @Override
    public ScenarioReport run() {
        ScenarioLog log = new ScenarioLog();
        compareReadbacks(log);
        compareProviderAnswers(log);
        rejectSpansAcrossNativeWork(log);
        return log.report();
    }

    private static void compareReadbacks(ScenarioLog log) {
        SimulatedLayer layer = new SimulatedLayer(ContextProfile.CORE);
        layer.beginFrame();
        layer.nativePass("terrain");
        layer.enterCallback();
        List<Long> bytes = ReadbackScript.play(layer);
        layer.exitCallback();
        layer.present();

        DirectDriver reference = new DirectDriver(ContextProfile.CORE);
        log.expectEqual(ReadbackScript.play(reference), bytes, "bytes written by each readback");
        log.expectEqual(ReadbackScript.BYTES_PER_ALIGNMENT, bytes, "row padding of each pack alignment");
        log.expectEqual(reference.providerCalls(), layer.providerCalls(), "provider calls of the readback callback");
        log.expectEqual(
                2L, layer.flushes().count(FlushReason.SYNCHRONOUS_READ), "readbacks that waited for the provider");
        log.expectEqual(List.of(), layer.ownershipViolations(), "attachment uses by the side that did not own them");
        log.observe("partial blit and 2 readbacks of the default framebuffer: " + layer.providerCalls().size()
                + " provider calls equal to the reference, " + bytes + " bytes with pack alignments 1 and 4");
    }

    private static void compareProviderAnswers(ScenarioLog log) {
        SimulatedLayer layer = new SimulatedLayer(ContextProfile.CORE);
        ProviderAnswersScript.script(layer.provider());
        layer.beginFrame();
        layer.enterCallback();
        List<Long> answers = ProviderAnswersScript.play(layer);
        layer.exitCallback();
        layer.present();

        DirectDriver reference = new DirectDriver(ContextProfile.CORE);
        ProviderAnswersScript.script(reference.provider());
        log.expectEqual(ProviderAnswersScript.play(reference), answers, "answers the mod received");
        log.expectEqual(ProviderAnswersScript.ANSWERS, answers, "answers the provider gave");
        log.expectEqual(
                reference.providerCalls(), layer.providerCalls(), "provider calls of the shader and query callback");
        log.expectEqual(
                1L, layer.flushes().count(FlushReason.SYNCHRONOUS_READ), "query reads that waited for the provider");
        log.observe("failed compile and link, a pending query and the error order: " + answers.size()
                + " answers equal to the reference");
    }

    private static void rejectSpansAcrossNativeWork(ScenarioLog log) {
        SimulatedLayer layer = new SimulatedLayer(ContextProfile.CORE);
        long result = NativeSpanFrame.play(layer);

        log.expectEqual(QueryCalls.NO_RESULT, result, "result of a query that missed native draws");
        boolean providerWasAsked =
                layer.providerCalls().stream().anyMatch(call -> call.startsWith(NativeSpanFrame.QUERY_RESULT_FUNCTION));
        log.expect(!providerWasAsked, "the provider never to be asked for a result it cannot know");
        log.expectEqual(GL_INVALID_OPERATION, layer.takeError(), "error raised for the incomplete query");
        log.expectEqual(
                REPORTED_ACROSS_NATIVE_WORK, ReportedConditions.of(layer.reports()), "operations reported to the user");
        log.observe("a query and a transform feedback across a native pass were reported, not answered");
    }
}
