package io.github.luasgraphicss.minemod.compatibility.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.ExtensionCatalog;
import io.github.luasgraphicss.minemod.compatibility.dispatch.table.Disposition;
import io.github.luasgraphicss.minemod.compatibility.natives.binding.NativeProviderException;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.provider.diagnosis.RejectionReason;
import io.github.luasgraphicss.minemod.compatibility.provider.identity.DriverKind;
import io.github.luasgraphicss.minemod.compatibility.provider.load.ProviderLoadException;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.ProviderTestEnvironment;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.StagedProvider;
import io.github.luasgraphicss.minemod.compatibility.runtime.provider.ProviderBundle;
import io.github.luasgraphicss.minemod.compatibility.runtime.provider.ProviderRejectedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

@EnabledIfSystemProperty(named = ProviderTestEnvironment.SYSTEM_PROVIDER, matches = ".+")
class ProviderSessionTest {
    @TempDir
    Path providerDirectory;

    @Test
    void theStagedProviderIsAcceptedAsZinkAndServesEveryExposedFunctionFromItsOwnImage() throws Exception {
        ProviderBundle bundle = ProviderTestEnvironment.stage(providerDirectory);

        try (CompatibilitySession session = ProviderTestEnvironment.open(bundle, ContextProfile.CORE)) {
            assertTrue(session.diagnosis().isAccepted(), session.diagnosis().lines().toString());
            assertEquals(DriverKind.ZINK, session.diagnosis().identity().driverKind());
            assertEquals(bundle.pin().mesaVersion(), session.diagnosis().identity().mesaVersion().orElse("none"));

            ExtensionCatalog approved = ExtensionCatalog.standard();
            assertFalse(session.exposedExtensions().isEmpty());
            assertTrue(approved.extensions().containsAll(session.exposedExtensions()));

            assertEquals(Disposition.PROVIDER, session.dispatch().entry("glCheckFramebufferStatus").disposition());
            assertEquals(Disposition.THUNK, session.dispatch().entry("glBindFramebuffer").disposition());
            assertEquals(Disposition.WITHHELD, session.dispatch().entry("glBegin").disposition());
            assertEquals(Disposition.WITHHELD, session.dispatch().entry("glNotAFunction").disposition());
        }
    }

    @Test
    void aModifiedEntryLibraryIsRefusedBeforeItIsLoaded() throws Exception {
        ProviderBundle bundle = ProviderTestEnvironment.stage(providerDirectory);
        Files.write(providerDirectory.resolve(StagedProvider.ENTRY), new byte[] {0}, StandardOpenOption.APPEND);

        ProviderLoadException refused = assertThrows(
                ProviderLoadException.class, () -> ProviderTestEnvironment.open(bundle, ContextProfile.CORE));

        assertEquals(ProviderLoadException.Reason.DIGEST_MISMATCH, refused.reason());
    }

    @Test
    void aModifiedDependencyIsRefusedBeforeItIsLoaded() throws Exception {
        ProviderBundle bundle = ProviderTestEnvironment.stage(providerDirectory);
        String dependency = bundle.dependencies().keySet().iterator().next();
        Files.write(providerDirectory.resolve(dependency), new byte[] {0}, StandardOpenOption.APPEND);

        NativeProviderException refused = assertThrows(
                NativeProviderException.class, () -> ProviderTestEnvironment.open(bundle, ContextProfile.CORE));

        assertTrue(refused.getMessage().contains(dependency), refused.getMessage());
    }

    @Test
    void aSoftwareDeviceIsRejectedByNameUnlessTheProfileAllowsIt() throws Exception {
        ProviderBundle bundle = ProviderTestEnvironment.stage(providerDirectory);
        boolean rendersOnTheProcessor;
        try (CompatibilitySession session = ProviderTestEnvironment.open(bundle, ContextProfile.CORE)) {
            rendersOnTheProcessor = session.diagnosis().identity().runsOnSoftwareDevice();
        }
        assumeTrue(rendersOnTheProcessor, "this machine renders on a GPU");

        ProviderRejectedException rejected = assertThrows(
                ProviderRejectedException.class,
                () -> CompatibilitySession.open(
                        bundle, ProviderTestEnvironment.hardwareOnly(ContextProfile.CORE), 4, 5));

        assertTrue(rejected.diagnosis().rejections().containsKey(RejectionReason.SOFTWARE_DEVICE));
    }
}
