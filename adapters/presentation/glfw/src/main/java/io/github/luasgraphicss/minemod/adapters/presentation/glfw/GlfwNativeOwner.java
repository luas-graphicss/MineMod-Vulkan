package io.github.luasgraphicss.minemod.adapters.presentation.glfw;

import io.github.luasgraphicss.minemod.adapters.presentation.glfw.surface.GlfwWindowSurface;
import io.github.luasgraphicss.minemod.adapters.presentation.lifecycle.GuardedPresentation;
import io.github.luasgraphicss.minemod.adapters.presentation.nativeframe.NativePresentation;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwner;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwners;
import io.github.luasgraphicss.minemod.adapters.presentation.renderer.RendererPresenter;
import io.github.luasgraphicss.minemod.adapters.presentation.renderer.WindowRenderers;
import io.github.luasgraphicss.minemod.adapters.presentation.renderer.session.BundledRenderers;

public final class GlfwNativeOwner {
    private GlfwNativeOwner() {}

    public static PresentationOwner installFrom(ClassLoader adapter) {
        return install(BundledRenderers.inGameDirectory(adapter));
    }

    static PresentationOwner install(WindowRenderers renderers) {
        PresentationOwner owner = new GuardedPresentation(new NativePresentation(
                new RendererPresenter(renderers, window -> new GlfwWindowSurface(window))));
        PresentationOwners.install(owner);
        return owner;
    }
}
