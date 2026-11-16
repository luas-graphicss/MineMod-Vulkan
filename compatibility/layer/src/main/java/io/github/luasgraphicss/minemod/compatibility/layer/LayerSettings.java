package io.github.luasgraphicss.minemod.compatibility.layer;

import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallCatalog;
import io.github.luasgraphicss.minemod.compatibility.layer.state.TrackedCapability;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public final class LayerSettings {
    private final CallCatalog catalog;
    private final ContextProfile profile;
    private final Set<String> exposedExtensions;
    private final List<TrackedCapability> wrapperState;
    private final int sceneFramebuffer;

    public LayerSettings(
            CallCatalog catalog,
            ContextProfile profile,
            Collection<String> exposedExtensions,
            List<TrackedCapability> wrapperState,
            int sceneFramebuffer) {
        if (sceneFramebuffer == 0) {
            throw new IllegalArgumentException("the scene framebuffer is a provider object, never the provider's default");
        }
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.profile = Objects.requireNonNull(profile, "profile");
        this.exposedExtensions = Collections.unmodifiableSet(new TreeSet<>(exposedExtensions));
        this.wrapperState = Collections.unmodifiableList(new ArrayList<>(wrapperState));
        this.sceneFramebuffer = sceneFramebuffer;
    }

    public CallCatalog catalog() {
        return catalog;
    }

    public ContextProfile profile() {
        return profile;
    }

    public Set<String> exposedExtensions() {
        return exposedExtensions;
    }

    public List<TrackedCapability> wrapperState() {
        return wrapperState;
    }

    public int sceneFramebuffer() {
        return sceneFramebuffer;
    }
}
