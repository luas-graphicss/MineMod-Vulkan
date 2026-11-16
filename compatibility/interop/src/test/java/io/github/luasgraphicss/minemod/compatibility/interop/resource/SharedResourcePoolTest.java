package io.github.luasgraphicss.minemod.compatibility.interop.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.compatibility.interop.device.DeviceIdentity;
import io.github.luasgraphicss.minemod.compatibility.interop.handle.HandleLedger;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.ExternalSupport;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.ExternalSupportQuery;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.PlannedShare;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.ShareableResource;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.SharingNegotiator;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.SharingRequest;
import io.github.luasgraphicss.minemod.compatibility.interop.platform.HandlePlatform;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class SharedResourcePoolTest {
    private static final int CYCLES = 1_000;

    @Test
    void repeatedCreationReloadAndDestructionLeavesNothingOpenOnLinux() {
        runLifecycleCycles(HandlePlatform.LINUX_FILE_DESCRIPTOR);
    }

    @Test
    void repeatedCreationReloadAndDestructionLeavesNothingOpenOnWindows() {
        runLifecycleCycles(HandlePlatform.WINDOWS_NT_HANDLE);
    }

    @Test
    void aRetiredResourceOutlivesTheFramesThatStillUseIt() {
        Fixture fixture = new Fixture(HandlePlatform.LINUX_FILE_DESCRIPTOR);
        SharedResource resource = fixture.pool.create(fixture.share);

        fixture.pool.retire(resource, 9);

        assertEquals(0, fixture.pool.collect(8));
        assertEquals(1, fixture.backend.providerObjectCount());
        assertEquals(1, fixture.pool.collect(9));
        assertEquals(0, fixture.backend.providerObjectCount());
        assertEquals(0, fixture.backend.rendererResourceCount());
        assertThrows(IllegalArgumentException.class, () -> fixture.pool.retire(resource, 10));
    }

    @Test
    void aFailedExportOrImportLeavesNoHalfSharedResource() {
        for (HandlePlatform platform : HandlePlatform.values()) {
            Fixture fixture = new Fixture(platform);

            fixture.backend.failExports(true);
            assertThrows(IllegalStateException.class, () -> fixture.pool.create(fixture.share));
            fixture.backend.failExports(false);
            fixture.backend.failImports(true);
            assertThrows(IllegalStateException.class, () -> fixture.pool.create(fixture.share));

            fixture.assertNothingLeft(platform.name());
        }
    }

    private static void runLifecycleCycles(HandlePlatform platform) {
        Fixture fixture = new Fixture(platform);
        for (int cycle = 0; cycle < CYCLES; cycle++) {
            SharedResource first = fixture.pool.create(fixture.share);
            SharedResource reloaded = fixture.pool.create(fixture.share);
            fixture.pool.retire(first, cycle);
            fixture.backend.failImports(cycle % 7 == 0);
            if (cycle % 7 == 0) {
                assertThrows(IllegalStateException.class, () -> fixture.pool.create(fixture.share));
            }
            fixture.backend.failImports(false);
            fixture.pool.retire(reloaded, cycle);
            assertEquals(2, fixture.pool.collect(cycle), "cycle " + cycle);
        }
        fixture.assertNothingLeft(platform.name());
    }

    private static final class Fixture {
        private final CountingResourceBackend backend;
        private final HandleLedger handles;
        private final SharedResourcePool pool;
        private final PlannedShare share;

        private Fixture(HandlePlatform platform) {
            backend = new CountingResourceBackend(platform.importTakesOwnership());
            handles = new HandleLedger(platform, backend);
            pool = new SharedResourcePool(backend, handles);
            share = planColourAttachment(platform);
        }

        private void assertNothingLeft(String where) {
            assertEquals(0, pool.liveCount(), where);
            assertEquals(0, pool.retiringCount(), where);
            assertEquals(0, handles.ownedCount(), where);
            assertEquals(0, backend.openHandleCount(), where);
            assertEquals(0, backend.providerObjectCount(), where);
            assertEquals(0, backend.rendererResourceCount(), where);
        }

        private static PlannedShare planColourAttachment(HandlePlatform platform) {
            DeviceIdentity gpu =
                    new DeviceIdentity(new byte[DeviceIdentity.UUID_BYTES], new byte[DeviceIdentity.UUID_BYTES]);
            ExternalSupportQuery query = new ExternalSupportQuery() {
                @Override
                public ExternalSupport supportFor(SharingRequest request) {
                    return ExternalSupport.shareable(true, 16384, 16384, 15, 1);
                }

                @Override
                public boolean binarySemaphoresShareable() {
                    return true;
                }
            };
            SharingRequest colour = SharingRequest.image(ShareableResource.COLOR_ATTACHMENT, 37, 854, 480, 1, 1);
            return SharingNegotiator.negotiate(
                            gpu, gpu, platform.requiredExtensions(), platform, Collections.singletonList(colour), query)
                    .shares()
                    .get(0);
        }
    }
}
