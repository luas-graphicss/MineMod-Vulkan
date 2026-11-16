package io.github.luasgraphicss.minemod.verification.scenario.mixed;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.state.submission.FlushReason;
import io.github.luasgraphicss.minemod.verification.scenario.Scenario;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioLog;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioReport;
import io.github.luasgraphicss.minemod.verification.scenario.simulation.SimulatedLayer;
import io.github.luasgraphicss.minemod.verification.scenario.simulation.TrackedState;
import java.util.List;
import java.util.Random;

public final class MixedRenderStateScenario implements Scenario {
    private static final long BASE_SEED = 0x6D69786564L;
    private static final int GENERATED_FRAMES = 10_000;
    private static final int STEPS_PER_FRAME = 24;
    private static final long GL_TRIANGLES = 0x0004;
    private static final long GL_TEXTURE_2D = 0x0DE1;
    private static final long GL_RGBA = 0x1908;
    private static final long GL_UNSIGNED_BYTE = 0x1401;
    private static final List<String> SCRIPTED_FRAME = List.of(
            "native: terrain",
            "provider: glEnable(3042)",
            "provider: glEnable(3089)",
            "provider: glDisable(3042)",
            "provider: glBindTexture(3553, TEXTURE#1)",
            "handoff: renderer submits and signals",
            "handoff: provider waits",
            "provider: glDrawArrays(4, 0, 3)",
            "handoff: provider signals and flushes",
            "handoff: renderer waits and reacquires",
            "native: entities",
            "provider: glEnable(3042)",
            "handoff: renderer submits and signals",
            "handoff: provider waits",
            "provider: glDrawArrays(4, 0, 6)",
            "handoff: provider finishes",
            "provider: glReadPixels(0, 0, 1, 1, 6408, 5121, 0)",
            "handoff: provider signals and flushes",
            "handoff: renderer waits and reacquires",
            "native: present");

    @Override
    public String name() {
        return "mixed-render-state";
    }

    @Override
    public ScenarioReport run() {
        ScenarioLog log = new ScenarioLog();
        playScriptedFrame(log);
        playGeneratedFrames(log);
        return log.report();
    }

    private static void playScriptedFrame(ScenarioLog log) {
        SimulatedLayer layer = new SimulatedLayer(ContextProfile.CORE);
        layer.beginFrame();
        layer.nativePass("terrain");
        layer.setThroughWrapper(TrackedState.BLEND, TrackedState.ENABLED);

        layer.enterCallback();
        layer.call("glEnable", TrackedState.SCISSOR_TEST.capability());
        layer.call("glDisable", TrackedState.BLEND.capability());
        int texture = layer.generate(ObjectKind.TEXTURE, 1)[0];
        layer.call("glBindTexture", GL_TEXTURE_2D, texture);
        layer.call("glDrawArrays", GL_TRIANGLES, 0, 3);
        layer.exitCallback();

        layer.nativePass("entities");

        layer.enterCallback();
        layer.call("glDrawArrays", GL_TRIANGLES, 0, 6);
        layer.call("glReadPixels", 0, 0, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        layer.exitCallback();
        layer.present();

        log.expectEqual(SCRIPTED_FRAME, layer.timeline().all(), "order of the scripted frame");
        log.expectEqual(
                TrackedState.ENABLED,
                layer.wrapperValue(TrackedState.SCISSOR_TEST),
                "scissor state the callback left behind");
        log.expectEqual(
                TrackedState.ENABLED, layer.wrapperValue(TrackedState.BLEND), "blend state the wrapper asked for");
        log.expectEqual(2L, layer.flushes().count(FlushReason.CALLBACK_NEEDS_ATTACHMENTS), "handovers to the provider");
        log.expectEqual(1L, layer.flushes().count(FlushReason.SYNCHRONOUS_READ), "synchronous reads");
        log.expectEqual(List.of(), layer.ownershipViolations(), "attachment ownership violations");
        log.observe("scripted frame: " + layer.timeline().size() + " events in the expected order, "
                + layer.flushes().recordedCount() + " recorded flushes");
    }

    private static void playGeneratedFrames(ScenarioLog log) {
        long roundTrips = 0;
        for (int index = 0; index < GENERATED_FRAMES; index++) {
            long seed = BASE_SEED + index;
            ContextProfile profile = index % 2 == 0 ? ContextProfile.CORE : ContextProfile.COMPATIBILITY;
            SimulatedLayer layer = new SimulatedLayer(profile);
            new GeneratedFrame(layer, new Random(seed))
                    .play(STEPS_PER_FRAME)
                    .ifPresent(failure -> log.fail("seed " + seed + ": " + failure));
            roundTrips += layer.handoff().completedRoundTrips();
        }
        log.observe(GENERATED_FRAMES + " generated frames from seed 0x" + Long.toHexString(BASE_SEED) + ", "
                + (long) GENERATED_FRAMES * STEPS_PER_FRAME + " steps, " + roundTrips + " attachment round trips");
    }
}
