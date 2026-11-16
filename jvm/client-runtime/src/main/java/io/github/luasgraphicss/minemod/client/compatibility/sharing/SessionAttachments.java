package io.github.luasgraphicss.minemod.client.compatibility.sharing;

import io.github.luasgraphicss.minemod.bridge.SceneSharing;
import io.github.luasgraphicss.minemod.bridge.sharing.AttachmentDescription;
import io.github.luasgraphicss.minemod.bridge.sharing.SharedAttachment;
import io.github.luasgraphicss.minemod.bridge.sharing.SharedAttachments;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.PlannedShare;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.SharingRequest;
import io.github.luasgraphicss.minemod.compatibility.layer.sharing.RendererResources;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class SessionAttachments implements RendererResources {
    private final Map<Long, Granted> granted = new HashMap<>();
    private final SceneSharing sharing;
    private long nextResource = 1;

    public SessionAttachments(SceneSharing sharing) {
        this.sharing = Objects.requireNonNull(sharing, "sharing");
    }

    @Override
    public long create(PlannedShare share) {
        SharingRequest request = share.request();
        SharedAttachment attachment = AttachmentRequests.attachmentOf(request)
                .orElseThrow(() -> new IllegalArgumentException("a scene session shares its attachments, not " + request));
        SharedAttachments shared = sharing.describe();
        AttachmentDescription description = shared.attachment(attachment);
        if (!AttachmentRequests.describes(request, description)) {
            throw new IllegalArgumentException(
                    "the session shares its " + attachment + " attachment as " + description + ", not as " + request);
        }
        long resource = nextResource++;
        granted.put(resource, new Granted(attachment, description.allocationBytes(), shared.generation()));
        return resource;
    }

    @Override
    public long allocationSize(long resource) {
        return grantedAs(resource).allocationBytes;
    }

    @Override
    public long exportHandle(long resource) {
        Granted attachment = grantedAs(resource);
        long generation = sharing.describe().generation();
        if (generation != attachment.generation) {
            throw new IllegalStateException("resource " + resource + " names the attachments of generation "
                    + attachment.generation + " and the session replaced them in generation " + generation);
        }
        return sharing.exportMemory(attachment.which);
    }

    @Override
    public void destroy(long resource) {
        grantedAs(resource);
        granted.remove(resource);
    }

    public int liveCount() {
        return granted.size();
    }

    private Granted grantedAs(long resource) {
        Granted attachment = granted.get(resource);
        if (attachment == null) {
            throw new IllegalArgumentException("the session granted no resource " + resource);
        }
        return attachment;
    }

    private static final class Granted {
        private final SharedAttachment which;
        private final long allocationBytes;
        private final long generation;

        private Granted(SharedAttachment which, long allocationBytes, long generation) {
            this.which = which;
            this.allocationBytes = allocationBytes;
            this.generation = generation;
        }
    }
}
