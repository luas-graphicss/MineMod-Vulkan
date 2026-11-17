package io.github.luasgraphicss.minemod.adapters.presentation.renderer;

import io.github.luasgraphicss.minemod.adapters.presentation.nativeframe.FramePresenter;
import io.github.luasgraphicss.minemod.bridge.PresentPacing;
import java.util.Objects;

public final class RendererPresenter implements FramePresenter {
    private static final Object NO_OPENGL_CAPABILITIES = null;
    private final WindowRenderers renderers;
    private final WindowSurfaces surfaces;
    private WindowRenderer renderer;

    public RendererPresenter(WindowRenderers renderers, WindowSurfaces surfaces) {
        this.renderers = Objects.requireNonNull(renderers, "renderers");
        this.surfaces = Objects.requireNonNull(surfaces, "surfaces");
    }

    @Override
    public void start(long window) {
        if (renderer != null) {
            throw new IllegalStateException("the native renderer already presents a window");
        }
        renderer = renderers.start(surfaces.of(window));
    }

    @Override
    public Object capabilities() {
        return NO_OPENGL_CAPABILITIES;
    }

    @Override
    public void present() {
        running().presentFrame();
    }

    @Override
    public void pace(boolean synchronised) {
        running().pace(synchronised ? PresentPacing.VSYNC : PresentPacing.IMMEDIATE);
    }

    @Override
    public void stop() {
        WindowRenderer stopped = running();
        renderer = null;
        stopped.close();
    }

    private WindowRenderer running() {
        if (renderer == null) {
            throw new IllegalStateException("the native renderer presents no window");
        }
        return renderer;
    }
}
