package io.github.luasgraphicss.minemod.compatibility.bridges.session;

import io.github.luasgraphicss.minemod.compatibility.spi.SceneAttachments;
import java.nio.ByteBuffer;

public interface BridgeHost {
    SceneAttachments attachments();

    void submit(String bridgeId, ByteBuffer commandStream);
}
