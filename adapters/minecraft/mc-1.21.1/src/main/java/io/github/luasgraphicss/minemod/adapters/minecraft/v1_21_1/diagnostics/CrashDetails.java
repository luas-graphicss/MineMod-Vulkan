package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.diagnostics;

import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.compatibility.ModRendering;
import io.github.luasgraphicss.minemod.client.diagnostics.RendererDiagnostics;
import net.minecraft.CrashReport;

public final class CrashDetails {
    private static final String CATEGORY = "MineMod Vulkan";
    private static final String RENDERER_DETAIL = "Renderer";
    private static final String PROVIDER_DETAIL = "OpenGL of mods";
    private static final String LINE_BREAK = "\n";
    private static final String DETAIL_LINE_BREAK = "\n\t\t";

    private CrashDetails() {}

    public static void describeIn(CrashReport crash) {
        ClientRendering.running()
                .ifPresent(renderer -> crash.addCategory(CATEGORY)
                        .setDetail(RENDERER_DETAIL, () -> indented(renderer.diagnostics())));
        ModRendering.running()
                .ifPresent(mods -> crash.addCategory(CATEGORY)
                        .setDetail(PROVIDER_DETAIL, () -> String.join(DETAIL_LINE_BREAK, mods.provider().lines())));
    }

    private static String indented(RendererDiagnostics diagnostics) {
        return diagnostics.report().trim().replace(LINE_BREAK, DETAIL_LINE_BREAK);
    }
}
