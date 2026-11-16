package io.github.luasgraphicss.minemod.compatibility.resources.framebuffer;

import java.util.Optional;

public enum FramebufferTarget {
    FRAMEBUFFER(0x8D40, true, true),
    READ_FRAMEBUFFER(0x8CA8, true, false),
    DRAW_FRAMEBUFFER(0x8CA9, false, true);

    private final int code;
    private final boolean read;
    private final boolean draw;

    FramebufferTarget(int code, boolean read, boolean draw) {
        this.code = code;
        this.read = read;
        this.draw = draw;
    }

    public static Optional<FramebufferTarget> fromCode(int code) {
        for (FramebufferTarget target : values()) {
            if (target.code == code) {
                return Optional.of(target);
            }
        }
        return Optional.empty();
    }

    public int code() {
        return code;
    }

    public boolean affectsRead() {
        return read;
    }

    public boolean affectsDraw() {
        return draw;
    }
}
