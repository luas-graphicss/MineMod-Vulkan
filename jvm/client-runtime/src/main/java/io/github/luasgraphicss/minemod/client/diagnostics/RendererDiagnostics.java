package io.github.luasgraphicss.minemod.client.diagnostics;

import io.github.luasgraphicss.minemod.bridge.SceneDiagnostics;
import io.github.luasgraphicss.minemod.client.reload.ResourceReload;
import java.util.List;

public final class RendererDiagnostics {
    private final SceneDiagnostics scene;
    private final ResourceReload reload;

    public RendererDiagnostics(SceneDiagnostics scene, ResourceReload reload) {
        this.scene = scene;
        this.reload = reload;
    }

    public List<String> lines() {
        return DiagnosticLines.describe(scene.metrics(), reload.lastOutcome());
    }

    public String report() {
        String refusal = reload.lastOutcome().report();
        return refusal.isEmpty() ? scene.report() : scene.report() + "reload refused:\n" + refusal + "\n";
    }
}
