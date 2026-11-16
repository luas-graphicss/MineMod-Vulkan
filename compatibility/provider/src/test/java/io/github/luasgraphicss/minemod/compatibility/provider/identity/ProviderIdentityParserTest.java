package io.github.luasgraphicss.minemod.compatibility.provider.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.GlVersion;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProviderIdentityParserTest {
    private static final String COMPATIBILITY_VERSION = "4.6 (Compatibility Profile) Mesa 25.0.7";

    @Test
    void zinkOnAGraphicsCardIsRecognised() {
        ProviderIdentity identity = parse(
                "zink Vulkan 1.3(NVIDIA GeForce RTX 3060 (NVIDIA_PROPRIETARY))", COMPATIBILITY_VERSION);

        assertEquals(DriverKind.ZINK, identity.driverKind());
        assertEquals("NVIDIA GeForce RTX 3060", identity.deviceName());
        assertEquals(GlVersion.of(4, 6), identity.glVersion());
        assertEquals(ContextProfile.COMPATIBILITY, identity.profile());
        assertEquals(Optional.of("25.0.7"), identity.mesaVersion());
        assertFalse(identity.runsOnSoftwareDevice());
    }

    @Test
    void zinkOnASoftwareVulkanDeviceIsStillSoftware() {
        ProviderIdentity identity = parse(
                "zink Vulkan 1.3(llvmpipe (LLVM 17.0.6, 256 bits) (MESA_LLVMPIPE))", COMPATIBILITY_VERSION);

        assertEquals(DriverKind.ZINK, identity.driverKind());
        assertEquals("llvmpipe (LLVM 17.0.6, 256 bits)", identity.deviceName());
        assertTrue(identity.runsOnSoftwareDevice());
    }

    @Test
    void aSoftwareRasterizerIsNotZink() {
        ProviderIdentity identity = parse("llvmpipe (LLVM 17.0.6, 256 bits)", "4.5 (Core Profile) Mesa 25.0.7");

        assertEquals(DriverKind.SOFTWARE_RASTERIZER, identity.driverKind());
        assertEquals(ContextProfile.CORE, identity.profile());
        assertTrue(identity.runsOnSoftwareDevice());
    }

    @Test
    void aVendorDriverIsForeignAndReportsNoMesaVersion() {
        ProviderIdentity identity = parse("NVIDIA GeForce RTX 3060/PCIe/SSE2", "4.6.0 NVIDIA 550.54.14");

        assertEquals(DriverKind.FOREIGN, identity.driverKind());
        assertEquals(Optional.empty(), identity.mesaVersion());
        assertEquals(GlVersion.of(4, 6), identity.glVersion());
    }

    @Test
    void aLegacyContextHasNoProfileMarkAndIsCompatibility() {
        ProviderIdentity identity = parse("zink Vulkan 1.2(AMD Radeon RX 580 (MESA_RADV))", "2.1 Mesa 25.0.7");

        assertEquals(GlVersion.of(2, 1), identity.glVersion());
        assertEquals(ContextProfile.COMPATIBILITY, identity.profile());
    }

    @Test
    void unreadableStringsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> parse("zink Vulkan 1.3", COMPATIBILITY_VERSION));
        assertThrows(IllegalArgumentException.class, () -> parse("llvmpipe", "OpenGL ES 3.2 Mesa 25.0.7"));
    }

    private static ProviderIdentity parse(String renderer, String version) {
        return ProviderIdentityParser.parse(new ProviderStrings(renderer, version));
    }
}
