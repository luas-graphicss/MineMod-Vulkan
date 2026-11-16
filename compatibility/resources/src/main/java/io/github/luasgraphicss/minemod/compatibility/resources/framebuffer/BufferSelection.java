package io.github.luasgraphicss.minemod.compatibility.resources.framebuffer;

public final class BufferSelection {
    private static final int GL_NONE = 0;
    private static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;

    private static final BufferSelection SCENE_COLOR = new BufferSelection(Outcome.SCENE_COLOR, GL_COLOR_ATTACHMENT0);
    private static final BufferSelection NO_BUFFER = new BufferSelection(Outcome.NO_BUFFER, GL_NONE);
    private static final BufferSelection FRONT_BUFFER_UNAVAILABLE =
            new BufferSelection(Outcome.FRONT_BUFFER_UNAVAILABLE, GL_NONE);

    private final Outcome outcome;
    private final int code;

    private BufferSelection(Outcome outcome, int code) {
        this.outcome = outcome;
        this.code = code;
    }

    static BufferSelection sceneColor() {
        return SCENE_COLOR;
    }

    static BufferSelection noBuffer() {
        return NO_BUFFER;
    }

    static BufferSelection frontBufferUnavailable() {
        return FRONT_BUFFER_UNAVAILABLE;
    }

    static BufferSelection rejected(int error) {
        return new BufferSelection(Outcome.REJECTED, error);
    }

    public Outcome outcome() {
        return outcome;
    }

    public boolean isApplied() {
        return outcome == Outcome.SCENE_COLOR || outcome == Outcome.NO_BUFFER;
    }

    public int providerBuffer() {
        if (!isApplied()) {
            throw new IllegalStateException("a " + outcome + " selection has no provider buffer");
        }
        return code;
    }

    public int error() {
        if (outcome != Outcome.REJECTED) {
            throw new IllegalStateException("a " + outcome + " selection raised no error");
        }
        return code;
    }

    public enum Outcome {
        SCENE_COLOR,
        NO_BUFFER,
        FRONT_BUFFER_UNAVAILABLE,
        REJECTED
    }
}
