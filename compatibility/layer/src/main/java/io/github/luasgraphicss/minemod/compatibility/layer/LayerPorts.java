package io.github.luasgraphicss.minemod.compatibility.layer;

import io.github.luasgraphicss.minemod.compatibility.contracts.report.OriginResolver;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeMemory;
import io.github.luasgraphicss.minemod.compatibility.resources.names.ContextNames;
import io.github.luasgraphicss.minemod.compatibility.state.submission.AttachmentHandoff;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import java.util.Objects;

public final class LayerPorts {
    private final ProviderCalls provider;
    private final NativeMemory memory;
    private final ContextNames names;
    private final AttachmentHandoff handoff;
    private final OriginResolver origins;

    public LayerPorts(
            ProviderCalls provider,
            NativeMemory memory,
            ContextNames names,
            AttachmentHandoff handoff,
            OriginResolver origins) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.memory = Objects.requireNonNull(memory, "memory");
        this.names = Objects.requireNonNull(names, "names");
        this.handoff = Objects.requireNonNull(handoff, "handoff");
        this.origins = Objects.requireNonNull(origins, "origins");
    }

    public ProviderCalls provider() {
        return provider;
    }

    public NativeMemory memory() {
        return memory;
    }

    public ContextNames names() {
        return names;
    }

    public AttachmentHandoff handoff() {
        return handoff;
    }

    public OriginResolver origins() {
        return origins;
    }
}
