package io.github.luasgraphicss.minemod.verification.journey.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class LoadedLibrariesTest {
    @Test
    void findsTheEntryPointsOfOpenGlAndZink() {
        LoadedLibraries libraries = new LoadedLibraries(List.of(
                "libEGL.so.1.1.0", "libGL.so.1", "libGLESv2.so.2", "libGLX.so.0", "libOpenGL.so.0", "zink_dri.so"));

        assertEquals(libraries.names(), libraries.openGl());
    }

    @Test
    void aVulkanProcessWithTheBindingsOfTheGameMapsNoOpenGl() {
        LoadedLibraries libraries = new LoadedLibraries(List.of(
                "libGLX_nvidia.so.0",
                "libglfw.so",
                "liblwjgl_opengl.so",
                "libminemod_jni.so",
                "libvulkan.so.1",
                "libwayland-egl.so.1"));

        assertEquals(List.of(), libraries.openGl());
        assertTrue(libraries.hasVulkanLoader());
        assertTrue(libraries.has(LoadedLibraries.NATIVE_RENDERER));
        assertFalse(libraries.has(LoadedLibraries.OPENGL_PROVIDER));
    }
}
