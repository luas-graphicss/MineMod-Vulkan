package io.github.luasgraphicss.minemod.adapters.frame.answers;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class OfferedCapabilities {
    private static final Set<String> FLAGS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "OpenGL11", "OpenGL12", "OpenGL13", "OpenGL14", "OpenGL15", "OpenGL20", "GL_NV_fog_distance")));

    private OfferedCapabilities() {}

    public static Set<String> flags() {
        return FLAGS;
    }
}
