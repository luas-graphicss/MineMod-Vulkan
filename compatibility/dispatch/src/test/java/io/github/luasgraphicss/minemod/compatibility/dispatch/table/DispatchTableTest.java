package io.github.luasgraphicss.minemod.compatibility.dispatch.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.compatibility.contracts.support.OperationSupport;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallCatalog;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class DispatchTableTest {
    private static final long IMAGE_START = 0x7000_0000L;
    private static final long IMAGE_END = 0x7100_0000L;
    private static final long THUNK = 0x5000_0000L;

    private final MappedSource provider = new MappedSource();
    private final MappedSource thunks = new MappedSource();

    @Test
    void aForwardedFunctionGoesStraightToTheProvider() {
        provider.with("glUniform1f", IMAGE_START + 0x10);

        DispatchEntry entry = table(ContextProfile.CORE).entry("glUniform1f");

        assertEquals(Disposition.PROVIDER, entry.disposition());
        assertEquals(IMAGE_START + 0x10, entry.address());
        assertEquals(Collections.emptyList(), thunks.asked());
    }

    @Test
    void anInterceptedFunctionEntersThroughItsThunk() {
        provider.with("glBindTexture", IMAGE_START + 0x20);
        thunks.with("glBindTexture", THUNK);

        DispatchTable table = table(ContextProfile.CORE);

        assertEquals(THUNK, table.addressOf("glBindTexture"));
        assertEquals(Disposition.THUNK, table.entry("glBindTexture").disposition());
        assertEquals(OperationSupport.SUPPORTED, table.entry("glBindTexture").support());
    }

    @Test
    void anInterceptedFunctionWithoutThunkIsWithheldRatherThanForwarded() {
        provider.with("glReadPixels", IMAGE_START + 0x30);

        DispatchEntry entry = table(ContextProfile.CORE).entry("glReadPixels");

        assertEquals(Disposition.WITHHELD, entry.disposition());
        assertEquals(0, entry.address());
        assertEquals(OperationSupport.UNVERIFIED, entry.support());
    }

    @Test
    void anUndescribedFunctionIsNeverAskedOfTheProvider() {
        provider.with("glProgramStringARB", IMAGE_START + 0x40);

        DispatchEntry entry = table(ContextProfile.COMPATIBILITY).entry("glProgramStringARB");

        assertEquals(Disposition.WITHHELD, entry.disposition());
        assertEquals(OperationSupport.UNVERIFIED, entry.support());
        assertEquals(Collections.emptyList(), provider.asked());
    }

    @Test
    void legacyFunctionsExistOnlyInACompatibilityContext() {
        provider.with("glVertex3f", IMAGE_START + 0x50);

        assertEquals(Disposition.PROVIDER, table(ContextProfile.COMPATIBILITY).entry("glVertex3f").disposition());

        DispatchEntry inCore = table(ContextProfile.CORE).entry("glVertex3f");
        assertEquals(Disposition.WITHHELD, inCore.disposition());
        assertEquals(OperationSupport.OUT_OF_CONTRACT, inCore.support());
    }

    @Test
    void aFunctionTheProviderLacksIsWithheld() {
        DispatchEntry entry = table(ContextProfile.CORE).entry("glTexStorage2D");

        assertEquals(Disposition.WITHHELD, entry.disposition());
        assertEquals(OperationSupport.OUT_OF_CONTRACT, entry.support());
    }

    @Test
    void aPointerFromAnotherLibraryStopsTheResolution() {
        provider.with("glUniform1f", IMAGE_END);

        assertThrows(ForeignFunctionException.class, () -> table(ContextProfile.CORE).addressOf("glUniform1f"));
    }

    @Test
    void anExtensionEntryPointResolvesUnderItsOwnName() {
        provider.with("glBindBufferARB", IMAGE_START + 0x60);
        thunks.with("glBindBufferARB", THUNK + 0x8);

        assertEquals(THUNK + 0x8, table(ContextProfile.COMPATIBILITY).addressOf("glBindBufferARB"));
    }

    @Test
    void everyResolutionIsKeptForTheReport() {
        provider.with("glUniform1f", IMAGE_START + 0x10);
        DispatchTable table = table(ContextProfile.CORE);

        table.addressOf("glUniform1f");
        table.addressOf("glUniform1f");
        table.addressOf("glProgramStringARB");

        assertEquals(2, table.entries().size());
        assertEquals(Collections.singletonList("glUniform1f"), provider.asked());
    }

    private DispatchTable table(ContextProfile profile) {
        return new DispatchTable(
                CallCatalog.standard(), profile, provider, LibraryImage.ofSegments(IMAGE_START, IMAGE_END), thunks);
    }
}
