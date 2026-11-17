package io.github.luasgraphicss.minemod.verification.differential.provider;

import io.github.luasgraphicss.minemod.compatibility.contexts.CompatibilityContext;
import io.github.luasgraphicss.minemod.compatibility.contexts.ContextRegistry;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeMemory;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.ScratchInts;
import io.github.luasgraphicss.minemod.compatibility.layer.scene.SceneFramebuffer;
import io.github.luasgraphicss.minemod.compatibility.runtime.scene.SceneSource;
import io.github.luasgraphicss.minemod.compatibility.runtime.scene.SceneTextures;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;

final class ReferenceContext implements GlCalls, AutoCloseable {
    private static final String BIND_FRAMEBUFFER = "glBindFramebuffer";
    private static final long DEFAULT_FRAMEBUFFER = 0;

    private final ContextRegistry contexts;
    private final CompatibilityContext context;
    private final ProviderCalls provider;
    private final int sceneFramebuffer;

    private ReferenceContext(
            ContextRegistry contexts, CompatibilityContext context, ProviderCalls provider, int sceneFramebuffer) {
        this.contexts = contexts;
        this.context = context;
        this.provider = provider;
        this.sceneFramebuffer = sceneFramebuffer;
    }

    static ReferenceContext open(
            ContextRegistry contexts, ProviderCalls provider, NativeMemory memory, SceneSource scene) {
        CompatibilityContext context = contexts.create();
        contexts.makeCurrent(context);
        try (ScratchInts scratch = new ScratchInts(memory, 1)) {
            SceneTextures textures = scene.attach(provider, scratch);
            int framebuffer = SceneFramebuffer.assemble(provider, scratch, textures.color(), textures.depthStencil());
            return new ReferenceContext(contexts, context, provider, framebuffer);
        } catch (RuntimeException failure) {
            contexts.destroy(context);
            throw failure;
        }
    }

    @Override
    public long call(String function, long... words) {
        if (function.equals(BIND_FRAMEBUFFER) && words[1] == DEFAULT_FRAMEBUFFER) {
            return provider.invoke(function, new long[] {words[0], sceneFramebuffer});
        }
        return provider.invoke(function, words);
    }

    @Override
    public void close() {
        contexts.destroy(context);
    }
}
