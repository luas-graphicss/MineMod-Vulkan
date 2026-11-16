package io.github.luasgraphicss.minemod.compatibility.provider.diagnosis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.provider.artifact.ArtifactDigest;
import io.github.luasgraphicss.minemod.compatibility.provider.artifact.ProviderPin;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.CapabilityRequirements;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.GlVersion;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ProviderCapabilities;
import io.github.luasgraphicss.minemod.compatibility.provider.identity.ProviderIdentityParser;
import io.github.luasgraphicss.minemod.compatibility.provider.identity.ProviderStrings;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProviderInspectorTest {
    private static final String ZINK_ON_HARDWARE = "zink Vulkan 1.3(AMD Radeon RX 6600 (MESA_RADV))";
    private static final String ZINK_ON_LAVAPIPE = "zink Vulkan 1.3(llvmpipe (LLVM 17.0.6, 256 bits) (MESA_LLVMPIPE))";
    private static final String PINNED_VERSION = "4.6 (Compatibility Profile) Mesa 25.0.7";
    private static final List<String> SHARING_EXTENSIONS =
            Arrays.asList("GL_EXT_memory_object", "GL_EXT_memory_object_fd", "GL_EXT_semaphore", "GL_EXT_semaphore_fd");
    private static final ProviderPin PIN = new ProviderPin(
            "25.0.7", ArtifactDigest.ofHex("00000000000000000000000000000000000000000000000000000000000000aa"));

    @Test
    void thePinnedZinkWithEveryRequirementIsAccepted() {
        ProviderDiagnosis diagnosis = inspect(ZINK_ON_HARDWARE, PINNED_VERSION, SHARING_EXTENSIONS, false);

        assertTrue(diagnosis.isAccepted());
        assertEquals("accepted", diagnosis.lines().get(diagnosis.lines().size() - 1));
    }

    @Test
    void anotherDriverIsRejectedByName() {
        ProviderDiagnosis diagnosis =
                inspect("NVIDIA GeForce RTX 3060/PCIe/SSE2", "4.6.0 NVIDIA 550.54.14", SHARING_EXTENSIONS, false);

        assertEquals(
                EnumSet.of(RejectionReason.NOT_ZINK, RejectionReason.MESA_VERSION_MISMATCH),
                diagnosis.rejections().keySet());
    }

    @Test
    void aSoftwareDeviceIsOnlyAcceptedWhenTheProfileAllowsIt() {
        assertEquals(
                Collections.singleton(RejectionReason.SOFTWARE_DEVICE),
                inspect(ZINK_ON_LAVAPIPE, PINNED_VERSION, SHARING_EXTENSIONS, false).rejections().keySet());
        assertTrue(inspect(ZINK_ON_LAVAPIPE, PINNED_VERSION, SHARING_EXTENSIONS, true).isAccepted());
    }

    @Test
    void anotherMesaReleaseIsNotThePin() {
        ProviderDiagnosis diagnosis = inspect(
                ZINK_ON_HARDWARE, "4.6 (Compatibility Profile) Mesa 25.1.0", SHARING_EXTENSIONS, false);

        assertEquals("pinned 25.0.7, found 25.1.0", diagnosis.rejections().get(RejectionReason.MESA_VERSION_MISMATCH));
    }

    @Test
    void theVersionAndTheProfileMustMeetTheRequirement() {
        ProviderDiagnosis diagnosis =
                inspect(ZINK_ON_HARDWARE, "3.1 (Core Profile) Mesa 25.0.7", SHARING_EXTENSIONS, false);

        assertEquals(
                EnumSet.of(RejectionReason.GL_VERSION_TOO_LOW, RejectionReason.WRONG_PROFILE),
                diagnosis.rejections().keySet());
    }

    @Test
    void everyMissingExtensionIsListed() {
        ProviderDiagnosis diagnosis = inspect(
                ZINK_ON_HARDWARE, PINNED_VERSION, Arrays.asList("GL_EXT_memory_object", "GL_EXT_semaphore"), false);

        assertFalse(diagnosis.isAccepted());
        assertEquals(
                "GL_EXT_memory_object_fd, GL_EXT_semaphore_fd",
                diagnosis.rejections().get(RejectionReason.MISSING_EXTENSIONS));
    }

    private static ProviderDiagnosis inspect(
            String renderer, String version, List<String> extensions, boolean softwareDeviceAllowed) {
        ProviderCapabilities capabilities = new ProviderCapabilities(
                ProviderIdentityParser.parse(new ProviderStrings(renderer, version)), extensions);
        CapabilityRequirements requirements = new CapabilityRequirements(
                GlVersion.of(3, 2), ContextProfile.COMPATIBILITY, SHARING_EXTENSIONS, softwareDeviceAllowed);
        return ProviderInspector.inspect(capabilities, PIN, requirements);
    }
}
