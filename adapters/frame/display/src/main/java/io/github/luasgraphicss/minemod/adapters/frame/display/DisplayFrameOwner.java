package io.github.luasgraphicss.minemod.adapters.frame.display;

import io.github.luasgraphicss.minemod.adapters.frame.session.RecordedFrameRenderer;
import io.github.luasgraphicss.minemod.adapters.presentation.display.standin.AwtStandInWindow;
import io.github.luasgraphicss.minemod.adapters.presentation.lifecycle.GuardedPresentation;
import io.github.luasgraphicss.minemod.adapters.presentation.nativeframe.NativeDisplayPresentation;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwner;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwners;
import io.github.luasgraphicss.minemod.adapters.presentation.renderer.RendererPresenter;
import io.github.luasgraphicss.minemod.adapters.presentation.renderer.session.BundledRenderers;

public final class DisplayFrameOwner {
    private DisplayFrameOwner() {}

    public static PresentationOwner installFrom(ClassLoader adapter) {
        AwtStandInWindow window = new AwtStandInWindow();
        PresentationOwner owner = new GuardedPresentation(new NativeDisplayPresentation(
                new RendererPresenter(
                        BundledRenderers.inGameDirectory(adapter, RecordedFrameRenderer::presenting),
                        display -> window.surface()),
                window));
        PresentationOwners.install(owner);
        return owner;
    }
}
