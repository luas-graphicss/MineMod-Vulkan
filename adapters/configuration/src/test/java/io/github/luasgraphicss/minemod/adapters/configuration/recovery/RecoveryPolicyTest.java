package io.github.luasgraphicss.minemod.adapters.configuration.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.configuration.settings.RendererSettings;
import io.github.luasgraphicss.minemod.adapters.configuration.settings.Settings;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RecoveryPolicyTest {
    @Test
    void startsSafeModeAfterLaunchesThatNeverPresentedAFrame() {
        RecoveryDecision decision = RecoveryPolicy.decide(
                Arrays.asList(LaunchPhase.CLEAN_EXIT, LaunchPhase.STARTED, LaunchPhase.STARTED), 2);

        assertTrue(decision.safeMode());
        assertEquals("the last 2 launch(es) ended before the first frame", decision.reason());
    }

    @Test
    void aLaunchThatPresentedAFrameEndsTheStreak() {
        RecoveryDecision decision = RecoveryPolicy.decide(
                Arrays.asList(LaunchPhase.STARTED, LaunchPhase.STARTED, LaunchPhase.FIRST_FRAME, LaunchPhase.STARTED), 2);

        assertFalse(decision.safeMode());
        assertEquals("1 launch(es) ended before the first frame, safe mode starts at 2", decision.reason());
    }

    @Test
    void aFirstLaunchIsNotARecovery() {
        assertFalse(RecoveryPolicy.decide(Collections.<LaunchPhase>emptyList(), 1).safeMode());
    }

    @Test
    void safeModeTurnsTheShaderPackOffWithoutRewritingTheChoice() {
        Map<String, String> stored = new HashMap<>();
        stored.put("shaders.pack", "Sunlit Valleys v4.zip");
        Settings chosen = Settings.resolve(stored, RendererSettings.all());

        Settings safe = SafeMode.apply(chosen);

        assertEquals("", safe.get(RendererSettings.SHADER_PACK));
        assertTrue(safe.get(RendererSettings.DIAGNOSTIC_OVERLAY));
        assertTrue(safe.get(RendererSettings.MOD_OPENGL));
        assertEquals("Sunlit Valleys v4.zip", chosen.get(RendererSettings.SHADER_PACK));
        assertEquals(Collections.singletonMap("shaders.pack", "Sunlit Valleys v4.zip"), stored);
    }
}
