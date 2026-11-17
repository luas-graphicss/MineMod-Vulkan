package io.github.luasgraphicss.minemod.client.compatibility.sharing;

import io.github.luasgraphicss.minemod.bridge.sharing.AttachmentDescription;
import io.github.luasgraphicss.minemod.bridge.sharing.SharedAttachment;
import io.github.luasgraphicss.minemod.bridge.sharing.SharedAttachments;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.ExternalSupport;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.ExternalSupportQuery;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.SharingRequest;
import java.util.Objects;
import java.util.Optional;

public final class SessionSupport implements ExternalSupportQuery {
    private static final boolean USES_DEDICATED_ALLOCATIONS = true;

    private final SharedAttachments shared;

    public SessionSupport(SharedAttachments shared) {
        this.shared = Objects.requireNonNull(shared, "shared");
    }

    @Override
    public ExternalSupport supportFor(SharingRequest request) {
        Optional<SharedAttachment> attachment = AttachmentRequests.attachmentOf(request);
        if (!attachment.isPresent()) {
            return ExternalSupport.unsupported();
        }
        AttachmentDescription description = shared.attachment(attachment.get());
        if (description.vulkanFormat() != request.format()) {
            return ExternalSupport.unsupported();
        }
        return ExternalSupport.shareable(
                USES_DEDICATED_ALLOCATIONS,
                description.width(),
                description.height(),
                AttachmentRequests.SINGLE_LEVEL,
                AttachmentRequests.SINGLE_SAMPLE);
    }

    @Override
    public boolean binarySemaphoresShareable() {
        return shared.sharesSemaphores();
    }
}
