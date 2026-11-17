package io.github.luasgraphicss.minemod.compatibility.layer.sharing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.compatibility.interop.platform.HandlePlatform;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.ScratchInts;
import io.github.luasgraphicss.minemod.compatibility.layer.support.HeapMemory;
import io.github.luasgraphicss.minemod.compatibility.layer.support.RecordingProvider;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ProviderHandoffTest {
    private static final long HANDLE = 122;
    private static final int SEMAPHORE = RecordingProvider.FIRST_NAME;

    private final HeapMemory memory = new HeapMemory();
    private final RecordingProvider provider = new RecordingProvider(memory);
    private final ScratchInts scratch = new ScratchInts(memory, 1);

    @Test
    void aSemaphoreIsImportedFromAFileDescriptor() {
        int semaphore =
                ProviderHandoff.importSemaphore(provider, scratch, HandlePlatform.LINUX_FILE_DESCRIPTOR, HANDLE);

        assertEquals(SEMAPHORE, semaphore);
        assertEquals(
                Arrays.asList(
                        "glGenSemaphoresEXT[1] -> " + SEMAPHORE,
                        "glImportSemaphoreFdEXT[" + SEMAPHORE + ", 38278, 122]"),
                provider.events());
    }

    @Test
    void aSemaphoreIsImportedFromAnNtHandle() {
        int semaphore = ProviderHandoff.importSemaphore(provider, scratch, HandlePlatform.WINDOWS_NT_HANDLE, HANDLE);

        assertEquals(SEMAPHORE, semaphore);
        assertEquals(
                Arrays.asList(
                        "glGenSemaphoresEXT[1] -> " + SEMAPHORE,
                        "glImportSemaphoreWin32HandleEXT[" + SEMAPHORE + ", 38279, 122]"),
                provider.events());
    }
}
