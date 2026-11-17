package io.github.luasgraphicss.minemod.adapters.presentation.display.standin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class AwtCanvasSurfaceTest {
    @Test
    void asksForTheSurfaceExtensionOfTheWindowSystemItRunsOn() {
        assertEquals(
                Arrays.asList("VK_KHR_surface", "VK_KHR_xlib_surface"), AwtCanvasSurface.extensionsOn("Linux"));
        assertEquals(
                Arrays.asList("VK_KHR_surface", "VK_KHR_win32_surface"), AwtCanvasSurface.extensionsOn("Windows 11"));
    }
}
