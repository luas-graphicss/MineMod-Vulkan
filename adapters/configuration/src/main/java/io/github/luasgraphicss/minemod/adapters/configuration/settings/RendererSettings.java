package io.github.luasgraphicss.minemod.adapters.configuration.settings;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class RendererSettings {
    public static final Setting<Boolean> MOD_OPENGL = Setting.flag("compatibility.mod-opengl", true);
    public static final Setting<String> SHADER_PACK = Setting.fileName("shaders.pack");
    public static final Setting<Boolean> DIAGNOSTIC_OVERLAY = Setting.flag("diagnostics.overlay", false);
    public static final Setting<Integer> FAILED_LAUNCHES_BEFORE_SAFE_MODE =
            Setting.ranged("recovery.failed-launches-before-safe-mode", 2, 1, 5);

    private static final List<Setting<?>> ALL = Collections.unmodifiableList(Arrays.<Setting<?>>asList(
            MOD_OPENGL, SHADER_PACK, DIAGNOSTIC_OVERLAY, FAILED_LAUNCHES_BEFORE_SAFE_MODE));

    private RendererSettings() {}

    public static List<Setting<?>> all() {
        return ALL;
    }
}
