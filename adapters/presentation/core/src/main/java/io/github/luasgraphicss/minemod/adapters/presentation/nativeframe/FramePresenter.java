package io.github.luasgraphicss.minemod.adapters.presentation.nativeframe;

public interface FramePresenter {
    void start(long window);

    Object capabilities();

    void present();

    void pace(boolean synchronised);

    void stop();
}
