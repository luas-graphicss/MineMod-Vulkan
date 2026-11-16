package io.github.luasgraphicss.minemod.bridge;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.bridge.sharing.AttachmentDescription;
import io.github.luasgraphicss.minemod.bridge.sharing.SharedAttachment;
import io.github.luasgraphicss.minemod.bridge.sharing.SharedAttachments;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SharingFieldsTest {
    private static final long[] DESCRIBED = {4096, 37, 2, 64, 32, 8192, 130, 3, 64, 32, 1, 7};

    @Test
    void readsTheFieldsInTheOrderOfTheNativeDescription() {
        SharedAttachments shared = SharingFields.decode(countedIdentity(), DESCRIBED);

        AttachmentDescription color = shared.attachment(SharedAttachment.COLOR);
        assertEquals(4096, color.allocationBytes());
        assertEquals(37, color.vulkanFormat());
        assertEquals(2, color.vulkanLayout());
        assertEquals(64, color.width());
        assertEquals(32, color.height());
        AttachmentDescription depthStencil = shared.attachment(SharedAttachment.DEPTH_STENCIL);
        assertEquals(8192, depthStencil.allocationBytes());
        assertEquals(130, depthStencil.vulkanFormat());
        assertEquals(3, depthStencil.vulkanLayout());
        assertTrue(shared.sharesSemaphores());
        assertEquals(7, shared.generation());
    }

    @Test
    void splitsTheIdentityIntoTheDeviceAndTheDriver() {
        byte[] identity = countedIdentity();

        SharedAttachments shared = SharingFields.decode(identity, DESCRIBED);

        assertArrayEquals(Arrays.copyOfRange(identity, 0, 16), shared.deviceUuid());
        assertArrayEquals(Arrays.copyOfRange(identity, 16, 32), shared.driverUuid());
    }

    @Test
    void reportsNoSemaphoresWhenTheFlagIsClear() {
        long[] withoutSemaphores = DESCRIBED.clone();
        withoutSemaphores[10] = 0;

        assertFalse(SharingFields.decode(countedIdentity(), withoutSemaphores).sharesSemaphores());
    }

    @Test
    void refusesAnotherNumberOfFieldsOrIdentityBytes() {
        assertThrows(IllegalArgumentException.class, () -> SharingFields.decode(countedIdentity(), new long[11]));
        assertThrows(IllegalArgumentException.class, () -> SharingFields.decode(new byte[31], DESCRIBED));
    }

    @Test
    void keepsTheDeviceWhenAReturnedUuidChanges() {
        SharedAttachments shared = SharingFields.decode(countedIdentity(), DESCRIBED);

        shared.deviceUuid()[0] = 99;

        assertEquals(0, shared.deviceUuid()[0]);
    }

    private static byte[] countedIdentity() {
        byte[] identity = new byte[SharingFields.IDENTITY_BYTES];
        for (int index = 0; index < identity.length; index++) {
            identity[index] = (byte) index;
        }
        return identity;
    }
}
