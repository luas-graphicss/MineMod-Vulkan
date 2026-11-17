package io.github.luasgraphicss.minemod.adapters.presentation.glfw;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.luasgraphicss.minemod.adapters.presentation.fixtures.InstalledOwners;
import io.github.luasgraphicss.minemod.adapters.presentation.lifecycle.GuardedPresentation;
import io.github.luasgraphicss.minemod.adapters.presentation.lifecycle.PresentationPhase;
import io.github.luasgraphicss.minemod.adapters.presentation.renderer.fixtures.RendererBundle;
import io.github.luasgraphicss.minemod.adapters.presentation.renderer.session.BundledRenderers;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

class NativeGlfwOnWindowTest {
    private static final String RENDERER_LIBRARY = "minemod.sceneLibrary";
    private static final String WINDOW_SYSTEM = "DISPLAY";
    private static final int FRAMES = 4;

    @TempDir
    Path scratch;

    private GuardedPresentation presentation;

    @BeforeEach
    void installTheNativeOwnerOverTheBuiltRenderer() throws IOException {
        String library = System.getProperty(RENDERER_LIBRARY);
        assumeTrue(library != null, "no renderer library was built for this run");
        assumeTrue(System.getenv(WINDOW_SYSTEM) != null, "this run has no window system");
        assumeTrue(GLFW.glfwInit(), "GLFW cannot reach the window system");
        ClassLoader bundle = RendererBundle.around(Paths.get(library), scratch.resolve("bundle"));
        presentation = (GuardedPresentation)
                GlfwNativeOwner.install(new BundledRenderers(bundle, scratch.resolve("natives")));
    }

    @AfterEach
    void releaseTheOwnerAndTheLibrary() {
        if (presentation != null) {
            InstalledOwners.release();
            GLFW.glfwTerminate();
        }
    }

    @Test
    void presentsTheWindowOfTheGameWithoutItsOpenGlContext() {
        long window = GlfwPresentationHooks.glfwCreateWindow(320, 240, "Minecraft", MemoryUtil.NULL, MemoryUtil.NULL);

        assertNotEquals(MemoryUtil.NULL, window);
        assertEquals(GLFW.GLFW_NO_API, GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_CLIENT_API));
        GlfwPresentationHooks.glfwMakeContextCurrent(window);
        assertNull(GlfwPresentationHooks.createCapabilities());
        GlfwPresentationHooks.glfwSwapInterval(1);
        for (int frame = 1; frame < FRAMES; frame++) {
            GlfwPresentationHooks.glfwSwapBuffers(window);
        }
        GlfwPresentationHooks.glfwSwapInterval(0);
        GLFW.glfwSetWindowSize(window, 400, 300);
        GLFW.glfwPollEvents();
        GlfwPresentationHooks.glfwSwapBuffers(window);
        GlfwPresentationHooks.glfwDestroyWindow(window);

        assertEquals(FRAMES, presentation.framesPresented());
        assertEquals(PresentationPhase.CLOSED, presentation.phase());
    }
}
