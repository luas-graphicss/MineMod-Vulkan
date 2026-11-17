package io.github.luasgraphicss.minemod.verification.ecosystem.session;

public final class SessionClock {
    private static final int NOTHING_PRESENTED = -1;

    private int world;
    private int frame;
    private int presentedFrame = NOTHING_PRESENTED;
    private long resourceGeneration;

    void enterWorld() {
        world++;
    }

    void beginFrame() {
        frame++;
    }

    void present() {
        presentedFrame = frame;
    }

    void publish(long generation) {
        resourceGeneration = generation;
    }

    public int world() {
        return world;
    }

    public int frame() {
        return frame;
    }

    public int presentedFrame() {
        return presentedFrame;
    }

    public long resourceGeneration() {
        return resourceGeneration;
    }
}
