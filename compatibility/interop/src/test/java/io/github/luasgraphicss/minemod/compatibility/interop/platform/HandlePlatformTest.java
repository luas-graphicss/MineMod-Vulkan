package io.github.luasgraphicss.minemod.compatibility.interop.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class HandlePlatformTest {
    @Test
    void windowsSharesNtHandlesWhateverItsEdition() {
        assertEquals(HandlePlatform.WINDOWS_NT_HANDLE, HandlePlatform.ofSystem("Windows 11"));
        assertEquals(HandlePlatform.WINDOWS_NT_HANDLE, HandlePlatform.ofSystem("WINDOWS SERVER 2025"));
    }

    @Test
    void linuxSharesFileDescriptors() {
        assertEquals(HandlePlatform.LINUX_FILE_DESCRIPTOR, HandlePlatform.ofSystem("Linux"));
    }

    @Test
    void aSystemWithoutASharedHandleTypeIsNamedInTheRefusal() {
        IllegalArgumentException refused =
                assertThrows(IllegalArgumentException.class, () -> HandlePlatform.ofSystem("Mac OS X"));

        assertEquals("the renderer shares no handle with an OpenGL provider on Mac OS X", refused.getMessage());
    }

    @Test
    void theRunningSystemSharesTheHandlesOfItsName() {
        assertEquals(HandlePlatform.ofSystem(System.getProperty("os.name")), HandlePlatform.current());
    }
}
