package io.github.luasgraphicss.minemod.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.bridge.sharing.AttachmentDescription;
import io.github.luasgraphicss.minemod.bridge.sharing.HandoffSemaphores;
import io.github.luasgraphicss.minemod.bridge.sharing.HandoffSynchronization;
import io.github.luasgraphicss.minemod.bridge.sharing.SharedAttachment;
import io.github.luasgraphicss.minemod.bridge.sharing.SharedAttachments;
import io.github.luasgraphicss.minemod.codec.CommandStreamWriter;
import io.github.luasgraphicss.minemod.codec.scene.SceneCommandWriter;
import io.github.luasgraphicss.minemod.contract.abi.AbiException;
import io.github.luasgraphicss.minemod.contract.abi.AbiResult;
import java.nio.ByteBuffer;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SceneSharingTest {
    private static final int EDGE = 64;
    private static final int RGBA_BYTES = 4;
    private static final int STREAM_BYTES = 256;
    private static final int VK_FORMAT_R8G8B8A8_UNORM = 37;
    private static final int VK_FORMAT_D32_SFLOAT_S8_UINT = 130;
    private static final int VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL = 2;
    private static final int VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL = 3;
    private static final byte OPAQUE = (byte) 0xFF;

    private NativeSceneHost host;
    private NativeSceneSession session;

    @BeforeEach
    void openASessionThatSharesItsAttachments() {
        Assumptions.assumeTrue(LoadedBridge.BRIDGE.drawsScenes());
        try {
            host = LoadedBridge.BRIDGE.openSceneHost(false, Collections.<String>emptyList());
            session = host.openSession(SceneSessionSetup.offscreen(EDGE, EDGE).sharingAttachments());
        } catch (AbiException unavailable) {
            Assumptions.abort("no Vulkan device shares scene attachments here: " + unavailable.getMessage());
        }
    }

    @AfterEach
    void closeTheSession() {
        if (session != null) {
            session.close();
        }
        if (host != null) {
            host.close();
        }
    }

    @Test
    void describesTheAttachmentsAProviderImports() {
        SharedAttachments shared = session.sharing().describe();

        AttachmentDescription color = shared.attachment(SharedAttachment.COLOR);
        assertEquals(VK_FORMAT_R8G8B8A8_UNORM, color.vulkanFormat());
        assertEquals(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, color.vulkanLayout());
        assertEquals(EDGE, color.width());
        assertEquals(EDGE, color.height());
        assertTrue(color.allocationBytes() >= EDGE * EDGE * RGBA_BYTES, color.toString());
        AttachmentDescription depthStencil = shared.attachment(SharedAttachment.DEPTH_STENCIL);
        assertEquals(VK_FORMAT_D32_SFLOAT_S8_UINT, depthStencil.vulkanFormat());
        assertEquals(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL, depthStencil.vulkanLayout());
        assertEquals(EDGE, depthStencil.width());
        assertTrue(namesADevice(shared.deviceUuid()));
        assertTrue(namesADevice(shared.driverUuid()));
        assertEquals(1, shared.generation());
    }

    @Test
    void describesNewAttachmentsAfterAResize() {
        session.resize(2 * EDGE, EDGE);

        SharedAttachments shared = session.sharing().describe();

        assertEquals(2, shared.generation());
        assertEquals(2 * EDGE, shared.attachment(SharedAttachment.COLOR).width());
        assertEquals(2 * EDGE, shared.attachment(SharedAttachment.DEPTH_STENCIL).width());
    }

    @Test
    void exportsTheMemoryOfEachAttachment() {
        SceneSharing sharing = session.sharing();

        long color = sharing.exportMemory(SharedAttachment.COLOR);
        long depthStencil = sharing.exportMemory(SharedAttachment.DEPTH_STENCIL);

        assertTrue(color >= 0, "color handle " + color);
        assertTrue(depthStencil >= 0, "depth-stencil handle " + depthStencil);
        assertNotEquals(color, depthStencil);
    }

    @Test
    void exportsTheSemaphoresOnlyWhereTheDeviceSharesThem() {
        SceneSharing sharing = session.sharing();

        if (sharing.describe().sharesSemaphores()) {
            HandoffSemaphores semaphores = sharing.exportSemaphores();
            assertNotEquals(semaphores.rendererDone(), semaphores.providerDone());
            return;
        }
        AbiException refused = assertThrows(AbiException.class, sharing::exportSemaphores);
        assertEquals(AbiResult.UNSUPPORTED_CAPABILITY, refused.result());
    }

    @Test
    void drawsTheRestOfAFrameAfterTakingItBack() {
        SceneSharing sharing = session.sharing();
        ByteBuffer pixels = ByteBuffer.allocateDirect(EDGE * EDGE * RGBA_BYTES);

        assertTrue(session.submit(frameStart()).isAccepted());
        sharing.releaseToProvider(HandoffSynchronization.BLOCKING);
        sharing.reclaimForRenderer();
        assertTrue(session.submit(frameEnd()).isAccepted());
        session.readFrame(pixels);

        assertEquals(OPAQUE, pixels.get(0));
        assertEquals(0, pixels.get(1));
        assertEquals(0, pixels.get(2));
        assertEquals(1, session.counters().framesDrawn());
    }

    @Test
    void refusesToEndAFrameTheProviderHolds() {
        SceneSharing sharing = session.sharing();
        assertTrue(session.submit(frameStart()).isAccepted());
        sharing.releaseToProvider(HandoffSynchronization.BLOCKING);

        assertEquals(AbiResult.INVALID_ARGUMENT, session.submit(frameEnd()).result());
        AbiException refused = assertThrows(
                AbiException.class, () -> sharing.releaseToProvider(HandoffSynchronization.BLOCKING));

        assertEquals(AbiResult.INVALID_ARGUMENT, refused.result());
        sharing.reclaimForRenderer();
        assertTrue(session.submit(frameEnd()).isAccepted());
    }

    @Test
    void refusesAHandoffWhileNoFrameIsOpen() {
        SceneSharing sharing = session.sharing();

        AbiException released = assertThrows(
                AbiException.class, () -> sharing.releaseToProvider(HandoffSynchronization.BLOCKING));
        AbiException reclaimed = assertThrows(AbiException.class, sharing::reclaimForRenderer);

        assertEquals(AbiResult.INVALID_ARGUMENT, released.result());
        assertEquals(AbiResult.INVALID_ARGUMENT, reclaimed.result());
    }

    @Test
    void sharesNothingFromASessionOpenedForItself() {
        try (NativeSceneSession alone = host.openSession(SceneSessionSetup.offscreen(EDGE, EDGE))) {
            SceneSharing sharing = alone.sharing();

            AbiException refused = assertThrows(AbiException.class, sharing::describe);

            assertEquals(AbiResult.INVALID_ARGUMENT, refused.result());
        }
    }

    @Test
    void refusesToShareAClosedSession() {
        SceneSharing sharing = session.sharing();
        session.close();

        assertThrows(IllegalStateException.class, sharing::describe);
        assertThrows(IllegalStateException.class, sharing::reclaimForRenderer);
    }

    private static boolean namesADevice(byte[] uuid) {
        for (byte part : uuid) {
            if (part != 0) {
                return true;
            }
        }
        return false;
    }

    private static ByteBuffer frameStart() {
        CommandStreamWriter stream = new CommandStreamWriter(ByteBuffer.allocateDirect(STREAM_BYTES));
        new SceneCommandWriter(stream).beginInterfaceFrame(1.0f, 0.0f, 0.0f, 1.0f);
        return stream.finish();
    }

    private static ByteBuffer frameEnd() {
        CommandStreamWriter stream = new CommandStreamWriter(ByteBuffer.allocateDirect(STREAM_BYTES));
        new SceneCommandWriter(stream).endFrame();
        return stream.finish();
    }
}
