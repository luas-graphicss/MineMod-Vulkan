package io.github.luasgraphicss.minemod.adapters.presentation.display;

import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwners;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.WindowRequest;
import org.lwjgl.LWJGLException;

final class DisplayOpening {
    interface LibraryCall {
        void run() throws LWJGLException;
    }

    private DisplayOpening() {}

    static void open(LibraryCall library) throws LWJGLException {
        try {
            PresentationOwners.current().openWindow(WindowRequest.ofDisplay(), () -> {
                try {
                    library.run();
                } catch (LWJGLException failure) {
                    throw new DisplayFailure(failure);
                }
                return WindowRequest.DISPLAY_WINDOW;
            });
        } catch (DisplayFailure failure) {
            throw failure.reason();
        }
    }
}
