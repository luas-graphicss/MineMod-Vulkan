package io.github.luasgraphicss.minemod.adapters.frame.glfw.calls;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GLCapabilities;

class CapabilityCallsTest {
    @Test
    void offersTheFixedPipelineAndNothingTheSceneDoesNotDraw() {
        GLCapabilities offered = CapabilityCalls.getCapabilities();

        assertTrue(offered.OpenGL13);
        assertTrue(offered.OpenGL15);
        assertFalse(offered.OpenGL30);
        assertFalse(offered.GL_ARB_framebuffer_object);
        assertFalse(offered.GL_EXT_framebuffer_object);
        assertFalse(offered.GL_ARB_shader_objects);
    }

    @Test
    void answersEveryCallWithTheSameCapabilities() {
        assertSame(CapabilityCalls.getCapabilities(), CapabilityCalls.getCapabilities());
    }
}
