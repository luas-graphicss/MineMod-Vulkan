package io.github.luasgraphicss.minemod.adapters.frame.glfw;

import io.github.luasgraphicss.minemod.adapters.frame.session.RecordedFrameRenderer;
import io.github.luasgraphicss.minemod.adapters.presentation.glfw.surface.GlfwWindowSurface;
import io.github.luasgraphicss.minemod.adapters.presentation.lifecycle.GuardedPresentation;
import io.github.luasgraphicss.minemod.adapters.presentation.nativeframe.NativePresentation;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwner;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwners;
import io.github.luasgraphicss.minemod.adapters.presentation.renderer.RendererPresenter;
import io.github.luasgraphicss.minemod.adapters.presentation.renderer.session.BundledRenderers;

public final class GlfwFrameOwner {
    private GlfwFrameOwner() {}

    public static PresentationOwner installFrom(ClassLoader adapter) {
        PresentationOwner owner = new GuardedPresentation(new NativePresentation(new RendererPresenter(
                BundledRenderers.inGameDirectory(adapter, RecordedFrameRenderer::presenting),
                window -> new GlfwWindowSurface(window))));
        PresentationOwners.install(owner);
        return owner;
    }
}
