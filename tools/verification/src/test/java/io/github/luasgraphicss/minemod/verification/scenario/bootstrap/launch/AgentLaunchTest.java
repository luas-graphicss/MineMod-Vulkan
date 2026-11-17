package io.github.luasgraphicss.minemod.verification.scenario.bootstrap.launch;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.verification.hooks.catalog.PresentationCalls;
import io.github.luasgraphicss.minemod.verification.hooks.fixtures.FixtureClient;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks.Windowing;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AgentLaunchTest {
    @TempDir
    Path directory;

    @Test
    void recordsTheClassesAChildJvmDefinesWithoutRunningThem() throws IOException, InterruptedException {
        byte[] window = FixtureClient.classCalling("a", Map.of("b", PresentationCalls.of(Windowing.GLFW)));
        Path client = FixtureClient.jarOf(directory.resolve("client.jar"), Map.of("a", window));

        AgentLaunch.Outcome outcome = AgentLaunch.run(List.of(), List.of(client), Set.of("a"), directory.resolve("run"));

        assertEquals(0, outcome.exitCode(), outcome.output());
        assertEquals(Set.of("a"), outcome.definedClasses().keySet());
        assertArrayEquals(window, outcome.definedClasses().get("a"));
    }

    @Test
    void tellsHowTheChildJvmEndedWhenAClassIsMissing() throws IOException, InterruptedException {
        Path client = FixtureClient.jarOf(
                directory.resolve("client.jar"), Map.of("a", FixtureClient.classCalling("a", Map.of("b", List.of()))));

        AgentLaunch.Outcome outcome = AgentLaunch.run(List.of(), List.of(client), Set.of("z"), directory.resolve("run"));

        assertNotEquals(0, outcome.exitCode());
        assertTrue(outcome.output().contains("ClassNotFoundException"), outcome.output());
        assertTrue(outcome.definedClasses().isEmpty());
    }
}
