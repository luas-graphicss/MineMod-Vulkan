package io.github.luasgraphicss.minemod.verification.scenario.matrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import io.github.luasgraphicss.minemod.verification.adapters.names.NameTable;
import io.github.luasgraphicss.minemod.verification.adapters.names.RuntimeNamespace;
import io.github.luasgraphicss.minemod.verification.adapters.plan.RuntimePlans;
import io.github.luasgraphicss.minemod.verification.hooks.catalog.LibraryCall;
import io.github.luasgraphicss.minemod.verification.hooks.catalog.PresentationCalls;
import io.github.luasgraphicss.minemod.verification.hooks.discovery.HookDiscovery;
import io.github.luasgraphicss.minemod.verification.hooks.fixtures.FixtureClient;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks.Windowing;
import io.github.luasgraphicss.minemod.verification.hooks.scan.CallSiteScanner;
import io.github.luasgraphicss.minemod.verification.hooks.scan.ClientArchive;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RenamedClientVerificationTest {
    private static final String CLIENT_SHA1 = "0123456789abcdef0123456789abcdef01234567";
    private static final List<LibraryCall> CALLS = PresentationCalls.of(Windowing.GLFW);
    private static final NameTable LOADER_NAMES = new NameTable(
            Map.of("a", "net/minecraft/class_1041"), Map.of(NameTable.methodKey("a", "b", "()V"), "method_4493"));

    @TempDir
    Path directory;

    @Test
    void redirectsThePinnedCallsOfAClientUnderTheNamesOfItsLoader() throws IOException {
        Path client = clientCalling("client.jar", CALLS);
        ReleaseHooks hooks = hooksPinnedOn(client);
        ReleasePlan loaderPlan = RuntimePlans.intermediary(hooks, LOADER_NAMES);

        try (ClientArchive archive = ClientArchive.open(client)) {
            assertEquals(List.of(), RenamedClientVerification.findFailures(hooks, loaderPlan, archive));
        }
    }

    @Test
    void acceptsAPlanInTheNamesTheClientAlreadyHas() throws IOException {
        Path client = clientCalling("client.jar", CALLS);
        ReleaseHooks hooks = hooksPinnedOn(client);
        ReleasePlan officialPlan = RuntimePlans.written(hooks, RuntimeNamespace.OFFICIAL);

        try (ClientArchive archive = ClientArchive.open(client)) {
            assertEquals(List.of(), RenamedClientVerification.findFailures(hooks, officialPlan, archive));
        }
    }

    @Test
    void refusesAClientThatCallsTheLibraryOnceMoreThanThePlanExpects() throws IOException {
        ReleaseHooks hooks = hooksPinnedOn(clientCalling("pinned.jar", CALLS));
        ReleasePlan loaderPlan = RuntimePlans.intermediary(hooks, LOADER_NAMES);
        List<LibraryCall> twoSwaps = new java.util.ArrayList<>(CALLS);
        twoSwaps.add(CALLS.get(3));

        try (ClientArchive changed = ClientArchive.open(clientCalling("changed.jar", twoSwaps))) {
            List<String> failures = RenamedClientVerification.findFailures(hooks, loaderPlan, changed);

            assertTrue(failures.stream().anyMatch(failure -> failure.contains("swap-buffers")), failures.toString());
        }
    }

    @Test
    void refusesAClientWithoutTheHookedClass() throws IOException {
        ReleaseHooks hooks = hooksPinnedOn(clientCalling("pinned.jar", CALLS));
        ReleasePlan loaderPlan = RuntimePlans.intermediary(hooks, LOADER_NAMES);
        Path other = FixtureClient.jarOf(
                directory.resolve("other.jar"), Map.of("z", FixtureClient.classCalling("z", Map.of("y", List.of()))));

        try (ClientArchive archive = ClientArchive.open(other)) {
            assertEquals(
                    List.of("the client has no class a"),
                    RenamedClientVerification.findFailures(hooks, loaderPlan, archive).subList(0, 1));
        }
    }

    private Path clientCalling(String name, List<LibraryCall> calls) throws IOException {
        return FixtureClient.jarOf(
                directory.resolve(name), Map.of("a", FixtureClient.classCalling("a", Map.of("b", calls))));
    }

    private static ReleaseHooks hooksPinnedOn(Path client) throws IOException {
        try (ClientArchive archive = ClientArchive.open(client)) {
            return new ReleaseHooks(
                    "fixture",
                    CLIENT_SHA1,
                    Windowing.GLFW,
                    HookDiscovery.pointsOf(CALLS, new CallSiteScanner(CALLS).scan(archive), site -> null));
        }
    }
}
