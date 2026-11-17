package io.github.luasgraphicss.minemod.verification.differential.provider;

import io.github.luasgraphicss.minemod.compatibility.layer.CompatibilityLayer;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeMemory;
import io.github.luasgraphicss.minemod.compatibility.natives.memory.JniMemory;
import io.github.luasgraphicss.minemod.compatibility.provider.identity.ProviderIdentity;
import io.github.luasgraphicss.minemod.compatibility.runtime.CompatibilitySession;
import io.github.luasgraphicss.minemod.compatibility.runtime.ContextPlan;
import io.github.luasgraphicss.minemod.compatibility.runtime.SessionContext;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.OwnedSceneTextures;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.ProviderTestEnvironment;
import io.github.luasgraphicss.minemod.verification.differential.Divergence;
import io.github.luasgraphicss.minemod.verification.differential.ScriptComparison;
import io.github.luasgraphicss.minemod.verification.differential.ScriptPlayer;
import io.github.luasgraphicss.minemod.verification.differential.ScriptStep;
import java.util.List;
import java.util.Optional;

public final class ProviderComparison implements ScriptComparison {
    private static final int WIDTH = ProviderTestEnvironment.SCENE_WIDTH;
    private static final int HEIGHT = ProviderTestEnvironment.SCENE_HEIGHT;
    private static final long GL_DEPTH_BUFFER_BIT = 0x0100;
    private static final long GL_STENCIL_BUFFER_BIT = 0x0400;
    private static final long GL_COLOR_BUFFER_BIT = 0x4000;

    private final CompatibilitySession session;
    private final NativeMemory memory = new JniMemory();
    private final AttachedScene scene = new AttachedScene(new OwnedSceneTextures(WIDTH, HEIGHT));
    private final ContextPlan plan = ProviderTestEnvironment.planFor(scene);

    ProviderComparison(CompatibilitySession session) {
        this.session = session;
    }

    @Override
    public String provider() {
        ProviderIdentity identity = session.diagnosis().identity();
        return "Mesa " + identity.mesaVersion().orElse("of an unreported release") + ", "
                + identity.driverKind().description() + " on " + identity.deviceName();
    }

    @Override
    public Optional<String> firstDifference(List<ScriptStep> script) {
        List<String> layerObservations = playThroughTheLayer(script);
        List<String> referenceObservations = playOnTheProvider(script);
        return Divergence.first("observation", referenceObservations, layerObservations);
    }

    @Override
    public void close() {
        session.close();
    }

    List<String> playThroughTheLayer(List<ScriptStep> script) {
        try (SessionContext context = session.createContext(plan)) {
            CompatibilityLayer layer = context.layer();
            layer.beginFrame();
            layer.enterCallback();
            List<String> observations = play(script, layer::call);
            layer.exitCallback();
            layer.present();
            return observations;
        }
    }

    List<String> playOnTheProvider(List<ScriptStep> script) {
        try (ReferenceContext reference = ReferenceContext.open(
                session.contexts(), scene.provider(), memory, new OwnedSceneTextures(WIDTH, HEIGHT))) {
            return play(script, reference);
        }
    }

    private List<String> play(List<ScriptStep> script, GlCalls gl) {
        gl.call("glViewport", 0, 0, WIDTH, HEIGHT);
        gl.call("glClear", GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
        try (ProviderTarget target = new ProviderTarget(gl, memory)) {
            return new ScriptPlayer(target).play(script);
        }
    }
}
