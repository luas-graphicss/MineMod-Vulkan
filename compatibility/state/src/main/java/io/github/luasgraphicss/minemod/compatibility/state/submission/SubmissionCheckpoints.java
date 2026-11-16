package io.github.luasgraphicss.minemod.compatibility.state.submission;

import java.util.Objects;

public final class SubmissionCheckpoints {
    private static final long NO_FRAME = -1;

    private final AttachmentHandoff handoff;
    private final FlushLog log;
    private AttachmentOwner owner = AttachmentOwner.RENDERER;
    private long frame = NO_FRAME;
    private int checkpoint;
    private boolean frameOpen;

    public SubmissionCheckpoints(AttachmentHandoff handoff, FlushLog log) {
        this.handoff = Objects.requireNonNull(handoff, "handoff");
        this.log = Objects.requireNonNull(log, "log");
    }

    public void beginFrame(long frameIndex) {
        if (frameOpen) {
            throw new IllegalStateException("frame " + frame + " was not presented");
        }
        if (frameIndex <= frame) {
            throw new IllegalArgumentException("frame " + frameIndex + " does not follow frame " + frame);
        }
        frame = frameIndex;
        checkpoint = 0;
        frameOpen = true;
    }

    public void beforeNativePass() {
        requireOpenFrame();
        if (owner == AttachmentOwner.PROVIDER) {
            reclaim(FlushReason.NATIVE_PASS_NEEDS_ATTACHMENTS);
        }
    }

    public void beforeProviderDraw() {
        requireOpenFrame();
        if (owner == AttachmentOwner.RENDERER) {
            handoff.releaseToProvider();
            owner = AttachmentOwner.PROVIDER;
            log.record(frame, checkpoint++, FlushReason.CALLBACK_NEEDS_ATTACHMENTS);
        }
    }

    public void beforeSynchronousRead() {
        beforeProviderDraw();
        handoff.finishProviderWork();
        log.record(frame, checkpoint++, FlushReason.SYNCHRONOUS_READ);
    }

    public void present() {
        requireOpenFrame();
        if (owner == AttachmentOwner.PROVIDER) {
            reclaim(FlushReason.PRESENTATION);
        }
        frameOpen = false;
    }

    public AttachmentOwner owner() {
        return owner;
    }

    private void reclaim(FlushReason reason) {
        handoff.reclaimForRenderer();
        owner = AttachmentOwner.RENDERER;
        log.record(frame, checkpoint++, reason);
    }

    private void requireOpenFrame() {
        if (!frameOpen) {
            throw new IllegalStateException("no frame is open");
        }
    }
}
