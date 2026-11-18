package io.github.luasgraphicss.minemod.verification.frame.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.verification.hooks.catalog.LibraryCall;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StandInsTest {
    private static final String FRAME = "io/github/luasgraphicss/minemod/adapters/frame/";
    private static final String MATRIX_MODE = "org/lwjgl/opengl/GL11.glMatrixMode(I)V";
    private static final String DISPLAY_CAPABILITIES =
            "org/lwjgl/opengl/GLContext.getCapabilities()Lorg/lwjgl/opengl/ContextCapabilities;";
    private static final String GLFW_CAPABILITIES =
            "org/lwjgl/opengl/GL.getCapabilities()Lorg/lwjgl/opengl/GLCapabilities;";

    @Test
    void answersTheFixedPipelineOnBothGenerationsOfLwjgl() throws IOException {
        Optional<String> matrices = Optional.of(FRAME + "calls/MatrixCalls");

        assertEquals(matrices, StandIns.ofLwjglGeneration(2).answering(MATRIX_MODE));
        assertEquals(matrices, StandIns.ofLwjglGeneration(3).answering(MATRIX_MODE));
    }

    @Test
    void answersTheCapabilitiesOfEachGenerationWithItsOwnClass() throws IOException {
        StandIns display = StandIns.ofLwjglGeneration(2);
        StandIns glfw = StandIns.ofLwjglGeneration(3);

        assertEquals(Optional.of(FRAME + "display/calls/ContextCalls"), display.answering(DISPLAY_CAPABILITIES));
        assertEquals(Optional.empty(), display.answering(GLFW_CAPABILITIES));
        assertEquals(Optional.of(FRAME + "glfw/calls/CapabilityCalls"), glfw.answering(GLFW_CAPABILITIES));
        assertEquals(Optional.empty(), glfw.answering(DISPLAY_CAPABILITIES));
    }

    @Test
    void namesEveryCallAfterItsOwnerAndItsSignature() throws IOException {
        StandIns standIns = StandIns.ofLwjglGeneration(3);
        List<String> points = standIns.calls().stream().map(LibraryCall::hook).toList();

        assertTrue(points.contains("GL11.glMatrixMode(I)V"), points.toString());
        assertEquals(points.size(), Set.copyOf(points).size());
        assertTrue(standIns.owners().contains("org/lwjgl/opengl/GL11"), standIns.owners().toString());
    }
}
