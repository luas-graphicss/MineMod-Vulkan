package io.github.luasgraphicss.minemod.compatibility.interop.handoff;

public interface SharingBackend {
    void submitRendererWorkAndSignal();

    void waitInProvider();

    void signalAndFlushProvider();

    void waitAndReacquireInRenderer();

    void finishProvider();
}
