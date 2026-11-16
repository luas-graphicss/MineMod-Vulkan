package io.github.luasgraphicss.minemod.compatibility.spi;

import java.nio.ByteBuffer;

public interface BridgeContext {
    SceneAttachments attachments();

    void submit(ByteBuffer commandStream);
}
