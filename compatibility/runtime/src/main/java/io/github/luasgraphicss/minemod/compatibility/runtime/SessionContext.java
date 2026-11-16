package io.github.luasgraphicss.minemod.compatibility.runtime;

import io.github.luasgraphicss.minemod.compatibility.contexts.CompatibilityContext;
import io.github.luasgraphicss.minemod.compatibility.contexts.ContextRegistry;
import io.github.luasgraphicss.minemod.compatibility.layer.CompatibilityLayer;
import io.github.luasgraphicss.minemod.compatibility.natives.gate.CallGate;
import io.github.luasgraphicss.minemod.compatibility.runtime.scene.SceneTextures;

public final class SessionContext implements AutoCloseable {
    private final ContextRegistry contexts;
    private final CompatibilityContext context;
    private final CompatibilityLayer layer;
    private final SceneTarget scene;
    private boolean closed;

    SessionContext(
            ContextRegistry contexts, CompatibilityContext context, CompatibilityLayer layer, SceneTarget scene) {
        this.contexts = contexts;
        this.context = context;
        this.layer = layer;
        this.scene = scene;
    }

    public CompatibilityLayer layer() {
        return layer;
    }

    public CompatibilityContext context() {
        return context;
    }

    public void makeCurrent() {
        contexts.makeCurrent(context);
        CallGate.bind(layer::call);
    }

    public void attachScene(SceneTextures textures) {
        if (!contexts.current().filter(context::equals).isPresent()) {
            throw new IllegalStateException("the scene of a context is replaced while that context is current");
        }
        scene.attach(textures);
    }

    public void release() {
        CallGate.unbind();
        contexts.releaseCurrent();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        layer.close();
        if (contexts.current().filter(context::equals).isPresent()) {
            CallGate.unbind();
        }
        contexts.destroy(context);
    }
}
