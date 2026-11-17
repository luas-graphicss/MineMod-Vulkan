package io.github.luasgraphicss.minemod.adapters.presentation.display;

import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwners;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.WindowRequest;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.PixelFormat;

public final class DisplayPresentationHooks {
    public static final String INTERNAL_NAME =
            "io/github/luasgraphicss/minemod/adapters/presentation/display/DisplayPresentationHooks";
    private static final int SYNCHRONISED = 1;
    private static final int IMMEDIATE = 0;

    private DisplayPresentationHooks() {}

    public static void create(PixelFormat format) throws LWJGLException {
        DisplayOpening.open(() -> Display.create(format));
    }

    public static void create() throws LWJGLException {
        DisplayOpening.open(Display::create);
    }

    public static void update() {
        PresentationOwners.current().presentFrame(WindowRequest.DISPLAY_WINDOW, Display::update);
    }

    public static void setVSyncEnabled(boolean enabled) {
        PresentationOwners.current()
                .pace(enabled ? SYNCHRONISED : IMMEDIATE, () -> Display.setVSyncEnabled(enabled));
    }

    public static void destroy() {
        PresentationOwners.current().closeWindow(WindowRequest.DISPLAY_WINDOW, Display::destroy);
    }
}
