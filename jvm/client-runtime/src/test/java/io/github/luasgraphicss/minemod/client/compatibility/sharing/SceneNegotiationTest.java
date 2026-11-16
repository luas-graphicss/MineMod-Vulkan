package io.github.luasgraphicss.minemod.client.compatibility.sharing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.bridge.sharing.AttachmentDescription;
import io.github.luasgraphicss.minemod.bridge.sharing.HandoffSynchronization;
import io.github.luasgraphicss.minemod.bridge.sharing.SharedAttachment;
import io.github.luasgraphicss.minemod.bridge.sharing.SharedAttachments;
import io.github.luasgraphicss.minemod.compatibility.interop.device.DeviceIdentity;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.ShareableResource;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.SharingRequest;
import io.github.luasgraphicss.minemod.compatibility.interop.platform.HandlePlatform;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class SceneNegotiationTest {
    private static final int VK_FORMAT_R8G8B8A8_UNORM = 37;
    private static final int VK_FORMAT_D32_SFLOAT_S8_UINT = 130;
    private static final int VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL = 2;
    private static final int VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL = 3;
    private static final int WIDTH = 854;
    private static final int HEIGHT = 480;
    private static final long COLOR_BYTES = 4L * WIDTH * HEIGHT;
    private static final long DEPTH_STENCIL_BYTES = 8L * WIDTH * HEIGHT;
    private static final byte RENDERER_GPU = 1;
    private static final byte ANOTHER_GPU = 2;

    private final List<String> providerExtensions =
            new ArrayList<>(HandlePlatform.LINUX_FILE_DESCRIPTOR.requiredExtensions());

    @Test
    void semaphoresSynchronizeTheHandoffWhereBothSidesShareThem() {
        SharingPlan plan = SceneNegotiation.negotiate(sessionSharing(true), gpu(RENDERER_GPU), providerExtensions);

        assertEquals(HandoffSynchronization.SEMAPHORES, plan.synchronization());
    }

    @Test
    void theHandoffBlocksWhereTheRendererSharesNoSemaphore() {
        SharingPlan plan = SceneNegotiation.negotiate(sessionSharing(false), gpu(RENDERER_GPU), providerExtensions);

        assertEquals(HandoffSynchronization.BLOCKING, plan.synchronization());
    }

    @Test
    void theHandoffBlocksWhereTheProviderLacksASemaphoreExtension() {
        providerExtensions.remove("GL_EXT_semaphore_fd");

        SharingPlan plan = SceneNegotiation.negotiate(sessionSharing(true), gpu(RENDERER_GPU), providerExtensions);

        assertEquals(HandoffSynchronization.BLOCKING, plan.synchronization());
    }

    @Test
    void eachAttachmentIsPlannedWithItsOwnFormatAndTheSizeOfTheSession() {
        SharingPlan plan = SceneNegotiation.negotiate(sessionSharing(true), gpu(RENDERER_GPU), providerExtensions);

        SharingRequest color = plan.share(SharedAttachment.COLOR).request();
        SharingRequest depthStencil = plan.share(SharedAttachment.DEPTH_STENCIL).request();
        assertEquals(ShareableResource.COLOR_ATTACHMENT, color.resource());
        assertEquals(VK_FORMAT_R8G8B8A8_UNORM, color.format());
        assertEquals(ShareableResource.DEPTH_STENCIL_ATTACHMENT, depthStencil.resource());
        assertEquals(VK_FORMAT_D32_SFLOAT_S8_UINT, depthStencil.format());
        assertEquals(WIDTH, depthStencil.width());
        assertEquals(HEIGHT, depthStencil.height());
        assertTrue(plan.share(SharedAttachment.COLOR).usesDedicatedAllocation());
    }

    @Test
    void aProviderOnAnotherGpuIsRefused() {
        SharingRefused refused = assertThrows(
                SharingRefused.class,
                () -> SceneNegotiation.negotiate(sessionSharing(true), gpu(ANOTHER_GPU), providerExtensions));

        assertEquals(
                "the renderer and the OpenGL provider cannot share the scene: "
                        + "the provider runs on a different GPU or driver than the renderer",
                refused.getMessage());
    }

    @Test
    void aProviderThatImportsNoMemoryIsRefusedWithEveryExtensionItLacks() {
        providerExtensions.removeAll(Arrays.asList("GL_EXT_memory_object", "GL_EXT_memory_object_fd"));

        SharingRefused refused = assertThrows(
                SharingRefused.class,
                () -> SceneNegotiation.negotiate(sessionSharing(false), gpu(RENDERER_GPU), providerExtensions));

        assertEquals(
                "the renderer and the OpenGL provider cannot share the scene: "
                        + "the provider lacks GL_EXT_memory_object; the provider lacks GL_EXT_memory_object_fd",
                refused.getMessage());
    }

    private static SharedAttachments sessionSharing(boolean sharesSemaphores) {
        return new SharedAttachments(
                uuidOf(RENDERER_GPU),
                uuidOf((byte) (RENDERER_GPU + 100)),
                new AttachmentDescription(
                        COLOR_BYTES, VK_FORMAT_R8G8B8A8_UNORM, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, WIDTH, HEIGHT),
                new AttachmentDescription(
                        DEPTH_STENCIL_BYTES,
                        VK_FORMAT_D32_SFLOAT_S8_UINT,
                        VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
                        WIDTH,
                        HEIGHT),
                sharesSemaphores,
                1);
    }

    private static DeviceIdentity gpu(byte seed) {
        return new DeviceIdentity(uuidOf(seed), uuidOf((byte) (seed + 100)));
    }

    private static byte[] uuidOf(byte seed) {
        byte[] uuid = new byte[DeviceIdentity.UUID_BYTES];
        Arrays.fill(uuid, seed);
        return uuid;
    }
}
