package io.github.luasgraphicss.minemod.verification.frame.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.verification.frame.catalog.StandIns;
import io.github.luasgraphicss.minemod.verification.frame.fixtures.FrameFixtures;
import io.github.luasgraphicss.minemod.verification.hooks.catalog.LibraryCall;
import io.github.luasgraphicss.minemod.verification.hooks.catalog.PresentationCalls;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks.Windowing;
import io.github.luasgraphicss.minemod.verification.hooks.scan.ClientArchive;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FramePointsTest {
    @TempDir
    Path directory;

    private StandIns standIns;

    @BeforeEach
    void readTheStandInsOfTheRelease() throws IOException {
        standIns = StandIns.ofLwjglGeneration(FrameFixtures.LWJGL_GENERATION);
    }

    @Test
    void pinsEveryAnsweredCallUnderTheMethodThatMakesIt() throws IOException {
        try (ClientArchive client = FrameFixtures.client(directory.resolve("client.jar"))) {
            ReleaseHooks points = FramePoints.pinnedOn(FrameFixtures.release(), client, standIns);

            assertEquals(
                    List.of(
                            "GL11.glLoadIdentity()V in a.m()V x2",
                            "GL11.glMatrixMode(I)V#1 in a.m()V x1",
                            "GL11.glMatrixMode(I)V#2 in a.n()V x1"),
                    points.points().stream()
                            .map(point -> point.id() + " in " + point.within() + " x" + point.matches())
                            .sorted()
                            .toList());
            assertEquals(4, points.callSites());
            assertEquals(Windowing.GLFW, points.windowing());
        }
    }

    @Test
    void refusesAClientThatCallsWhatNoStandInAnswers() throws IOException {
        try (ClientArchive client = FrameFixtures.clientCalling(
                directory.resolve("client.jar"), Map.of("m", List.of(FrameFixtures.NEVER_ANSWERED)))) {
            IOException refusal = assertThrows(
                    IOException.class, () -> FramePoints.pinnedOn(FrameFixtures.release(), client, standIns));

            assertTrue(refusal.getMessage().contains("GL11.glNeverAnswered()V"), refusal.getMessage());
        }
    }

    @Test
    void leavesTheCallsOfThePresentationToItsHooks() throws IOException {
        LibraryCall createCapabilities = PresentationCalls.of(Windowing.GLFW).stream()
                .filter(call -> call.hook().equals("capabilities"))
                .findFirst()
                .orElseThrow();
        try (ClientArchive client = FrameFixtures.clientCalling(
                directory.resolve("client.jar"), Map.of("m", List.of(createCapabilities)))) {
            assertEquals(List.of(), FramePoints.pinnedOn(FrameFixtures.release(), client, standIns).points());
            assertEquals(Map.of(), FramePoints.leftWithTheLibrary(client, standIns, Windowing.GLFW));
        }
    }

    @Test
    void countsTheCallsThatStayWithTheLibrary() throws IOException {
        try (ClientArchive client = FrameFixtures.clientCalling(
                directory.resolve("client.jar"),
                Map.of("m", List.of(FrameFixtures.USE_PROGRAM, FrameFixtures.MATRIX_MODE, FrameFixtures.USE_PROGRAM)))) {
            assertEquals(Map.of("GL20", 2), FramePoints.leftWithTheLibrary(client, standIns, Windowing.GLFW));
        }
    }
}
