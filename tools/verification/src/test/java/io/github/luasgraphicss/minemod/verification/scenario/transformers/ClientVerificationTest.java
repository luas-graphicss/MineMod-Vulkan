package io.github.luasgraphicss.minemod.verification.scenario.transformers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

class ClientVerificationTest {
    private static final String CLIENT_SHA1 = "0123456789abcdef0123456789abcdef01234567";
    private static final List<LibraryCall> CALLS = PresentationCalls.of(Windowing.GLFW);

    @TempDir
    Path directory;

    @Test
    void acceptsTheClientItsHooksWerePinnedOn() throws IOException {
        Path client = clientCalling("client.jar", CALLS);

        try (ClientArchive archive = ClientArchive.open(client)) {
            assertEquals(List.of(), ClientVerification.findFailures(hooksPinnedOn(client), archive));
        }
    }

    @Test
    void refusesAClientThatCallsTheLibraryOnceMore() throws IOException {
        ReleaseHooks hooks = hooksPinnedOn(clientCalling("pinned.jar", CALLS));
        List<LibraryCall> twoSwaps = new java.util.ArrayList<>(CALLS);
        twoSwaps.add(CALLS.get(3));

        try (ClientArchive archive = ClientArchive.open(clientCalling("changed.jar", twoSwaps))) {
            List<String> failures = ClientVerification.findFailures(hooks, archive);
            assertFalse(failures.isEmpty());
            assertFalse(failures.stream().noneMatch(failure -> failure.contains("swap-buffers")), failures.toString());
        }
    }

    @Test
    void refusesAClientWithoutTheHookedClass() throws IOException {
        ReleaseHooks hooks = hooksPinnedOn(clientCalling("pinned.jar", CALLS));
        Path other = FixtureClient.jarOf(
                directory.resolve("other.jar"), Map.of("z", FixtureClient.classCalling("z", Map.of("y", List.of()))));

        try (ClientArchive archive = ClientArchive.open(other)) {
            assertFalse(ClientVerification.findFailures(hooks, archive).isEmpty());
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
