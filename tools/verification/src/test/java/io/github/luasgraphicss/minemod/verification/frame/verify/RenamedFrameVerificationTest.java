package io.github.luasgraphicss.minemod.verification.frame.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.FramePlan;
import io.github.luasgraphicss.minemod.adapters.transformers.callsite.CallSiteRedirect;
import io.github.luasgraphicss.minemod.adapters.transformers.point.MethodTarget;
import io.github.luasgraphicss.minemod.adapters.transformers.point.TransformPoint;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import io.github.luasgraphicss.minemod.verification.adapters.names.RuntimeNamespace;
import io.github.luasgraphicss.minemod.verification.adapters.plan.AdapterPlan;
import io.github.luasgraphicss.minemod.verification.frame.catalog.StandIns;
import io.github.luasgraphicss.minemod.verification.frame.fixtures.FrameFixtures;
import io.github.luasgraphicss.minemod.verification.frame.plan.FramePoints;
import io.github.luasgraphicss.minemod.verification.frame.storage.FramePlanFile;
import io.github.luasgraphicss.minemod.verification.hooks.plan.ReleasePlans;
import io.github.luasgraphicss.minemod.verification.hooks.scan.ClientArchive;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RenamedFrameVerificationTest {
    private static final String RUNTIME_CLASS = "net/minecraft/client/renderer/GlStateManager";

    @TempDir
    Path directory;

    private StandIns standIns;

    @BeforeEach
    void readTheStandInsOfTheRelease() throws IOException {
        standIns = StandIns.ofLwjglGeneration(FrameFixtures.LWJGL_GENERATION);
    }

    @Test
    void handsEveryPinnedCallToItsStandInUnderTheNamesOfTheLoader() throws IOException {
        try (ClientArchive client = FrameFixtures.client(directory.resolve("client.jar"))) {
            ReleasePlan official = pinnedOn(client);
            ReleasePlan runtime = underTheNamesOfTheLoader(official);

            assertEquals(
                    List.of(),
                    RenamedFrameVerification.findFailures(official, runtime, atLaunch(runtime), client, standIns));
        }
    }

    @Test
    void reportsAClientThatMakesAPinnedCallOnceMore() throws IOException {
        ReleasePlan official;
        try (ClientArchive pinned = FrameFixtures.client(directory.resolve("pinned.jar"))) {
            official = pinnedOn(pinned);
        }
        try (ClientArchive other = FrameFixtures.clientCalling(
                directory.resolve("other.jar"),
                Map.of(
                        "m",
                        List.of(FrameFixtures.MATRIX_MODE, FrameFixtures.LOAD_IDENTITY, FrameFixtures.LOAD_IDENTITY),
                        "n",
                        List.of(FrameFixtures.MATRIX_MODE, FrameFixtures.MATRIX_MODE)))) {
            List<String> failures =
                    RenamedFrameVerification.findFailures(official, official, atLaunch(official), other, standIns);

            assertEquals(1, failures.size(), failures.toString());
            assertTrue(failures.getFirst().contains("matched 2 times in a.n()V, expected 1"), failures.toString());
        }
    }

    @Test
    void reportsAPointTheLoaderDoesNotCarry() throws IOException {
        try (ClientArchive client = FrameFixtures.client(directory.resolve("client.jar"))) {
            ReleasePlan official = pinnedOn(client);
            ReleasePlan shorter = new ReleasePlan(
                    official.release(), official.clientSha1(), official.redirects().subList(0, 1));

            List<String> failures =
                    RenamedFrameVerification.findFailures(official, shorter, atLaunch(shorter), client, standIns);

            assertEquals(2, failures.size(), failures.toString());
            assertTrue(failures.getFirst().startsWith("the plan of the loader has no point "), failures.toString());
        }
    }

    private ReleasePlan pinnedOn(ClientArchive client) throws IOException {
        return ReleasePlans.planOf(FramePoints.pinnedOn(FrameFixtures.release(), client, standIns));
    }

    private FramePlan atLaunch(ReleasePlan runtime) throws IOException {
        String text = FramePlanFile.render(AdapterPlan.written(runtime, RuntimeNamespace.OFFICIAL), standIns);
        return FramePlan.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static ReleasePlan underTheNamesOfTheLoader(ReleasePlan official) {
        List<CallSiteRedirect> redirects = official.redirects().stream()
                .map(redirect -> {
                    MethodTarget within = redirect.point().target();
                    MethodTarget renamed = new MethodTarget(RUNTIME_CLASS, "func_" + within.name(), within.descriptor());
                    TransformPoint point = TransformPoint.hook(
                            redirect.point().id(), renamed, redirect.point().expectedMatches());
                    return new CallSiteRedirect(point, redirect.callee());
                })
                .toList();
        return new ReleasePlan(official.release(), official.clientSha1(), redirects);
    }
}
