package io.github.luasgraphicss.minemod.compatibility.layer.sharing;

import io.github.luasgraphicss.minemod.compatibility.interop.platform.HandlePlatform;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;

final class HandleImports {
    private static final int GL_HANDLE_TYPE_OPAQUE_FD = 0x9586;
    private static final int GL_HANDLE_TYPE_OPAQUE_WIN32 = 0x9587;

    private final String memoryImport;
    private final String semaphoreImport;
    private final int handleType;

    private HandleImports(String memoryImport, String semaphoreImport, int handleType) {
        this.memoryImport = memoryImport;
        this.semaphoreImport = semaphoreImport;
        this.handleType = handleType;
    }

    static HandleImports on(HandlePlatform platform) {
        switch (platform) {
            case LINUX_FILE_DESCRIPTOR:
                return new HandleImports("glImportMemoryFdEXT", "glImportSemaphoreFdEXT", GL_HANDLE_TYPE_OPAQUE_FD);
            case WINDOWS_NT_HANDLE:
                return new HandleImports(
                        "glImportMemoryWin32HandleEXT", "glImportSemaphoreWin32HandleEXT", GL_HANDLE_TYPE_OPAQUE_WIN32);
            default:
                throw new IllegalArgumentException("the provider imports no handle of " + platform);
        }
    }

    void memory(ProviderCalls provider, int memoryObject, long allocationSize, long handle) {
        provider.invoke(memoryImport, new long[] {memoryObject, allocationSize, handleType, handle});
    }

    void semaphore(ProviderCalls provider, int semaphore, long handle) {
        provider.invoke(semaphoreImport, new long[] {semaphore, handleType, handle});
    }
}
