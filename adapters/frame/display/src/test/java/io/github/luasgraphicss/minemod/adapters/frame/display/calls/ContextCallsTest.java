package io.github.luasgraphicss.minemod.adapters.frame.display.calls;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.ContextCapabilities;

class ContextCallsTest {
    @Test
    void offersTheFixedPipelineAndNothingTheSceneDoesNotDraw() {
        ContextCapabilities offered = ContextCalls.getCapabilities();

        assertTrue(offered.OpenGL13);
        assertTrue(offered.OpenGL15);
        assertFalse(offered.OpenGL30);
        assertFalse(offered.GL_ARB_framebuffer_object);
        assertFalse(offered.GL_EXT_framebuffer_object);
        assertFalse(offered.GL_ARB_shader_objects);
    }

    @Test
    void answersEveryCallWithTheSameCapabilities() {
        assertSame(ContextCalls.getCapabilities(), ContextCalls.getCapabilities());
    }
}
