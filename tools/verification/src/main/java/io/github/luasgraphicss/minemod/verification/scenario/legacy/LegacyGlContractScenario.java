package io.github.luasgraphicss.minemod.verification.scenario.legacy;

import io.github.luasgraphicss.minemod.compatibility.contracts.support.OperationSupport;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallCatalog;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallDescription;
import io.github.luasgraphicss.minemod.compatibility.dispatch.table.DispatchEntry;
import io.github.luasgraphicss.minemod.compatibility.dispatch.table.DispatchTable;
import io.github.luasgraphicss.minemod.compatibility.dispatch.table.Disposition;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.verification.scenario.Scenario;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioLog;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioReport;
import io.github.luasgraphicss.minemod.verification.scenario.simulation.DirectDriver;
import io.github.luasgraphicss.minemod.verification.scenario.simulation.GlDriver;
import io.github.luasgraphicss.minemod.verification.scenario.simulation.SimulatedDispatch;
import io.github.luasgraphicss.minemod.verification.scenario.simulation.SimulatedLayer;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class LegacyGlContractScenario implements Scenario {
    private static final long GL_TRIANGLES = 0x0004;
    private static final long GL_QUADS = 0x0007;
    private static final long GL_GREATER = 0x0204;
    private static final long GL_TEXTURE_2D = 0x0DE1;
    private static final long GL_COMPILE = 0x1300;
    private static final long GL_MODELVIEW = 0x1700;
    private static final long GL_SMOOTH = 0x1D01;
    private static final long UNGENERATED_TEXTURE = 77;
    private static final List<String> LEGACY_FUNCTIONS = List.of(
            "glBegin", "glEnd", "glVertex3f", "glColor4f", "glPushMatrix", "glPopMatrix", "glTranslatef",
            "glGenLists", "glNewList", "glCallList", "glCallLists", "glAlphaFunc", "glShadeModel", "glTexEnvi",
            "glEnableClientState", "glVertexPointer");
    private static final String FIRST_HANDOVER = "handoff: renderer submits and signals";

    private final CallCatalog catalog = CallCatalog.standard();

    @Override
    public String name() {
        return "legacy-gl-contract";
    }

    @Override
    public ScenarioReport run() {
        ScenarioLog log = new ScenarioLog();
        checkDispatch(log);
        compareWithReference(log);
        checkConstructsLeftOpen(log);
        return log.report();
    }

    private void checkDispatch(ScenarioLog log) {
        DispatchTable compatibility = SimulatedDispatch.table(catalog, ContextProfile.COMPATIBILITY);
        DispatchTable core = SimulatedDispatch.table(catalog, ContextProfile.CORE);
        for (String function : LEGACY_FUNCTIONS) {
            log.expect(
                    compatibility.entry(function).disposition() != Disposition.WITHHELD,
                    function + " to resolve in a compatibility context");
            DispatchEntry inCore = core.entry(function);
            log.expect(
                    inCore.disposition() == Disposition.WITHHELD && inCore.support() == OperationSupport.OUT_OF_CONTRACT,
                    function + " to be withheld from a core context as outside the contract");
        }
        log.expectEqual(
                Disposition.PROVIDER,
                compatibility.entry("glVertex3f").disposition(),
                "per-vertex calls go straight to the provider");
        log.expectEqual(Disposition.THUNK, compatibility.entry("glBegin").disposition(), "glBegin enters the layer");
        log.expectEqual(
                OperationSupport.UNVERIFIED,
                compatibility.entry("glProgramStringARB").support(),
                "an undescribed function stays unverified");
        log.observe("compatibility context: " + dispositions(compatibility));
        log.observe("core context: " + dispositions(core));
    }

    private void compareWithReference(ScenarioLog log) {
        SimulatedLayer layer = new SimulatedLayer(ContextProfile.COMPATIBILITY);
        layer.beginFrame();
        layer.nativePass("terrain");
        layer.enterCallback();
        playLegacyCallback(layer);
        List<String> leftOpen = layer.exitCallback();
        layer.present();

        DirectDriver reference = new DirectDriver(ContextProfile.COMPATIBILITY);
        playLegacyCallback(reference);

        log.expectEqual(reference.providerCalls(), layer.providerCalls(), "provider calls of the legacy callback");
        log.expectEqual(List.of(), leftOpen, "constructs left open by a balanced callback");
        log.expectEqual(1L, layer.handoff().completedRoundTrips(), "attachment round trips of the callback");
        List<String> timeline = layer.timeline().all();
        log.expect(
                timeline.indexOf(FIRST_HANDOVER) > timeline.lastIndexOf("provider: glEndList()"),
                "no handover while the display lists were being compiled");
        log.expect(
                timeline.indexOf(FIRST_HANDOVER) < timeline.indexOf("provider: glCallList(DISPLAY_LIST#1)"),
                "the handover to come before the first list is executed");
        log.expectEqual(0, layer.takeError(), "error left by the legacy callback");
        log.observe("legacy callback: " + layer.providerCalls().size()
                + " provider calls equal to the reference, 1 attachment round trip");
    }

    private static void checkConstructsLeftOpen(ScenarioLog log) {
        SimulatedLayer layer = new SimulatedLayer(ContextProfile.COMPATIBILITY);
        layer.beginFrame();
        layer.enterCallback();
        layer.call("glPushMatrix");
        layer.call("glBegin", GL_QUADS);
        List<String> leftOpen = layer.exitCallback();
        layer.present();

        log.expectEqual(2, leftOpen.size(), "constructs reported as left open");
        log.expectEqual(2, layer.reports().reportedOperations().size(), "reports of constructs left open");
        log.expect(
                layer.providerCalls().contains("glEnd()"),
                "the open primitive to be closed before the attachments go back");
        log.expectEqual(List.of(), layer.ownershipViolations(), "attachment ownership violations");
        log.observe("a callback that left a primitive and a matrix level open was closed and reported");
    }

    private static void playLegacyCallback(GlDriver gl) {
        int first = gl.generateLists(2);
        gl.call("glNewList", first, GL_COMPILE);
        gl.call("glBegin", GL_QUADS);
        for (int vertex = 0; vertex < 4; vertex++) {
            gl.call("glVertex3f", vertex, vertex, 0);
        }
        gl.call("glEnd");
        gl.call("glEndList");
        gl.call("glNewList", first + 1, GL_COMPILE);
        gl.call("glColor4f", 1, 1, 1, 1);
        gl.call("glEndList");

        gl.call("glShadeModel", GL_SMOOTH);
        gl.call("glAlphaFunc", GL_GREATER, 0);
        gl.call("glBindTexture", GL_TEXTURE_2D, UNGENERATED_TEXTURE);
        gl.call("glMatrixMode", GL_MODELVIEW);
        gl.call("glPushMatrix");
        gl.call("glTranslatef", 1, 2, 3);
        gl.call("glCallList", first);
        gl.setListBase(first);
        gl.callLists(1, 0, 9);
        gl.call("glPopMatrix");

        gl.call("glBegin", GL_TRIANGLES);
        for (int vertex = 0; vertex < 3; vertex++) {
            gl.call("glVertex3f", vertex, 0, vertex);
        }
        gl.call("glEnd");
        gl.deleteLists(first, 2);
    }

    private String dispositions(DispatchTable table) {
        Map<Disposition, Integer> counts = new EnumMap<>(Disposition.class);
        for (CallDescription description : catalog.descriptions()) {
            counts.merge(table.entry(description.function()).disposition(), 1, Integer::sum);
        }
        return counts.getOrDefault(Disposition.PROVIDER, 0) + " functions forwarded, "
                + counts.getOrDefault(Disposition.THUNK, 0) + " through the layer, "
                + counts.getOrDefault(Disposition.WITHHELD, 0) + " withheld";
    }
}
