package io.github.luasgraphicss.minemod.compatibility.resources.framebuffer;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.resources.names.NameTable;
import java.util.EnumSet;
import java.util.Set;

public final class FramebufferBindings {
    public static final int DEFAULT_FRAMEBUFFER = 0;

    private final NameTable framebuffers;
    private final int sceneBacking;
    private int drawName = DEFAULT_FRAMEBUFFER;
    private int readName = DEFAULT_FRAMEBUFFER;

    public FramebufferBindings(NameTable framebuffers, int sceneBacking) {
        if (framebuffers.kind() != ObjectKind.FRAMEBUFFER) {
            throw new IllegalArgumentException("framebuffer bindings cannot resolve " + framebuffers.kind() + " names");
        }
        this.framebuffers = framebuffers;
        this.sceneBacking = sceneBacking;
    }

    public int bind(FramebufferTarget target, int name) {
        int backing = backingOf(name);
        if (target.affectsDraw()) {
            drawName = name;
        }
        if (target.affectsRead()) {
            readName = name;
        }
        return backing;
    }

    public Set<FramebufferTarget> revertBindingsOf(int name) {
        Set<FramebufferTarget> reverted = EnumSet.noneOf(FramebufferTarget.class);
        if (name == DEFAULT_FRAMEBUFFER) {
            return reverted;
        }
        if (drawName == name) {
            drawName = DEFAULT_FRAMEBUFFER;
            reverted.add(FramebufferTarget.DRAW_FRAMEBUFFER);
        }
        if (readName == name) {
            readName = DEFAULT_FRAMEBUFFER;
            reverted.add(FramebufferTarget.READ_FRAMEBUFFER);
        }
        return reverted;
    }

    public int backingOf(int name) {
        return name == DEFAULT_FRAMEBUFFER ? sceneBacking : framebuffers.backingOf(name);
    }

    public int drawBinding() {
        return drawName;
    }

    public int readBinding() {
        return readName;
    }

    public boolean drawsToDefault() {
        return drawName == DEFAULT_FRAMEBUFFER;
    }

    public boolean readsFromDefault() {
        return readName == DEFAULT_FRAMEBUFFER;
    }
}
