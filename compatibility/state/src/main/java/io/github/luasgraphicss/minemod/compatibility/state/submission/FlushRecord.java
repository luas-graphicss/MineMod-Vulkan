package io.github.luasgraphicss.minemod.compatibility.state.submission;

import java.util.Objects;

public final class FlushRecord {
    private final long frame;
    private final int checkpoint;
    private final FlushReason reason;

    public FlushRecord(long frame, int checkpoint, FlushReason reason) {
        this.frame = frame;
        this.checkpoint = checkpoint;
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public long frame() {
        return frame;
    }

    public int checkpoint() {
        return checkpoint;
    }

    public FlushReason reason() {
        return reason;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof FlushRecord)) {
            return false;
        }
        FlushRecord record = (FlushRecord) other;
        return frame == record.frame && checkpoint == record.checkpoint && reason == record.reason;
    }

    @Override
    public int hashCode() {
        return Objects.hash(frame, checkpoint, reason);
    }

    @Override
    public String toString() {
        return "frame " + frame + " checkpoint " + checkpoint + ": " + reason;
    }
}
