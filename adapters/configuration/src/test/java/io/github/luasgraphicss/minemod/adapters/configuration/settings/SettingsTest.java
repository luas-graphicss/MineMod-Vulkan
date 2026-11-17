package io.github.luasgraphicss.minemod.adapters.configuration.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SettingsTest {
    @Test
    void anEmptyFileYieldsTheDefaults() {
        Settings settings = Settings.resolve(Collections.<String, String>emptyMap(), RendererSettings.all());

        assertTrue(settings.get(RendererSettings.MOD_OPENGL));
        assertEquals("", settings.get(RendererSettings.SHADER_PACK));
        assertFalse(settings.get(RendererSettings.DIAGNOSTIC_OVERLAY));
        assertEquals(2, settings.get(RendererSettings.FAILED_LAUNCHES_BEFORE_SAFE_MODE));
        assertTrue(settings.rejected().isEmpty());
    }

    @Test
    void readsTheStoredValuesOfTheSchema() {
        Map<String, String> stored = new HashMap<>();
        stored.put("compatibility.mod-opengl", " false ");
        stored.put("shaders.pack", "Sunlit Valleys v4.zip");
        stored.put("recovery.failed-launches-before-safe-mode", "5");

        Settings settings = Settings.resolve(stored, RendererSettings.all());

        assertFalse(settings.get(RendererSettings.MOD_OPENGL));
        assertEquals("Sunlit Valleys v4.zip", settings.get(RendererSettings.SHADER_PACK));
        assertEquals(5, settings.get(RendererSettings.FAILED_LAUNCHES_BEFORE_SAFE_MODE));
        assertEquals(
                Arrays.asList(
                        "compatibility.mod-opengl=false",
                        "shaders.pack=Sunlit Valleys v4.zip",
                        "diagnostics.overlay=false",
                        "recovery.failed-launches-before-safe-mode=5"),
                settings.describe(RendererSettings.all()));
    }

    @Test
    void aValueOutsideTheSchemaFallsBackAndIsReported() {
        Map<String, String> stored = new HashMap<>();
        stored.put("compatibility.mod-opengl", "maybe");
        stored.put("recovery.failed-launches-before-safe-mode", "9");
        stored.put("shaders.pack", "../saves/New World/level.dat");

        Settings settings = Settings.resolve(stored, RendererSettings.all());

        assertTrue(settings.get(RendererSettings.MOD_OPENGL));
        assertEquals(2, settings.get(RendererSettings.FAILED_LAUNCHES_BEFORE_SAFE_MODE));
        assertEquals("", settings.get(RendererSettings.SHADER_PACK));
        assertEquals(
                Arrays.asList(
                        "compatibility.mod-opengl is \"maybe\", expected true or false; using true",
                        "shaders.pack is \"../saves/New World/level.dat\", expected a file name without directories; using ",
                        "recovery.failed-launches-before-safe-mode is \"9\", expected 1 to 5; using 2"),
                settings.rejected());
    }

    @Test
    void keepsTheKeysOfOtherVersionsWhenASettingChanges() {
        Settings settings = Settings.resolve(
                        Collections.singletonMap("presentation.future-option", "7"), RendererSettings.all())
                .with(RendererSettings.DIAGNOSTIC_OVERLAY, Boolean.TRUE);

        assertTrue(settings.get(RendererSettings.DIAGNOSTIC_OVERLAY));
        assertEquals("7", settings.stored().get("presentation.future-option"));
        assertEquals("true", settings.stored().get("diagnostics.overlay"));
        assertTrue(settings.rejected().isEmpty());
    }

    @Test
    void aShaderPackIsNamedByAFileNameAlone() {
        assertTrue(RendererSettings.SHADER_PACK.parse("pack.zip").isPresent());
        assertFalse(RendererSettings.SHADER_PACK.parse("packs/pack.zip").isPresent());
        assertFalse(RendererSettings.SHADER_PACK.parse("..").isPresent());
        assertFalse(RendererSettings.SHADER_PACK.parse("C:\\packs\\pack.zip").isPresent());
    }
}
