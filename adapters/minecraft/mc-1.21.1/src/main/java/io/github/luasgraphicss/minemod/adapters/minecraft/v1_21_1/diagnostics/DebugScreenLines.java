package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.diagnostics;

import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.compatibility.ModRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.frame.GameFrame;
import java.util.List;

public final class DebugScreenLines {
    private static final String SECTION_BREAK = "";
    private static final String LEFT_OUT_DRAWS = "draws left out of the scene: ";
    private static final String OPENGL_OF_MODS = "OpenGL of mods, ";

    private DebugScreenLines() {}

    public static void appendTo(List<String> lines) {
        ClientRendering.running().ifPresent(renderer -> {
            lines.add(SECTION_BREAK);
            lines.addAll(renderer.diagnostics().lines());
            lines.add(LEFT_OUT_DRAWS + GameFrame.leftOutDraws());
        });
        ModRendering.running().ifPresent(mods -> {
            for (String line : mods.provider().lines()) {
                lines.add(OPENGL_OF_MODS + line);
            }
        });
    }
}
