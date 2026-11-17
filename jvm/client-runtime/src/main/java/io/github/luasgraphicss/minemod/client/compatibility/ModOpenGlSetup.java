package io.github.luasgraphicss.minemod.client.compatibility;

import io.github.luasgraphicss.minemod.compatibility.contracts.report.OriginResolver;
import io.github.luasgraphicss.minemod.compatibility.layer.state.TrackedCapability;
import io.github.luasgraphicss.minemod.compatibility.runtime.provider.ProviderBundle;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ModOpenGlSetup {
    private final Path providerNatives;
    private final ProviderBundle bundle;
    private final ContextRequest context;
    private final OriginResolver origins;
    private final List<TrackedCapability> wrapperState;

    public ModOpenGlSetup(
            Path providerNatives,
            ProviderBundle bundle,
            ContextRequest context,
            OriginResolver origins,
            List<TrackedCapability> wrapperState) {
        this.providerNatives = Objects.requireNonNull(providerNatives, "providerNatives");
        this.bundle = Objects.requireNonNull(bundle, "bundle");
        this.context = Objects.requireNonNull(context, "context");
        this.origins = Objects.requireNonNull(origins, "origins");
        this.wrapperState = Collections.unmodifiableList(new ArrayList<>(wrapperState));
    }

    public Path providerNatives() {
        return providerNatives;
    }

    public ProviderBundle bundle() {
        return bundle;
    }

    public ContextRequest context() {
        return context;
    }

    public OriginResolver origins() {
        return origins;
    }

    public List<TrackedCapability> wrapperState() {
        return wrapperState;
    }
}
