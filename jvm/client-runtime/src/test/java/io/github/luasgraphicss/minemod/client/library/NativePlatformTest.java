package io.github.luasgraphicss.minemod.client.library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class NativePlatformTest {
    @Test
    void bundlesTheLibraryUnderThePlatformItWasBuiltFor() {
        assertEquals(
                "natives/linux-x86_64/libminemod_jni.so", NativePlatform.of("Linux", "amd64").libraryResource());
        assertEquals(
                "natives/linux-aarch64/libminemod_jni.so", NativePlatform.of("Linux", "aarch64").libraryResource());
        assertEquals(
                "natives/windows-x86_64/minemod_jni.dll", NativePlatform.of("Windows 11", "x86_64").libraryResource());
    }

    @Test
    void keepsTheDigestNextToTheLibrary() {
        assertEquals(
                "natives/linux-x86_64/libminemod_jni.so.sha256", NativePlatform.of("Linux", "amd64").digestResource());
    }

    @Test
    void bundlesTheOpenGlProviderNextToTheRenderer() {
        assertEquals(
                "natives/linux-x86_64/libminemod_gl_provider_jni.so",
                NativePlatform.of("Linux", "amd64").openGlProvider().libraryResource());
        assertEquals(
                "natives/windows-x86_64/minemod_gl_provider_jni.dll.sha256",
                NativePlatform.of("Windows 11", "x86_64").openGlProvider().digestResource());
    }

    @Test
    void refusesASystemWithoutANativeRenderer() {
        assertThrows(NativeLibraryUnavailable.class, () -> NativePlatform.of("Mac OS X", "aarch64"));
    }

    @Test
    void refusesAnArchitectureWithoutANativeRenderer() {
        assertThrows(NativeLibraryUnavailable.class, () -> NativePlatform.of("Linux", "riscv64"));
    }
}
