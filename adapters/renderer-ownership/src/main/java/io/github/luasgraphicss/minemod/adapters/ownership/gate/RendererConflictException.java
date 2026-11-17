package io.github.luasgraphicss.minemod.adapters.ownership.gate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RendererConflictException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private static final String ADVICE =
            "Remove a renderer named above or start without MineMod Vulkan; neither is switched off for you.";

    private final ArrayList<String> diagnosis;

    RendererConflictException(List<String> diagnosis) {
        super("the world stays closed while renderers dispute the pipeline: " + String.join("; ", diagnosis) + ". "
                + ADVICE);
        this.diagnosis = new ArrayList<>(diagnosis);
    }

    public List<String> diagnosis() {
        return Collections.unmodifiableList(diagnosis);
    }
}
