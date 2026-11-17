package io.github.luasgraphicss.minemod.compatibility.layer.sharing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.ShareableResource;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.SharingRequest;
import io.github.luasgraphicss.minemod.compatibility.interop.platform.HandlePlatform;
import io.github.luasgraphicss.minemod.compatibility.layer.fixtures.HeapMemory;
import io.github.luasgraphicss.minemod.compatibility.layer.fixtures.RecordingProvider;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.ScratchInts;
import io.github.luasgraphicss.minemod.compatibility.state.error.ErrorLedger;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProviderMemoryImportsTest {
    private static final int GL_INVALID_OPERATION = 0x0502;
    private static final int VK_FORMAT_R8G8B8A8_UNORM = 37;
    private static final long ALLOCATION_BYTES = 4096;
    private static final long HANDLE = 122;
    private static final int MEMORY_OBJECT = RecordingProvider.FIRST_NAME;
    private static final int TEXTURE = RecordingProvider.FIRST_NAME + 1;

    private final HeapMemory memory = new HeapMemory();
    private final RecordingProvider provider = new RecordingProvider(memory);
    private final ScratchInts scratch = new ScratchInts(memory, 1);
    private final SharingRequest color =
            SharingRequest.image(ShareableResource.COLOR_ATTACHMENT, VK_FORMAT_R8G8B8A8_UNORM, 32, 16, 1, 1);

    @Test
    void aFileDescriptorIsImportedThroughTheEntryPointOfFileDescriptors() {
        int texture = importsOn(HandlePlatform.LINUX_FILE_DESCRIPTOR).importMemory(color, ALLOCATION_BYTES, HANDLE);

        assertEquals(TEXTURE, texture);
        assertImported("glImportMemoryFdEXT[" + MEMORY_OBJECT + ", 4096, 38278, 122]");
    }

    @Test
    void anNtHandleIsImportedThroughTheEntryPointOfWin32Handles() {
        int texture = importsOn(HandlePlatform.WINDOWS_NT_HANDLE).importMemory(color, ALLOCATION_BYTES, HANDLE);

        assertEquals(TEXTURE, texture);
        assertImported("glImportMemoryWin32HandleEXT[" + MEMORY_OBJECT + ", 4096, 38279, 122]");
    }

    @Test
    void anImportTheProviderRefusesLeavesNoObjectBehindOnEitherPlatform() {
        provider.refuse("glTexStorageMem2DEXT", GL_INVALID_OPERATION);
        for (HandlePlatform platform : HandlePlatform.values()) {
            ProviderMemoryImports imports = importsOn(platform);

            IllegalStateException refused = assertThrows(
                    IllegalStateException.class, () -> imports.importMemory(color, ALLOCATION_BYTES, HANDLE));

            assertTrue(refused.getMessage().endsWith("with error 0x502"), refused.getMessage());
            assertEquals(0, imports.liveCount(), platform.name());
            List<String> deleted = provider.events().subList(provider.events().size() - 2, provider.events().size());
            assertTrue(deleted.get(0).startsWith("glDeleteTextures["), deleted.toString());
            assertTrue(deleted.get(1).startsWith("glDeleteMemoryObjectsEXT["), deleted.toString());
        }
    }

    private ProviderMemoryImports importsOn(HandlePlatform platform) {
        return new ProviderMemoryImports(provider, scratch, new ErrorLedger(provider::takeError), platform);
    }

    private void assertImported(String importCall) {
        assertEquals("glCreateMemoryObjectsEXT[1] -> " + MEMORY_OBJECT, provider.events().get(0));
        assertEquals(importCall, provider.events().get(1));
    }
}
