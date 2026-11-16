package io.github.luasgraphicss.minemod.compatibility.interop.negotiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.interop.device.DeviceIdentity;
import io.github.luasgraphicss.minemod.compatibility.interop.platform.HandlePlatform;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SharingNegotiatorTest {
    private static final int RGBA8 = 37;
    private static final int D24S8 = 129;
    private static final int ONE_SAMPLE = 1;
    private static final int FOUR_SAMPLES = 4;
    private static final SharingRequest COLOR =
            SharingRequest.image(ShareableResource.COLOR_ATTACHMENT, RGBA8, 1920, 1080, 1, ONE_SAMPLE);
    private static final SharingRequest DEPTH =
            SharingRequest.image(ShareableResource.DEPTH_STENCIL_ATTACHMENT, D24S8, 1920, 1080, 1, ONE_SAMPLE);
    private static final SharingRequest STAGING = SharingRequest.buffer(4096);

    private final DeviceIdentity renderer = identity(1);
    private final Map<ShareableResource, ExternalSupport> support = new EnumMap<>(ShareableResource.class);
    private final List<String> extensions = new ArrayList<>(HandlePlatform.LINUX_FILE_DESCRIPTOR.requiredExtensions());
    private boolean semaphoresShareable = true;

    SharingNegotiatorTest() {
        support.put(ShareableResource.COLOR_ATTACHMENT, ExternalSupport.shareable(false, 16384, 16384, 15, 1));
        support.put(ShareableResource.DEPTH_STENCIL_ATTACHMENT, ExternalSupport.shareable(true, 16384, 16384, 15, 1));
        support.put(ShareableResource.BUFFER, ExternalSupport.shareable(false, 1L << 30, 1, 1, 1));
    }

    @Test
    void everyRequestedResourceIsPlannedWithTheAllocationItsDriverNeeds() {
        SharingOutcome outcome = negotiate(identity(1), Arrays.asList(COLOR, DEPTH, STAGING));

        assertTrue(outcome.isAccepted());
        assertEquals(3, outcome.shares().size());
        assertFalse(outcome.shares().get(0).usesDedicatedAllocation());
        assertTrue(outcome.shares().get(1).usesDedicatedAllocation());
    }

    @Test
    void aDifferentGpuIsRejectedBeforeAnythingIsShared() {
        SharingOutcome outcome = negotiate(identity(2), Collections.singletonList(COLOR));

        assertEquals(
                Collections.singletonList("the provider runs on a different GPU or driver than the renderer"),
                outcome.rejections());
        assertEquals(Collections.emptyList(), outcome.shares());
    }

    @Test
    void colourSupportSaysNothingAboutDepthStencil() {
        support.put(ShareableResource.DEPTH_STENCIL_ATTACHMENT, ExternalSupport.unsupported());

        SharingOutcome outcome = negotiate(identity(1), Arrays.asList(COLOR, DEPTH));

        assertEquals(Collections.singletonList(DEPTH + " cannot be exported and imported"), outcome.rejections());
        assertEquals(Collections.emptyList(), outcome.shares());
    }

    @Test
    void sampleCountsAndSizesAreCheckedPerResource() {
        SharingRequest multisampled =
                SharingRequest.image(ShareableResource.COLOR_ATTACHMENT, RGBA8, 1920, 1080, 1, FOUR_SAMPLES);
        SharingRequest oversized =
                SharingRequest.image(ShareableResource.COLOR_ATTACHMENT, RGBA8, 32768, 1080, 1, ONE_SAMPLE);

        SharingOutcome outcome = negotiate(identity(1), Arrays.asList(multisampled, oversized));

        assertEquals(
                Arrays.asList(
                        multisampled + " cannot be shared with that sample count",
                        oversized + " exceeds the shareable size"),
                outcome.rejections());
    }

    @Test
    void theExtensionsOfThePlatformMustAllBePresent() {
        extensions.remove("GL_EXT_semaphore_fd");
        semaphoresShareable = false;

        SharingOutcome outcome = negotiate(identity(1), Collections.singletonList(COLOR));

        assertEquals(
                Arrays.asList(
                        "the provider lacks GL_EXT_semaphore_fd", "binary semaphores cannot be shared on this device"),
                outcome.rejections());
    }

    @Test
    void windowsAsksForItsOwnHandleExtensions() {
        SharingOutcome outcome = SharingNegotiator.negotiate(
                renderer,
                identity(1),
                extensions,
                HandlePlatform.WINDOWS_NT_HANDLE,
                Collections.singletonList(COLOR),
                query());

        assertEquals(
                Arrays.asList(
                        "the provider lacks GL_EXT_memory_object_win32", "the provider lacks GL_EXT_semaphore_win32"),
                outcome.rejections());
    }

    private SharingOutcome negotiate(DeviceIdentity provider, List<SharingRequest> requests) {
        return SharingNegotiator.negotiate(
                renderer, provider, extensions, HandlePlatform.LINUX_FILE_DESCRIPTOR, requests, query());
    }

    private ExternalSupportQuery query() {
        return new ExternalSupportQuery() {
            @Override
            public ExternalSupport supportFor(SharingRequest request) {
                return support.get(request.resource());
            }

            @Override
            public boolean binarySemaphoresShareable() {
                return semaphoresShareable;
            }
        };
    }

    private static DeviceIdentity identity(int seed) {
        byte[] deviceUuid = new byte[DeviceIdentity.UUID_BYTES];
        byte[] driverUuid = new byte[DeviceIdentity.UUID_BYTES];
        Arrays.fill(deviceUuid, (byte) seed);
        Arrays.fill(driverUuid, (byte) (seed + 100));
        return new DeviceIdentity(deviceUuid, driverUuid);
    }
}
