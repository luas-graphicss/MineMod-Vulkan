package io.github.luasgraphicss.minemod.client.compatibility.sharing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.bridge.SceneSharing;
import io.github.luasgraphicss.minemod.bridge.sharing.SharedAttachment;
import io.github.luasgraphicss.minemod.bridge.sharing.SharedAttachments;
import io.github.luasgraphicss.minemod.client.compatibility.support.SharedSession;
import io.github.luasgraphicss.minemod.compatibility.interop.device.DeviceIdentity;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.PlannedShare;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = SharedSession.LIBRARY, matches = ".+")
class SessionAttachmentsTest {
    private static final int WIDTH = 64;
    private static final int HEIGHT = 32;
    private static final int RGBA_BYTES = 4;

    private SharedSession renderer;
    private SceneSharing sharing;
    private SessionAttachments attachments;

    @BeforeEach
    void openASessionThatSharesItsAttachments() {
        renderer = SharedSession.open(WIDTH, HEIGHT);
        sharing = renderer.sharing();
        attachments = new SessionAttachments(sharing);
    }

    @AfterEach
    void closeTheSession() {
        renderer.close();
    }

    @Test
    void grantsAnAttachmentOfTheSessionWithTheSizeOfItsAllocation() {
        long color = attachments.create(shareOf(SharedAttachment.COLOR));

        assertTrue(attachments.allocationSize(color) >= WIDTH * HEIGHT * RGBA_BYTES);
        assertEquals(1, attachments.liveCount());
    }

    @Test
    void exportsAHandleOfItsOwnForEachAttachment() {
        long color = attachments.create(shareOf(SharedAttachment.COLOR));
        long depthStencil = attachments.create(shareOf(SharedAttachment.DEPTH_STENCIL));

        long colorHandle = attachments.exportHandle(color);
        long depthStencilHandle = attachments.exportHandle(depthStencil);

        assertTrue(colorHandle >= 0, "color handle " + colorHandle);
        assertNotEquals(colorHandle, depthStencilHandle);
        sharing.closeExported(colorHandle);
        sharing.closeExported(depthStencilHandle);
    }

    @Test
    void refusesToExportAnAttachmentTheSessionReplaced() {
        long color = attachments.create(shareOf(SharedAttachment.COLOR));
        renderer.resize(2 * WIDTH, HEIGHT);

        assertThrows(IllegalStateException.class, () -> attachments.exportHandle(color));
    }

    @Test
    void refusesAShareOfASizeTheSessionNoLongerHas() {
        PlannedShare planned = shareOf(SharedAttachment.COLOR);
        renderer.resize(2 * WIDTH, HEIGHT);

        assertThrows(IllegalArgumentException.class, () -> attachments.create(planned));
        assertEquals(0, attachments.liveCount());
    }

    @Test
    void forgetsAnAttachmentOnceItIsDestroyed() {
        long color = attachments.create(shareOf(SharedAttachment.COLOR));

        attachments.destroy(color);

        assertEquals(0, attachments.liveCount());
        assertThrows(IllegalArgumentException.class, () -> attachments.allocationSize(color));
        assertThrows(IllegalArgumentException.class, () -> attachments.destroy(color));
    }

    private PlannedShare shareOf(SharedAttachment attachment) {
        SharedAttachments shared = sharing.describe();
        DeviceIdentity sameGpu = new DeviceIdentity(shared.deviceUuid(), shared.driverUuid());
        return SceneNegotiation.negotiate(shared, sameGpu, SceneNegotiation.PLATFORM.requiredExtensions())
                .share(attachment);
    }
}
