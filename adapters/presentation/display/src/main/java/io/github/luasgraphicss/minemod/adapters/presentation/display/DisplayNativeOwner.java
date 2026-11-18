package io.github.luasgraphicss.minemod.adapters.presentation.display;

import io.github.luasgraphicss.minemod.adapters.presentation.display.standin.AwtStandInWindow;
import io.github.luasgraphicss.minemod.adapters.presentation.lifecycle.GuardedPresentation;
import io.github.luasgraphicss.minemod.adapters.presentation.nativeframe.NativeDisplayPresentation;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwner;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwners;
import io.github.luasgraphicss.minemod.adapters.presentation.renderer.RendererPresenter;
import io.github.luasgraphicss.minemod.adapters.presentation.renderer.WindowRenderers;
import io.github.luasgraphicss.minemod.adapters.presentation.renderer.session.BundledRenderers;

public final class DisplayNativeOwner {
    private DisplayNativeOwner() {}

    public static PresentationOwner installFrom(ClassLoader adapter) {
        return install(BundledRenderers.inGameDirectory(adapter));
    }

    static PresentationOwner install(WindowRenderers renderers) {
        AwtStandInWindow window = new AwtStandInWindow();
        PresentationOwner owner = new GuardedPresentation(new NativeDisplayPresentation(
                new RendererPresenter(renderers, display -> window.surface()), window));
        PresentationOwners.install(owner);
        return owner;
    }
}
