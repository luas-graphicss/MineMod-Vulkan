package io.github.luasgraphicss.minemod.compatibility.layer.sharing;

public interface RendererHandoff {
    void submitWorkAndSignal();

    void waitAndReacquire();
}
