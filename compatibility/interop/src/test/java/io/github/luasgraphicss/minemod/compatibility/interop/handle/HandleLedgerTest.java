package io.github.luasgraphicss.minemod.compatibility.interop.handle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.compatibility.interop.platform.HandlePlatform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class HandleLedgerTest {
    private static final long HANDLE = 41;

    private final List<Long> closed = new ArrayList<>();

    @Test
    void anImportedFileDescriptorBelongsToTheProviderAndIsNotClosedHere() {
        HandleLedger ledger = new HandleLedger(HandlePlatform.LINUX_FILE_DESCRIPTOR, closed::add);
        ledger.exported(HANDLE);

        ledger.imported(HANDLE);

        assertEquals(Collections.emptyList(), closed);
        assertEquals(0, ledger.ownedCount());
    }

    @Test
    void anImportedNtHandleIsStillOursAndIsClosedOnce() {
        HandleLedger ledger = new HandleLedger(HandlePlatform.WINDOWS_NT_HANDLE, closed::add);
        ledger.exported(HANDLE);

        ledger.imported(HANDLE);

        assertEquals(Collections.singletonList(HANDLE), closed);
        assertEquals(0, ledger.ownedCount());
    }

    @Test
    void aHandleThatWasNeverImportedIsClosedOnEitherPlatform() {
        for (HandlePlatform platform : HandlePlatform.values()) {
            closed.clear();
            HandleLedger ledger = new HandleLedger(platform, closed::add);
            ledger.exported(HANDLE);

            ledger.abandoned(HANDLE);

            assertEquals(Collections.singletonList(HANDLE), closed, platform.name());
        }
    }

    @Test
    void aHandleIsNeverClosedTwice() {
        HandleLedger ledger = new HandleLedger(HandlePlatform.LINUX_FILE_DESCRIPTOR, closed::add);
        ledger.exported(HANDLE);
        ledger.abandoned(HANDLE);

        assertThrows(IllegalStateException.class, () -> ledger.abandoned(HANDLE));
        assertThrows(IllegalStateException.class, () -> ledger.imported(HANDLE));
        assertEquals(Collections.singletonList(HANDLE), closed);
    }

    @Test
    void anOpenHandleCannotBeExportedAgain() {
        HandleLedger ledger = new HandleLedger(HandlePlatform.LINUX_FILE_DESCRIPTOR, closed::add);
        ledger.exported(HANDLE);

        assertThrows(IllegalStateException.class, () -> ledger.exported(HANDLE));
        assertEquals(1, ledger.ownedCount());
    }
}
