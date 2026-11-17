package io.github.luasgraphicss.minemod.client.compatibility.sharing;

import io.github.luasgraphicss.minemod.bridge.sharing.AttachmentDescription;
import io.github.luasgraphicss.minemod.bridge.sharing.SharedAttachment;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.ShareableResource;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.SharingRequest;
import java.util.Optional;

final class AttachmentRequests {
    static final int SINGLE_LEVEL = 1;
    static final int SINGLE_SAMPLE = 1;

    private AttachmentRequests() {}

    static SharingRequest requestFor(SharedAttachment attachment, AttachmentDescription description) {
        ShareableResource resource = attachment == SharedAttachment.COLOR
                ? ShareableResource.COLOR_ATTACHMENT
                : ShareableResource.DEPTH_STENCIL_ATTACHMENT;
        return SharingRequest.image(
                resource,
                description.vulkanFormat(),
                description.width(),
                description.height(),
                SINGLE_LEVEL,
                SINGLE_SAMPLE);
    }

    static Optional<SharedAttachment> attachmentOf(SharingRequest request) {
        switch (request.resource()) {
            case COLOR_ATTACHMENT:
                return Optional.of(SharedAttachment.COLOR);
            case DEPTH_STENCIL_ATTACHMENT:
                return Optional.of(SharedAttachment.DEPTH_STENCIL);
            default:
                return Optional.empty();
        }
    }

    static boolean describes(SharingRequest request, AttachmentDescription description) {
        return request.format() == description.vulkanFormat()
                && request.width() == description.width()
                && request.height() == description.height()
                && request.mipLevels() == SINGLE_LEVEL
                && request.samples() == SINGLE_SAMPLE;
    }
}
