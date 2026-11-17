package io.github.luasgraphicss.minemod.verification.ecosystem.session;

import io.github.luasgraphicss.minemod.adapters.configuration.recovery.LaunchPhase;
import io.github.luasgraphicss.minemod.adapters.configuration.recovery.RecoveryDecision;
import io.github.luasgraphicss.minemod.adapters.configuration.recovery.RecoveryPolicy;
import io.github.luasgraphicss.minemod.adapters.configuration.recovery.SafeMode;
import io.github.luasgraphicss.minemod.adapters.configuration.settings.RendererSettings;
import io.github.luasgraphicss.minemod.adapters.configuration.settings.Settings;
import java.util.List;
import java.util.Map;

public record Launch(Settings settings, String recovery) {
    public static Launch first(String shaderPack) {
        return after(Map.of(RendererSettings.SHADER_PACK.key(), shaderPack), List.of());
    }

    public static Launch after(Map<String, String> storedSettings, List<LaunchPhase> previousLaunches) {
        Settings chosen = Settings.resolve(storedSettings, RendererSettings.all());
        RecoveryDecision decision = RecoveryPolicy.decide(
                previousLaunches, chosen.get(RendererSettings.FAILED_LAUNCHES_BEFORE_SAFE_MODE));
        Settings effective = decision.safeMode() ? SafeMode.apply(chosen) : chosen;
        return new Launch(effective, (decision.safeMode() ? "safe mode, " : "normal, ") + decision.reason());
    }

    public boolean runsAShaderPack() {
        return !settings.get(RendererSettings.SHADER_PACK).isEmpty();
    }
}
