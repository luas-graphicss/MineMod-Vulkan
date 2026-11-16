package io.github.luasgraphicss.minemod.contract.session;

public final class SessionCounters {
    private final long submissionsAccepted;
    private final long submissionsRejected;
    private final long submissionsAborted;
    private final long commandsExecuted;
    private final long buffersCreated;
    private final long buffersDestroyed;
    private final long liveBuffers;
    private final long liveBufferBytes;

    public SessionCounters(
            long submissionsAccepted,
            long submissionsRejected,
            long submissionsAborted,
            long commandsExecuted,
            long buffersCreated,
            long buffersDestroyed,
            long liveBuffers,
            long liveBufferBytes) {
        this.submissionsAccepted = submissionsAccepted;
        this.submissionsRejected = submissionsRejected;
        this.submissionsAborted = submissionsAborted;
        this.commandsExecuted = commandsExecuted;
        this.buffersCreated = buffersCreated;
        this.buffersDestroyed = buffersDestroyed;
        this.liveBuffers = liveBuffers;
        this.liveBufferBytes = liveBufferBytes;
    }

    public long submissionsAccepted() {
        return submissionsAccepted;
    }

    public long submissionsRejected() {
        return submissionsRejected;
    }

    public long submissionsAborted() {
        return submissionsAborted;
    }

    public long commandsExecuted() {
        return commandsExecuted;
    }

    public long buffersCreated() {
        return buffersCreated;
    }

    public long buffersDestroyed() {
        return buffersDestroyed;
    }

    public long liveBuffers() {
        return liveBuffers;
    }

    public long liveBufferBytes() {
        return liveBufferBytes;
    }
}
