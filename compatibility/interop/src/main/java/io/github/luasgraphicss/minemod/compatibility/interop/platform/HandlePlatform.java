package io.github.luasgraphicss.minemod.compatibility.interop.platform;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public enum HandlePlatform {
    LINUX_FILE_DESCRIPTOR(true, "GL_EXT_memory_object_fd", "GL_EXT_semaphore_fd"),
    WINDOWS_NT_HANDLE(false, "GL_EXT_memory_object_win32", "GL_EXT_semaphore_win32");

    private final boolean importTakesOwnership;
    private final List<String> memoryExtensions;
    private final List<String> requiredExtensions;

    HandlePlatform(boolean importTakesOwnership, String memoryExtension, String semaphoreExtension) {
        this.importTakesOwnership = importTakesOwnership;
        this.memoryExtensions = Collections.unmodifiableList(Arrays.asList("GL_EXT_memory_object", memoryExtension));
        this.requiredExtensions = Collections.unmodifiableList(
                Arrays.asList("GL_EXT_memory_object", memoryExtension, "GL_EXT_semaphore", semaphoreExtension));
    }

    public static HandlePlatform current() {
        return ofSystem(System.getProperty("os.name"));
    }

    public static HandlePlatform ofSystem(String systemName) {
        String system = systemName.toLowerCase(Locale.ROOT);
        if (system.startsWith("windows")) {
            return WINDOWS_NT_HANDLE;
        }
        if (system.startsWith("linux")) {
            return LINUX_FILE_DESCRIPTOR;
        }
        throw new IllegalArgumentException("the renderer shares no handle with an OpenGL provider on " + systemName);
    }

    public boolean importTakesOwnership() {
        return importTakesOwnership;
    }

    public List<String> memoryExtensions() {
        return memoryExtensions;
    }

    public List<String> requiredExtensions() {
        return requiredExtensions;
    }
}
