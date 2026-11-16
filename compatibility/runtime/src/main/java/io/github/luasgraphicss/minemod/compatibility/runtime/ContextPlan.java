package io.github.luasgraphicss.minemod.compatibility.runtime;

import io.github.luasgraphicss.minemod.compatibility.contracts.report.OriginResolver;
import io.github.luasgraphicss.minemod.compatibility.layer.state.TrackedCapability;
import io.github.luasgraphicss.minemod.compatibility.runtime.scene.SceneSource;
import io.github.luasgraphicss.minemod.compatibility.state.submission.AttachmentHandoff;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class ContextPlan {
    private final SceneSource scene;
    private final Function<ProviderCalls, AttachmentHandoff> handoff;
    private final OriginResolver origins;
    private final List<TrackedCapability> wrapperState;

    public ContextPlan(
            SceneSource scene,
            Function<ProviderCalls, AttachmentHandoff> handoff,
            OriginResolver origins,
            List<TrackedCapability> wrapperState) {
        this.scene = Objects.requireNonNull(scene, "scene");
        this.handoff = Objects.requireNonNull(handoff, "handoff");
        this.origins = Objects.requireNonNull(origins, "origins");
        this.wrapperState = Collections.unmodifiableList(new ArrayList<>(wrapperState));
    }

    public SceneSource scene() {
        return scene;
    }

    public Function<ProviderCalls, AttachmentHandoff> handoff() {
        return handoff;
    }

    public OriginResolver origins() {
        return origins;
    }

    public List<TrackedCapability> wrapperState() {
        return wrapperState;
    }
}
