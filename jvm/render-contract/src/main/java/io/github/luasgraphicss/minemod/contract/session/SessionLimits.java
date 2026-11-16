package io.github.luasgraphicss.minemod.contract.session;

public final class SessionLimits {
    private final int maxBuffers;
    private final long maxBufferBytes;

    public SessionLimits(int maxBuffers, long maxBufferBytes) {
        if (maxBuffers < 1) {
            throw new IllegalArgumentException("a session needs room for at least one buffer: " + maxBuffers);
        }
        if (maxBufferBytes < 1) {
            throw new IllegalArgumentException("a session needs a positive buffer byte budget: " + maxBufferBytes);
        }
        this.maxBuffers = maxBuffers;
        this.maxBufferBytes = maxBufferBytes;
    }

    public int maxBuffers() {
        return maxBuffers;
    }

    public long maxBufferBytes() {
        return maxBufferBytes;
    }

    @Override
    public String toString() {
        return "up to " + maxBuffers + " buffer(s) holding " + maxBufferBytes + " byte(s)";
    }
}
