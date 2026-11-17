package io.github.luasgraphicss.minemod.verification.hooks.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ReleaseHooks(String release, String clientSha1, Windowing windowing, List<HookPoint> points) {
    public enum Windowing {
        @JsonProperty("glfw") GLFW,
        @JsonProperty("lwjgl2-display") LWJGL2_DISPLAY;

        public static Windowing ofLwjglGeneration(int generation) {
            return generation == 2 ? LWJGL2_DISPLAY : GLFW;
        }
    }

    public ReleaseHooks {
        points = List.copyOf(points);
    }

    public int callSites() {
        return points.stream().mapToInt(HookPoint::matches).sum();
    }
}
