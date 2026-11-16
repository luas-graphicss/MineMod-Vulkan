package io.github.luasgraphicss.minemod.bridge;

import io.github.luasgraphicss.minemod.bridge.sharing.AttachmentDescription;
import io.github.luasgraphicss.minemod.bridge.sharing.SharedAttachment;
import io.github.luasgraphicss.minemod.bridge.sharing.SharedAttachments;
import java.util.Arrays;

final class SharingFields {
    static final int IDENTITY_BYTES = 2 * SharedAttachments.UUID_BYTES;
    static final int COUNT = 12;

    private static final int FIELDS_PER_ATTACHMENT = 5;
    private static final int ALLOCATION_BYTES = 0;
    private static final int VULKAN_FORMAT = 1;
    private static final int VULKAN_LAYOUT = 2;
    private static final int WIDTH = 3;
    private static final int HEIGHT = 4;
    private static final int FLAGS = 10;
    private static final int GENERATION = 11;
    private static final long SHARES_SEMAPHORES = 1;

    private SharingFields() {}

    static SharedAttachments decode(byte[] identity, long[] fields) {
        if (identity.length != IDENTITY_BYTES || fields.length != COUNT) {
            throw new IllegalArgumentException(
                    "shared attachments have " + IDENTITY_BYTES + " identity bytes and " + COUNT + " fields, found "
                            + identity.length + " and " + fields.length);
        }
        return new SharedAttachments(
                Arrays.copyOfRange(identity, 0, SharedAttachments.UUID_BYTES),
                Arrays.copyOfRange(identity, SharedAttachments.UUID_BYTES, IDENTITY_BYTES),
                attachment(fields, SharedAttachment.COLOR),
                attachment(fields, SharedAttachment.DEPTH_STENCIL),
                (fields[FLAGS] & SHARES_SEMAPHORES) != 0,
                fields[GENERATION]);
    }

    private static AttachmentDescription attachment(long[] fields, SharedAttachment which) {
        int first = which.code() * FIELDS_PER_ATTACHMENT;
        return new AttachmentDescription(
                fields[first + ALLOCATION_BYTES],
                (int) fields[first + VULKAN_FORMAT],
                (int) fields[first + VULKAN_LAYOUT],
                (int) fields[first + WIDTH],
                (int) fields[first + HEIGHT]);
    }
}
