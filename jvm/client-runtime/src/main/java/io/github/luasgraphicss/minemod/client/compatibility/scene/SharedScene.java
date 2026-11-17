package io.github.luasgraphicss.minemod.client.compatibility.scene;

import io.github.luasgraphicss.minemod.bridge.SceneSharing;
import io.github.luasgraphicss.minemod.client.compatibility.handoff.RecordedFrame;
import io.github.luasgraphicss.minemod.client.compatibility.handoff.SceneHandoff;
import io.github.luasgraphicss.minemod.client.compatibility.sharing.SceneImports;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.ScratchInts;
import io.github.luasgraphicss.minemod.compatibility.natives.memory.JniMemory;
import io.github.luasgraphicss.minemod.compatibility.runtime.scene.SceneSource;
import io.github.luasgraphicss.minemod.compatibility.runtime.scene.SceneTextures;
import io.github.luasgraphicss.minemod.compatibility.state.submission.AttachmentHandoff;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import java.util.Objects;
import java.util.Optional;

public final class SharedScene implements SceneSource, AutoCloseable {
    private final JniMemory memory = new JniMemory();
    private final SceneSharing sharing;
    private final RecordedFrame frame;
    private SceneImports imports;
    private SceneHandoff handoff;

    public SharedScene(SceneSharing sharing, RecordedFrame frame) {
        this.sharing = Objects.requireNonNull(sharing, "sharing");
        this.frame = Objects.requireNonNull(frame, "frame");
    }

    @Override
    public SceneTextures attach(ProviderCalls provider, ScratchInts scratch) {
        imports = new SceneImports(sharing, provider, memory);
        return imports.current().textures();
    }

    public AttachmentHandoff handoffThrough(ProviderCalls provider) {
        try {
            handoff = SceneHandoff.open(frame, sharing, provider, memory, imports.current());
        } catch (RuntimeException failure) {
            imports.close();
            throw failure;
        }
        return handoff;
    }

    public Optional<SceneTextures> followSession(long lastFrameOnTheReplaced) {
        if (!imports.followSession(lastFrameOnTheReplaced)) {
            return Optional.empty();
        }
        handoff.retarget(imports.current());
        return Optional.of(imports.current().textures());
    }

    public SceneImports imports() {
        return imports;
    }

    @Override
    public void close() {
        handoff.close();
        imports.close();
    }
}
