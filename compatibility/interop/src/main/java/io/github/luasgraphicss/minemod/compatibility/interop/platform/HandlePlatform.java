package io.github.luasgraphicss.minemod.compatibility.interop.platform;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum HandlePlatform {
    LINUX_FILE_DESCRIPTOR(true, "GL_EXT_memory_object_fd", "GL_EXT_semaphore_fd"),
    WINDOWS_NT_HANDLE(false, "GL_EXT_memory_object_win32", "GL_EXT_semaphore_win32");

    private final boolean importTakesOwnership;
    private final List<String> requiredExtensions;

    HandlePlatform(boolean importTakesOwnership, String memoryExtension, String semaphoreExtension) {
        this.importTakesOwnership = importTakesOwnership;
        this.requiredExtensions = Collections.unmodifiableList(
                Arrays.asList("GL_EXT_memory_object", memoryExtension, "GL_EXT_semaphore", semaphoreExtension));
    }

    public boolean importTakesOwnership() {
        return importTakesOwnership;
    }

    public List<String> requiredExtensions() {
        return requiredExtensions;
    }
}
