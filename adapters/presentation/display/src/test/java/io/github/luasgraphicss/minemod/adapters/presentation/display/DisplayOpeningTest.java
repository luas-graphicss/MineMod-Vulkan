package io.github.luasgraphicss.minemod.adapters.presentation.display;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.lwjgl.LWJGLException;

class DisplayOpeningTest {
    @Test
    void givesTheGameTheFailureOfTheLibraryUnchanged() {
        LWJGLException noPixelFormat = new LWJGLException("no pixel format");

        LWJGLException thrown = assertThrows(LWJGLException.class, () -> DisplayOpening.open(() -> {
            throw noPixelFormat;
        }));

        assertSame(noPixelFormat, thrown);
    }
}
