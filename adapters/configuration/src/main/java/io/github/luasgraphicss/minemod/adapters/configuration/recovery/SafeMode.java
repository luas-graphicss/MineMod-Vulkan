package io.github.luasgraphicss.minemod.adapters.configuration.recovery;

import io.github.luasgraphicss.minemod.adapters.configuration.settings.RendererSettings;
import io.github.luasgraphicss.minemod.adapters.configuration.settings.Settings;

public final class SafeMode {
    private SafeMode() {}

    public static Settings apply(Settings chosen) {
        return chosen.with(RendererSettings.SHADER_PACK, "").with(RendererSettings.DIAGNOSTIC_OVERLAY, Boolean.TRUE);
    }
}
