package io.github.luasgraphicss.minemod.compatibility.spi;

public interface RenderBridge {
    BridgeDescriptor descriptor();

    void attach(BridgeContext context);

    long serve(String operation, long[] arguments);

    void detach();
}
