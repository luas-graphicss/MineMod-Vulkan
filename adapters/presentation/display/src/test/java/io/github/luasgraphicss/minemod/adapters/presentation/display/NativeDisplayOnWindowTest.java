package io.github.luasgraphicss.minemod.adapters.presentation.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.luasgraphicss.minemod.adapters.presentation.lifecycle.GuardedPresentation;
import io.github.luasgraphicss.minemod.adapters.presentation.lifecycle.PresentationPhase;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwner;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwners;
import io.github.luasgraphicss.minemod.adapters.presentation.renderer.fixtures.RendererBundle;
import io.github.luasgraphicss.minemod.adapters.presentation.renderer.session.BundledRenderers;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.PixelFormat;

class NativeDisplayOnWindowTest {
    private static final String RENDERER_LIBRARY = "minemod.sceneLibrary";
    private static final int FRAMES = 4;

    @TempDir
    Path scratch;

    private PresentationOwner owner;

    @BeforeEach
    void installTheNativeOwnerOverTheBuiltRenderer() throws IOException {
        String library = System.getProperty(RENDERER_LIBRARY);
        assumeTrue(library != null, "no renderer library was built for this run");
        assumeFalse(GraphicsEnvironment.isHeadless(), "this run has no window system");
        ClassLoader bundle = RendererBundle.around(Paths.get(library), scratch.resolve("bundle"));
        owner = DisplayNativeOwner.install(new BundledRenderers(bundle, scratch.resolve("natives")));
    }

    @AfterEach
    void releaseTheOwner() {
        if (owner != null) {
            PresentationOwners.release(owner);
        }
    }

    @Test
    void presentsTheDisplayOfTheGameWithoutItsOpenGlContext() throws LWJGLException {
        GuardedPresentation presentation = (GuardedPresentation) owner;

        assertFalse(presentation.keepsOpenGlContext());
        DisplayPresentationHooks.create(new PixelFormat());
        DisplayPresentationHooks.setVSyncEnabled(true);
        for (int frame = 1; frame < FRAMES; frame++) {
            DisplayPresentationHooks.update();
        }
        DisplayPresentationHooks.setVSyncEnabled(false);
        DisplayPresentationHooks.update();
        DisplayPresentationHooks.destroy();

        assertEquals(FRAMES, presentation.framesPresented());
        assertEquals(PresentationPhase.CLOSED, presentation.phase());
    }
}
